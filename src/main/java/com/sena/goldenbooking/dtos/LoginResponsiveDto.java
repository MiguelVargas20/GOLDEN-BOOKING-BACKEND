package com.sena.goldenbooking.dtos;

import java.util.List;

import com.sena.goldenbooking.models.Rol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class LoginResponsiveDto {
    private String token; //Token autenticación //Nombre de usuario autenticado (mensaje de bienvenida)
    
    private String id; //Id del usuario autenticado
    private String username;
    private String email; //Correo electrónico del usuario autenticado

    private List<Rol> roles; //Roles del usuario autenticado para autorización

}
