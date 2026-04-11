package com.sena.goldenbooking.models;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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
    
    @Indexed(unique = true)
    private String user;

    private String pwd;

    private List<Rol> rls;
}
