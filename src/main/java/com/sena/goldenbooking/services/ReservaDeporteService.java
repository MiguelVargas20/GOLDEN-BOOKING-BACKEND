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

    // Elimina una reserva de deporte existente con el ID proporcionado.
    // docUsuarioSolicitante y esAdmin se usan para validar que quien cancela
    // sea el dueño de la reserva o un administrador (protección IDOR).
    void cancelar(String id, String docUsuarioSolicitante, boolean esAdmin);

    // Devuelve las reservas de deporte del usuario autenticado (endpoint dedicado, sin filtrar en el frontend)
    List<ReservaDeporteDto> obtenerPorUsuario(String docUsuario);

    // Devuelve una página de DTOs de reserva de deporte según los parámetros de paginación proporcionados
    Page<ReservaDeporteDto> listarTodasPaginadas(Pageable pageable);
}