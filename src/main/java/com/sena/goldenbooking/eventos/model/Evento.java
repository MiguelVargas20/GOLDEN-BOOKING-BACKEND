package com.sena.goldenbooking.eventos.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Evento del club (baile, festival, torneo...) que se anuncia en la portada del cliente. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Evento")
public class Evento {

    @Id
    private String id;

    private String titulo;

    private String descripcion;

    private CategoriaEvento categoria;

    @Indexed
    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    private String lugar;

    /** Cupos disponibles (null = sin límite). */
    private Integer cupo;

    /** Valor de la entrada (0 o null = gratis). */
    private Double precio;

    /** Imagen en GridFS (null = imagen por defecto de la categoría). */
    private String imagenId;

    /** Si es false el evento es un borrador que solo ve el admin. */
    private boolean publicado;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;
}
