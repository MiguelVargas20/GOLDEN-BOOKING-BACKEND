package com.sena.goldenbooking.reservas.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Persona que acompaña al titular en una reserva (familiar, amigo, menor de edad). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Acompañante asociado a la reserva.")
public class MiembroReserva {

    @NotBlank(message = "El nombre del acompañante es obligatorio.")
    @Size(min = 2, max = 80, message = "El nombre del acompañante debe tener entre 2 y 80 caracteres.")
    @Schema(example = "Sofía Pérez")
    private String nombre;

    @NotBlank(message = "El tipo de documento del acompañante es obligatorio.")
    @Pattern(regexp = "CC|TI|CE|PA|RC", message = "Tipo de documento no válido (CC, TI, CE, PA o RC).")
    @Schema(description = "CC cédula, TI tarjeta de identidad, CE cédula de extranjería, PA pasaporte, RC registro civil.", example = "TI")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento del acompañante es obligatorio.")
    @Pattern(regexp = "^[A-Za-z0-9]{5,15}$", message = "El documento del acompañante debe tener entre 5 y 15 letras o números.")
    @Schema(example = "1023456789")
    private String numeroDocumento;
}
