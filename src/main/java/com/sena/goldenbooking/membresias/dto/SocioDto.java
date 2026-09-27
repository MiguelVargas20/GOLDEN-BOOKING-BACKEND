package com.sena.goldenbooking.membresias.dto;

import java.time.LocalDateTime;

import com.sena.goldenbooking.usuarios.model.TipoMembresia;

import io.swagger.v3.oas.annotations.media.Schema;

/** Cliente en el panel de socios. */
@Schema(description = "Cliente con su categoría de socio y cuántas reservas ha hecho.")
public record SocioDto(
        String idUsuario,
        String nombre,
        String documento,
        String email,
        TipoMembresia membresia,
        LocalDateTime fechaMembresia,
        @Schema(description = "Reservas confirmadas o finalizadas (hotel + deporte).") long reservas,
        @Schema(description = "true si alcanzó el mínimo configurado y aún no es socio.") boolean sugerido) {
}
