package com.sena.goldenbooking.exception;

/**
 * Se lanza cuando un recurso genérico (habitación, mensaje, usuario, etc.)
 * no existe. Complementa a ReservaNoEncontradaException, que es específica
 * del dominio de reservas.
 * El GlobalExceptionHandler la traduce a HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}