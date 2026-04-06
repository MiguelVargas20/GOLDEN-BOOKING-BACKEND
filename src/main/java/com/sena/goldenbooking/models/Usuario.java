package com.sena.goldenbooking.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "UsuarioPerfil")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Usuario {
 
    @Id
    private String id;
 
    /** Nombre completo del usuario */
    private String nomUsr;

    /** Apellido completo del usuario */
    private String apellUsr;
 
    /** Documento de identidad embebido */
    private Documento docId;
 
    /** Teléfono de contacto */
    private String tel;
 
    /** Correo electrónico (único en el sistema) */
    @Indexed(unique = true)
    private String correo;
 
    /** Dirección de residencia */
    private Direccion dir;
 
    /** Fecha de nacimiento (String ISO: yyyy-MM-dd) */
    private LocalDate fNac;
 
 
    /** * Estado del usuario: "activo" | "inactivo"*/
    private EstadoUsuario estado;
 
    /** Fecha de registro en el sistema */
    private LocalDateTime fReg;
}
