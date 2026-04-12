package com.sena.goldenbooking.dtos;

import java.time.LocalDateTime;

import com.sena.goldenbooking.models.EstadoReserva;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ReservaHotelDto.java
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder

public class ReservaHotelDto {

    private String idH;

    private String docUsuario;

    private String idHabitacion;

    private String numeroHabitacion;   // back llena

    private String tHabitacion;        // back llena

    private Double pNoche;             // back llena

    private String estHabitacion;      // ← corregido: String no Double

    private LocalDateTime fCheckIn;

    private LocalDateTime fCheckOut;

    private Integer noch;              // back calcula

    private Double pTotal;             // back calcula

    private EstadoReserva estado;      // ← agregado

    private LocalDateTime fReserva;    // ← agregado
}