package com.sena.goldenbooking.dtos;

import com.sena.goldenbooking.models.Direccion;
import com.sena.goldenbooking.models.Documento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder

public class UsuarioDto {
    private String id;
    private String nombre;
    private String apellido;
    private Documento documento;
    private Direccion direccion;
    private String email;

}
