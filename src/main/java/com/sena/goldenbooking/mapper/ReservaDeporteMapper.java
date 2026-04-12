package com.sena.goldenbooking.mapper;

import java.util.List;
import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.models.ReservaDeporte;

public interface ReservaDeporteMapper {

    // Mapeo entre ReservaDeporte y ReservaDeporteDto
    ReservaDeporte toReservaDeporte(ReservaDeporteDto dto);

    // Mapeo de ReservaDeporte a ReservaDeporteDto
    ReservaDeporteDto toDto(ReservaDeporte reservaDeporte);

    // Mapeo de lista de ReservaDeporte a lista de ReservaDeporteDto
    List<ReservaDeporteDto> toDtoList(List<ReservaDeporte> lista);

    //  Actualizar una ReservaDeporte existente con datos de un ReservaDeporteDto
    void actualizarReservaDeporte(ReservaDeporteDto dto, ReservaDeporte reservaDeporte);
}