package com.sena.goldenbooking.reportes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Reservas e ingresos de un rango de fechas (por fecha de inicio / check-in). */
@Schema(description = "Reporte de reservas e ingresos por rango de fechas.")
public record ReporteDto(
        LocalDate desde,
        LocalDate hasta,
        LocalDateTime generadoEn,
        ResumenReporteDto resumen,
        List<FilaReporteDto> filas) {
}
