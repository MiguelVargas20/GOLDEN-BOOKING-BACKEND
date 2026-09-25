package com.sena.goldenbooking.reservas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sena.goldenbooking.compartido.email.EmailService;
import com.sena.goldenbooking.reservas.model.CanceladaPor;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.Reserva;
import com.sena.goldenbooking.reservas.repository.ReservaRepository;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

/** Pruebas del cierre automático: finalizar confirmadas y vencer pendientes. */
class CierreReservasServiceTest {

    private ReservaDeporteRepository reservaDeporteRepo;
    private ReservaHotelRepository reservaHotelRepo;
    private ReservaRepository reservaRepo;
    private EmailService emailService;
    private CierreReservasService service;

    private final LocalDateTime ahora = LocalDateTime.of(2026, 9, 25, 16, 0);

    @BeforeEach
    void setUp() {
        reservaDeporteRepo = mock(ReservaDeporteRepository.class);
        reservaHotelRepo = mock(ReservaHotelRepository.class);
        reservaRepo = mock(ReservaRepository.class);
        emailService = mock(EmailService.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        service = new CierreReservasService(reservaDeporteRepo, reservaHotelRepo, reservaRepo, emailService, usuarioService);

        when(usuarioService.obtenerMapaPorDocNums(any())).thenReturn(
                Map.of("1", UsuarioDto.builder().nombre("Ana").email("ana@test.com").build()));
        when(reservaDeporteRepo.findByEstadoAndFechaFinReservaBefore(any(), any())).thenReturn(List.of());
        when(reservaHotelRepo.findByEstadoAndFechaCheckOutBefore(any(), any())).thenReturn(List.of());
        when(reservaDeporteRepo.findByEstadoAndFechaReservaBefore(any(), any())).thenReturn(List.of());
        when(reservaHotelRepo.findByEstadoAndFechaCheckInBefore(any(), any())).thenReturn(List.of());
    }

    @Test
    void finalizaConfirmadasTerminadasYSincronizaElPadre() {
        Reserva padre = Reserva.builder().id("p1").estado(EstadoReserva.CONFIRMADA).build();
        when(reservaRepo.findById("p1")).thenReturn(Optional.of(padre));
        ReservaDeporte rd = ReservaDeporte.builder().idReserva("p1").estado(EstadoReserva.CONFIRMADA).build();
        // reserva antigua sin padre: no debe romper el job
        ReservaHotel rh = ReservaHotel.builder().idReserva(null).estado(EstadoReserva.CONFIRMADA).build();
        when(reservaDeporteRepo.findByEstadoAndFechaFinReservaBefore(EstadoReserva.CONFIRMADA, ahora)).thenReturn(List.of(rd));
        when(reservaHotelRepo.findByEstadoAndFechaCheckOutBefore(EstadoReserva.CONFIRMADA, ahora)).thenReturn(List.of(rh));

        assertEquals(2, service.finalizarConfirmadas(ahora));
        assertEquals(EstadoReserva.FINALIZADA, rd.getEstado());
        assertEquals(EstadoReserva.FINALIZADA, rh.getEstado());
        assertEquals(EstadoReserva.FINALIZADA, padre.getEstado());
        verify(reservaRepo, never()).findById(null);
    }

    @Test
    void vencePendientesConMotivoYAvisaAlCliente() {
        ReservaDeporte rd = ReservaDeporte.builder().docUsuario("1").tipoCancha("Cancha 1")
                .fechaReserva(ahora.minusHours(1)).fechaFinReserva(ahora).estado(EstadoReserva.PENDIENTE).build();
        ReservaHotel rh = ReservaHotel.builder().docUsuario("1")
                .fechaCheckIn(ahora.minusDays(1)).fechaCheckOut(ahora.plusDays(1)).estado(EstadoReserva.PENDIENTE).build();
        when(reservaDeporteRepo.findByEstadoAndFechaReservaBefore(EstadoReserva.PENDIENTE, ahora)).thenReturn(List.of(rd));
        // hotel: margen hasta que termine el día del check-in
        when(reservaHotelRepo.findByEstadoAndFechaCheckInBefore(EstadoReserva.PENDIENTE, ahora.toLocalDate().atStartOfDay()))
                .thenReturn(List.of(rh));

        assertEquals(2, service.vencerPendientes(ahora));

        assertEquals(EstadoReserva.CANCELADA, rd.getEstado());
        assertEquals(EstadoReserva.CANCELADA, rh.getEstado());
        assertEquals(CanceladaPor.SISTEMA, rd.getCanceladaPor());
        assertEquals(CanceladaPor.SISTEMA, rh.getCanceladaPor());
        assertEquals(CierreReservasService.MOTIVO_VENCIDA, rh.getMotivoCancelacion());
        assertEquals(ahora, rd.getFechaCancelacion());
        verify(emailService, times(2)).enviarCorreoHtml(eq("ana@test.com"), anyString(), anyString());
    }

    @Test
    void unCorreoFallidoNoFrenaElCierreDelResto() {
        ReservaDeporte a = ReservaDeporte.builder().docUsuario("1").tipoCancha("A")
                .fechaReserva(ahora.minusHours(2)).estado(EstadoReserva.PENDIENTE).build();
        ReservaDeporte b = ReservaDeporte.builder().docUsuario("1").tipoCancha("B")
                .fechaReserva(ahora.minusHours(1)).estado(EstadoReserva.PENDIENTE).build();
        when(reservaDeporteRepo.findByEstadoAndFechaReservaBefore(any(), any())).thenReturn(List.of(a, b));
        doThrow(new RuntimeException("SMTP caído")).when(emailService).enviarCorreoHtml(anyString(), anyString(), anyString());

        assertEquals(2, service.vencerPendientes(ahora));
        assertEquals(EstadoReserva.CANCELADA, b.getEstado());
    }
}
