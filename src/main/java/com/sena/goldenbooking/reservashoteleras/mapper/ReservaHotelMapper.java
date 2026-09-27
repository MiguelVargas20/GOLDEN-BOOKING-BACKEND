package com.sena.goldenbooking.reservashoteleras.mapper;

import java.util.List;

import com.sena.goldenbooking.reservashoteleras.dto.ReservaHotelDto;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;

public interface ReservaHotelMapper {

    // Mapeo entre ReservaHotel y ReservaHotelDto
    ReservaHotel toReservaHotel(ReservaHotelDto dto);

    // Mapeo de ReservaHotel a ReservaHotelDto
    ReservaHotelDto toDto(ReservaHotel reservaHotel);

    // Mapeo de lista de ReservaHotel a lista de ReservaHotelDto
    List<ReservaHotelDto> toDtoList(List<ReservaHotel> lista);

    // Actualizar una ReservaHotel existente con datos de un ReservaHotelDto
    void actualizarReservaHotel(ReservaHotelDto dto, ReservaHotel reservaHotel);

}