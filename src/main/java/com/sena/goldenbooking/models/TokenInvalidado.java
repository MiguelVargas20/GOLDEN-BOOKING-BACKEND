package com.sena.goldenbooking.models;

import java.util.Date;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tokens_invalidados")
public class TokenInvalidado {

    @Id
    private String token;           // El token mismo es el ID (evita duplicados)


    
    @Indexed(expireAfterSeconds = 0) // MongoDB borra el documento cuando llegue a 'expiracion'
    private Date expiracion;         // Misma fecha de expiración que el JWT
}