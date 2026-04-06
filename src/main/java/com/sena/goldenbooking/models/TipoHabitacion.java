package com.sena.goldenbooking.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "TipoHabitacion")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class TipoHabitacion {
 @Id
    private String id;
 
    /** Nombre del tipo (ej: "Habitación Individual") */
    private String nomTipo;
 
    /** Descripción del tipo */
    private String desc;
 
    /** Capacidad máxima de personas */
    private Integer cap;
}
