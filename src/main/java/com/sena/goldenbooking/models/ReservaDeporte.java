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
// Índice compuesto: findSolapadas() filtra por tipoCancha (igualdad) y
// compara contra fechaReserva/fechaFinReserva (rango). Mongo solo puede
// usar de forma eficiente UN rango por consulta, así que este índice
// prioriza el campo de igualdad (tipoCancha) + un campo de fecha; sigue
// ayudando bastante porque tipoCancha ya reduce el universo de documentos.
@CompoundIndexes({
    @CompoundIndex(name = "tipoCancha_fecha_idx", def = "{'tipoCancha': 1, 'fechaReserva': 1}")
})
public class ReservaDeporte {
    @Id

    private String idReservaDeporte;

    private String idReserva;        // referencia a Reserva padre

    // Se consulta en findByDocUsuario (endpoint /mis-reservas)
    @Indexed
    private String docUsuario;  // ← agregar

    private String tipoCancha;

    private String implementosAlquilados;

    private boolean requiereEntrenador;

    private LocalDateTime fechaReserva;

    private LocalDateTime fechaFinReserva;

    private Double precio;

    private EstadoReserva estado;

    private boolean recordatorio24hEnviado;

    private boolean recordatorio2hEnviado;
}