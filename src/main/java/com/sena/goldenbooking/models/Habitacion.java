package com.sena.goldenbooking.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "Habitacion")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data

public class Habitacion {
 @Id
    private String id;
 
    /** Número visible de la habitación (ej: "101") */
    private String numHab;
 
    /** ID referencia al tipo de habitación */
    private TipoHabitacion tipoHabitacion;
 
    /** Precio por noche en pesos colombianos */
    private Double precNoche;
 
    /*** Estado de la habitación:
     * "disponible" | "ocupada" | "mantenimiento"*/
    private String estado;
 
    /** Descripción de la habitación */
    private String desc;
 
}
 




