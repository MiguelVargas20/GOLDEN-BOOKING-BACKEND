package com.sena.goldenbooking.dashboard.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de una habitación en el día de hoy.")
public record HabitacionHoyDto(
        String id,
        @Schema(example = "101") String numero,
        @Schema(example = "Suite") String tipo,
        @Schema(description = "DISPONIBLE, OCUPADA, RESERVADA_PENDIENTE o MANTENIMIENTO.") String estadoHoy,
        @Schema(description = "Huésped actual (si está ocupada o reservada).") String huesped,
        @Schema(description = "Check-out del huésped actual.") LocalDateTime hasta) {
}
