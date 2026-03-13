package com.sena.goldenbooking.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sena.goldenbooking.models.Documento;
import com.sena.goldenbooking.models.Rol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder

public class UsuarioRegistroDto {
    private String nombre;
    private String apellido;
    private Documento documento;
    private String email;
    private String usuario;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private List<Rol> roles;

}
