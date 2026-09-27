package com.sena.goldenbooking.notificaciones.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.sena.goldenbooking.reservas.model.TipoReserva;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aviso para la campana del cliente: su reserva fue aprobada, cancelada,
 * reprogramada o venció, o ya puede calificarla.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Notificacion")
@CompoundIndex(name = "doc_fecha_idx", def = "{'docUsuario': 1, 'fecha': -1}")
@Schema(description = "Notificación de la campana del cliente.")
public class Notificacion {

    @Id
    private String id;

    @Schema(hidden = true)
    private String docUsuario;

    private TipoNotificacion tipo;

    @Schema(description = "HOTEL o DEPORTE.")
    private TipoReserva categoria;

    @Schema(description = "Id de la reserva de hotel o deporte a la que se refiere.")
    private String idReserva;

    private String titulo;

    private String mensaje;

    private LocalDateTime fecha;

    private boolean leida;
}
