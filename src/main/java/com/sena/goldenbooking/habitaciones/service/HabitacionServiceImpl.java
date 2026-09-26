package com.sena.goldenbooking.habitaciones.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.compartido.imagenes.AlmacenImagenes;
import com.sena.goldenbooking.habitaciones.dto.HabitacionDto;
import com.sena.goldenbooking.habitaciones.mapper.HabitacionMapper;
import com.sena.goldenbooking.habitaciones.model.EstadoHabitacion;
import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.habitaciones.repository.HabitacionRepository;
import com.sena.goldenbooking.habitaciones.repository.TipoHabitacionRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HabitacionServiceImpl implements HabitacionService {

    // Inyectamos el repositorio y el mapper a través del constructor
    private final HabitacionRepository habRepo;
    private final HabitacionMapper habMapper;

    // Constructor para inyección de dependencias
    private final AlmacenImagenes imagenes;

    private final TipoHabitacionRepository tipoRepo;

    public HabitacionServiceImpl(HabitacionRepository habRepo, HabitacionMapper habMapper, AlmacenImagenes imagenes,
                                 TipoHabitacionRepository tipoRepo) {
        this.habRepo = habRepo;
        this.habMapper = habMapper;
        this.imagenes = imagenes;
        this.tipoRepo = tipoRepo;
    }

    /**
     * Valida los datos y toma el tipo de habitación de la base de datos por su
     * id. Antes se guardaba el objeto que mandaba el navegador tal cual: si
     * llegaba con otros nombres de campo (nombreTipoHabitacion en vez de
     * nomTipo) el tipo quedaba vacío y el catálogo mostraba "—".
     */
    private void validarYResolverTipo(HabitacionDto dto, String idActual) {
        if (dto.getNumeroHabitacion() == null || dto.getNumeroHabitacion().isBlank()) {
            throw new SolicitudInvalidaException("El número de habitación es obligatorio.");
        }
        dto.setNumeroHabitacion(dto.getNumeroHabitacion().trim());
        if (dto.getPrecioNoche() == null || dto.getPrecioNoche() <= 0) {
            throw new SolicitudInvalidaException("El precio por noche debe ser mayor a cero.");
        }
        if (dto.getEstadoHabitacion() == null) {
            dto.setEstadoHabitacion(EstadoHabitacion.DISPONIBLE);
        }
        boolean repetido = habRepo.findAllByNumHab(dto.getNumeroHabitacion()).stream()
                .anyMatch(otra -> !otra.getId().equals(idActual));
        if (repetido) {
            throw new ConflictoDeNegocioException("Ya existe una habitación con el número " + dto.getNumeroHabitacion() + ".");
        }

        String idTipo = dto.getDatosTipoHabitacion() != null ? dto.getDatosTipoHabitacion().getId() : null;
        if (idTipo == null || idTipo.isBlank()) {
            throw new SolicitudInvalidaException("Selecciona un tipo de habitación.");
        }
        dto.setDatosTipoHabitacion(tipoRepo.findById(idTipo)
                .orElseThrow(() -> new SolicitudInvalidaException("El tipo de habitación elegido no existe.")));
    }

    /* Implementación de los métodos definidos en la interfaz HabitacionService */
    @Override
    public HabitacionDto crear(HabitacionDto dto) {
        log.info("Creando habitación: {}", dto.getNumeroHabitacion());

        validarYResolverTipo(dto, null);
        Habitacion habitacion = habMapper.toHabitacion(dto);
        habitacion.setId(null); // lo asigna Mongo

        // Guardamos en MongoDB y mapeamos el resultado
        HabitacionDto resultado = habMapper.toDto(habRepo.save(habitacion));
        
        log.info("Habitación {} creada con ID: {}", dto.getNumeroHabitacion(), resultado.getId());
        return resultado;
    }

    /* Método para listar todas las habitaciones, devolviendo una lista de DTOs */
    @Override
    public List<HabitacionDto> listarTodas() {
        List<HabitacionDto> lista = habMapper.toDtoList(habRepo.findAll());
        log.info("Listado de habitaciones solicitado. Total: {}", lista.size());
        return lista;
    }

    /* Método para listar habitaciones por estado, devolviendo una lista de DTOs */
    @Override
    public List<HabitacionDto> listarPorEstado(EstadoHabitacion estado) {
        List<HabitacionDto> lista = habMapper.toDtoList(habRepo.findByEstado(estado));
        log.info("Habitaciones en estado '{}': {}", estado, lista.size());
        return lista;
    }

    /* Método para listar habitaciones por tipo, devolviendo una lista de DTOs */
    @Override
    public List<HabitacionDto> listarPorTipo(String idTipoHabitacion) {
        List<HabitacionDto> lista = habMapper.toDtoList(habRepo.findByTipoHabitacion_Id(idTipoHabitacion));
        log.info("Habitaciones solicitadas por tipo de habitación ID '{}'. Total: {}", idTipoHabitacion, lista.size());
        return lista;
    }

    /* Método para obtener una habitación por su ID, devolviendo un DTO */
    @Override
    public HabitacionDto obtenerPorId(String id) {
        return habRepo.findById(id)
                .map(hab -> {
                    log.info("Habitación encontrada con ID: {}", id);
                    return habMapper.toDto(hab);
                })
                .orElseThrow(() -> {
                    log.warn("Habitación no encontrada con ID: {}", id);
                    return new RecursoNoEncontradoException("Habitación no encontrada con ID: " + id);
                });
    }

    /* Método para actualizar una habitación existente a partir de un DTO, devolviendo el DTO actualizado */
    @Override
    public HabitacionDto actualizar(String id, HabitacionDto dto) {
        log.info("Actualizando habitación con ID: {}", id);
        Habitacion habExistente = habRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Actualización fallida: habitación con ID {} no encontrada.", id);
                    return new RecursoNoEncontradoException("No se puede actualizar, ID no encontrado: " + id);
                });
        
        validarYResolverTipo(dto, id);
        habMapper.actualizarHabitacion(dto, habExistente); // la imagen (imagenId) no se toca aquí
        
        HabitacionDto resultado = habMapper.toDto(habRepo.save(habExistente));
        log.info("Habitación con ID: {} actualizada correctamente.", id);
        return resultado;
    }

    /* Método para cambiar el estado de una habitación, devolviendo el DTO actualizado */
    @Override
    public HabitacionDto cambiarEstado(String id, EstadoHabitacion nuevoEstado) {
        log.info("Cambiando estado de habitación ID: {} a '{}'", id, nuevoEstado);
        Habitacion hab = habRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cambio de estado fallido: habitación ID {} no encontrada.", id);
                    return new RecursoNoEncontradoException("Habitación no encontrada: " + id);
                });
        
        hab.setEstado(nuevoEstado);
        HabitacionDto resultado = habMapper.toDto(habRepo.save(hab));
        log.info("Estado de habitación ID: {} cambiado a '{}' correctamente.", id, nuevoEstado);
        return resultado;
    }

    /* Método para eliminar una habitación por su ID */
    @Override
    public void eliminar(String id) {
        log.info("Eliminando habitación con ID: {}", id);
        Habitacion hab = habRepo.findById(id).orElseThrow(() -> {
            log.warn("Eliminación fallida: habitación con ID {} no encontrada.", id);
            return new RecursoNoEncontradoException("No se puede eliminar, ID no existe: " + id);
        });
        habRepo.deleteById(id);
        imagenes.borrar(hab.getImagenId());
        log.info("Habitación con ID: {} eliminada correctamente.", id);
    }

    // Método para listar todas las habitaciones con paginación, devolviendo una página de DTOs
    @Override
    public Page<HabitacionDto> listarTodasPaginadas(Pageable pageable) {
        log.info("Listado paginado de habitaciones. Página: {}", pageable.getPageNumber());
        return habRepo.findAll(pageable).map(habMapper::toDto);
    }

    // ── Imagen (GridFS, mismas reglas que los espacios deportivos) ─────────

    @Override
    public HabitacionDto subirImagen(String id, MultipartFile archivo) {
        Habitacion hab = buscar(id);
        String nuevoId = imagenes.guardar(archivo, "habitacion-" + id);
        String anterior = hab.getImagenId();
        hab.setImagenId(nuevoId);
        Habitacion guardada = habRepo.save(hab);
        imagenes.borrar(anterior); // la vieja se borra solo cuando la nueva ya quedó guardada
        return habMapper.toDto(guardada);
    }

    @Override
    public HabitacionDto eliminarImagen(String id) {
        Habitacion hab = buscar(id);
        imagenes.borrar(hab.getImagenId());
        hab.setImagenId(null);
        return habMapper.toDto(habRepo.save(hab));
    }

    @Override
    public GridFsResource obtenerImagen(String id) {
        Habitacion hab = buscar(id);
        if (hab.getImagenId() == null) {
            throw new RecursoNoEncontradoException("Esta habitación no tiene imagen propia.");
        }
        return imagenes.obtener(hab.getImagenId());
    }

    private Habitacion buscar(String id) {
        return habRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La habitación no existe."));
    }
}
