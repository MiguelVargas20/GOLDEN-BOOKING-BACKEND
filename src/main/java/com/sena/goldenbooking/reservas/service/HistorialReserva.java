package com.sena.goldenbooking.reservas.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.reservas.model.AccionReserva;
import com.sena.goldenbooking.reservas.model.EventoReserva;

/**
 * Arma los eventos del historial de una reserva. Quién hizo el cambio se toma
 * del usuario autenticado en la petición; si no hay (tareas programadas como
 * el cierre automático), queda como "sistema".
 */
public final class HistorialReserva {

    public static final String SISTEMA = "SISTEMA";

    private HistorialReserva() {
        // Clase de utilidades: no se instancia
    }

    /** Evento hecho por el usuario de la petición actual. */
    public static EventoReserva evento(AccionReserva accion, String detalle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return delSistema(accion, detalle);
        }
        boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROL_ADMIN".equals(a.getAuthority()));
        return EventoReserva.builder()
                .accion(accion)
                .fecha(ZonaHoraria.ahora())
                .usuario(auth.getName())
                .rol(esAdmin ? "ADMINISTRADOR" : "CLIENTE")
                .detalle(detalle)
                .build();
    }

    /** Evento automático (cierre de reservas, vencimientos). */
    public static EventoReserva delSistema(AccionReserva accion, String detalle) {
        return EventoReserva.builder()
                .accion(accion)
                .fecha(ZonaHoraria.ahora())
                .usuario("sistema")
                .rol(SISTEMA)
                .detalle(detalle)
                .build();
    }

    /** Agrega el evento (las reservas antiguas no tienen historial). */
    public static List<EventoReserva> agregar(List<EventoReserva> historial, EventoReserva evento) {
        List<EventoReserva> lista = historial != null ? new ArrayList<>(historial) : new ArrayList<>();
        lista.add(evento);
        return lista;
    }
}
