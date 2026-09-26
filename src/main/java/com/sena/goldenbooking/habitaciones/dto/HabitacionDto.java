package com.sena.goldenbooking.habitaciones.dto;

import java.util.List;

import com.sena.goldenbooking.habitaciones.model.EstadoHabitacion;
import com.sena.goldenbooking.habitaciones.model.TipoHabitacion;

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
    private EstadoHabitacion estadoHabitacion;
 
    private String descripcion;

    /** URL pública de la imagen subida (solo lectura; null si no tiene). */
    private String imagenUrl;

    /** Galería completa (hasta 5, la primera es la portada). Solo lectura. */
    private List<ImagenHabitacionDto> imagenes;

    /** Una imagen de la galería. */
    public record ImagenHabitacionDto(String id, String url) {
    }

}
