package com.sena.goldenbooking.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sena.goldenbooking.models.Direccion;
import com.sena.goldenbooking.models.Documento;
import com.sena.goldenbooking.models.EstadoUsuario;

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
    private String telefono; //Telefono de contacto del usuario
    private String email; // Correo electronico usuario
    private Direccion direccion; //Direccion de residencia del usuario
    private LocalDate fechaNacimiento; //Fecha de nacimiento del usuario (String ISO: yyyy-MM-dd)
    private EstadoUsuario estado; //Estado del usuario: "activo" | "inactivo"  
    private LocalDateTime fechaRegistro; //Fecha de registro del usuario en el sistema M-ddTHH:mm:ss)

}
