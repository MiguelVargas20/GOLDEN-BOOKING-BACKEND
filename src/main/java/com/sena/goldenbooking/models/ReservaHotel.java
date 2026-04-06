package com.sena.goldenbooking.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.Data;

@Data
public class ReservaHotel {

    private String id;

    private Habitacion datosH; // Suite, Doble, Simple

}