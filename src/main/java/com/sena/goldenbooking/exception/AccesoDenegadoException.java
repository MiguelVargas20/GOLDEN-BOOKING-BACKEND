package com.sena.goldenbooking.exception;

/**
 * Se lanza cuando un usuario autenticado intenta operar sobre un recurso
 * que no le pertenece (por ejemplo, cancelar la reserva de otro usuario)
 * y no tiene rol de administrador. El GlobalExceptionHandler la traduce a HTTP 403.
 */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}