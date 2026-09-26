package com.sena.goldenbooking.reservas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sena.goldenbooking.reservas.model.AccionReserva;
import com.sena.goldenbooking.reservas.model.EventoReserva;

/** Quién queda registrado en el historial de una reserva. */
class HistorialReservaTest {

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static void autenticar(String usuario, String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority(rol))));
    }

    @Test
    void registraAlAdministradorQueHizoElCambio() {
        autenticar("admin", "ROL_ADMIN");

        EventoReserva e = HistorialReserva.evento(AccionReserva.CONFIRMADA, null);

        assertEquals("admin", e.getUsuario());
        assertEquals("ADMINISTRADOR", e.getRol());
        assertEquals(AccionReserva.CONFIRMADA, e.getAccion());
    }

    @Test
    void registraAlCliente() {
        autenticar("laura", "ROL_CLIENTE");

        EventoReserva e = HistorialReserva.evento(AccionReserva.CANCELADA, "Cambio de planes");

        assertEquals("laura", e.getUsuario());
        assertEquals("CLIENTE", e.getRol());
        assertEquals("Cambio de planes", e.getDetalle());
    }

    @Test
    void sinUsuarioAutenticadoQuedaComoSistema() {
        EventoReserva e = HistorialReserva.evento(AccionReserva.FINALIZADA, null);

        assertEquals("sistema", e.getUsuario());
        assertEquals(HistorialReserva.SISTEMA, e.getRol());
    }

    @Test
    void agregaAlFinalAunqueLaReservaAntiguaNoTengaHistorial() {
        List<EventoReserva> lista = HistorialReserva.agregar(null, HistorialReserva.delSistema(AccionReserva.CREADA, null));
        lista = HistorialReserva.agregar(lista, HistorialReserva.delSistema(AccionReserva.CONFIRMADA, null));

        assertEquals(2, lista.size());
        assertEquals(AccionReserva.CONFIRMADA, lista.get(1).getAccion());
    }
}
