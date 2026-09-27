package com.sena.goldenbooking.reportes.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/** Una reserva del reporte. */
@Schema(description = "Reserva incluida en el reporte.")
public record FilaReporteDto(
        @Schema(description = "HOTEL o DEPORTE.") String categoria,
        String idReserva,
        String cliente,
        String documento,
        @Schema(description = "Espacio deportivo o \"Habitación 101\".") String lugar,
        LocalDateTime inicio,
        LocalDateTime fin,
        String estado,
        Double total,
        LocalDateTime solicitada,
        boolean registradaEnRecepcion) {
}
