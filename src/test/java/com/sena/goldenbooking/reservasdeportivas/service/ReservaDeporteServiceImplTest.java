package com.sena.goldenbooking.reservasdeportivas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.email.EmailService;
import com.sena.goldenbooking.compartido.exception.AccesoDenegadoException;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.membresias.dto.BeneficioVigente;
import com.sena.goldenbooking.membresias.service.MembresiaService;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.service.NotificacionService;
import com.sena.goldenbooking.reservas.model.AccionReserva;
import com.sena.goldenbooking.reservas.model.CanceladaPor;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.Reserva;
import com.sena.goldenbooking.reservas.repository.ReservaRepository;
import com.sena.goldenbooking.reservas.service.AvisosAdminService;
import com.sena.goldenbooking.reservasdeportivas.dto.ReservaDeporteDto;
import com.sena.goldenbooking.reservasdeportivas.mapper.ReservaDeporteMapperImpl;
import com.sena.goldenbooking.reservasdeportivas.model.EspacioDeportivo;
import com.sena.goldenbooking.reservasdeportivas.model.EstadoEspacio;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

/**
 * Pruebas unitarias (sin Spring ni Mongo) de la creación de reservas deportivas
 * y de su flujo de aprobación (PENDIENTE → CONFIRMADA / CANCELADA).
 */
class ReservaDeporteServiceImplTest {

    private ReservaDeporteRepository reservaDeporteRepo;
    private ReservaRepository reservaRepo;
    private EspacioDeportivoService espacioService;
    private UsuarioService usuarioService;
    private ReservaDeporteServiceImpl service;
    private AvisosAdminService avisosAdmin;
    private NotificacionService notificaciones;
    private MembresiaService membresias;

    /** Mañana a las 10:00 (dentro del horario 06:00 - 22:00 del espacio). */
    private final LocalDateTime mananaDiez = ZonaHoraria.ahora().plusDays(1).with(LocalTime.of(10, 0));

    @BeforeEach
    void setUp() {
        reservaDeporteRepo = mock(ReservaDeporteRepository.class);
        reservaRepo = mock(ReservaRepository.class);
        espacioService = mock(EspacioDeportivoService.class);
        usuarioService = mock(UsuarioService.class);
        avisosAdmin = mock(AvisosAdminService.class);
        notificaciones = mock(NotificacionService.class);
        membresias = mock(MembresiaService.class);
        when(membresias.beneficiosDe(anyString())).thenReturn(BeneficioVigente.sinBeneficios(365));
        service = new ReservaDeporteServiceImpl(
                reservaDeporteRepo,
                reservaRepo,
                new ReservaDeporteMapperImpl(),
                mock(SimpMessagingTemplate.class),
                mock(EmailService.class),
                usuarioService,
                espacioService,
                avisosAdmin,
                notificaciones,
                membresias);

        when(espacioService.obtenerReservable("e1")).thenReturn(EspacioDeportivo.builder()
                .id("e1").nombre("Cancha 1").tarifaHora(50000.0)
                .horaApertura(LocalTime.of(6, 0)).horaCierre(LocalTime.of(22, 0))
                .estado(EstadoEspacio.ACTIVO).build());
        when(reservaDeporteRepo.findSolapadasEnEspacio(anyString(), any(), any())).thenReturn(List.of());
        when(usuarioService.obtenerPorDocNum("123")).thenReturn(
                UsuarioDto.builder().nombre("Ana").email("ana@test.com").estado(EstadoUsuario.ACTIVO).build());
        when(reservaRepo.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaDeporteRepo.save(any(ReservaDeporte.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ReservaDeporteDto dto(LocalDateTime inicio, LocalDateTime fin) {
        return ReservaDeporteDto.builder()
                .docUsuario("123")
                .espacioId("e1")
                .fInicioReserva(inicio)
                .fFinReserva(fin)
                .build();
    }

    private void reservaExistente(EstadoReserva estado, LocalDateTime inicio) {
        when(reservaDeporteRepo.findById("r1")).thenReturn(Optional.of(ReservaDeporte.builder()
                .idReservaDeporte("r1").docUsuario("123").espacioId("e1").tipoCancha("Cancha 1")
                .fechaReserva(inicio).fechaFinReserva(inicio.plusHours(1)).estado(estado).build()));
    }

    // ── Crear ──────────────────────────────────────────────────────────────

    @Test
    void creaLaReservaPendienteConElNombreYLaTarifaDelEspacio() {
        ReservaDeporteDto creada = service.crear(dto(mananaDiez, mananaDiez.plusMinutes(90)));

        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());
        assertEquals("Cancha 1", creada.getTCancha());
        assertEquals("e1", creada.getEspacioId());
        assertEquals(75000.0, creada.getPr()); // 1h30 a 50.000/h
        assertNotNull(creada.getFechaSolicitud());
        // el admin recibe el aviso en vivo de la nueva solicitud
        verify(avisosAdmin).nuevaReserva(org.mockito.ArgumentMatchers.eq("DEPORTE"), any(), org.mockito.ArgumentMatchers.eq("123"),
                org.mockito.ArgumentMatchers.eq("Cancha 1"), any(), any());
    }

    @Test
    void rechazaReservaConInicioEnElPasado() {
        LocalDateTime ayer = mananaDiez.minusDays(2);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.crear(dto(ayer, ayer.plusHours(2))));

        assertEquals("La fecha de inicio no puede estar en el pasado.", ex.getMessage());
        verify(reservaRepo, never()).save(any());
    }

    @Test
    void rechazaReservaDeMenosDeUnaHora() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.crear(dto(mananaDiez, mananaDiez.plusMinutes(30))));

        assertEquals("La reserva debe durar al menos una hora.", ex.getMessage());
    }

    @Test
    void rechazaReservaFueraDelHorarioDelEspacio() {
        LocalDateTime noche = mananaDiez.with(LocalTime.of(21, 30));

        assertThrows(SolicitudInvalidaException.class, () -> service.crear(dto(noche, noche.plusHours(1))));
    }

    @Test
    void rechazaHorarioOcupado() {
        when(reservaDeporteRepo.findSolapadasEnEspacio(anyString(), any(), any()))
                .thenReturn(List.of(new ReservaDeporte()));

        assertThrows(ConflictoDeNegocioException.class,
                () -> service.crear(dto(mananaDiez, mananaDiez.plusHours(1))));
    }

    // ── Reserva registrada por el admin (recepción) ────────────────────────

    @Test
    void adminPuedeRegistrarlaYaConfirmada() {
        ReservaDeporteDto creada = service.crear(dto(mananaDiez, mananaDiez.plusHours(1)), true, true);

        assertEquals(EstadoReserva.CONFIRMADA, creada.getEstado());
        assertEquals(true, creada.isRegistradaPorAdministrador());
        assertNotNull(creada.getFechaConfirmacion());
        // la registró el propio admin: no hace falta avisarle
        verify(avisosAdmin, never()).nuevaReserva(any(), any(), any(), any(), any(), any());
    }

    @Test
    void siNoLaConfirmaQuedaPendienteAunqueLaRegistreElAdmin() {
        ReservaDeporteDto creada = service.crear(dto(mananaDiez, mananaDiez.plusHours(1)), true, false);

        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());
    }

    @Test
    void rechazaDocumentoQueNoPerteneceANingunCliente() {
        when(usuarioService.obtenerPorDocNum("123")).thenThrow(new RecursoNoEncontradoException("no existe"));

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crear(dto(mananaDiez, mananaDiez.plusHours(1)), true, true));
        verify(reservaRepo, never()).save(any());
    }

    @Test
    void rechazaClienteInactivo() {
        when(usuarioService.obtenerPorDocNum("123")).thenReturn(
                UsuarioDto.builder().nombre("Ana").estado(EstadoUsuario.INACTIVO).build());

        assertThrows(ConflictoDeNegocioException.class,
                () -> service.crear(dto(mananaDiez, mananaDiez.plusHours(1)), true, false));
    }

    // ── Aprobar / cancelar ─────────────────────────────────────────────────

    @Test
    void adminApruebaUnaReservaPendiente() {
        reservaExistente(EstadoReserva.PENDIENTE, mananaDiez);

        ReservaDeporteDto confirmada = service.confirmar("r1");

        assertEquals(EstadoReserva.CONFIRMADA, confirmada.getEstado());
        assertNotNull(confirmada.getFechaConfirmacion());
    }

    @Test
    void noSeConfirmaUnaReservaCancelada() {
        reservaExistente(EstadoReserva.CANCELADA, mananaDiez);
        assertThrows(ConflictoDeNegocioException.class, () -> service.confirmar("r1"));
    }

    @Test
    void adminDebeIndicarMotivoParaCancelar() {
        reservaExistente(EstadoReserva.PENDIENTE, mananaDiez);

        assertThrows(SolicitudInvalidaException.class, () -> service.cancelar("r1", "999", true, "  "));
        verify(reservaDeporteRepo, never()).save(any());
    }

    @Test
    void adminCancelaConMotivoYQuedaRegistrado() {
        reservaExistente(EstadoReserva.CONFIRMADA, mananaDiez);

        ReservaDeporteDto cancelada = service.cancelar("r1", "999", true, " Cancha en mantenimiento ");

        ArgumentCaptor<ReservaDeporte> captor = ArgumentCaptor.forClass(ReservaDeporte.class);
        verify(reservaDeporteRepo).save(captor.capture());
        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        assertEquals(CanceladaPor.ADMINISTRADOR, captor.getValue().getCanceladaPor());
        assertEquals("Cancha en mantenimiento", captor.getValue().getMotivoCancelacion());
    }

    @Test
    void clienteNoCancelaConMenosDe24Horas() {
        reservaExistente(EstadoReserva.PENDIENTE, ZonaHoraria.ahora().plusHours(3));

        assertThrows(ConflictoDeNegocioException.class, () -> service.cancelar("r1", "123", false, null));
    }

    @Test
    void clienteNoCancelaReservasAjenas() {
        reservaExistente(EstadoReserva.PENDIENTE, mananaDiez.plusDays(5));

        assertThrows(AccesoDenegadoException.class,
                () -> service.cancelar("r1", "otro-documento", false, null));
    }

    // ── Historial y notificaciones ─────────────────────────────────────────

    @Test
    void alCrearQuedaElPrimerEventoDelHistorial() {
        ReservaDeporteDto creada = service.crear(dto(mananaDiez, mananaDiez.plusHours(1)), true, false);

        assertEquals(1, creada.getHistorial().size());
        assertEquals(AccionReserva.CREADA, creada.getHistorial().get(0).getAccion());
        assertEquals("Registrada en recepción", creada.getHistorial().get(0).getDetalle());
    }

    @Test
    void alAprobarSeRegistraEnElHistorialYSeNotificaAlCliente() {
        reservaExistente(EstadoReserva.PENDIENTE, mananaDiez);

        ReservaDeporteDto confirmada = service.confirmar("r1");

        assertEquals(AccionReserva.CONFIRMADA, confirmada.getHistorial().get(0).getAccion());
        assertNotNull(confirmada.getHistorial().get(0).getFecha());
        verify(notificaciones).notificar(eq("123"), eq(TipoNotificacion.RESERVA_APROBADA), any(), eq("r1"), eq("Reserva aprobada"), anyString());
    }

    @Test
    void siCancelaElAdminSeNotificaAlClienteConElMotivo() {
        reservaExistente(EstadoReserva.CONFIRMADA, mananaDiez);

        ReservaDeporteDto cancelada = service.cancelar("r1", "999", true, "Cancha en mantenimiento");

        assertEquals("Cancha en mantenimiento", cancelada.getHistorial().get(0).getDetalle());
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificaciones).notificar(eq("123"), eq(TipoNotificacion.RESERVA_CANCELADA), any(), eq("r1"), anyString(), mensaje.capture());
        assertEquals(true, mensaje.getValue().contains("Cancha en mantenimiento"));
    }

    @Test
    void siCancelaElClienteNoSeLeNotificaASiMismo() {
        reservaExistente(EstadoReserva.PENDIENTE, mananaDiez.plusDays(3));

        service.cancelar("r1", "123", false, null);

        verify(notificaciones, never()).notificar(any(), any(), any(), any(), any(), any());
    }

    // ── Reprogramar ────────────────────────────────────────────────────────

    @Test
    void reprogramaRecalculaElPrecioYGuardaElHorarioAnterior() {
        reservaExistente(EstadoReserva.PENDIENTE, mananaDiez.plusDays(3));
        LocalDateTime nuevo = mananaDiez.plusDays(4).with(LocalTime.of(15, 0));

        ReservaDeporteDto r = service.reprogramar("r1", nuevo, nuevo.plusHours(2), "123", false);

        assertEquals(nuevo, r.getFInicioReserva());
        assertEquals(nuevo.plusHours(2), r.getFFinReserva());
        assertEquals(100000.0, r.getPr()); // 2 h a 50.000/h
        assertEquals(EstadoReserva.PENDIENTE, r.getEstado());
        assertEquals(AccionReserva.REPROGRAMADA, r.getHistorial().get(0).getAccion());
        assertEquals(true, r.getHistorial().get(0).getDetalle().startsWith("Horario anterior:"));
        verify(avisosAdmin).reservaReprogramadaPorCliente(eq("DEPORTE"), eq("r1"), eq("123"), anyString(), eq(nuevo), any());
    }

    @Test
    void siElClienteReprogramaUnaConfirmadaVuelveAPendiente() {
        reservaExistente(EstadoReserva.CONFIRMADA, mananaDiez.plusDays(3));
        LocalDateTime nuevo = mananaDiez.plusDays(5);

        ReservaDeporteDto r = service.reprogramar("r1", nuevo, nuevo.plusHours(1), "123", false);

        assertEquals(EstadoReserva.PENDIENTE, r.getEstado());
        assertNull(r.getFechaConfirmacion());
    }

    @Test
    void siReprogramaElAdminSigueConfirmadaYSeNotificaAlCliente() {
        reservaExistente(EstadoReserva.CONFIRMADA, ZonaHoraria.ahora().plusHours(2)); // el admin no tiene límite de 24 h
        LocalDateTime nuevo = mananaDiez.plusDays(2);

        ReservaDeporteDto r = service.reprogramar("r1", nuevo, nuevo.plusHours(1), "999", true);

        assertEquals(EstadoReserva.CONFIRMADA, r.getEstado());
        verify(notificaciones).notificar(eq("123"), eq(TipoNotificacion.RESERVA_REPROGRAMADA), any(), eq("r1"), anyString(), anyString());
    }

    @Test
    void reprogramarNoChocaConsigoMismaPeroSiConOtraReserva() {
        reservaExistente(EstadoReserva.PENDIENTE, mananaDiez.plusDays(3));
        LocalDateTime nuevo = mananaDiez.plusDays(3).plusMinutes(30);
        // la única reserva "solapada" es ella misma: se permite correrla media hora
        when(reservaDeporteRepo.findSolapadasEnEspacio(anyString(), any(), any()))
                .thenReturn(List.of(ReservaDeporte.builder().idReservaDeporte("r1").build()));
        assertEquals(nuevo, service.reprogramar("r1", nuevo, nuevo.plusHours(1), "123", false).getFInicioReserva());

        reservaExistente(EstadoReserva.PENDIENTE, mananaDiez.plusDays(3));
        when(reservaDeporteRepo.findSolapadasEnEspacio(anyString(), any(), any()))
                .thenReturn(List.of(ReservaDeporte.builder().idReservaDeporte("otra").build()));
        assertThrows(ConflictoDeNegocioException.class,
                () -> service.reprogramar("r1", nuevo, nuevo.plusHours(1), "123", false));
    }

    @Test
    void reprogramarRespetaLasReglasDeHorarioYEstado() {
        reservaExistente(EstadoReserva.PENDIENTE, mananaDiez.plusDays(3));
        LocalDateTime noche = mananaDiez.plusDays(4).with(LocalTime.of(21, 30));
        assertThrows(SolicitudInvalidaException.class, () -> service.reprogramar("r1", noche, noche.plusHours(1), "123", false));
        assertThrows(SolicitudInvalidaException.class,
                () -> service.reprogramar("r1", mananaDiez.plusDays(3), mananaDiez.plusDays(3).plusHours(1), "123", false));

        reservaExistente(EstadoReserva.CANCELADA, mananaDiez.plusDays(3));
        assertThrows(ConflictoDeNegocioException.class,
                () -> service.reprogramar("r1", mananaDiez.plusDays(5), mananaDiez.plusDays(5).plusHours(1), "123", false));
    }

    @Test
    void elClienteNoReprogramaConMenosDe24HorasNiReservasAjenas() {
        reservaExistente(EstadoReserva.PENDIENTE, ZonaHoraria.ahora().plusHours(3));
        assertThrows(ConflictoDeNegocioException.class,
                () -> service.reprogramar("r1", mananaDiez.plusDays(5), mananaDiez.plusDays(5).plusHours(1), "123", false));

        reservaExistente(EstadoReserva.PENDIENTE, mananaDiez.plusDays(3));
        assertThrows(AccesoDenegadoException.class,
                () -> service.reprogramar("r1", mananaDiez.plusDays(5), mananaDiez.plusDays(5).plusHours(1), "otro", false));
    }
}
