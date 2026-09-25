package com.sena.goldenbooking.services;

import com.sena.goldenbooking.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.models.EstadoReserva;

/**
 * Reglas del flujo de estados de una reserva (hotel y deporte usan las mismas):
 *
 * <pre>
 *   PENDIENTE ──(admin aprueba)──▶ CONFIRMADA ──(job diario al terminar)──▶ FINALIZADA
 *       │                              │
 *       └──────(cancelar)──────────────┴──▶ CANCELADA
 * </pre>
 */
final class ReglasEstadoReserva {

    static final int LONGITUD_MINIMA_MOTIVO = 5;

    private ReglasEstadoReserva() {
        // Clase de utilidades: no se instancia
    }

    /** Solo una reserva PENDIENTE se puede aprobar. */
    static void validarConfirmable(EstadoReserva estado) {
        switch (estado) {
            case PENDIENTE -> { /* ok */ }
            case CONFIRMADA -> throw new ConflictoDeNegocioException("La reserva ya está confirmada.");
            case CANCELADA -> throw new ConflictoDeNegocioException("No se puede confirmar una reserva cancelada.");
            case FINALIZADA -> throw new ConflictoDeNegocioException("No se puede confirmar una reserva finalizada.");
        }
    }

    /** Solo PENDIENTE o CONFIRMADA se pueden cancelar. */
    static void validarCancelable(EstadoReserva estado) {
        switch (estado) {
            case PENDIENTE, CONFIRMADA -> { /* ok */ }
            case CANCELADA -> throw new ConflictoDeNegocioException("La reserva ya está cancelada.");
            case FINALIZADA -> throw new ConflictoDeNegocioException("Una reserva finalizada no se puede cancelar.");
        }
    }

    /**
     * El admin debe explicar por qué cancela (el motivo le llega al cliente por
     * correo). Para el cliente el motivo es opcional.
     * @return el motivo sin espacios sobrantes, o null si no se envió.
     */
    static String validarMotivo(String motivo, boolean esAdmin) {
        String limpio = motivo == null ? null : motivo.trim();
        if (esAdmin && (limpio == null || limpio.length() < LONGITUD_MINIMA_MOTIVO)) {
            throw new SolicitudInvalidaException(
                    "Indica el motivo de la cancelación (mínimo " + LONGITUD_MINIMA_MOTIVO
                            + " caracteres). Se le enviará al cliente.");
        }
        return (limpio == null || limpio.isEmpty()) ? null : limpio;
    }
}
