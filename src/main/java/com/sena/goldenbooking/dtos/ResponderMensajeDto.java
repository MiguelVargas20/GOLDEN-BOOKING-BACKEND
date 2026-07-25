package com.sena.goldenbooking.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponderMensajeDto {

    @NotBlank(message = "La respuesta no puede estar vacía.")
    @Size(min = 2, max = 2000, message = "La respuesta debe tener entre 2 y 2000 caracteres.")
    private String respuesta;
}