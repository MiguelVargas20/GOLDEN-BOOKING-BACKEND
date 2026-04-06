package com.sena.goldenbooking.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class LoginDto {

    @JsonProperty("correo")

    private String email;
    
    /** Contraseña en texto plano (se compara con el hash BCrypt) */
    private String password;

}
