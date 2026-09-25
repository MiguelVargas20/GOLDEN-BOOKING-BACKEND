package com.sena.goldenbooking.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.dashboard.dto.DashboardDto;
import com.sena.goldenbooking.dashboard.dto.HabitacionHoyDto;
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

/** Pruebas unitarias del armado del dashboard (sin Spring ni Mongo). */
class DashboardServiceTest {

    private ReservaDeporteRepository reservaDeporteRepo;
    private ReservaHotelRepository reservaHotelRepo;
    private ReservaRepository reservaRepo;
    private HabitacionRepository habitacionRepo;
    private DashboardService service;

    private final LocalDate hoy = ZonaHoraria.ahora().toLocalDate();

    @BeforeEach
    void setUp() {
        reservaDeporteRepo = mock(ReservaDeporteRepository.class);
        reservaHotelRepo = mock(ReservaHotelRepository.class);
        reservaRepo = mock(ReservaRepository.class);
        habitacionRepo = mock(HabitacionRepository.class);
        MensajeRepository mensajeRepo = mock(MensajeRepository.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        service = new DashboardService(reservaDeporteRepo, reservaHotelRepo, reservaRepo,
                habitacionRepo, mensajeRepo, usuarioService);

        when(usuarioService.obtenerMapaPorDocNums(any())).thenReturn(
                Map.of("1", UsuarioDto.builder().nombre("Ana").apellido("Gómez").build()));
        when(mensajeRepo.countByLeidoFalse()).thenReturn(3L);
        when(reservaDeporteRepo.findByFechaReservaGreaterThanEqualAndFechaReservaLessThanAndEstadoNot(any(), any(), any()))
                .thenReturn(List.of());
        when(reservaHotelRepo.findByFechaCheckInLessThanAndFechaCheckOutGreaterThanEqualAndEstadoIn(any(), any(), any()))
                .thenReturn(List.of());
        when(reservaDeporteRepo.findByEstadoOrderByFechaReservaAsc(any(), any())).thenReturn(List.of());
        when(reservaHotelRepo.findByEstadoOrderByFechaCheckInAsc(any(), any())).thenReturn(List.of());
        when(reservaRepo.findByFechaReservaGreaterThanEqual(any())).thenReturn(List.of());
        when(reservaRepo.findByFechaInicioGreaterThanEqualAndFechaInicioLessThanAndEstadoIn(any(), any(), any()))
                .thenReturn(List.of());
        when(habitacionRepo.findAll()).thenReturn(List.of());
    }

    @Test
    void periodoSeLimitaEntre7y90Dias() {
        assertEquals(7, service.generar(1).periodoDias());
        assertEquals(90, service.generar(500).periodoDias());
        assertEquals(7, service.generar(1).tendencia().size());
    }

    @Test
    void estadoDeHabitacionesSegunReservasQueCubrenHoy() {
        Habitacion h101 = Habitacion.builder().id("h1").numHab("101").estado(EstadoHabitacion.DISPONIBLE).build();
        Habitacion h102 = Habitacion.builder().id("h2").numHab("102").estado(EstadoHabitacion.DISPONIBLE).build();
        Habitacion h103 = Habitacion.builder().id("h3").numHab("103").estado(EstadoHabitacion.MANTENIMIENTO).build();
        Habitacion h104 = Habitacion.builder().id("h4").numHab("104").estado(EstadoHabitacion.DISPONIBLE).build();
        when(habitacionRepo.findAll()).thenReturn(List.of(h104, h103, h102, h101));

        LocalDateTime ayer = hoy.minusDays(1).atTime(15, 0);
        ReservaHotel confirmada = ReservaHotel.builder().idHotelReserva("r1").idHabitacion("h1").docUsuario("1")
                .fechaCheckIn(ayer).fechaCheckOut(hoy.plusDays(2).atTime(12, 0)).estado(EstadoReserva.CONFIRMADA).build();
        ReservaHotel pendiente = ReservaHotel.builder().idHotelReserva("r2").idHabitacion("h2").docUsuario("1")
                .fechaCheckIn(hoy.atTime(15, 0)).fechaCheckOut(hoy.plusDays(1).atTime(12, 0)).estado(EstadoReserva.PENDIENTE).build();
        // sale hoy: la habitación ya cuenta como libre
        ReservaHotel saleHoy = ReservaHotel.builder().idHotelReserva("r3").idHabitacion("h4").docUsuario("1")
                .fechaCheckIn(ayer).fechaCheckOut(hoy.atTime(12, 0)).estado(EstadoReserva.CONFIRMADA).build();
        when(reservaHotelRepo.findByFechaCheckInLessThanAndFechaCheckOutGreaterThanEqualAndEstadoIn(any(), any(), any()))
                .thenReturn(List.of(confirmada, pendiente, saleHoy));

        DashboardDto d = service.generar(14);

        List<String> estados = d.habitaciones().stream().map(HabitacionHoyDto::estadoHoy).toList();
        assertEquals(List.of("OCUPADA", "RESERVADA_PENDIENTE", "MANTENIMIENTO", "DISPONIBLE"), estados);
        assertEquals("Ana Gómez", d.habitaciones().get(0).huesped());
        assertEquals(1, d.indicadores().habitacionesOcupadas());
        assertEquals(1, d.indicadores().checkInsHoy());
        assertEquals(1, d.indicadores().checkOutsHoy());
        // agenda: check-out 12:00 antes que check-in 15:00
        assertEquals(List.of("CHECK_OUT", "CHECK_IN"), d.agendaHoy().stream().map(e -> e.tipo()).toList());
        assertEquals(3, d.indicadores().mensajesNoLeidos());
    }

    @Test
    void tendenciaCuentaPorDiaYTipoYEspaciosTopOrdenados() {
        when(reservaRepo.findByFechaReservaGreaterThanEqual(any())).thenReturn(List.of(
                Reserva.builder().tipo(TipoReserva.DEPORTE).fechaReserva(hoy.atTime(9, 0)).build(),
                Reserva.builder().tipo(TipoReserva.DEPORTE).fechaReserva(hoy.atTime(10, 0)).build(),
                Reserva.builder().tipo(TipoReserva.HOTEL).fechaReserva(hoy.atTime(11, 0)).build()));

        LocalDateTime diez = hoy.atTime(LocalTime.of(10, 0));
        when(reservaDeporteRepo.findByFechaReservaGreaterThanEqualAndFechaReservaLessThanAndEstadoNot(any(), any(), any()))
                .thenReturn(List.of(
                        ReservaDeporte.builder().espacioId("e1").tipoCancha("Cancha 1").docUsuario("1")
                                .fechaReserva(diez).fechaFinReserva(diez.plusHours(2)).estado(EstadoReserva.CONFIRMADA).build(),
                        ReservaDeporte.builder().espacioId("e2").tipoCancha("Cancha 2").docUsuario("1")
                                .fechaReserva(diez).fechaFinReserva(diez.plusHours(1)).estado(EstadoReserva.PENDIENTE).build(),
                        ReservaDeporte.builder().espacioId("e2").tipoCancha("Cancha 2").docUsuario("1")
                                .fechaReserva(diez.plusHours(3)).fechaFinReserva(null).estado(EstadoReserva.PENDIENTE).build()));

        DashboardDto d = service.generar(7);

        var ultimo = d.tendencia().get(d.tendencia().size() - 1);
        assertEquals(hoy, ultimo.fecha());
        assertEquals(2, ultimo.deporte());
        assertEquals(1, ultimo.hotel());
        assertEquals("e2", d.espaciosTop().get(0).espacioId());
        assertEquals(1.0, d.espaciosTop().get(0).horas());
    }
}
