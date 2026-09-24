package com.sena.goldenbooking.dtos;

import java.time.LocalDateTime;

import com.sena.goldenbooking.models.EstadoReserva;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// ReservaDeporteDto.java
@Data @NoArgsConstructor @AllArgsConstructor @Builder


public class ReservaDeporteDto {

    private String idD;

    @NotBlank(message = "El documento del usuario es obligatorio.")
    private String docUsuario;         // ← agregado

    @NotBlank(message = "El ID de la cancha es obligatorio.")
    // Solo letras (con tildes), números, espacios y guiones. Antes aceptaba
    // cualquier texto: un cliente podía guardar "<img src=x onerror=...>" como
    // nombre de cancha y ese valor se renderizaba como HTML en el panel admin
    // y en los correos (XSS almacenado).
    @Pattern(regexp = "^[\\p{L}0-9 \\-]{1,50}$", message = "El nombre de la cancha contiene caracteres no permitidos.")
    private String tCancha;

    private String implAlquilados;

    private boolean rqrEntrenador;

    @NotNull(message = "Las fechas de inicio y fin son obligatorias.")
    private LocalDateTime fInicioReserva;
    
    @NotNull(message = "Las fechas de inicio y fin son obligatorias.")
    private LocalDateTime fFinReserva;
    
    private Double pr;

    private EstadoReserva estado;      // ← agregado

    private LocalDateTime fReserva;    // ← agregado
}