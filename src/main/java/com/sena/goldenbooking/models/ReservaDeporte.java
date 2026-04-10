package com.sena.goldenbooking.models;

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
}