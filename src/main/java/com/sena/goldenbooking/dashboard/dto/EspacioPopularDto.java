package com.sena.goldenbooking.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Espacio deportivo y cuánto se reservó en el periodo.")
public record EspacioPopularDto(String espacioId, String nombre, long reservas, double horas) {
}
