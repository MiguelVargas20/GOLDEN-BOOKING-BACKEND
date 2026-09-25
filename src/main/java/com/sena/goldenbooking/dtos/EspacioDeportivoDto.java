package com.sena.goldenbooking.dtos;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sena.goldenbooking.models.EstadoEspacio;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Espacio deportivo reservable (cancha, piscina, pista...).")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspacioDeportivoDto {

    // Letras (con tildes), números, espacios y guiones: el nombre se muestra en
    // el panel admin y en los correos, así que no se aceptan caracteres HTML.
    private static final String PATRON_TEXTO = "^[\\p{L}0-9 \\-]+$";

    @Schema(description = "Identificador.", accessMode = Schema.AccessMode.READ_ONLY, example = "66f1c2a9e4b0a1b2c3d4e5f6")
    private String id;

    @Schema(description = "Nombre único del espacio.", example = "Cancha de Fútbol 1")
    @NotBlank(message = "El nombre del espacio es obligatorio.")
    @Size(max = 60, message = "El nombre no puede superar 60 caracteres.")
    @Pattern(regexp = PATRON_TEXTO, message = "El nombre solo puede tener letras, números, espacios y guiones.")
    private String nombre;

    @Schema(description = "Deporte o categoría. Define la imagen por defecto si no se sube una.", example = "Fútbol")
    @NotBlank(message = "El deporte es obligatorio.")
    @Size(max = 40, message = "El deporte no puede superar 40 caracteres.")
    @Pattern(regexp = PATRON_TEXTO, message = "El deporte solo puede tener letras, números, espacios y guiones.")
    private String deporte;

    @Schema(description = "Descripción visible en el catálogo.", example = "Cancha sintética con iluminación nocturna.")
    @Size(max = 500, message = "La descripción no puede superar 500 caracteres.")
    private String descripcion;

    @Schema(description = "Capacidad máxima de personas.", example = "22")
    @NotNull(message = "La capacidad es obligatoria.")
    @Min(value = 1, message = "La capacidad debe ser al menos 1.")
    @Max(value = 1000, message = "La capacidad no puede superar 1000.")
    private Integer capacidad;

    @Schema(description = "Tarifa por hora en pesos colombianos.", example = "50000")
    @NotNull(message = "La tarifa por hora es obligatoria.")
    @Positive(message = "La tarifa por hora debe ser mayor a cero.")
    private Double tarifaHora;

    @Schema(description = "Hora de apertura (HH:mm, hora de Colombia).", type = "string", example = "06:00")
    @NotNull(message = "La hora de apertura es obligatoria.")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaApertura;

    @Schema(description = "Hora de cierre (HH:mm, hora de Colombia).", type = "string", example = "22:00")
    @NotNull(message = "La hora de cierre es obligatoria.")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaCierre;

    @Schema(description = "Estado. Si no se envía al crear, queda ACTIVO.", example = "ACTIVO")
    private EstadoEspacio estado;

    @Schema(description = "URL de la imagen subida (null si usa la imagen por defecto del deporte).",
            accessMode = Schema.AccessMode.READ_ONLY, example = "/api/espacios-deportivos/66f1.../imagen")
    private String imagenUrl;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaCreacion;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaActualizacion;
}
