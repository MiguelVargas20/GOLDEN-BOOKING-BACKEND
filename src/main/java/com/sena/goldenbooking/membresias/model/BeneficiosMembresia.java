package com.sena.goldenbooking.membresias.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Beneficios de una categoría de socio. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiosMembresia {

    @Min(value = 0, message = "El descuento no puede ser negativo.")
    @Max(value = 50, message = "El descuento máximo es 50 %.")
    @Schema(description = "Descuento en porcentaje sobre el precio de cada reserva.", example = "10")
    private double descuento;

    @Min(value = 1, message = "La anticipación mínima es 1 día.")
    @Max(value = 730, message = "La anticipación máxima es 730 días.")
    @Schema(description = "Con cuántos días de anticipación puede reservar.", example = "180")
    private int diasAnticipacion;

    @Size(max = 300, message = "La descripción admite máximo 300 caracteres.")
    @Schema(description = "Otros beneficios, en texto (se muestran al cliente).", example = "Toalla de cortesía y parqueadero.")
    private String otrosBeneficios;
}
