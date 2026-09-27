package com.sena.goldenbooking.calificaciones.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.calificaciones.dto.CalificacionDto;
import com.sena.goldenbooking.calificaciones.dto.ResumenCalificacionDto;
import com.sena.goldenbooking.calificaciones.model.Calificacion;
import com.sena.goldenbooking.calificaciones.repository.CalificacionRepository;
import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.exception.AccesoDenegadoException;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.ReservaNoEncontradaException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.reservas.model.AccionReserva;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.EventoReserva;
import com.sena.goldenbooking.reservas.model.TipoReserva;
import com.sena.goldenbooking.reservas.service.HistorialReserva;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

import lombok.extern.slf4j.Slf4j;

/**
 * Calificaciones de espacios y habitaciones. El cliente califica (1 a 5
 * estrellas y un comentario opcional) una reserva suya que ya FINALIZÓ, una
 * sola vez. El promedio se muestra en los catálogos.
 */
@Slf4j
@Service
public class CalificacionService {

    /** Cuántas opiniones se muestran en el detalle de un espacio o habitación. */
    static final int MAXIMO_OPINIONES = 20;

    private final CalificacionRepository repo;
    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaHotelRepository reservaHotelRepo;
    private final UsuarioService usuarioService;

    public CalificacionService(CalificacionRepository repo, ReservaDeporteRepository reservaDeporteRepo,
                               ReservaHotelRepository reservaHotelRepo, UsuarioService usuarioService) {
        this.repo = repo;
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaHotelRepo = reservaHotelRepo;
        this.usuarioService = usuarioService;
    }

    public CalificacionDto calificar(CalificacionDto dto, String docUsuario) {
        if (dto.getPuntuacion() < 1 || dto.getPuntuacion() > 5) {
            throw new SolicitudInvalidaException("La calificación va de 1 a 5 estrellas.");
        }
        if (repo.existsByIdReserva(dto.getIdReserva())) {
            throw new ConflictoDeNegocioException("Ya calificaste esta reserva.");
        }
        String comentario = dto.getComentario() == null || dto.getComentario().isBlank() ? null : dto.getComentario().trim();
        EventoReserva evento = HistorialReserva.evento(AccionReserva.CALIFICADA,
                dto.getPuntuacion() + (dto.getPuntuacion() == 1 ? " estrella" : " estrellas"));

        Calificacion.CalificacionBuilder calificacion = Calificacion.builder()
                .categoria(dto.getCategoria())
                .idReserva(dto.getIdReserva())
                .docUsuario(docUsuario)
                .nombreCliente(nombrePublico(docUsuario))
                .puntuacion(dto.getPuntuacion())
                .comentario(comentario)
                .fecha(ZonaHoraria.ahora());

        if (dto.getCategoria() == TipoReserva.DEPORTE) {
            ReservaDeporte rd = reservaDeporteRepo.findById(dto.getIdReserva())
                    .orElseThrow(() -> new ReservaNoEncontradaException("La reserva no existe."));
            validar(rd.getDocUsuario(), rd.getEstado(), docUsuario);
            calificacion.idRecurso(rd.getEspacioId()).nombreRecurso(rd.getTipoCancha());
            rd.setHistorial(HistorialReserva.agregar(rd.getHistorial(), evento));
            reservaDeporteRepo.save(rd);
        } else {
            ReservaHotel rh = reservaHotelRepo.findById(dto.getIdReserva())
                    .orElseThrow(() -> new ReservaNoEncontradaException("La reserva no existe."));
            validar(rh.getDocUsuario(), rh.getEstado(), docUsuario);
            String numero = rh.getDatosH() != null && rh.getDatosH().getNumHab() != null ? rh.getDatosH().getNumHab() : "—";
            calificacion.idRecurso(rh.getIdHabitacion()).nombreRecurso("Habitación " + numero);
            rh.setHistorial(HistorialReserva.agregar(rh.getHistorial(), evento));
            reservaHotelRepo.save(rh);
        }

        Calificacion guardada = repo.save(calificacion.build());
        log.info("Reserva {} ({}) calificada con {} estrellas.", dto.getIdReserva(), dto.getCategoria(), dto.getPuntuacion());
        return aDto(guardada);
    }

    /** Promedio y cantidad por espacio o habitación (para los catálogos). */
    public List<ResumenCalificacionDto> resumen(TipoReserva categoria) {
        return repo.findByCategoria(categoria).stream()
                .collect(Collectors.groupingBy(Calificacion::getIdRecurso))
                .entrySet().stream()
                .map(e -> {
                    double promedio = e.getValue().stream().mapToInt(Calificacion::getPuntuacion).average().orElse(0);
                    return new ResumenCalificacionDto(e.getKey(), Math.round(promedio * 10) / 10.0, e.getValue().size());
                })
                .toList();
    }

    /** Opiniones más recientes de un espacio o habitación. */
    public List<CalificacionDto> listarPorRecurso(TipoReserva categoria, String idRecurso) {
        return repo.findByCategoriaAndIdRecursoOrderByFechaDesc(categoria, idRecurso, PageRequest.of(0, MAXIMO_OPINIONES))
                .stream().map(this::aDto).toList();
    }

    /** Calificaciones del cliente (así "Mis reservas" sabe cuáles ya calificó). */
    public List<CalificacionDto> listarMias(String docUsuario) {
        return repo.findByDocUsuario(docUsuario).stream().map(this::aDto).toList();
    }

    private static void validar(String dueno, EstadoReserva estado, String docUsuario) {
        if (!dueno.equals(docUsuario)) {
            throw new AccesoDenegadoException("Solo puedes calificar tus propias reservas.");
        }
        if (estado != EstadoReserva.FINALIZADA) {
            throw new ConflictoDeNegocioException("Solo puedes calificar una reserva cuando ya finalizó.");
        }
    }

    /** "Laura P." — en las opiniones públicas no se muestra el nombre completo. */
    private String nombrePublico(String docUsuario) {
        try {
            UsuarioDto u = usuarioService.obtenerPorDocNum(docUsuario);
            String inicial = u.getApellido() != null && !u.getApellido().isBlank() ? " " + u.getApellido().charAt(0) + "." : "";
            return u.getNombre() + inicial;
        } catch (Exception e) {
            return "Cliente";
        }
    }

    private CalificacionDto aDto(Calificacion c) {
        return CalificacionDto.builder()
                .id(c.getId())
                .categoria(c.getCategoria())
                .idReserva(c.getIdReserva())
                .idRecurso(c.getIdRecurso())
                .nombreRecurso(c.getNombreRecurso())
                .nombreCliente(c.getNombreCliente())
                .puntuacion(c.getPuntuacion())
                .comentario(c.getComentario())
                .fecha(c.getFecha())
                .build();
    }
}
