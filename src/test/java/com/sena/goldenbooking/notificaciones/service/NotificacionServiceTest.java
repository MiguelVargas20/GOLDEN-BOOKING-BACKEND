package com.sena.goldenbooking.notificaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sena.goldenbooking.compartido.exception.AccesoDenegadoException;
import com.sena.goldenbooking.notificaciones.model.Notificacion;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.repository.NotificacionRepository;
import com.sena.goldenbooking.reservas.model.TipoReserva;

class NotificacionServiceTest {

    private NotificacionRepository repo;
    private NotificacionService service;

    @BeforeEach
    void setUp() {
        repo = mock(NotificacionRepository.class);
        service = new NotificacionService(repo);
        when(repo.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void creaLaNotificacionSinLeer() {
        service.notificar("123", TipoNotificacion.RESERVA_APROBADA, TipoReserva.HOTEL, "rh1", "Reserva aprobada", "Tu reserva...");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(captor.capture());
        assertEquals("123", captor.getValue().getDocUsuario());
        assertEquals("rh1", captor.getValue().getIdReserva());
        assertFalse(captor.getValue().isLeida());
    }

    @Test
    void unErrorAlGuardarNoRompeLaOperacionQueLaOrigino() {
        when(repo.save(any(Notificacion.class))).thenThrow(new RuntimeException("Mongo caído"));

        service.notificar("123", TipoNotificacion.CALIFICAR, TipoReserva.DEPORTE, "r1", "t", "m");
        // no lanza excepción
    }

    @Test
    void sinDocumentoNoSeGuardaNada() {
        service.notificar(null, TipoNotificacion.CALIFICAR, TipoReserva.DEPORTE, "r1", "t", "m");
        verify(repo, never()).save(any());
    }

    @Test
    void marcaComoLeidaSoloLasPropias() {
        when(repo.findById("n1")).thenReturn(Optional.of(Notificacion.builder().id("n1").docUsuario("123").build()));

        assertTrue(service.marcarLeida("n1", "123").isLeida());
        assertThrows(AccesoDenegadoException.class, () -> service.marcarLeida("n1", "otro"));
    }

    @Test
    void marcaTodasLasPendientesDelCliente() {
        Notificacion a = Notificacion.builder().id("a").docUsuario("123").build();
        Notificacion b = Notificacion.builder().id("b").docUsuario("123").build();
        when(repo.findByDocUsuarioAndLeidaFalse("123")).thenReturn(List.of(a, b));

        assertEquals(2, service.marcarTodasLeidas("123"));
        assertTrue(a.isLeida() && b.isLeida());
    }
}
