package com.sena.goldenbooking.mapper;

import java.util.List;

import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.models.Reserva;

public interface ReservaMapper {

    /* Convertir DTO a modelo */
    Reserva toReserva(ReservaDto dto);

    /* Convertir modelo a DTO */
    ReservaDto toDto(Reserva reserva);

    /* Convertir una lista de modelos a una lista de DTOs */
    List<ReservaDto> toDtoList(List<Reserva> reservas);

    /* Actualizar una reserva con los datos del DTO */
    void actualizarReserva(ReservaDto dto, Reserva reserva);
}

