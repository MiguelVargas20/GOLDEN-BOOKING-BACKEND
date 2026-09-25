package com.sena.goldenbooking.dashboard.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento de la agenda del día.")
public record EventoAgendaDto(
        @Schema(description = "DEPORTE, CHECK_IN o CHECK_OUT.") String tipo,
        @Schema(description = "Id de la reserva (deportiva u hotelera).") String idReserva,
        @Schema(description = "Hora del evento (inicio de la reserva, llegada o salida).") LocalDateTime hora,
        @Schema(description = "Hora de fin (solo reservas deportivas).") LocalDateTime fin,
        @Schema(description = "Espacio deportivo o habitación.") String lugar,
        @Schema(description = "Nombre del cliente.") String cliente,
        @Schema(description = "Estado de la reserva.") String estado) {
}
