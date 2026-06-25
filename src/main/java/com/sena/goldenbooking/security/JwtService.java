package com.sena.goldenbooking.security;

import java.security.Key;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${miclavesecreta}")
    private String CLAVE;

    // Convierte el String en una Key válida para firmar con HS256
    private Key obtenerClave() {
        return Keys.hmacShaKeyFor(CLAVE.getBytes());
    }


    /**
     * Genera el token JWT con los datos del usuario.
     * Los roles vienen del modelo (no del DTO) porque el DTO no los expone.
     *
     * @param email  — sujeto del token (identificador único)
     * @param roles  — lista de roles desde el modelo Usuario
     * @param nombre — desde UsuarioDto
     * @param apellido — desde UsuarioDto
     * @param id     — desde UsuarioDto
     */
    public String generarToken(String email, List<String> roles, String nombre, String apellido, String id) {
        return Jwts.builder()
                .setSubject(email)                          // email como identificador principal
                .claim("roles", roles)                      // roles del usuario
                .claim("nombre", nombre)                    // para mostrar en el frontend
                .claim("apellido", apellido)
                .claim("id", id)                            // útil para consultas desde React
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hora
                .signWith(obtenerClave(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extrae todos los claims del token
    public Claims obtenerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(obtenerClave())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Extrae el email (subject) del token
    public String extraerEmail(String token) {
        return obtenerClaims(token).getSubject();
    }

    // Extrae los roles del token
    public List<String> extraerRoles(String token) {
        return obtenerClaims(token).get("roles", List.class);
    }

    // Extrae el id del usuario desde el token
    public String extraerId(String token) {
        return obtenerClaims(token).get("id", String.class);
    }

    // Valida que el token no haya expirado
    public boolean tokenValido(String token) {
        try {
            return !obtenerClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false; // token malformado o expirado
        }
    }
    // Extrae la fecha de expiración del token
    public Date extraerExpiracion(String token) {
        return obtenerClaims(token).getExpiration();
    }
}