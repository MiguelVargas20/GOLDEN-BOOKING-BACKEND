package com.sena.goldenbooking.reservas.model;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un cambio en el historial de una reserva: qué pasó, cuándo y quién lo hizo.
 * Se guarda dentro de la reserva (hotel o deporte) y lo ve el administrador.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Cambio en el historial de una reserva.")
public class EventoReserva {

    private AccionReserva accion;

    private LocalDateTime fecha;

    @Schema(description = "Usuario que hizo el cambio (\"sistema\" si fue automático).", example = "admin")
    private String usuario;

    @Schema(description = "CLIENTE, ADMINISTRADOR o SISTEMA.", example = "ADMINISTRADOR")
    private String rol;

    @Schema(description = "Información adicional: motivo, fechas anteriores, calificación...")
    private String detalle;
}
