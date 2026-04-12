package com.sena.goldenbooking.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
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

public class ReservaDeporte {
    @Id

    private String idReservaDeporte;

    private String idReserva;        // referencia a Reserva padre

    private String docUsuario;  // ← agregar

    private String tipoCancha;

    private String implementosAlquilados;

    private boolean requiereEntrenador;

    private LocalDateTime fechaReserva;

    private LocalDateTime fechaFinReserva;

    private Double precio;
}
