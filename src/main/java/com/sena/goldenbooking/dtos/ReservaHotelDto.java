package com.sena.goldenbooking.dtos;

import java.time.LocalDateTime;

import com.sena.goldenbooking.models.Habitacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ReservaHotelDto {
    
    private String id;

    private Habitacion datosHabitacion; // Suite, Doble, Simple

    private LocalDateTime fechaCheckIn;

    private LocalDateTime fechaCheckOut;
}
