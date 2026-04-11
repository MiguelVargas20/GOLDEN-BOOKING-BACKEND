package com.sena.goldenbooking.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class TipoHabitacionDto {
    private String id;
 
    /** Nombre del tipo (ej: "Habitación Individual") */
    private String nombreTipoHabitacion;
 
    /** Descripción del tipo */
    private String descripcion;
 
    /** Capacidad máxima de personas */
    private Integer capacidadMaxima;
}
