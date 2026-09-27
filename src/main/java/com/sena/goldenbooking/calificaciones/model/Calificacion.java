package com.sena.goldenbooking.calificaciones.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.sena.goldenbooking.reservas.model.TipoReserva;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Opinión del cliente sobre un espacio deportivo o una habitación, hecha
 * después de que su reserva finalizó. Una sola por reserva.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Calificacion")
@CompoundIndex(name = "recurso_idx", def = "{'categoria': 1, 'idRecurso': 1, 'fecha': -1}")
public class Calificacion {

    @Id
    private String id;

    private TipoReserva categoria;

    /** Id de la reserva de hotel o deporte calificada (una calificación por reserva). */
    @Indexed(unique = true)
    private String idReserva;

    /** Id del espacio deportivo o de la habitación. */
    private String idRecurso;

    /** Nombre del espacio o "Habitación 101" (copia para mostrar). */
    private String nombreRecurso;

    @Indexed
    private String docUsuario;

    private String nombreCliente;

    /** De 1 a 5 estrellas. */
    private int puntuacion;

    private String comentario;

    private LocalDateTime fecha;
}
