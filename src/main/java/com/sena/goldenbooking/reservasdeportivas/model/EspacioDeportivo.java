package com.sena.goldenbooking.reservasdeportivas.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Espacio deportivo que se puede reservar (cancha, piscina, pista...).
 *
 * Antes los espacios no existían en la base de datos: eran 10 tarjetas fijas
 * en el frontend y la reserva guardaba el nombre como texto libre, así que el
 * backend aceptaba cualquier nombre ("Futbol" y "Fútbol" eran canchas
 * distintas que se podían reservar a la misma hora). Ahora el admin los
 * administra y cada reserva apunta a un espacio real por su id.
 */
@Document(collection = "EspacioDeportivo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspacioDeportivo {

    @Id
    private String id;

    /** Nombre visible y único (ej. "Cancha de Fútbol 1"). */
    @Indexed(unique = true)
    private String nombre;

    /** Deporte o categoría (ej. "Fútbol", "Tenis"). Define la imagen por defecto. */
    private String deporte;

    private String descripcion;

    /** Capacidad máxima de personas. */
    private Integer capacidad;

    /** Tarifa por hora en pesos colombianos (cada espacio tiene la suya). */
    private Double tarifaHora;

    /** Horario en el que se puede reservar (hora de Colombia). */
    private LocalTime horaApertura;
    private LocalTime horaCierre;

    /** Id del archivo de imagen en GridFS (null = usar la imagen por defecto del deporte). */
    private String imagenId;

    private EstadoEspacio estado;

    /** Implementos que se ofrecen al reservar (vacío = los sugeridos para el deporte). */
    private List<String> implementos;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
