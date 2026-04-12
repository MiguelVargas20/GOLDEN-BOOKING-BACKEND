package com.sena.goldenbooking.mapper;

import java.util.List;
import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.models.Reserva;

public interface ReservaMapper {

    // Mapeo entre Reserva y ReservaDto
    Reserva toReserva(ReservaDto dto);

    // Mapeo de Reserva a ReservaDto
    ReservaDto toDto(Reserva reserva);

    // Mapeo de lista de Reservas a lista de ReservaDtos
    List<ReservaDto> toDtoList(List<Reserva> reservas);

    // Actualizar una Reserva existente con datos de un ReservaDto
    void actualizarReserva(ReservaDto dto, Reserva reserva);
}