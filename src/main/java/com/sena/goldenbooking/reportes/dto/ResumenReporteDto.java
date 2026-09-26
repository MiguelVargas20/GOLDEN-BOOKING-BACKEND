package com.sena.goldenbooking.reportes.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/** Totales del reporte. Los ingresos cuentan solo reservas CONFIRMADAS y FINALIZADAS (igual que el dashboard). */
@Schema(description = "Totales del reporte.")
public record ResumenReporteDto(
        long totalReservas,
        long reservasDeporte,
        long reservasHotel,
        @Schema(description = "Cantidad por estado (PENDIENTE, CONFIRMADA, CANCELADA, FINALIZADA).") Map<String, Long> porEstado,
        @Schema(description = "Ingresos de reservas confirmadas y finalizadas.") double ingresos,
        double ingresosDeporte,
        double ingresosHotel) {
}
