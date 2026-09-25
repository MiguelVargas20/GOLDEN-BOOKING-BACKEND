package com.sena.goldenbooking.dashboard.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.dashboard.dto.DashboardDto;
import com.sena.goldenbooking.dashboard.dto.EspacioPopularDto;
import com.sena.goldenbooking.dashboard.dto.EventoAgendaDto;
import com.sena.goldenbooking.dashboard.dto.HabitacionHoyDto;
import com.sena.goldenbooking.dashboard.dto.IndicadoresDto;
import com.sena.goldenbooking.dashboard.dto.PuntoTendenciaDto;
import com.sena.goldenbooking.dashboard.dto.ReservaPendienteDto;
import com.sena.goldenbooking.habitaciones.model.EstadoHabitacion;
import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.habitaciones.repository.HabitacionRepository;
import com.sena.goldenbooking.mensajes.repository.MensajeRepository;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.Reserva;
import com.sena.goldenbooking.reservas.model.TipoReserva;
import com.sena.goldenbooking.reservas.repository.ReservaRepository;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

import lombok.extern.slf4j.Slf4j;

/**
 * Arma el resumen del dashboard del administrador: indicadores del día,
 * agenda, pendientes de aprobación, estado de las habitaciones, tendencia de
 * reservas y espacios más reservados.
 *
 * Solo lee datos (no modifica nada). Las consultas traen únicamente los
 * documentos del rango necesario (hoy, el mes o el periodo elegido) y el resto
 * se agrupa en memoria, que para el volumen de este sistema es lo más simple.
 */
@Slf4j
@Service
public class DashboardService {

    /** Periodo permitido para la tendencia y los espacios más reservados. */
    public static final int DIAS_MINIMO = 7;
    public static final int DIAS_MAXIMO = 90;

    private static final int MAX_PENDIENTES = 8;
    private static final int MAX_ESPACIOS_TOP = 6;

    /** Estados que cuentan como "reservado" (ocupan fechas u horarios). */
    private static final Set<EstadoReserva> ACTIVAS = EnumSet.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);
    /** Estados que generan ingreso. */
    private static final Set<EstadoReserva> COBRADAS = EnumSet.of(EstadoReserva.CONFIRMADA, EstadoReserva.FINALIZADA);

    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaHotelRepository reservaHotelRepo;
    private final ReservaRepository reservaRepo;
    private final HabitacionRepository habitacionRepo;
    private final MensajeRepository mensajeRepo;
    private final UsuarioService usuarioService;

    public DashboardService(ReservaDeporteRepository reservaDeporteRepo,
                            ReservaHotelRepository reservaHotelRepo,
                            ReservaRepository reservaRepo,
                            HabitacionRepository habitacionRepo,
                            MensajeRepository mensajeRepo,
                            UsuarioService usuarioService) {
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaHotelRepo = reservaHotelRepo;
        this.reservaRepo = reservaRepo;
        this.habitacionRepo = habitacionRepo;
        this.mensajeRepo = mensajeRepo;
        this.usuarioService = usuarioService;
    }

    public DashboardDto generar(int dias) {
        int periodo = Math.max(DIAS_MINIMO, Math.min(DIAS_MAXIMO, dias));
        LocalDateTime ahora = ZonaHoraria.ahora();
        LocalDate hoy = ahora.toLocalDate();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime inicioManana = inicioHoy.plusDays(1);

        // ── Datos de hoy ────────────────────────────────────────
        List<ReservaDeporte> deporteHoy = reservaDeporteRepo
                .findByFechaReservaGreaterThanEqualAndFechaReservaLessThanAndEstadoNot(inicioHoy, inicioManana, EstadoReserva.CANCELADA);
        List<ReservaHotel> hotelHoy = reservaHotelRepo
                .findByFechaCheckInLessThanAndFechaCheckOutGreaterThanEqualAndEstadoIn(inicioManana, inicioHoy, ACTIVAS);

        // ── Pendientes (las más próximas primero) ───────────────
        List<ReservaDeporte> pendDeporte = reservaDeporteRepo
                .findByEstadoOrderByFechaReservaAsc(EstadoReserva.PENDIENTE, PageRequest.of(0, MAX_PENDIENTES));
        List<ReservaHotel> pendHotel = reservaHotelRepo
                .findByEstadoOrderByFechaCheckInAsc(EstadoReserva.PENDIENTE, PageRequest.of(0, MAX_PENDIENTES));

        // Nombres de clientes: una sola consulta para todo lo que se muestra
        List<String> documentos = Stream.of(
                        deporteHoy.stream().map(ReservaDeporte::getDocUsuario),
                        hotelHoy.stream().map(ReservaHotel::getDocUsuario),
                        pendDeporte.stream().map(ReservaDeporte::getDocUsuario),
                        pendHotel.stream().map(ReservaHotel::getDocUsuario))
                .flatMap(s -> s)
                .distinct()
                .toList();
        Map<String, UsuarioDto> clientes = usuarioService.obtenerMapaPorDocNums(documentos);

        List<Habitacion> habitaciones = habitacionRepo.findAll();
        List<HabitacionHoyDto> estadoHabitaciones = estadoHabitaciones(habitaciones, hotelHoy, hoy, clientes);

        IndicadoresDto indicadores = new IndicadoresDto(
                reservaDeporteRepo.countByEstado(EstadoReserva.PENDIENTE),
                reservaHotelRepo.countByEstado(EstadoReserva.PENDIENTE),
                deporteHoy.size(),
                hotelHoy.stream().filter(r -> r.getFechaCheckIn().toLocalDate().equals(hoy)).count(),
                hotelHoy.stream().filter(r -> r.getFechaCheckOut().toLocalDate().equals(hoy)).count(),
                habitaciones.size(),
                contar(estadoHabitaciones, "OCUPADA"),
                contar(estadoHabitaciones, "RESERVADA_PENDIENTE"),
                contar(estadoHabitaciones, "MANTENIMIENTO"),
                ingresos(hoy.withDayOfMonth(1)),
                ingresos(hoy.withDayOfMonth(1).minusMonths(1)),
                mensajeRepo.countByLeidoFalse());

        return new DashboardDto(
                hoy,
                ahora,
                periodo,
                indicadores,
                agenda(deporteHoy, hotelHoy, hoy, clientes),
                pendientes(pendDeporte, pendHotel, clientes),
                estadoHabitaciones,
                tendencia(hoy, periodo),
                espaciosTop(inicioHoy.minusDays(periodo - 1L), inicioManana));
    }

    // ── Agenda del día ─────────────────────────────────────────────────────

    private List<EventoAgendaDto> agenda(List<ReservaDeporte> deporteHoy, List<ReservaHotel> hotelHoy,
                                         LocalDate hoy, Map<String, UsuarioDto> clientes) {
        List<EventoAgendaDto> eventos = new ArrayList<>();
        deporteHoy.forEach(r -> eventos.add(new EventoAgendaDto("DEPORTE", r.getIdReservaDeporte(),
                r.getFechaReserva(), r.getFechaFinReserva(), r.getTipoCancha(),
                nombre(clientes, r.getDocUsuario()), r.getEstado().name())));
        hotelHoy.forEach(r -> {
            String lugar = "Habitación " + numeroHabitacion(r);
            if (r.getFechaCheckIn().toLocalDate().equals(hoy)) {
                eventos.add(new EventoAgendaDto("CHECK_IN", r.getIdHotelReserva(), r.getFechaCheckIn(), null,
                        lugar, nombre(clientes, r.getDocUsuario()), r.getEstado().name()));
            }
            if (r.getFechaCheckOut().toLocalDate().equals(hoy)) {
                eventos.add(new EventoAgendaDto("CHECK_OUT", r.getIdHotelReserva(), r.getFechaCheckOut(), null,
                        lugar, nombre(clientes, r.getDocUsuario()), r.getEstado().name()));
            }
        });
        eventos.sort(Comparator.comparing(EventoAgendaDto::hora, Comparator.nullsLast(Comparator.naturalOrder())));
        return eventos;
    }

    // ── Pendientes ─────────────────────────────────────────────────────────

    private List<ReservaPendienteDto> pendientes(List<ReservaDeporte> deporte, List<ReservaHotel> hotel,
                                                 Map<String, UsuarioDto> clientes) {
        return Stream.concat(
                        deporte.stream().map(r -> new ReservaPendienteDto("DEPORTE", r.getIdReservaDeporte(),
                                r.getTipoCancha(), nombre(clientes, r.getDocUsuario()),
                                r.getFechaReserva(), r.getFechaFinReserva(), r.getPrecio(), r.getFechaSolicitud())),
                        hotel.stream().map(r -> new ReservaPendienteDto("HOTEL", r.getIdHotelReserva(),
                                "Habitación " + numeroHabitacion(r), nombre(clientes, r.getDocUsuario()),
                                r.getFechaCheckIn(), r.getFechaCheckOut(), r.getPrecioTotal(), r.getFechaSolicitud())))
                .sorted(Comparator.comparing(ReservaPendienteDto::inicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_PENDIENTES)
                .toList();
    }

    // ── Habitaciones ───────────────────────────────────────────────────────

    /**
     * DISPONIBLE, OCUPADA (reserva confirmada que cubre la noche de hoy),
     * RESERVADA_PENDIENTE (la reserva que cubre hoy aún no se aprueba) o
     * MANTENIMIENTO. El día de check-out la habitación ya cuenta como libre
     * (igual que en la validación de reservas).
     */
    private List<HabitacionHoyDto> estadoHabitaciones(List<Habitacion> habitaciones, List<ReservaHotel> hotelHoy,
                                                      LocalDate hoy, Map<String, UsuarioDto> clientes) {
        Map<String, ReservaHotel> ocupacionHoy = new LinkedHashMap<>();
        hotelHoy.stream()
                .filter(r -> !r.getFechaCheckIn().toLocalDate().isAfter(hoy) && r.getFechaCheckOut().toLocalDate().isAfter(hoy))
                // si hubiera dos (una pendiente y una confirmada), gana la confirmada
                .sorted(Comparator.comparing(r -> r.getEstado() == EstadoReserva.CONFIRMADA ? 0 : 1))
                .forEach(r -> ocupacionHoy.putIfAbsent(r.getIdHabitacion(), r));

        return habitaciones.stream()
                .sorted(Comparator.comparing(h -> h.getNumHab() == null ? "" : h.getNumHab()))
                .map(h -> {
                    String tipo = h.getTipoHabitacion() != null ? h.getTipoHabitacion().getNomTipo() : null;
                    if (h.getEstado() == EstadoHabitacion.MANTENIMIENTO) {
                        return new HabitacionHoyDto(h.getId(), h.getNumHab(), tipo, "MANTENIMIENTO", null, null);
                    }
                    ReservaHotel r = ocupacionHoy.get(h.getId());
                    if (r == null) {
                        return new HabitacionHoyDto(h.getId(), h.getNumHab(), tipo, "DISPONIBLE", null, null);
                    }
                    String estado = r.getEstado() == EstadoReserva.CONFIRMADA ? "OCUPADA" : "RESERVADA_PENDIENTE";
                    return new HabitacionHoyDto(h.getId(), h.getNumHab(), tipo, estado,
                            nombre(clientes, r.getDocUsuario()), r.getFechaCheckOut());
                })
                .toList();
    }

    // ── Tendencia y espacios ───────────────────────────────────────────────

    /** Reservas recibidas por día (según la fecha en que se crearon), de hotel y deporte. */
    private List<PuntoTendenciaDto> tendencia(LocalDate hoy, int dias) {
        LocalDate desde = hoy.minusDays(dias - 1L);
        Map<LocalDate, long[]> porDia = new LinkedHashMap<>();
        for (int i = 0; i < dias; i++) {
            porDia.put(desde.plusDays(i), new long[2]);
        }
        for (Reserva r : reservaRepo.findByFechaReservaGreaterThanEqual(desde.atStartOfDay())) {
            if (r.getFechaReserva() == null || r.getTipo() == null) continue;
            long[] conteo = porDia.get(r.getFechaReserva().toLocalDate());
            if (conteo != null) {
                conteo[r.getTipo() == TipoReserva.DEPORTE ? 0 : 1]++;
            }
        }
        return porDia.entrySet().stream()
                .map(e -> new PuntoTendenciaDto(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    /** Espacios con más reservas (no canceladas) que se usan en el periodo. */
    private List<EspacioPopularDto> espaciosTop(LocalDateTime desde, LocalDateTime hasta) {
        Map<String, List<ReservaDeporte>> porEspacio = reservaDeporteRepo
                .findByFechaReservaGreaterThanEqualAndFechaReservaLessThanAndEstadoNot(desde, hasta, EstadoReserva.CANCELADA)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getEspacioId() != null ? r.getEspacioId() : "sin-espacio:" + r.getTipoCancha()));

        return porEspacio.entrySet().stream()
                .map(e -> {
                    List<ReservaDeporte> lista = e.getValue();
                    double horas = lista.stream()
                            // reservas antiguas pueden no tener hora de fin
                            .filter(r -> r.getFechaFinReserva() != null)
                            .mapToDouble(r -> Duration.between(r.getFechaReserva(), r.getFechaFinReserva()).toMinutes() / 60.0)
                            .sum();
                    return new EspacioPopularDto(e.getKey(), lista.get(0).getTipoCancha(), lista.size(),
                            Math.round(horas * 10) / 10.0);
                })
                .sorted(Comparator.comparingLong(EspacioPopularDto::reservas).reversed()
                        .thenComparing(Comparator.comparingDouble(EspacioPopularDto::horas).reversed()))
                .limit(MAX_ESPACIOS_TOP)
                .toList();
    }

    /** Ingresos de las reservas confirmadas/finalizadas que se usan en el mes que empieza en inicioMes. */
    private double ingresos(LocalDate inicioMes) {
        return reservaRepo.findByFechaInicioGreaterThanEqualAndFechaInicioLessThanAndEstadoIn(
                        inicioMes.atStartOfDay(), inicioMes.plusMonths(1).atStartOfDay(), COBRADAS)
                .stream()
                .mapToDouble(r -> r.getPrecioTotal() != null ? r.getPrecioTotal() : 0)
                .sum();
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private static long contar(List<HabitacionHoyDto> habitaciones, String estado) {
        return habitaciones.stream().filter(h -> estado.equals(h.estadoHoy())).count();
    }

    private static String nombre(Map<String, UsuarioDto> clientes, String documento) {
        UsuarioDto c = clientes.get(documento);
        return c != null ? (c.getNombre() + " " + c.getApellido()).trim() : "Doc. " + documento;
    }

    private static String numeroHabitacion(ReservaHotel r) {
        return r.getDatosH() != null && r.getDatosH().getNumHab() != null ? r.getDatosH().getNumHab() : "—";
    }
}
