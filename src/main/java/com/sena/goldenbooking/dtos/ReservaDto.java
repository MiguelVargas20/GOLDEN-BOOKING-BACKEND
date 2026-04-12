
package com.sena.goldenbooking.dtos;

import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.TipoReserva;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ReservaDto {
    
    private String idR;

    
    private String docUsuario; // Referencia al documento del usuario que hizo la reserva

    private LocalDateTime fReserva;

    private EstadoReserva est;

    private TipoReserva tp;
    
    // Tiempos y costos
    private LocalDateTime fInicio;

    private LocalDateTime fFin;

    private Double pTotal;
}