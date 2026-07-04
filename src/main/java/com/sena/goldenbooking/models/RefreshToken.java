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
 * Representa un refresh token emitido a un usuario.
 *
 * Nunca se guarda el token en texto plano: se persiste solo su hash (SHA-256).
 * Cada login crea una nueva "familia" (familyId). Cada refresh exitoso
 * ROTA el token: invalida el actual (revocado=true) y crea uno nuevo con
 * el mismo familyId. Si alguien intenta reusar un token ya revocado,
 * se interpreta como robo/reuso y se revoca toda la familia, forzando
 * un nuevo login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash; // SHA-256 hex del token crudo (nunca se guarda el crudo)

    @Indexed
    private String userId; // Usuario.id (== UsuarioAuth.id)

    @Indexed
    private String familyId; // agrupa la cadena de rotación de una misma sesión/dispositivo

    private boolean revocado;

    private Date creadoEn;

    private Date revocadoEn;

    @Indexed(expireAfterSeconds = 0) // Mongo TTL: borra el documento al llegar la fecha
    private Date expiracion;
}