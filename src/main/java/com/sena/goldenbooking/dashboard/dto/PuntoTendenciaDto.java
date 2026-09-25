package com.sena.goldenbooking.dashboard.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reservas recibidas en un día, por tipo.")
public record PuntoTendenciaDto(LocalDate fecha, long deporte, long hotel) {
}
