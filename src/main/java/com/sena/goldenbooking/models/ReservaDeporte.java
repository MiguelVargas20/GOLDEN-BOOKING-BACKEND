package com.sena.goldenbooking.models;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservaDeporte {

    private String tCancha;           // TENIS, FUTBOL, SQUASH
    private String impleAlqul;        // raquetas, balones, etc.
    private boolean rquireEntrenador;
    private LocalDateTime fechaReserva;
    private LocalDateTime fechaFinReserva;
    private Double precio;   

}