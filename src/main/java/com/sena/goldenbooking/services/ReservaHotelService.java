package com.sena.goldenbooking.services;

import java.util.List;
import com.sena.goldenbooking.dtos.ReservaHotelDto;
import com.sena.goldenbooking.dtos.RangoOcupadoDto;

public interface ReservaHotelService {

    // Crea una nueva reserva de hotel a partir de un DTO y devuelve el DTO resultante
    ReservaHotelDto crear(ReservaHotelDto dto);

    // Devuelve una lista de todas las reservas de hotel en forma de DTOs
    List<ReservaHotelDto> listarTodas();

    // Devuelve un DTO de reserva de hotel correspondiente al ID proporcionado
    ReservaHotelDto obtenerPorId(String id);

    // Devuelve una lista de DTOs de reserva de hotel correspondientes al ID de reserva proporcionado
    List<ReservaHotelDto> obtenerPorReserva(String idReserva);

    // Actualiza una reserva de hotel existente con el ID proporcionado utilizando los datos del DTO y devuelve el DTO actualizado
    ReservaHotelDto actualizar(String id, ReservaHotelDto dto);

    // Elimina una reserva de hotel existente con el ID proporcionado.
    // docUsuarioSolicitante y esAdmin se usan para validar que quien cancela
    // sea el dueño de la reserva o un administrador (protección IDOR).
    void cancelar(String id, String docUsuarioSolicitante, boolean esAdmin);

    // Devuelve una lista de DTOs de reserva de hotel correspondientes al documento de usuario proporcionado
    List<ReservaHotelDto> obtenerPorUsuario(String docUsuario);

    // Devuelve los rangos de fechas en los que una habitación específica
    // ya tiene reservas activas (no canceladas). El frontend lo usa para
    // bloquear esas fechas en el selector antes de que el usuario intente reservar.
    List<RangoOcupadoDto> obtenerFechasOcupadas(String idHabitacion);
}