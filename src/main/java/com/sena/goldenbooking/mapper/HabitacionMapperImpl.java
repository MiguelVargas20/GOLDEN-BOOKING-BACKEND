package com.sena.goldenbooking.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sena.goldenbooking.dtos.HabitacionDto;
import com.sena.goldenbooking.models.Habitacion;

@Component
public class HabitacionMapperImpl implements HabitacionMapper {

    @Override
    public Habitacion toHabitacion(HabitacionDto dto) {
        if (dto == null) return null;
        
        return Habitacion.builder()
                .id(dto.getId())
                .numHab(dto.getNumeroHabitacion())
                .tipoHabitacion(dto.getDatosTipoHabitacion()) 
                .precNoche(dto.getPrecioNoche())
                .estado(dto.getEstadoHabitacion())
                .desc(dto.getDescripcion())
                .build();
    }

    @Override
    public HabitacionDto toDto(Habitacion hab) {
        if (hab == null) return null;

        return HabitacionDto.builder()
                .id(hab.getId())
                .numeroHabitacion(hab.getNumHab())
                .datosTipoHabitacion(hab.getTipoHabitacion())
                .precioNoche(hab.getPrecNoche())
                .estadoHabitacion(hab.getEstado())
                .descripcion(hab.getDesc())
                .build();
    }

    @Override
    public List<HabitacionDto> toDtoList(List<Habitacion> lista) {
        if (lista == null) return null;
        return lista.stream().map(this::toDto).toList();
    }

    @Override
    public void actualizarHabitacion(HabitacionDto dto, Habitacion hab) {
        if (dto == null || hab == null) return;
        hab.setNumHab(dto.getNumeroHabitacion());
        hab.setTipoHabitacion(dto.getDatosTipoHabitacion());
        hab.setPrecNoche(dto.getPrecioNoche());
        hab.setEstado(dto.getEstadoHabitacion());
        hab.setDesc(dto.getDescripcion());
    }
}
