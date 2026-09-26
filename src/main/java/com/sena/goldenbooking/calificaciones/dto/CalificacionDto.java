package com.sena.goldenbooking.calificaciones.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.sena.goldenbooking.reservas.model.TipoReserva;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Calificación de una reserva finalizada. Para crearla basta con categoria, idReserva, puntuacion y comentario. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Calificación (1 a 5 estrellas) de un espacio o una habitación.")
public class CalificacionDto {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotNull(message = "Indica si es una reserva de HOTEL o DEPORTE.")
    @Schema(example = "DEPORTE")
    private TipoReserva categoria;

    @NotBlank(message = "Indica la reserva que calificas.")
    @Schema(description = "Id de la reserva de hotel o deporte (debe estar FINALIZADA).")
    private String idReserva;

    @Min(value = 1, message = "La calificación va de 1 a 5 estrellas.")
    @Max(value = 5, message = "La calificación va de 1 a 5 estrellas.")
    @Schema(example = "5")
    private int puntuacion;

    @Size(max = 500, message = "El comentario admite máximo 500 caracteres.")
    @Schema(example = "Excelente cancha y muy buena atención.")
    private String comentario;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String idRecurso;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String nombreRecurso;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Nombre del cliente (solo el primer nombre y la inicial).")
    private String nombreCliente;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fecha;
}
