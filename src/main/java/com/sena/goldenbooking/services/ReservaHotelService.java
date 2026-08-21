package com.sena.goldenbooking.services;

import java.util.List;

import com.sena.goldenbooking.dtos.RangoOcupadoDto;
import com.sena.goldenbooking.dtos.ReservaHotelDto;

public interface ReservaHotelService {

    // Crea una nueva reserva de hotel a partir de un DTO y devuelve el DTO resultante
    ReservaHotelDto crear(ReservaHotelDto dto);

    // Devuelve una lista de todas las reservas de hotel en forma de DTOs
    List<ReservaHotelDto> listarTodas();

    // Devuelve un DTO de reserva de hotel correspondiente al ID proporcionado.
    // docUsuarioSolicitante y esAdmin se usan para validar que quien consulta
    // sea el dueño de la reserva o un administrador (protección IDOR).
    ReservaHotelDto obtenerPorId(String id, String docUsuarioSolicitante, boolean esAdmin);

    // Devuelve una lista de DTOs de reserva de hotel correspondientes al ID de reserva proporcionado
    List<ReservaHotelDto> obtenerPorReserva(String idReserva, String docUsuarioSolicitante, boolean esAdmin);

    // Actualiza una reserva de hotel existente con el ID proporcionado utilizando los datos del DTO y devuelve el DTO actualizado.
    // docUsuarioSolicitante y esAdmin se usan para validar que quien actualiza
    // sea el dueño de la reserva o un administrador (protección IDOR — mismo
    // patrón que ya se aplica en cancelar()).
    ReservaHotelDto actualizar(String id, ReservaHotelDto dto, String docUsuarioSolicitante, boolean esAdmin);

    // Elimina una reserva de hotel existente con el ID proporcionado.
    // docUsuarioSolicitante y esAdmin se usan para validar que quien cancela
    // sea el dueño de la reserva o un administrador (protección IDOR).
    void cancelar(String id, String docUsuarioSolicitante, boolean esAdmin);

    // Devuelve una lista de DTOs de reserva de hotel correspondientes al documento de usuario proporcionado
    List<ReservaHotelDto> obtenerPorUsuario(String docUsuario);

    // Marca una reserva PENDIENTE como CONFIRMADA. Solo la llama un ADMIN
    // (el chequeo de rol vive en el controller, igual que en el resto del service).
    void confirmar(String id);

    // Devuelve los rangos de fechas en los que una habitación específica
    // ya tiene reservas activas (no canceladas). El frontend lo usa para
    // bloquear esas fechas en el selector antes de que el usuario intente reservar.
    List<RangoOcupadoDto> obtenerFechasOcupadas(String idHabitacion);
}