package com.sena.goldenbooking.dtos;

import java.time.LocalDateTime;

import com.sena.goldenbooking.models.EstadoReserva;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// ReservaHotelDto.java
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder

public class ReservaHotelDto {

    private String idH;

    @NotBlank(message = "El documento del usuario es obligatorio.")
    private String docUsuario;

    @NotBlank(message = "El ID de la habitación es obligatorio.")
    private String idHabitacion;

    private String numeroHabitacion;   // back llena

    private String tHabitacion;        // back llena

    private Double pNoche;             // back llena

    private String estHabitacion;      // ← corregido: String no Double

    @NotNull(message = "Las fechas de check-in y check-out son obligatorias.")
    private LocalDateTime fCheckIn;

    @NotNull(message = "Las fechas de check-in y check-out son obligatorias.")
    private LocalDateTime fCheckOut;

    private Integer noch;              // back calcula

    private Double pTotal;             // back calcula

    private EstadoReserva estado;      // ← agregado

    private LocalDateTime fReserva;    // ← agregado
}