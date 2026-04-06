package com.sena.goldenbooking.dtos;

import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sena.goldenbooking.models.Direccion;
import com.sena.goldenbooking.models.Documento;
import com.sena.goldenbooking.models.EstadoUsuario;
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

    // DATOS PARA PERFIL (Colección Usuario)
    private String nombre;
    private String apellido;
    private Documento documento; // Asegúrate que incluya tipo y numero
    private String telefono;
    private String email; 
    private Direccion direccion;
    private LocalDate fechaNacimiento;
    private EstadoUsuario estado; // "activo" por defecto

    // DATOS PARA AUTH (Colección UsuarioAuth)
    // Eliminamos 'username' si vamos a usar el 'email' como login
    // O lo dejamos si quieres que el usuario elija un apodo (ej: "juanito123")
    private String username; 

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // Agregamos esto para que el Service sepa qué permisos darle
    private List<Rol> roles; 
}