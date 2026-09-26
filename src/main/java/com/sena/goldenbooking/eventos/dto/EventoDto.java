package com.sena.goldenbooking.eventos.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.sena.goldenbooking.eventos.model.CategoriaEvento;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Evento del club.")
public class EventoDto {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @NotBlank(message = "El título es obligatorio.")
    @Size(min = 3, max = 80, message = "El título debe tener entre 3 y 80 caracteres.")
    @Schema(example = "Noche de salsa")
    private String titulo;

    @Size(max = 1000, message = "La descripción admite máximo 1000 caracteres.")
    @Schema(example = "Clase gratuita a las 7 p. m. y baile con orquesta en vivo.")
    private String descripcion;

    @NotNull(message = "Elige la categoría del evento.")
    private CategoriaEvento categoria;

    @NotNull(message = "Indica cuándo empieza el evento.")
    @Schema(example = "2026-10-17T19:00:00")
    private LocalDateTime fechaInicio;

    @NotNull(message = "Indica cuándo termina el evento.")
    @Schema(example = "2026-10-17T23:00:00")
    private LocalDateTime fechaFin;

    @NotBlank(message = "Indica el lugar del evento.")
    @Size(max = 80, message = "El lugar admite máximo 80 caracteres.")
    @Schema(example = "Salón principal")
    private String lugar;

    @Min(value = 1, message = "El cupo mínimo es 1 persona.")
    @Max(value = 100_000, message = "El cupo es demasiado alto.")
    @Schema(description = "Cupos (vacío = sin límite).", example = "120")
    private Integer cupo;

    @PositiveOrZero(message = "El precio no puede ser negativo.")
    @Schema(description = "Valor de la entrada (0 o vacío = gratis).", example = "0")
    private Double precio;

    @Schema(description = "false = borrador (los clientes no lo ven).", example = "true")
    private boolean publicado;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String imagenUrl;

    @Schema(description = "Creado en los últimos 7 días (se marca como Nuevo).", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean nuevo;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaCreacion;
}
