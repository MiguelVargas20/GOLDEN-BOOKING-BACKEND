package com.sena.goldenbooking.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sena.goldenbooking.dtos.HabitacionDto;
import com.sena.goldenbooking.models.Habitacion;

/**
 * NOTA (Hallazgo B3 de la auditoría): los nombres de campo NO coinciden 1:1
 * entre el modelo interno (Habitacion) y lo que expone la API (HabitacionDto).
 * No es un bug — este mapper los traduce bien — pero conviene tener la tabla
 * a mano en vez de tener que adivinar leyendo el código de abajo:
 *
 *   Habitacion (Mongo)      HabitacionDto (API/JSON)
 *   ──────────────────      ────────────────────────
 *   numHab               →  numeroHabitacion
 *   tipoHabitacion        →  datosTipoHabitacion
 *   precNoche             →  precioNoche
 *   estado                →  estadoHabitacion
 *   desc                  →  descripcion
 *
 * OJO si renombras algo aquí: numeroHabitacion, estadoHabitacion, etc. son
 * el contrato JSON que ya consume el frontend (GET /api/habitaciones
 * devuelve esos nombres). Cambiarlos rompe el frontend en silencio —
 * cualquier renombre real debe coordinarse con el equipo de frontend, no
 * hacerse solo del lado del backend.
 */
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