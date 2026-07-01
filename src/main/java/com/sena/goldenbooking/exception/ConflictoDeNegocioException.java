package com.sena.goldenbooking.exception;

/**
 * Excepción genérica para conflictos de negocio: horario ya ocupado,
 * reserva ya cancelada, etc. El GlobalExceptionHandler la traduce a HTTP 409.
 * Se mantiene una sola clase (en vez de una por caso) para no sobre-diseñar
 * un proyecto de este tamaño; el mensaje es lo que distingue el caso.
 */
public class ConflictoDeNegocioException extends RuntimeException {
    public ConflictoDeNegocioException(String mensaje) {
        super(mensaje);
    }
}