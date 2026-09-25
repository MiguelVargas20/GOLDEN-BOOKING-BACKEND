package com.sena.goldenbooking.services;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sena.goldenbooking.dtos.RangoOcupadoDto;
import com.sena.goldenbooking.dtos.ReservaHotelDto;
import com.sena.goldenbooking.models.EstadoReserva;

public interface ReservaHotelService {

    /** Crea la reserva en estado PENDIENTE (valida fechas, mantenimiento y solapamiento). */
    ReservaHotelDto crear(ReservaHotelDto dto);

    /** Listado del admin (con nombre y correo del cliente), filtrable por estado. */
    Page<ReservaHotelDto> listarAdmin(EstadoReserva estado, Pageable pageable);

    /** Cantidad de reservas por estado, para los indicadores del panel del admin. */
    Map<EstadoReserva, Long> resumenPorEstado();

    /** Solo el dueño de la reserva o un ADMIN (protección IDOR). */
    ReservaHotelDto obtenerPorId(String id, String docUsuarioSolicitante, boolean esAdmin);

    List<ReservaHotelDto> obtenerPorReserva(String idReserva, String docUsuarioSolicitante, boolean esAdmin);

    /** Sin campos editables por ahora (fechas y precio no se cambian sin revalidar disponibilidad). */
    ReservaHotelDto actualizar(String id, ReservaHotelDto dto, String docUsuarioSolicitante, boolean esAdmin);

    /**
     * Cancela la reserva. El CLIENTE solo la suya y con 24 h de anticipación;
     * el ADMIN cualquiera, con motivo obligatorio que se envía al cliente.
     */
    ReservaHotelDto cancelar(String id, String docUsuarioSolicitante, boolean esAdmin, String motivo);

    /** Reservas del usuario autenticado (más recientes primero). */
    List<ReservaHotelDto> obtenerPorUsuario(String docUsuario);

    /** ADMIN aprueba una reserva PENDIENTE → CONFIRMADA y se avisa al cliente por correo (con .ics). */
    ReservaHotelDto confirmar(String id);

    List<RangoOcupadoDto> obtenerFechasOcupadas(String idHabitacion);
}
