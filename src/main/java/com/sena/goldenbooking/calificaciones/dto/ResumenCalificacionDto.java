package com.sena.goldenbooking.calificaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Promedio de estrellas de un espacio o habitación. */
@Schema(description = "Promedio de calificaciones de un espacio o habitación.")
public record ResumenCalificacionDto(
        @Schema(description = "Id del espacio deportivo o de la habitación.") String idRecurso,
        @Schema(description = "Promedio de 1 a 5, con un decimal.", example = "4.5") double promedio,
        @Schema(description = "Cuántas calificaciones tiene.", example = "12") long total) {
}
