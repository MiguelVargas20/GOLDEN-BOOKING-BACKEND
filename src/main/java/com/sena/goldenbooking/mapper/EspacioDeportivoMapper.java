package com.sena.goldenbooking.mapper;

import java.util.List;

import com.sena.goldenbooking.dtos.EspacioDeportivoDto;
import com.sena.goldenbooking.models.EspacioDeportivo;

public interface EspacioDeportivoMapper {

    EspacioDeportivo toEntity(EspacioDeportivoDto dto);

    EspacioDeportivoDto toDto(EspacioDeportivo espacio);

    List<EspacioDeportivoDto> toDtoList(List<EspacioDeportivo> espacios);

    /** Copia al espacio existente solo los campos editables (no id, imagen ni fechas). */
    void actualizar(EspacioDeportivoDto dto, EspacioDeportivo espacio);
}
