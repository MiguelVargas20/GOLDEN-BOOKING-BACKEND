package com.sena.goldenbooking.models;

import lombok.Data;

@Data
public class DetalleDeporte {
    private String tipoCancha; // Tenis, Futbol, Squash
    private String implementosAlquilados; // raquetas, balones
    private boolean requiereEntrenador;
}