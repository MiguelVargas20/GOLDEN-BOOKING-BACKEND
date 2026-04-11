package com.sena.goldenbooking.dtos;

import com.sena.goldenbooking.models.TipoHabitacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class HabitacionDto {
    private String id;
    private String numeroHabitacion;
 
    /** ID referencia al tipo de habitación */
    private TipoHabitacion datosTipoHabitacion;
 
    /** Precio por noche en pesos colombianos */
    private Double precioNoche;
 
    /*** Estado de la habitación:
     * "disponible" | "ocupada" | "mantenimiento"*/
    private String estadoHabitacion;
 
    private String descripcion;

}
