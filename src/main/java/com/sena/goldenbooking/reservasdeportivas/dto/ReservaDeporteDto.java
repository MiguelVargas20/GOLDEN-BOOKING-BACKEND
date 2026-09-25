package com.sena.goldenbooking.reservasdeportivas.dto;

import java.time.LocalDateTime;

import com.sena.goldenbooking.reservas.model.CanceladaPor;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reserva de un espacio deportivo.
 *
 * Para CREAR solo se envían: espacioId, fInicioReserva, fFinReserva y los
 * extras (implAlquilados, rqrEntrenador). El resto lo calcula el servidor:
 * nombre del espacio, precio (según la tarifa del espacio), estado inicial
 * PENDIENTE y, si quien reserva es CLIENTE, su documento.
 */
@Schema(description = "Reserva de un espacio deportivo.")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaDeporteDto {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Identificador de la reserva.")
    private String idD;

    @Schema(description = "Documento del titular. Si reserva un CLIENTE se toma de su sesión y se ignora este valor; "
            + "un ADMIN puede reservar a nombre de otra persona.", example = "1001234567")
    @NotBlank(message = "El documento del usuario es obligatorio.")
    private String docUsuario;

    @Schema(description = "Id del espacio deportivo a reservar (GET /api/espacios-deportivos).",
            example = "66f1c2a9e4b0a1b2c3d4e5f6")
    private String espacioId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Nombre del espacio al momento de reservar.",
            example = "Cancha de Fútbol 1")
    private String tCancha;

    @Schema(description = "Implementos que se alquilan (opcional).", example = "2 balones, petos")
    @Size(max = 200, message = "Los implementos no pueden superar 200 caracteres.")
    private String implAlquilados;

    @Schema(description = "Si se solicita entrenador.")
    private boolean rqrEntrenador;

    @Schema(description = "Inicio (hora de Colombia, sin zona).", example = "2026-10-01T10:00:00")
    @NotNull(message = "Las fechas de inicio y fin son obligatorias.")
    private LocalDateTime fInicioReserva;

    @Schema(description = "Fin (hora de Colombia, sin zona). Mínimo 1 hora después del inicio.", example = "2026-10-01T12:00:00")
    @NotNull(message = "Las fechas de inicio y fin son obligatorias.")
    private LocalDateTime fFinReserva;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Precio total calculado con la tarifa del espacio.")
    private Double pr;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "PENDIENTE al crearse; el admin la aprueba (CONFIRMADA) o la cancela.")
    private EstadoReserva estado;

    // ── Trazabilidad (solo lectura) ──────────────────────────────────────
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "true si la registró un administrador a nombre del cliente.")
    private boolean registradaPorAdministrador;
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
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
