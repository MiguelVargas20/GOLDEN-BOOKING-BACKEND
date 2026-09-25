package com.sena.goldenbooking.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Representa un horario en el que una cancha YA está reservada (activa, no cancelada).
// Igual que RangoOcupadoDto (hotel), a propósito NO incluye docUsuario ni ningún dato
// del dueño de la reserva: este DTO se expone a CUALQUIER cliente autenticado (no solo
// admin) para que el calendario de disponibilidad funcione antes de intentar reservar.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RangoOcupadoDeporteDto {

    private String espacioId;
    private String tipoCancha;
    private LocalDateTime inicio;
    private LocalDateTime fin;
}