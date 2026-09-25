package com.sena.goldenbooking.auth.service;

public interface RefreshTokenService {

    /**
     * Resultado de emitir o rotar un refresh token: el valor crudo
     * (solo existe en memoria/cookie, nunca se persiste así) y su fecha
     * de expiración, junto con el id del usuario dueño.
     */
    record RefreshTokenPair(String rawToken, java.util.Date expiracion, String userId) {}

    /**
     * Crea una nueva familia de refresh tokens (usado en login).
     */
    RefreshTokenPair crearNuevoRefreshToken(String userId);

    /**
     * Valida el refresh token crudo recibido y, si es válido, lo rota:
     * revoca el actual y emite uno nuevo de la misma familia.
     *
     * Si el token no existe, expiró, o ya estaba revocado (reuso/robo),
     * lanza RefreshTokenInvalidoException y revoca toda la familia.
     */
    RefreshTokenPair rotar(String rawToken);

    /**
     * Revoca únicamente el refresh token recibido (usado en logout).
     * No lanza error si el token ya no existe.
     */
    void revocarPorToken(String rawToken);

    /**
     * Revoca TODOS los refresh tokens vigentes del usuario (todas sus
     * sesiones/dispositivos). Se usa al cambiar o restablecer la contraseña:
     * si alguien había robado una sesión, deja de poder renovarla.
     */
    void revocarTodosDelUsuario(String userId);
}