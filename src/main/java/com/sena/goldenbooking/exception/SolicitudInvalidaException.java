package com.sena.goldenbooking.exception;

/**
 * Datos de la solicitud que no cumplen una regla de negocio (fechas en el
 * pasado, fin antes que inicio, campos obligatorios...). Responde 400 con el
 * mensaje tal cual, así que el mensaje debe estar escrito para el usuario.
 *
 * Reemplaza el uso de IllegalArgumentException para estos casos: esa
 * excepción también la lanzan librerías (Spring, Java) con mensajes técnicos,
 * y el GlobalExceptionHandler ya no los muestra al usuario.
 */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
