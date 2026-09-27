package com.sena.goldenbooking.calendario.dto;

import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Un espacio deportivo o una habitación, con sus reservas de la semana. */
@Schema(description = "Espacio o habitación con sus reservas de la semana.")
public record FilaCalendarioDto(
        String id,
        @Schema(description = "Nombre del espacio o \"Habitación 101\".") String nombre,
        @Schema(description = "Deporte o tipo de habitación.") String detalle,
        @Schema(description = "Estado del espacio o habitación (ACTIVO, MANTENIMIENTO, DISPONIBLE...).") String estado,
        @Schema(description = "Solo espacios deportivos.") LocalTime horaApertura,
        @Schema(description = "Solo espacios deportivos.") LocalTime horaCierre,
        List<BloqueCalendarioDto> reservas) {
}
