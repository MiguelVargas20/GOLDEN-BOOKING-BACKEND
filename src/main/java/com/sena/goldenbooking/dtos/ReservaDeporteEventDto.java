package com.sena.goldenbooking.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaDeporteEventDto {
    private String espacioId;     // qué espacio fue reservado
    private String fecha;         // qué fecha
    private String horaInicio;    // qué hora inicio
    private String horaFin;       // qué hora fin
    private String estado;        // "OCUPADO" o "DISPONIBLE"
    private String mensaje;       // mensaje para mostrar en el front
}