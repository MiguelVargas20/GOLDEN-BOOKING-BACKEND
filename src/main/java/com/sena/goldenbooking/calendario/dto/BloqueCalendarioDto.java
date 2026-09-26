package com.sena.goldenbooking.calendario.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/** Una reserva dibujada en el calendario. */
@Schema(description = "Reserva dentro del calendario semanal.")
public record BloqueCalendarioDto(
        @Schema(description = "Id de la reserva de hotel o deporte.") String idReserva,
        @Schema(description = "Nombre del cliente.") String cliente,
        LocalDateTime inicio,
        LocalDateTime fin,
        @Schema(description = "PENDIENTE, CONFIRMADA o FINALIZADA.") String estado) {
}
