package com.sena.goldenbooking.dtos;

import java.time.LocalDateTime;

import com.sena.goldenbooking.models.CanceladaPor;
import com.sena.goldenbooking.models.EstadoHabitacion;
import com.sena.goldenbooking.models.EstadoReserva;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reserva de una habitación de hotel.
 *
 * Para CREAR solo se envían: idHabitacion, fCheckIn y fCheckOut (y docUsuario,
 * que se ignora si reserva un CLIENTE). Noches, precio y estado (PENDIENTE)
 * los calcula el servidor.
 */
@Schema(description = "Reserva de una habitación de hotel.")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaHotelDto {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Identificador de la reserva.")
    private String idH;

    @Schema(description = "Documento del titular. Si reserva un CLIENTE se toma de su sesión.", example = "1001234567")
    @NotBlank(message = "El documento del usuario es obligatorio.")
    private String docUsuario;

    @Schema(description = "Id de la habitación.", example = "66f1c2a9e4b0a1b2c3d4e5f6")
    @NotBlank(message = "El ID de la habitación es obligatorio.")
    private String idHabitacion;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "101")
    private String numeroHabitacion;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "Suite")
    private String tHabitacion;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Precio por noche al momento de reservar.")
    private Double pNoche;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private EstadoHabitacion estHabitacion;

    @Schema(description = "Check-in.", example = "2026-10-01T15:00:00")
    @NotNull(message = "Las fechas de check-in y check-out son obligatorias.")
    private LocalDateTime fCheckIn;

    @Schema(description = "Check-out.", example = "2026-10-03T12:00:00")
    @NotNull(message = "Las fechas de check-in y check-out son obligatorias.")
    private LocalDateTime fCheckOut;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Integer noch;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Double pTotal;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "PENDIENTE al crearse; el admin la aprueba (CONFIRMADA) o la cancela.")
    private EstadoReserva estado;

    // ── Trazabilidad (solo lectura) ──────────────────────────────────────
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "true si la registró un administrador a nombre del cliente.")
    private boolean registradaPorAdministrador;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Cuándo se hizo la solicitud.")
    private LocalDateTime fechaSolicitud;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaConfirmacion;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaCancelacion;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private CanceladaPor canceladaPor;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String motivoCancelacion;

    // ── Datos del cliente (solo en los listados del admin) ────────────────
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Solo en los listados del administrador.")
    private String nombreCliente;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Solo en los listados del administrador.")
    private String correoCliente;
}
