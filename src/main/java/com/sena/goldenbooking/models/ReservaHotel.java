package com.sena.goldenbooking.models;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ReservaHotel {

    private String tipoHabitacion; // Suite, Doble, Simple
    private Integer numeroPersonas;
    private Integer numeroHabitacion;
    private LocalDateTime fechaCheckIn;
    private LocalDateTime fechaCheckOut;
}