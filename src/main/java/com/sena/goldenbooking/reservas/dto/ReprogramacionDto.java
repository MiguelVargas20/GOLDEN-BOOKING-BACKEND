package com.sena.goldenbooking.reservas.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nuevas fechas de una reserva. Deporte: inicio y fin del horario. Hotel: día
 * de check-in y de check-out (la hora se ajusta a 3:00 p. m. y 12:00 m.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Nuevas fechas para reprogramar una reserva.")
public class ReprogramacionDto {

    @NotNull(message = "Indica la nueva fecha de inicio.")
    @Schema(example = "2026-10-15T10:00:00")
    private LocalDateTime inicio;

    @NotNull(message = "Indica la nueva fecha de fin.")
    @Schema(example = "2026-10-15T11:00:00")
    private LocalDateTime fin;
}
