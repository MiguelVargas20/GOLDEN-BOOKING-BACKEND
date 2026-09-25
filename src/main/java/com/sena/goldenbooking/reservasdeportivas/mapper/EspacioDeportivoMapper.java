package com.sena.goldenbooking.reservasdeportivas.mapper;

import java.util.List;

import com.sena.goldenbooking.reservasdeportivas.dto.EspacioDeportivoDto;
import com.sena.goldenbooking.reservasdeportivas.model.EspacioDeportivo;

public interface EspacioDeportivoMapper {

    EspacioDeportivo toEntity(EspacioDeportivoDto dto);

    EspacioDeportivoDto toDto(EspacioDeportivo espacio);

    List<EspacioDeportivoDto> toDtoList(List<EspacioDeportivo> espacios);

    /** Copia al espacio existente solo los campos editables (no id, imagen ni fechas). */
    void actualizar(EspacioDeportivoDto dto, EspacioDeportivo espacio);
}
