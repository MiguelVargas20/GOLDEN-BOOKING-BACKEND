package com.sena.goldenbooking.exception;

/**
 * Se lanza cuando una reserva (o su reserva padre) no existe.
 * El GlobalExceptionHandler la traduce a HTTP 404.
 */
public class ReservaNoEncontradaException extends RuntimeException {
    public ReservaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}