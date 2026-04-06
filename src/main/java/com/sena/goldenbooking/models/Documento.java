package com.sena.goldenbooking.models;

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
    private String tipo;   // CC, CE, Pasaporte, NIT
    private String numeroD; // Número del documento
}