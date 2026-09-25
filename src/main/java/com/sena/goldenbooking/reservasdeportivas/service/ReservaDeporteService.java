package com.sena.goldenbooking.reservasdeportivas.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.dto.RangoOcupadoDeporteDto;
import com.sena.goldenbooking.reservasdeportivas.dto.ReservaDeporteDto;

public interface ReservaDeporteService {

    /**
     * Crea la reserva en estado PENDIENTE. Valida que el espacio exista y esté
     * ACTIVO, que el horario esté dentro de su apertura/cierre y que no se
     * cruce con otra reserva; el precio sale de la tarifa del espacio.
     *
     * Si la registra un ADMIN a nombre de un cliente (recepción), puede quedar
     * CONFIRMADA de una vez: el cliente recibe solo el correo de confirmación.
     */
    ReservaDeporteDto crear(ReservaDeporteDto dto, boolean registradaPorAdmin, boolean confirmarDeInmediato);

    /** Reserva hecha por el propio cliente: queda PENDIENTE. */
    default ReservaDeporteDto crear(ReservaDeporteDto dto) {
        return crear(dto, false, false);
    }

    /** Listado del admin (con nombre y correo del cliente), filtrable por estado. */
    Page<ReservaDeporteDto> listarAdmin(EstadoReserva estado, Pageable pageable);

    /** Cantidad de reservas por estado, para los indicadores del panel del admin. */
    Map<EstadoReserva, Long> resumenPorEstado();

    /** Solo el dueño de la reserva o un ADMIN (protección IDOR). */
    ReservaDeporteDto obtenerPorId(String id, String docUsuarioSolicitante, boolean esAdmin);

    List<ReservaDeporteDto> obtenerPorReserva(String idReserva, String docUsuarioSolicitante, boolean esAdmin);

    /** Solo permite cambiar los extras (implementos, entrenador). Dueño o ADMIN. */
    ReservaDeporteDto actualizar(String id, ReservaDeporteDto dto, String docUsuarioSolicitante, boolean esAdmin);

    /** ADMIN aprueba una reserva PENDIENTE → CONFIRMADA y se avisa al cliente por correo (con .ics). */
    ReservaDeporteDto confirmar(String id);

    /**
     * Cancela la reserva. El CLIENTE solo la suya y con 24 h de anticipación;
     * el ADMIN cualquiera, con motivo obligatorio que se envía al cliente.
     */
    ReservaDeporteDto cancelar(String id, String docUsuarioSolicitante, boolean esAdmin, String motivo);

    /** Reservas del usuario autenticado (más recientes primero). */
    List<ReservaDeporteDto> obtenerPorUsuario(String docUsuario);

    /** Horarios ocupados que aún no terminan (sin datos del titular), para el calendario del cliente. */
    List<RangoOcupadoDeporteDto> obtenerFechasOcupadas();
}
