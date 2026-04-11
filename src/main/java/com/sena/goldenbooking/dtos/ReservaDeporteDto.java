package com.sena.goldenbooking.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ReservaDeporteDto {
    
    private String id;

    private String tipoCancha; // Tenis, Futbol, Squash

    private String implementosAlquilados; // raquetas, balones

    private boolean requiereEntrenador;

}
