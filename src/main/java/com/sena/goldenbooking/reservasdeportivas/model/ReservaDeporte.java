package com.sena.goldenbooking.reservasdeportivas.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.sena.goldenbooking.reservas.model.CanceladaPor;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.EventoReserva;
import com.sena.goldenbooking.reservas.model.MiembroReserva;

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

    /** Acompañantes del titular (nombre y documento; los menores con tarjeta de identidad). */
    @Builder.Default
    private List<MiembroReserva> miembros = new ArrayList<>();

    /** Descuento de socio aplicado al precio (porcentaje; 0 o null = sin descuento). */
    private Double descuento;

    /** Quién la creó, aprobó, canceló o reprogramó y cuándo (lo ve el administrador). */
    @Builder.Default
    private List<EventoReserva> historial = new ArrayList<>();
}