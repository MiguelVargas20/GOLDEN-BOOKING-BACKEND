package com.sena.goldenbooking.models;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contador de intentos usado para rate limiting (login y recuperación de
 * contraseña). Mismo patrón que TokenInvalidado: el documento se autoborra
 * de MongoDB cuando pasa 'expiracion' (índice TTL), así no hace falta
 * ningún job aparte para limpiar entradas viejas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "intentos_fallidos")
public class IntentoFallido {

    // La clave identifica QUÉ se está limitando, ej:
    // "login:admin1" o "recuperacion:admin@test.com"
    @Id
    private String clave;

    private int contador;

    @Indexed(expireAfterSeconds = 0) // MongoDB borra el documento al llegar a 'expiracion'
    private Date expiracion;
}