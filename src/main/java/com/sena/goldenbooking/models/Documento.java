package com.sena.goldenbooking.models;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clase embebida que representa un documento de identidad.
 * Se usa como campo anidado en modelos principales (no es una colección propia).
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Documento {
    @NotBlank(message = "El tipo de documento es obligatorio.")
    private String tipo;   // CC, CE, Pasaporte, NIT

    @NotBlank(message = "El número de documento es obligatorio.")
    private String numeroD; // Número del documento
}