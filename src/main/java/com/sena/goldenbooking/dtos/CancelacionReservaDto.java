package com.sena.goldenbooking.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Datos para cancelar una reserva.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelacionReservaDto {

    @Schema(description = "Motivo de la cancelación. OBLIGATORIO (mínimo 5 caracteres) cuando cancela el administrador; "
            + "se le envía al cliente por correo. Opcional para el cliente.",
            example = "La cancha estará en mantenimiento ese día.")
    @Size(max = 300, message = "El motivo no puede superar 300 caracteres.")
    private String motivo;
}
