package com.sena.goldenbooking.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeDto {

    private String id;

    @NotBlank(message = "El nombre es obligatorio.")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no tiene un formato válido.")
    private String correo;

    @NotBlank(message = "El mensaje no puede estar vacío.")
    @Size(min = 5, max = 1000, message = "El mensaje debe tener entre 5 y 1000 caracteres.")
    private String contenido;

    private LocalDateTime fechaEnvio;
    private Boolean leido;
}   