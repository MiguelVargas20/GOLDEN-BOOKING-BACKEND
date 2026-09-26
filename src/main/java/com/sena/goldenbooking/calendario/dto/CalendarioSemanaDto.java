package com.sena.goldenbooking.calendario.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Ocupación de una semana (7 días desde "desde") de todos los espacios y habitaciones. */
@Schema(description = "Ocupación semanal de espacios deportivos y habitaciones.")
public record CalendarioSemanaDto(
        LocalDate desde,
        @Schema(description = "Último día incluido (desde + 6).") LocalDate hasta,
        List<FilaCalendarioDto> espacios,
        List<FilaCalendarioDto> habitaciones) {
}
