package com.sena.goldenbooking.services;

import java.util.List;
import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.models.EstadoReserva;

public interface ReservaService {
    
    /** Crea una reserva (Hotel o Deporte) y asigna fecha de registro */
    ReservaDto crearReserva(ReservaDto dto);

    /** Lista todas las reservas del sistema (ADMIN) */
    List<ReservaDto> listarTodas();

    /** Obtiene el historial de reservas de un cliente por su documento */
    List<ReservaDto> listarPorCliente(String docUsuario);

    /** Filtra reservas por estado (PENDIENTE, CONFIRMADA, CANCELADA) */
    List<ReservaDto> listarPorEstado(EstadoReserva estado);

    /** Obtiene detalle de una reserva por su ID único */
    ReservaDto obtenerPorId(String id);

    /** Actualiza datos de la reserva (fechas, estado, precio) */
    ReservaDto actualizarReserva(String id, ReservaDto dto);

    /** Cambia el estado de la reserva (ej: de PENDIENTE a CONFIRMADA) */
    ReservaDto cambiarEstado(String id, EstadoReserva nuevoEstado);

    /** Elimina una reserva del sistema */
    void eliminarReserva(String id);
}