package com.sena.goldenbooking.dashboard.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reserva que espera aprobación del administrador.")
public record ReservaPendienteDto(
        @Schema(description = "DEPORTE u HOTEL.") String tipo,
        @Schema(description = "Id de la reserva (para aprobarla o cancelarla).") String idReserva,
        @Schema(description = "Espacio deportivo o habitación.") String lugar,
        String cliente,
        LocalDateTime inicio,
        LocalDateTime fin,
        Double total,
        @Schema(description = "Cuándo la solicitó el cliente (puede ser null en reservas antiguas).") LocalDateTime fechaSolicitud) {
}
