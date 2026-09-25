package com.sena.goldenbooking.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
// ReservaDeporte.java
@Document(collection = "ReservaDeporte")
// Índice compuesto para findSolapadasEnEspacio(): igualdad por espacioId y
// rango por fechaReserva (Mongo solo aprovecha un rango por consulta).
@CompoundIndexes({
    @CompoundIndex(name = "espacio_fecha_idx", def = "{'espacioId': 1, 'fechaReserva': 1}")
})
public class ReservaDeporte {
    @Id

    private String idReservaDeporte;

    private String idReserva;        // referencia a Reserva padre

    // Se consulta en findByDocUsuario (endpoint /mis-reservas)
    @Indexed
    private String docUsuario;  // ← agregar

    /** Espacio deportivo reservado (referencia a EspacioDeportivo). */
    @Indexed
    private String espacioId;

    /** Nombre del espacio al momento de reservar (copia para mostrar el historial). */
    private String tipoCancha;

    private String implementosAlquilados;

    private boolean requiereEntrenador;

    private LocalDateTime fechaReserva;

    private LocalDateTime fechaFinReserva;

    private Double precio;

    private EstadoReserva estado;

    // ── Trazabilidad del flujo de aprobación (PENDIENTE → CONFIRMADA / CANCELADA) ──
    /** true si la registró un ADMIN a nombre del cliente (ej. en recepción). */
    private boolean registradaPorAdministrador;

    /** Cuándo el cliente hizo la solicitud. */
    private LocalDateTime fechaSolicitud;
    /** Cuándo el admin la aprobó. */
    private LocalDateTime fechaConfirmacion;
    /** Cuándo se canceló, quién la canceló y por qué (el motivo es obligatorio si cancela el admin). */
    private LocalDateTime fechaCancelacion;
    private CanceladaPor canceladaPor;
    private String motivoCancelacion;

    private boolean recordatorio24hEnviado;

    private boolean recordatorio2hEnviado;
}