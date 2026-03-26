package com.sena.goldenbooking.models;

import lombok.Data;

@Data

public class ReservaDeporte {
    private String tipoHabitacion; // Suite, Doble, Simple
    private Integer numeroPersonas;
    private boolean incluyeDesayuno;
}
