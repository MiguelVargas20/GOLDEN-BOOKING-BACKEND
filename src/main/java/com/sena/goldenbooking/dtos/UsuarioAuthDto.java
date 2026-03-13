package com.sena.goldenbooking.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sena.goldenbooking.models.Rol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data

public class UsuarioAuthDto {
    private String id;
    private String usuario;

    //La contraseña solo se escribe, no se lee en respuesta JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    //Lista los roles para autorización
    //Roles controlados por enum(Solo los enumerados)
    private List<Rol> roles;


}
