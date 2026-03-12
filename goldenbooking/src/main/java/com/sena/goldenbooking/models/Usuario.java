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
    private String id;
    private String nom;
    private String ape;
    private Documento doc;
    private Direccion dir;
    private String email;

}
