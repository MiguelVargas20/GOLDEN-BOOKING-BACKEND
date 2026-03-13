package com.sena.goldenbooking.models;

import org.springframework.data.annotation.Id;
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
    private String id; //Id unico user(model)
    private String nom; //Nombre unico user(model)
    private String ape; //Apellido unico user(model)
    private Documento doc; //Documento unico user(model)
    private String email; //Correo unico user (model)

}
