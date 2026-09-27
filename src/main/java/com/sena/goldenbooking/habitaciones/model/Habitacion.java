package com.sena.goldenbooking.habitaciones.model;

import java.util.ArrayList;
import java.util.List;

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
    private EstadoHabitacion estado;
 
    /** Descripción de la habitación */
    private String desc;

    /** Imagen subida por el admin (id del archivo en GridFS); null = imagen por defecto */
    private String imagenId;
 

    /** Galería (máximo 5; la primera es la portada). */
    private List<String> imagenesIds;

    /** Máximo de imágenes por habitación. */
    public static final int MAXIMO_IMAGENES = 5;

    /** Imágenes en orden; las habitaciones antiguas solo tenían imagenId. */
    public List<String> galeria() {
        if (imagenesIds != null && !imagenesIds.isEmpty()) return new ArrayList<>(imagenesIds);
        List<String> lista = new ArrayList<>();
        if (imagenId != null) lista.add(imagenId);
        return lista;
    }

    /** Guarda la galería y deja imagenId como la portada (la primera). */
    public void guardarGaleria(List<String> lista) {
        imagenesIds = new ArrayList<>(lista);
        imagenId = lista.isEmpty() ? null : lista.get(0);
    }
}
