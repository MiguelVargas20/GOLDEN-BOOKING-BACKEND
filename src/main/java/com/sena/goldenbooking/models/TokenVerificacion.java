package com.sena.goldenbooking.models;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "TokenVerificacion")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class TokenVerificacion {

    @Id
    private String id;

    /** El código único que se manda por correo */
    @Indexed(unique = true)
    private String token;

    /** Correo del usuario al que pertenece este token */
    private String correo;

    /** Tipo de token: distingue verificación de cuenta vs. recuperación de contraseña */
    private TipoToken tipo;

    /** Fecha de creación — MongoDB borra el documento automáticamente al expirar (ver índice TTL) */
    @Indexed(expireAfterSeconds = 3600) // expira en 1 hora
    private LocalDateTime fechaCreacion;
}