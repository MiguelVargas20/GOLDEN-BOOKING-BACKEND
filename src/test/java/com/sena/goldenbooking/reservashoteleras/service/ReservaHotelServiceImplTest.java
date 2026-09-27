package com.sena.goldenbooking.reservashoteleras.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.email.EmailService;
import com.sena.goldenbooking.compartido.exception.AccesoDenegadoException;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.habitaciones.model.EstadoHabitacion;
import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.habitaciones.repository.HabitacionRepository;
import com.sena.goldenbooking.membresias.dto.BeneficioVigente;
import com.sena.goldenbooking.membresias.service.MembresiaService;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.service.NotificacionService;
import com.sena.goldenbooking.reservas.model.AccionReserva;
import com.sena.goldenbooking.reservas.model.CanceladaPor;
import com.sena.goldenbooking.reservas.model.MiembroReserva;
import com.sena.goldenbooking.usuarios.model.TipoMembresia;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.Reserva;
import com.sena.goldenbooking.reservas.repository.ReservaRepository;
import com.sena.goldenbooking.reservas.service.AvisosAdminService;
import com.sena.goldenbooking.reservashoteleras.dto.ReservaHotelDto;
import com.sena.goldenbooking.reservashoteleras.mapper.ReservaHotelMapperImpl;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

/**
 * Pruebas unitarias (sin Spring ni Mongo) de las reservas hoteleras: creación
 * (horarios del hotel, noches, precio, fechas cruzadas) y flujo de aprobación
 * y cancelación.
 */
class ReservaHotelServiceImplTest {

    private ReservaHotelRepository reservaHotelRepo;
    private ReservaRepository reservaRepo;
    private HabitacionRepository habitacionRepo;
    private UsuarioService usuarioService;
    private AvisosAdminService avisosAdmin;
    private NotificacionService notificaciones;
    private MembresiaService membresias;
    private ReservaHotelServiceImpl service;

    private final LocalDate enDiezDias = ZonaHoraria.ahora().toLocalDate().plusDays(10);

    @BeforeEach
    void setUp() {
        reservaHotelRepo = mock(ReservaHotelRepository.class);
        reservaRepo = mock(ReservaRepository.class);
        habitacionRepo = mock(HabitacionRepository.class);
        usuarioService = mock(UsuarioService.class);
        avisosAdmin = mock(AvisosAdminService.class);
        notificaciones = mock(NotificacionService.class);
        membresias = mock(MembresiaService.class);
        when(membresias.beneficiosDe(anyString())).thenReturn(BeneficioVigente.sinBeneficios(365));
        service = new ReservaHotelServiceImpl(reservaHotelRepo, reservaRepo, habitacionRepo,
                new ReservaHotelMapperImpl(), mock(EmailService.class), usuarioService, avisosAdmin, notificaciones,
                membresias);

        when(habitacionRepo.findById("h1")).thenReturn(Optional.of(habitacion(EstadoHabitacion.DISPONIBLE)));
        when(usuarioService.obtenerPorDocNum("123")).thenReturn(
                UsuarioDto.builder().nombre("Ana").email("ana@test.com").estado(EstadoUsuario.ACTIVO).build());
        when(reservaHotelRepo.findByIdHabitacionAndEstadoNot(anyString(), any())).thenReturn(List.of());
        when(reservaRepo.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId("padre1");
            return r;
        });
        when(reservaHotelRepo.save(any(ReservaHotel.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Habitacion habitacion(EstadoHabitacion estado) {
        return Habitacion.builder().id("h1").numHab("101").precNoche(180000.0).estado(estado).build();
    }

    /** Como llega del front: el día elegido a medianoche. */
    private ReservaHotelDto dto(LocalDate entrada, LocalDate salida) {
        return ReservaHotelDto.builder()
                .docUsuario("123").idHabitacion("h1")
                .fCheckIn(entrada.atStartOfDay()).fCheckOut(salida.atStartOfDay())
                .build();
    }

    private void reservaExistente(EstadoReserva estado, LocalDateTime checkIn) {
        when(reservaHotelRepo.findById("rh1")).thenReturn(Optional.of(ReservaHotel.builder()
                .idHotelReserva("rh1").idReserva("padre1").docUsuario("123").idHabitacion("h1")
                .datosH(habitacion(EstadoHabitacion.DISPONIBLE))
                .fechaCheckIn(checkIn).fechaCheckOut(checkIn.plusDays(2)).noches(2).precioTotal(360000.0)
                .estado(estado).build()));
        when(reservaRepo.findById("padre1")).thenReturn(Optional.of(Reserva.builder().id("padre1").estado(estado).build()));
    }

    // ── Crear ──────────────────────────────────────────────────────────────

    @Test
    void creaLaReservaPendienteConHorariosDelHotelNochesYPrecio() {
        ReservaHotelDto creada = service.crear(dto(enDiezDias, enDiezDias.plusDays(3)), false, false);

        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());
        assertEquals(enDiezDias.atTime(15, 0), creada.getFCheckIn());
        assertEquals(enDiezDias.plusDays(3).atTime(12, 0), creada.getFCheckOut());
        assertEquals(3, creada.getNoch());
        assertEquals(540000.0, creada.getPTotal());
        assertNull(creada.getFechaConfirmacion());
        verify(avisosAdmin).nuevaReserva(anyString(), any(), anyString(), anyString(), any(), any());
    }

    @Test
    void guardaTambienLaReservaPadreConElMismoEstado() {
        service.crear(dto(enDiezDias, enDiezDias.plusDays(1)), false, false);

        ArgumentCaptor<Reserva> padre = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepo).save(padre.capture());
        assertEquals(EstadoReserva.PENDIENTE, padre.getValue().getEstado());
        assertEquals(180000.0, padre.getValue().getPrecioTotal());
    }

    @Test
    void elAdminPuedeConfirmarlaDeInmediatoYNoSeAvisaASiMismo() {
        ReservaHotelDto creada = service.crear(dto(enDiezDias, enDiezDias.plusDays(2)), true, true);

        assertEquals(EstadoReserva.CONFIRMADA, creada.getEstado());
        assertTrue(creada.isRegistradaPorAdministrador());
        assertNotNull(creada.getFechaConfirmacion());
        verify(avisosAdmin, never()).nuevaReserva(anyString(), any(), anyString(), anyString(), any(), any());
    }

    @Test
    void unClienteNoPuedeCrearlaConfirmadaAunqueLoPida() {
        ReservaHotelDto creada = service.crear(dto(enDiezDias, enDiezDias.plusDays(2)), false, true);

        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());
    }

    @Test
    void rechazaCheckOutIgualOAnteriorAlCheckIn() {
        assertThrows(SolicitudInvalidaException.class,
                () -> service.crear(dto(enDiezDias, enDiezDias), false, false));
    }

    @Test
    void rechazaCheckInEnElPasado() {
        LocalDate ayer = ZonaHoraria.ahora().toLocalDate().minusDays(1);
        assertThrows(SolicitudInvalidaException.class,
                () -> service.crear(dto(ayer, ayer.plusDays(2)), false, false));
    }

    @Test
    void rechazaHabitacionEnMantenimiento() {
        when(habitacionRepo.findById("h1")).thenReturn(Optional.of(habitacion(EstadoHabitacion.MANTENIMIENTO)));

        assertThrows(ConflictoDeNegocioException.class,
                () -> service.crear(dto(enDiezDias, enDiezDias.plusDays(1)), false, false));
    }

    @Test
    void rechazaFechasQueSeCruzanConOtraReserva() {
        when(reservaHotelRepo.findByIdHabitacionAndEstadoNot("h1", EstadoReserva.CANCELADA)).thenReturn(List.of(
                ReservaHotel.builder().fechaCheckIn(enDiezDias.plusDays(1).atTime(15, 0))
                        .fechaCheckOut(enDiezDias.plusDays(4).atTime(12, 0)).build()));

        assertThrows(ConflictoDeNegocioException.class,
                () -> service.crear(dto(enDiezDias, enDiezDias.plusDays(2)), false, false));
        verify(reservaHotelRepo, never()).save(any());
    }

    @Test
    void permiteEntrarElMismoDiaEnQueSaleOtroHuesped() {
        when(reservaHotelRepo.findByIdHabitacionAndEstadoNot("h1", EstadoReserva.CANCELADA)).thenReturn(List.of(
                ReservaHotel.builder().fechaCheckIn(enDiezDias.minusDays(2).atTime(15, 0))
                        .fechaCheckOut(enDiezDias.atTime(12, 0)).build()));

        ReservaHotelDto creada = service.crear(dto(enDiezDias, enDiezDias.plusDays(1)), false, false);

        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());
    }

    @Test
    void exigeQueElTitularSeaUnClienteRegistrado() {
        when(usuarioService.obtenerPorDocNum("123")).thenThrow(new RecursoNoEncontradoException("Usuario no encontrado"));

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crear(dto(enDiezDias, enDiezDias.plusDays(1)), true, false));
    }

    @Test
    void rechazaDatosIncompletos() {
        ReservaHotelDto sinHabitacion = dto(enDiezDias, enDiezDias.plusDays(1));
        sinHabitacion.setIdHabitacion(null);

        assertThrows(SolicitudInvalidaException.class, () -> service.crear(sinHabitacion, false, false));
    }

    // ── Aprobar ────────────────────────────────────────────────────────────

    @Test
    void confirmaUnaPendienteYSincronizaLaReservaPadre() {
        reservaExistente(EstadoReserva.PENDIENTE, enDiezDias.atTime(15, 0));

        ReservaHotelDto confirmada = service.confirmar("rh1");

        assertEquals(EstadoReserva.CONFIRMADA, confirmada.getEstado());
        assertNotNull(confirmada.getFechaConfirmacion());
        ArgumentCaptor<Reserva> padre = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepo).save(padre.capture());
        assertEquals(EstadoReserva.CONFIRMADA, padre.getValue().getEstado());
    }

    @Test
    void noConfirmaUnaReservaCancelada() {
        reservaExistente(EstadoReserva.CANCELADA, enDiezDias.atTime(15, 0));

        assertThrows(ConflictoDeNegocioException.class, () -> service.confirmar("rh1"));
    }

    // ── Cancelar ───────────────────────────────────────────────────────────

    @Test
    void elAdminCancelaConMotivoYQuedaRegistradoQuienLaCancelo() {
        reservaExistente(EstadoReserva.CONFIRMADA, enDiezDias.atTime(15, 0));

        ReservaHotelDto cancelada = service.cancelar("rh1", "999", true, "Habitación en remodelación");

        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        assertEquals(CanceladaPor.ADMINISTRADOR, cancelada.getCanceladaPor());
        assertEquals("Habitación en remodelación", cancelada.getMotivoCancelacion());
        assertNotNull(cancelada.getFechaCancelacion());
        verify(avisosAdmin, never()).reservaCanceladaPorCliente(anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void elAdminDebeEscribirUnMotivo() {
        reservaExistente(EstadoReserva.PENDIENTE, enDiezDias.atTime(15, 0));

        assertThrows(SolicitudInvalidaException.class, () -> service.cancelar("rh1", "999", true, " "));
    }

    @Test
    void elClienteCancelaLaSuyaYSeAvisaAlAdmin() {
        reservaExistente(EstadoReserva.PENDIENTE, enDiezDias.atTime(15, 0));

        ReservaHotelDto cancelada = service.cancelar("rh1", "123", false, null);

        assertEquals(CanceladaPor.CLIENTE, cancelada.getCanceladaPor());
        verify(avisosAdmin).reservaCanceladaPorCliente(anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void unClienteNoPuedeCancelarLaReservaDeOtro() {
        reservaExistente(EstadoReserva.PENDIENTE, enDiezDias.atTime(15, 0));

        assertThrows(AccesoDenegadoException.class, () -> service.cancelar("rh1", "456", false, null));
    }

    @Test
    void elClienteNoPuedeCancelarConMenosDe24Horas() {
        reservaExistente(EstadoReserva.CONFIRMADA, ZonaHoraria.ahora().plusHours(5));

        assertThrows(ConflictoDeNegocioException.class, () -> service.cancelar("rh1", "123", false, null));
    }

    @Test
    void noSeCancelaDosVeces() {
        reservaExistente(EstadoReserva.CANCELADA, enDiezDias.atTime(15, 0));

        assertThrows(ConflictoDeNegocioException.class, () -> service.cancelar("rh1", "999", true, "Motivo largo"));
    }

    // ── Consultas ──────────────────────────────────────────────────────────

    @Test
    void unClienteNoPuedeVerLaReservaDeOtro() {
        reservaExistente(EstadoReserva.PENDIENTE, enDiezDias.atTime(15, 0));

        assertThrows(AccesoDenegadoException.class, () -> service.obtenerPorId("rh1", "456", false));
        assertEquals("rh1", service.obtenerPorId("rh1", "999", true).getIdH());
    }

    // ── Historial y notificaciones ─────────────────────────────────────────

    @Test
    void aprobarYCancelarQuedanEnElHistorialYSeNotificaAlCliente() {
        reservaExistente(EstadoReserva.PENDIENTE, enDiezDias.atTime(15, 0));
        ReservaHotelDto confirmada = service.confirmar("rh1");
        assertEquals(AccionReserva.CONFIRMADA, confirmada.getHistorial().get(0).getAccion());
        verify(notificaciones).notificar(eq("123"), eq(TipoNotificacion.RESERVA_APROBADA), any(), eq("rh1"), anyString(), anyString());

        reservaExistente(EstadoReserva.CONFIRMADA, enDiezDias.atTime(15, 0));
        service.cancelar("rh1", "999", true, "Habitación en remodelación");
        verify(notificaciones).notificar(eq("123"), eq(TipoNotificacion.RESERVA_CANCELADA), any(), eq("rh1"), anyString(), anyString());
    }

    // ── Reprogramar ────────────────────────────────────────────────────────

    @Test
    void reprogramaConHorariosDelHotelYRecalculaNochesYPrecio() {
        reservaExistente(EstadoReserva.PENDIENTE, enDiezDias.atTime(15, 0));
        LocalDate nuevaEntrada = enDiezDias.plusDays(5);

        ReservaHotelDto r = service.reprogramar("rh1", nuevaEntrada.atStartOfDay(), nuevaEntrada.plusDays(3).atStartOfDay(), "123", false);

        assertEquals(nuevaEntrada.atTime(15, 0), r.getFCheckIn());
        assertEquals(nuevaEntrada.plusDays(3).atTime(12, 0), r.getFCheckOut());
        assertEquals(3, r.getNoch());
        assertEquals(540000.0, r.getPTotal());
        assertEquals(AccionReserva.REPROGRAMADA, r.getHistorial().get(0).getAccion());
        ArgumentCaptor<Reserva> padre = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepo).save(padre.capture());
        assertEquals(nuevaEntrada.atTime(15, 0), padre.getValue().getFechaInicio());
        assertEquals(540000.0, padre.getValue().getPrecioTotal());
        verify(avisosAdmin).reservaReprogramadaPorCliente(eq("HOTEL"), eq("rh1"), eq("123"), anyString(), any(), any());
    }

    @Test
    void siElClienteReprogramaUnaConfirmadaVuelveAPendiente() {
        reservaExistente(EstadoReserva.CONFIRMADA, enDiezDias.atTime(15, 0));

        ReservaHotelDto r = service.reprogramar("rh1", enDiezDias.plusDays(1).atStartOfDay(),
                enDiezDias.plusDays(2).atStartOfDay(), "123", false);

        assertEquals(EstadoReserva.PENDIENTE, r.getEstado());
        assertNull(r.getFechaConfirmacion());
    }

    @Test
    void siReprogramaElAdminSigueConfirmadaYSeNotificaAlCliente() {
        reservaExistente(EstadoReserva.CONFIRMADA, enDiezDias.atTime(15, 0));

        ReservaHotelDto r = service.reprogramar("rh1", enDiezDias.plusDays(1).atStartOfDay(),
                enDiezDias.plusDays(2).atStartOfDay(), "999", true);

        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
        verify(notificaciones).notificar(eq("123"), eq(TipoNotificacion.RESERVA_REPROGRAMADA), any(), eq("rh1"), anyString(), anyString());
    }

    @Test
    void reprogramarIgnoraSuPropiaReservaAlBuscarCruces() {
        reservaExistente(EstadoReserva.PENDIENTE, enDiezDias.atTime(15, 0));
        when(reservaHotelRepo.findByIdHabitacionAndEstadoNot("h1", EstadoReserva.CANCELADA)).thenReturn(List.of(
                ReservaHotel.builder().idHotelReserva("rh1").fechaCheckIn(enDiezDias.atTime(15, 0))
                        .fechaCheckOut(enDiezDias.plusDays(2).atTime(12, 0)).build()));

        // alargar la misma estadía un día se permite
        ReservaHotelDto r = service.reprogramar("rh1", enDiezDias.atStartOfDay(), enDiezDias.plusDays(3).atStartOfDay(), "123", false);
        assertEquals(3, r.getNoch());
    }

    @Test
    void reprogramarRechazaFechasOcupadasPorOtraReserva() {
        reservaExistente(EstadoReserva.PENDIENTE, enDiezDias.atTime(15, 0));
        when(reservaHotelRepo.findByIdHabitacionAndEstadoNot("h1", EstadoReserva.CANCELADA)).thenReturn(List.of(
                ReservaHotel.builder().idHotelReserva("otra").fechaCheckIn(enDiezDias.plusDays(4).atTime(15, 0))
                        .fechaCheckOut(enDiezDias.plusDays(6).atTime(12, 0)).build()));

        assertThrows(ConflictoDeNegocioException.class, () -> service.reprogramar("rh1",
                enDiezDias.plusDays(5).atStartOfDay(), enDiezDias.plusDays(7).atStartOfDay(), "123", false));
    }

    @Test
    void reprogramarValidaFechasEstadoY24Horas() {
        reservaExistente(EstadoReserva.PENDIENTE, enDiezDias.atTime(15, 0));
        assertThrows(SolicitudInvalidaException.class, () -> service.reprogramar("rh1",
                enDiezDias.plusDays(3).atStartOfDay(), enDiezDias.plusDays(3).atStartOfDay(), "123", false));
        assertThrows(SolicitudInvalidaException.class, () -> service.reprogramar("rh1",
                enDiezDias.atStartOfDay(), enDiezDias.plusDays(2).atStartOfDay(), "123", false)); // mismas fechas

        reservaExistente(EstadoReserva.FINALIZADA, enDiezDias.atTime(15, 0));
        assertThrows(ConflictoDeNegocioException.class, () -> service.reprogramar("rh1",
                enDiezDias.plusDays(3).atStartOfDay(), enDiezDias.plusDays(4).atStartOfDay(), "123", false));

        reservaExistente(EstadoReserva.CONFIRMADA, ZonaHoraria.ahora().plusHours(5));
        assertThrows(ConflictoDeNegocioException.class, () -> service.reprogramar("rh1",
                enDiezDias.plusDays(3).atStartOfDay(), enDiezDias.plusDays(4).atStartOfDay(), "123", false));
        assertThrows(AccesoDenegadoException.class, () -> service.reprogramar("rh1",
                enDiezDias.plusDays(3).atStartOfDay(), enDiezDias.plusDays(4).atStartOfDay(), "otro", false));
    }

    // ── Acompañantes y beneficios de socio ─────────────────────────────────

    @Test
    void guardaLosAcompanantesSinSuperarLaCapacidadDelTipo() {
        when(habitacionRepo.findById("h1")).thenReturn(Optional.of(Habitacion.builder().id("h1").numHab("101").precNoche(180000.0)
                .estado(EstadoHabitacion.DISPONIBLE)
                .tipoHabitacion(com.sena.goldenbooking.habitaciones.model.TipoHabitacion.builder().cap(2).build()).build()));
        ReservaHotelDto conUno = dto(enDiezDias, enDiezDias.plusDays(1));
        conUno.setMiembros(List.of(MiembroReserva.builder().nombre("Sofía Pérez").tipoDocumento("TI").numeroDocumento("1023456789").build()));

        assertEquals(1, service.crear(conUno, false, false).getMiembros().size());

        ReservaHotelDto conDos = dto(enDiezDias.plusDays(5), enDiezDias.plusDays(6));
        conDos.setMiembros(List.of(
                MiembroReserva.builder().nombre("Sofía Pérez").tipoDocumento("TI").numeroDocumento("1023456789").build(),
                MiembroReserva.builder().nombre("Juan Pérez").tipoDocumento("CC").numeroDocumento("80123456").build()));
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(conDos, false, false));
    }

    @Test
    void aplicaElDescuentoDeSocioYLoGuarda() {
        when(membresias.beneficiosDe("123")).thenReturn(new BeneficioVigente(TipoMembresia.MIEMBRO, 10, 365));

        ReservaHotelDto creada = service.crear(dto(enDiezDias, enDiezDias.plusDays(2)), false, false);

        assertEquals(324000.0, creada.getPTotal()); // 2 noches x 180.000 - 10 %
        assertEquals(10.0, creada.getDescuento());
    }

    @Test
    void unClienteSinMembresiaNoReservaMasAllaDeSuAnticipacion() {
        when(membresias.beneficiosDe("123")).thenReturn(new BeneficioVigente(TipoMembresia.NINGUNA, 0, 5));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.crear(dto(enDiezDias, enDiezDias.plusDays(1)), false, false));
        assertTrue(ex.getMessage().contains("Los socios pueden reservar con más anticipación"));
        // en recepción (admin) no aplica el límite
        assertEquals(EstadoReserva.PENDIENTE, service.crear(dto(enDiezDias, enDiezDias.plusDays(1)), true, false).getEstado());
    }

    @Test
    void siNoSePuedenLeerLosBeneficiosSeReservaSinEllos() {
        when(membresias.beneficiosDe("123")).thenThrow(new RuntimeException("Mongo caído"));
        assertEquals(180000.0, service.crear(dto(enDiezDias, enDiezDias.plusDays(1)), false, false).getPTotal());
    }

    @Test
    void elDuenoActualizaSusAcompanantesYQuedaEnElHistorial() {
        reservaExistente(EstadoReserva.CONFIRMADA, enDiezDias.atTime(15, 0));

        ReservaHotelDto r = service.actualizarMiembros("rh1",
                List.of(MiembroReserva.builder().nombre("Sofía Pérez").tipoDocumento("TI").numeroDocumento("1023456789").build()), "123", false);

        assertEquals(1, r.getMiembros().size());
        assertEquals(AccionReserva.ACOMPANANTES, r.getHistorial().get(r.getHistorial().size() - 1).getAccion());
        assertThrows(AccesoDenegadoException.class, () -> service.actualizarMiembros("rh1", List.of(), "otro", false));
    }
}
