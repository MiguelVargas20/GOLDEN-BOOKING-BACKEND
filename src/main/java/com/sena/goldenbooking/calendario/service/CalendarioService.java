package com.sena.goldenbooking.calendario.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.calendario.dto.BloqueCalendarioDto;
import com.sena.goldenbooking.calendario.dto.CalendarioSemanaDto;
import com.sena.goldenbooking.calendario.dto.FilaCalendarioDto;
import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.habitaciones.repository.HabitacionRepository;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.model.EstadoEspacio;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.EspacioDeportivoRepository;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

/**
 * Calendario de ocupación para el administrador: por cada espacio deportivo y
 * cada habitación, las reservas (no canceladas) de una semana.
 */
@Service
public class CalendarioService {

    static final int DIAS = 7;
    private static final List<EstadoReserva> VISIBLES =
            List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.FINALIZADA);

    private final EspacioDeportivoRepository espacioRepo;
    private final HabitacionRepository habitacionRepo;
    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaHotelRepository reservaHotelRepo;
    private final UsuarioService usuarioService;

    public CalendarioService(EspacioDeportivoRepository espacioRepo, HabitacionRepository habitacionRepo,
                             ReservaDeporteRepository reservaDeporteRepo, ReservaHotelRepository reservaHotelRepo,
                             UsuarioService usuarioService) {
        this.espacioRepo = espacioRepo;
        this.habitacionRepo = habitacionRepo;
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaHotelRepo = reservaHotelRepo;
        this.usuarioService = usuarioService;
    }

    public CalendarioSemanaDto semana(LocalDate desde) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = desde.plusDays(DIAS).atStartOfDay();

        List<ReservaDeporte> deporte = reservaDeporteRepo
                .findByFechaReservaGreaterThanEqualAndFechaReservaLessThanAndEstadoNot(inicio, fin, EstadoReserva.CANCELADA);
        // Estadías que tocan la semana (también las que empezaron antes)
        List<ReservaHotel> hotel = reservaHotelRepo
                .findByFechaCheckInLessThanAndFechaCheckOutGreaterThanEqualAndEstadoIn(fin, inicio, VISIBLES);

        Map<String, UsuarioDto> clientes = usuarioService.obtenerMapaPorDocNums(Stream.concat(
                deporte.stream().map(ReservaDeporte::getDocUsuario),
                hotel.stream().map(ReservaHotel::getDocUsuario)).distinct().toList());

        Map<String, List<ReservaDeporte>> deportePorEspacio = deporte.stream()
                .filter(r -> r.getEspacioId() != null)
                .collect(Collectors.groupingBy(ReservaDeporte::getEspacioId));
        Map<String, List<ReservaHotel>> hotelPorHabitacion = hotel.stream()
                .filter(r -> r.getIdHabitacion() != null)
                .collect(Collectors.groupingBy(ReservaHotel::getIdHabitacion));

        List<FilaCalendarioDto> espacios = espacioRepo
                .findByEstadoIn(List.of(EstadoEspacio.ACTIVO, EstadoEspacio.MANTENIMIENTO), Sort.by("nombre"))
                .stream()
                .map(e -> new FilaCalendarioDto(e.getId(), e.getNombre(), e.getDeporte(), e.getEstado().name(),
                        e.getHoraApertura(), e.getHoraCierre(),
                        deportePorEspacio.getOrDefault(e.getId(), List.of()).stream()
                                .sorted(Comparator.comparing(ReservaDeporte::getFechaReserva))
                                .map(r -> new BloqueCalendarioDto(r.getIdReservaDeporte(), nombre(clientes, r.getDocUsuario()),
                                        r.getFechaReserva(), r.getFechaFinReserva(), estado(r.getEstado())))
                                .toList()))
                .toList();

        List<FilaCalendarioDto> habitaciones = habitacionRepo.findAll().stream()
                .sorted(Comparator.comparing(h -> h.getNumHab() != null ? h.getNumHab() : ""))
                .map(h -> new FilaCalendarioDto(h.getId(), "Habitación " + h.getNumHab(), tipo(h),
                        h.getEstado() != null ? h.getEstado().name() : null, null, null,
                        hotelPorHabitacion.getOrDefault(h.getId(), List.of()).stream()
                                .sorted(Comparator.comparing(ReservaHotel::getFechaCheckIn))
                                .map(r -> new BloqueCalendarioDto(r.getIdHotelReserva(), nombre(clientes, r.getDocUsuario()),
                                        r.getFechaCheckIn(), r.getFechaCheckOut(), estado(r.getEstado())))
                                .toList()))
                .toList();

        return new CalendarioSemanaDto(desde, desde.plusDays(DIAS - 1L), espacios, habitaciones);
    }

    private static String tipo(Habitacion h) {
        return h.getTipoHabitacion() != null ? h.getTipoHabitacion().getNomTipo() : null;
    }

    private static String nombre(Map<String, UsuarioDto> clientes, String documento) {
        UsuarioDto c = documento != null ? clientes.get(documento) : null;
        if (c == null) return documento != null ? "Doc. " + documento : "Cliente";
        return ((c.getNombre() != null ? c.getNombre() : "") + " " + (c.getApellido() != null ? c.getApellido() : "")).trim();
    }

    private static String estado(EstadoReserva estado) {
        return estado != null ? estado.name() : EstadoReserva.PENDIENTE.name();
    }
}
