package com.sena.goldenbooking.dtos;

import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.TipoReserva;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ReservaDto {
    
    private String idR;

    // FIX hallazgo #17: el controller no tenía @Valid, pero además este DTO no
    // tenía NINGUNA anotación de validación — poner @Valid solo sin esto no
    // habría cambiado nada. Con las dos cosas juntas, un POST/PUT sin
    // docUsuario o sin fechas ahora se rechaza con un 400 claro en vez de
    // llegar al service y fallar más adelante con un error menos claro.
    @NotBlank(message = "El documento del usuario es obligatorio.")
    private String docUsuario; // Referencia al documento del usuario que hizo la reserva

    private LocalDateTime fReserva;

    private EstadoReserva est;

    private TipoReserva tp;
    
    // Tiempos y costos
    @NotNull(message = "La fecha de inicio es obligatoria.")
    private LocalDateTime fInicio;

    @NotNull(message = "La fecha de fin es obligatoria.")
    private LocalDateTime fFin;

    private Double pTotal;
}