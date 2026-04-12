package com.sena.goldenbooking.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class LoginDto {

    @JsonProperty("username")

    @NotBlank(message = "El nombre de usuario es obligatorio.")
    private String username;
    
    @NotBlank(message = "La contraseña es obligatoria.")
    /** Contraseña en texto plano (se compara con el hash BCrypt) */
    private String password;

}
