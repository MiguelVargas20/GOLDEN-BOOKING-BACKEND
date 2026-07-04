package com.sena.goldenbooking.exception;

/**
 * Se lanza cuando el refresh token no existe, expiró, o fue reusado
 * (indicio de robo). El GlobalExceptionHandler la traduce a HTTP 401.
 */
public class RefreshTokenInvalidoException extends RuntimeException {
    public RefreshTokenInvalidoException(String mensaje) {
        super(mensaje);
    }
}