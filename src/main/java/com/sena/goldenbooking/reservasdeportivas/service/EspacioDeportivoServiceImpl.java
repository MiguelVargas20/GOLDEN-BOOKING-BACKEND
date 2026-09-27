package com.sena.goldenbooking.reservasdeportivas.service;

import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.compartido.imagenes.AlmacenImagenes;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.dto.EspacioDeportivoDto;
import com.sena.goldenbooking.reservasdeportivas.mapper.EspacioDeportivoMapper;
import com.sena.goldenbooking.reservasdeportivas.model.EspacioDeportivo;
import com.sena.goldenbooking.reservasdeportivas.model.EstadoEspacio;
import com.sena.goldenbooking.reservasdeportivas.repository.EspacioDeportivoRepository;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EspacioDeportivoServiceImpl implements EspacioDeportivoService {

    /** Tamaño máximo de imagen (también limitado en spring.servlet.multipart.*). */
    private static final Sort ORDEN_CATALOGO = Sort.by("deporte").ascending().and(Sort.by("nombre").ascending());

    private final EspacioDeportivoRepository repo;
    private final ReservaDeporteRepository reservaDeporteRepo;
    private final EspacioDeportivoMapper mapper;
    private final AlmacenImagenes imagenes;

    public EspacioDeportivoServiceImpl(EspacioDeportivoRepository repo,
                                       ReservaDeporteRepository reservaDeporteRepo,
                                       EspacioDeportivoMapper mapper,
                                       AlmacenImagenes imagenes) {
        this.repo = repo;
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.mapper = mapper;
        this.imagenes = imagenes;
    }

    // ── Consultas ──────────────────────────────────────────────────────────

    @Override
    public List<EspacioDeportivoDto> listar(boolean incluirInactivos) {
        List<EspacioDeportivo> espacios = incluirInactivos
                ? repo.findAll(ORDEN_CATALOGO)
                : repo.findByEstadoIn(EnumSet.of(EstadoEspacio.ACTIVO, EstadoEspacio.MANTENIMIENTO), ORDEN_CATALOGO);
        return mapper.toDtoList(espacios);
    }

    @Override
    public EspacioDeportivoDto obtenerPorId(String id) {
        return mapper.toDto(buscar(id));
    }

    @Override
    public EspacioDeportivo obtenerReservable(String id) {
        if (id == null || id.isBlank()) {
            throw new SolicitudInvalidaException("Debes seleccionar el espacio deportivo a reservar.");
        }
        EspacioDeportivo espacio = buscar(id);
        if (espacio.getEstado() == EstadoEspacio.MANTENIMIENTO) {
            throw new ConflictoDeNegocioException("Este espacio está en mantenimiento y no se puede reservar por ahora.");
        }
        if (espacio.getEstado() != EstadoEspacio.ACTIVO) {
            throw new ConflictoDeNegocioException("Este espacio no está disponible para reservas.");
        }
        return espacio;
    }

    // ── Administración ─────────────────────────────────────────────────────

    @Override
    public EspacioDeportivoDto crear(EspacioDeportivoDto dto) {
        validarHorario(dto);
        EspacioDeportivo espacio = mapper.toEntity(dto);
        if (repo.existsByNombreIgnoreCase(espacio.getNombre())) {
            throw new ConflictoDeNegocioException("Ya existe un espacio deportivo con ese nombre.");
        }
        if (espacio.getEstado() == null) {
            espacio.setEstado(EstadoEspacio.ACTIVO);
        }
        espacio.setFechaCreacion(ZonaHoraria.ahora());
        espacio.setFechaActualizacion(espacio.getFechaCreacion());

        EspacioDeportivo guardado = repo.save(espacio);
        log.info("Espacio deportivo creado: {} ({})", guardado.getNombre(), guardado.getId());
        return mapper.toDto(guardado);
    }

    @Override
    public EspacioDeportivoDto actualizar(String id, EspacioDeportivoDto dto) {
        validarHorario(dto);
        EspacioDeportivo espacio = buscar(id);
        String nombreNuevo = dto.getNombre() == null ? null : dto.getNombre().trim().replaceAll("\\s+", " ");
        if (repo.existsByNombreIgnoreCaseAndIdNot(nombreNuevo, id)) {
            throw new ConflictoDeNegocioException("Ya existe otro espacio deportivo con ese nombre.");
        }
        mapper.actualizar(dto, espacio);
        espacio.setFechaActualizacion(ZonaHoraria.ahora());

        log.info("Espacio deportivo actualizado: {} ({})", espacio.getNombre(), id);
        return mapper.toDto(repo.save(espacio));
    }

    @Override
    public EspacioDeportivoDto cambiarEstado(String id, EstadoEspacio estado) {
        if (estado == null) {
            throw new SolicitudInvalidaException("Debes indicar el nuevo estado del espacio.");
        }
        EspacioDeportivo espacio = buscar(id);
        espacio.setEstado(estado);
        espacio.setFechaActualizacion(ZonaHoraria.ahora());
        log.info("Espacio deportivo {} cambió a estado {}", id, estado);
        // Las reservas ya existentes no se tocan: el admin decide si las cancela
        // desde la gestión de reservas (así puede avisar a cada cliente con un motivo).
        return mapper.toDto(repo.save(espacio));
    }

    @Override
    public void eliminar(String id) {
        EspacioDeportivo espacio = buscar(id);

        if (reservaDeporteRepo.existsByEspacioIdAndEstadoNotAndFechaFinReservaAfter(
                id, EstadoReserva.CANCELADA, ZonaHoraria.ahora())) {
            throw new ConflictoDeNegocioException(
                    "No se puede eliminar: el espacio tiene reservas vigentes. Cancélalas primero o márcalo como INACTIVO.");
        }
        if (reservaDeporteRepo.existsByEspacioId(id)) {
            // Borrarlo dejaría reservas pasadas apuntando a un espacio inexistente
            throw new ConflictoDeNegocioException(
                    "No se puede eliminar: el espacio tiene historial de reservas. Márcalo como INACTIVO para ocultarlo.");
        }

        imagenes.borrar(espacio.getImagenId());
        repo.delete(espacio);
        log.info("Espacio deportivo eliminado: {} ({})", espacio.getNombre(), id);
    }

    // ── Imágenes (GridFS) ──────────────────────────────────────────────────

    @Override
    public EspacioDeportivoDto subirImagen(String id, MultipartFile archivo) {
        EspacioDeportivo espacio = buscar(id);
        String nuevoId = imagenes.guardar(archivo, "espacio-" + id);
        String anterior = espacio.getImagenId();

        espacio.setImagenId(nuevoId);
        espacio.setFechaActualizacion(ZonaHoraria.ahora());
        EspacioDeportivo guardado = repo.save(espacio);

        imagenes.borrar(anterior); // se borra la vieja solo cuando la nueva ya quedó guardada
        return mapper.toDto(guardado);
    }

    @Override
    public EspacioDeportivoDto eliminarImagen(String id) {
        EspacioDeportivo espacio = buscar(id);
        imagenes.borrar(espacio.getImagenId());
        espacio.setImagenId(null);
        espacio.setFechaActualizacion(ZonaHoraria.ahora());
        return mapper.toDto(repo.save(espacio));
    }

    @Override
    public GridFsResource obtenerImagen(String id) {
        EspacioDeportivo espacio = buscar(id);
        if (espacio.getImagenId() == null) {
            throw new RecursoNoEncontradoException("Este espacio no tiene imagen propia.");
        }
        return imagenes.obtener(espacio.getImagenId());
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private EspacioDeportivo buscar(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El espacio deportivo no existe."));
    }

    private void validarHorario(EspacioDeportivoDto dto) {
        if (dto.getHoraApertura() != null && dto.getHoraCierre() != null
                && !dto.getHoraCierre().isAfter(dto.getHoraApertura())) {
            throw new SolicitudInvalidaException("La hora de cierre debe ser posterior a la hora de apertura.");
        }
    }

}
