package com.sena.goldenbooking.mapper;

import java.util.List;

import com.sena.goldenbooking.dtos.HabitacionDto;
import com.sena.goldenbooking.models.Habitacion;

public interface HabitacionMapper {
    /* Convertir DTO a modelo */
    Habitacion toHabitacion(HabitacionDto dto);

    /* Convertir modelo a DTO */
    HabitacionDto toDto(Habitacion habitacion);
    
    /* Convertir una lista de modelos a una lista de DTOs */
    List<HabitacionDto> toDtoList(List<Habitacion> habitaciones);

    /* Actualizar una habitación con los datos del DTO */
    void actualizarHabitacion(HabitacionDto dto, Habitacion habitacion);
}
