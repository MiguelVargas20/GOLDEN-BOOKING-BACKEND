package com.sena.goldenbooking.mapper;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.sena.goldenbooking.dtos.TipoHabitacionDto;
import com.sena.goldenbooking.models.TipoHabitacion;

@Component
public class TipoHabitacionMapperImpl implements TipoHabitacionMapper {

    // Implementación de los métodos de mapeo
    @Override
    public TipoHabitacion toTipoHabitacion(TipoHabitacionDto dto) {
        if (dto == null) return null;

        // Mapea los campos del DTO a la entidad
        return TipoHabitacion.builder()
                .id(dto.getId())
                .nomTipo(dto.getNombreTipoHabitacion())
                .desc(dto.getDescripcion())
                .cap(dto.getCapacidadMaxima())
                .build();
    }

    // Implementación del método para mapear una entidad a un DTO
    @Override
    public TipoHabitacionDto toDto(TipoHabitacion tipo) {
        if (tipo == null) return null;
        return TipoHabitacionDto.builder()
                .id(tipo.getId())
                .nombreTipoHabitacion(tipo.getNomTipo())
                .descripcion(tipo.getDesc())
                .capacidadMaxima(tipo.getCap())
                .build();
    }

    // Implementación del método para mapear una lista de entidades a una lista de DTOs
    @Override
    public List<TipoHabitacionDto> toDtoList(List<TipoHabitacion> lista) {
        if (lista == null) return null;
        return lista.stream().map(this::toDto).collect(Collectors.toList());
    }

    // Implementación del método para actualizar los datos de una entidad con los datos de un DTO
    @Override
    public void actualizar(TipoHabitacionDto dto, TipoHabitacion tipo) {
        if (dto == null || tipo == null) return;
        tipo.setNomTipo(dto.getNombreTipoHabitacion());
        tipo.setDesc(dto.getDescripcion());
        tipo.setCap(dto.getCapacidadMaxima());
    }
}