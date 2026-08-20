package com.sena.goldenbooking.services;

import java.util.List;
import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.TipoReserva;

public interface ReservaService {

    // Crea una nueva reserva a partir de un DTO y devuelve el DTO resultante
    ReservaDto crearReserva(ReservaDto dto);

    // Devuelve una lista de todas las reservas en forma de DTOs
    List<ReservaDto> listarReservas();

    // Devuelve un DTO de reserva correspondiente al ID proporcionado.
    // docUsuarioSolicitante/esAdmin: fix IDOR — solo el dueño o un ADMIN pueden verla.
    ReservaDto obtenerPorId(String id, String docUsuarioSolicitante, boolean esAdmin);

    // Devuelve una lista de DTOs de reserva correspondientes al documento del usuario proporcionado.
    // Solo el propio dueño (docUsuarioSolicitante == documentoUsuario) o un ADMIN pueden consultarlas.
    List<ReservaDto> obtenerPorUsuario(String documentoUsuario, String docUsuarioSolicitante, boolean esAdmin);

    // Actualiza una reserva existente con el ID proporcionado. Fix IDOR: solo el dueño o un ADMIN.
    ReservaDto actualizarReserva(String id, ReservaDto dto, String docUsuarioSolicitante, boolean esAdmin);

    // Devuelve una lista de DTOs de reserva correspondientes al tipo de reserva proporcionado
    List<ReservaDto> obtenerPorEstado(EstadoReserva estado);
    
    // Devuelve una lista de DTOs de reserva correspondientes al estado de reserva proporcionado.
    // Solo el propio dueño o un ADMIN pueden consultarlas.
    List<ReservaDto> obtenerPorUsuarioYTipo(String documentoUsuario, TipoReserva tipo, String docUsuarioSolicitante, boolean esAdmin);

    //  Cancela una reserva existente con el ID proporcionado. Fix IDOR: solo el dueño o un ADMIN.
    void cancelarReserva(String id, String docUsuarioSolicitante, boolean esAdmin);
}