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

    // Devuelve un DTO de reserva correspondiente al ID proporcionado
    ReservaDto obtenerPorId(String id);

    // Devuelve una lista de DTOs de reserva correspondientes al documento del usuario proporcionado
    List<ReservaDto> obtenerPorUsuario(String documentoUsuario);

    // Actualiza una reserva existente con el ID proporcionado utilizando los datos del DTO y devuelve el DTO actualizado
    ReservaDto actualizarReserva(String id, ReservaDto dto);

    // Devuelve una lista de DTOs de reserva correspondientes al tipo de reserva proporcionado
    List<ReservaDto> obtenerPorEstado(EstadoReserva estado);
    
    // Devuelve una lista de DTOs de reserva correspondientes al estado de reserva proporcionado
    List<ReservaDto> obtenerPorUsuarioYTipo(String documentoUsuario, TipoReserva tipo);

    //  Elimina una reserva existente con el ID proporcionado
    void cancelarReserva(String id);
}