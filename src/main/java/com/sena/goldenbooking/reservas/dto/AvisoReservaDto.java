package com.sena.goldenbooking.reservas.dto;

import java.time.LocalDateTime;

/**
 * Aviso en vivo para el administrador (WebSocket "/topic/admin/reservas")
 * cuando un cliente crea o cancela una reserva.
 *
 * @param categoria DEPORTE | HOTEL
 * @param accion    NUEVA | CANCELADA | REPROGRAMADA
 */
public record AvisoReservaDto(
        String categoria,
        String accion,
        String idReserva,
        String cliente,
        String lugar,
        LocalDateTime inicio,
        LocalDateTime fin) {
}
