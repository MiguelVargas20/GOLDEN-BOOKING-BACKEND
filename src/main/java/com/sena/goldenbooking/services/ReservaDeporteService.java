package com.sena.goldenbooking.services;

import java.util.List;
import com.sena.goldenbooking.dtos.ReservaDeporteDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservaDeporteService {

    // Crea una nueva reserva de deporte a partir de un DTO y devuelve el DTO resultante
    ReservaDeporteDto crear(ReservaDeporteDto dto);

    // Devuelve una lista de todas las reservas de deporte en forma de DTOs
    List<ReservaDeporteDto> listarTodas();

    // Devuelve un DTO de reserva de deporte correspondiente al ID proporcionado
    ReservaDeporteDto obtenerPorId(String id);

    // Devuelve una lista de DTOs de reserva de deporte correspondientes al ID de reserva proporcionado
    List<ReservaDeporteDto> obtenerPorReserva(String idReserva);

    // Actualiza una reserva de deporte existente con el ID proporcionado utilizando los datos del DTO y devuelve el DTO actualizado
    ReservaDeporteDto actualizar(String id, ReservaDeporteDto dto);

    // Elimina una reserva de deporte existente con el ID proporcionado
    void cancelar(String id);

    // Devuelve una página de DTOs de reserva de deporte según los parámetros de paginación proporcionados
    Page<ReservaDeporteDto> listarTodasPaginadas(Pageable pageable);
}