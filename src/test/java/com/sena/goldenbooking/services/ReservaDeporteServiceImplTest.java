package com.sena.goldenbooking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.sena.goldenbooking.config.ZonaHoraria;
import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.mapper.ReservaDeporteMapperImpl;
import com.sena.goldenbooking.models.CanceladaPor;
import com.sena.goldenbooking.models.EspacioDeportivo;
import com.sena.goldenbooking.models.EstadoEspacio;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.EstadoUsuario;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.ReservaDeporte;
import com.sena.goldenbooking.repositories.ReservaDeporteRepository;
import com.sena.goldenbooking.repositories.ReservaRepository;

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

    /** Mañana a las 10:00 (dentro del horario 06:00 - 22:00 del espacio). */
    private final LocalDateTime mananaDiez = ZonaHoraria.ahora().plusDays(1).with(LocalTime.of(10, 0));

    @BeforeEach
    void setUp() {
        reservaDeporteRepo = mock(ReservaDeporteRepository.class);
        reservaRepo = mock(ReservaRepository.class);
        espacioService = mock(EspacioDeportivoService.class);
        usuarioService = mock(UsuarioService.class);
        service = new ReservaDeporteServiceImpl(
                reservaDeporteRepo,
                reservaRepo,
                new ReservaDeporteMapperImpl(),
                mock(SimpMessagingTemplate.class),
                mock(EmailService.class),
                usuarioService,
                espacioService);

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

        assertThrows(com.sena.goldenbooking.exception.AccesoDenegadoException.class,
                () -> service.cancelar("r1", "otro-documento", false, null));
    }
}
