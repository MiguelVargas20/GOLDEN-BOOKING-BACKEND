package com.sena.goldenbooking.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "UsuarioAuth")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UsuarioAuth {  
    @Id
    private String id;
    private String user;

}
