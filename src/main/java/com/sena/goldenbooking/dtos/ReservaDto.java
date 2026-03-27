
package com.sena.goldenbooking.dtos;

import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.TipoReserva;
import com.sena.goldenbooking.models.ReservaHotel;
import com.sena.goldenbooking.models.ReservaDeporte;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class ReservaDto {
    
    private String id;
    private String usuarioId;
    private LocalDateTime fechaReserva;
    private EstadoReserva estado;
    private TipoReserva tipo;
    
    // Detalles anidados
    private ReservaHotel detalleHotel;
    private ReservaDeporte detalleDeporte;

    // Tiempos y costos
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Double precioTotal;
}