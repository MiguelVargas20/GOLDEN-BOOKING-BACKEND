package com.sena.goldenbooking.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Representa un rango de fechas en el que una habitación YA está reservada.
// El frontend usa esto para deshabilitar esas fechas en el datepicker,
// en vez de dejar que el usuario elija y recién ahí enterarse del error.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RangoOcupadoDto {

    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
}