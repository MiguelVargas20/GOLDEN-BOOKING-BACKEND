package com.sena.goldenbooking.reservas.service;

import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

/**
 * Reglas del flujo de estados de una reserva (hotel y deporte usan las mismas):
 *
 * <pre>
 *   PENDIENTE ──(admin aprueba)──▶ CONFIRMADA ──(job diario al terminar)──▶ FINALIZADA
 *       │                              │
 *       └──────(cancelar)──────────────┴──▶ CANCELADA
 * </pre>
 */
public final class ReglasEstadoReserva {

    public static final int LONGITUD_MINIMA_MOTIVO = 5;

    private ReglasEstadoReserva() {
        // Clase de utilidades: no se instancia
    }

    /** Solo una reserva PENDIENTE se puede aprobar. */
    public static void validarConfirmable(EstadoReserva estado) {
        switch (estado) {
            case PENDIENTE -> { /* ok */ }
            case CONFIRMADA -> throw new ConflictoDeNegocioException("La reserva ya está confirmada.");
            case CANCELADA -> throw new ConflictoDeNegocioException("No se puede confirmar una reserva cancelada.");
            case FINALIZADA -> throw new ConflictoDeNegocioException("No se puede confirmar una reserva finalizada.");
        }
    }

    /** Solo PENDIENTE o CONFIRMADA se pueden cancelar. */
    public static void validarCancelable(EstadoReserva estado) {
        switch (estado) {
            case PENDIENTE, CONFIRMADA -> { /* ok */ }
            case CANCELADA -> throw new ConflictoDeNegocioException("La reserva ya está cancelada.");
            case FINALIZADA -> throw new ConflictoDeNegocioException("Una reserva finalizada no se puede cancelar.");
        }
    }

    /**
     * El titular de la reserva debe existir y estar ACTIVO. Antes no se
     * validaba: un ADMIN podía crear una reserva con un documento que no
     * pertenecía a nadie (y el correo de aviso fallaba en silencio).
     */
    public static void validarCliente(UsuarioService usuarioService, String documento) {
        UsuarioDto cliente;
        try {
            cliente = usuarioService.obtenerPorDocNum(documento);
        } catch (RecursoNoEncontradoException e) {
            throw new RecursoNoEncontradoException("No existe un cliente registrado con el documento " + documento + ".");
        }
        if (cliente.getEstado() == EstadoUsuario.INACTIVO) {
            throw new ConflictoDeNegocioException("La cuenta de este cliente está inactiva. No se pueden registrar reservas a su nombre.");
        }
    }

    /**
     * El admin debe explicar por qué cancela (el motivo le llega al cliente por
     * correo). Para el cliente el motivo es opcional.
     * @return el motivo sin espacios sobrantes, o null si no se envió.
     */
    public static String validarMotivo(String motivo, boolean esAdmin) {
        String limpio = motivo == null ? null : motivo.trim();
        if (esAdmin && (limpio == null || limpio.length() < LONGITUD_MINIMA_MOTIVO)) {
            throw new SolicitudInvalidaException(
                    "Indica el motivo de la cancelación (mínimo " + LONGITUD_MINIMA_MOTIVO
                            + " caracteres). Se le enviará al cliente.");
        }
        return (limpio == null || limpio.isEmpty()) ? null : limpio;
    }

    /** Solo se cambia la fecha de reservas PENDIENTES o CONFIRMADAS. */
    public static void validarReprogramable(EstadoReserva estado) {
        if (estado == EstadoReserva.CANCELADA) {
            throw new ConflictoDeNegocioException("No se puede reprogramar una reserva cancelada.");
        }
        if (estado == EstadoReserva.FINALIZADA) {
            throw new ConflictoDeNegocioException("No se puede reprogramar una reserva finalizada.");
        }
    }
}
