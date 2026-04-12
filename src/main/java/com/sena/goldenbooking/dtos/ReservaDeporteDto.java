package com.sena.goldenbooking.dtos;

import java.time.LocalDateTime;

import com.sena.goldenbooking.models.EstadoReserva;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ReservaDeporteDto.java
@Data @NoArgsConstructor @AllArgsConstructor @Builder


public class ReservaDeporteDto {

    private String idD;

    private String docUsuario;         // ← agregado

    private String tCancha;

    private String implAlquilados;

    private boolean rqrEntrenador;

    private LocalDateTime fInicioReserva;
    
    private LocalDateTime fFinReserva;
    
    private Double pr;

    private EstadoReserva estado;      // ← agregado

    private LocalDateTime fReserva;    // ← agregado
}