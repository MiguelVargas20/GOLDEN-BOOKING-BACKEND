package com.sena.goldenbooking.dtos;

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
    private String id; //Id unico para cada usuario
    private String nombre; //Nombre usuario
    private String apellido; //Apellido usuario
    private Documento documento; //Documento de Identidad del usuario (Tipo y Num)
    private String email; // Correo electronico usuario

}
