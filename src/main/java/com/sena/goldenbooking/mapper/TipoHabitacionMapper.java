package com.sena.goldenbooking.mapper;

import java.util.List;
import com.sena.goldenbooking.dtos.TipoHabitacionDto;
import com.sena.goldenbooking.models.TipoHabitacion;

public interface TipoHabitacionMapper {

    // Mapea un DTO a una entidad
    TipoHabitacion toTipoHabitacion(TipoHabitacionDto dto);

    // Mapea una entidad a un DTO
    TipoHabitacionDto toDto(TipoHabitacion tipo);

    // Mapea una lista de entidades a una lista de DTOs
    List<TipoHabitacionDto> toDtoList(List<TipoHabitacion> lista);

    // Actualiza los datos de una entidad con los datos de un DTO
    void actualizar(TipoHabitacionDto dto, TipoHabitacion tipo);
}