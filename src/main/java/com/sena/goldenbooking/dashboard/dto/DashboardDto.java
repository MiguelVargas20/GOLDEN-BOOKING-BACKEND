package com.sena.goldenbooking.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Todo lo que muestra el dashboard del administrador, en una sola respuesta
 * (una sola petición al cargar y en cada actualización automática).
 */
@Schema(description = "Resumen operativo del día para el panel del administrador.")
public record DashboardDto(
        @Schema(description = "Día que se resume (hora de Colombia).") LocalDate fecha,
        @Schema(description = "Cuándo se generó este resumen.") LocalDateTime generadoEn,
        @Schema(description = "Días que abarcan la tendencia y los espacios más reservados.") int periodoDias,
        IndicadoresDto indicadores,
        @Schema(description = "Reservas deportivas, check-ins y check-outs de hoy, en orden de hora.") List<EventoAgendaDto> agendaHoy,
        @Schema(description = "Reservas pendientes de aprobación, la más próxima primero.") List<ReservaPendienteDto> pendientes,
        @Schema(description = "Estado de cada habitación hoy.") List<HabitacionHoyDto> habitaciones,
        @Schema(description = "Reservas recibidas por día en el periodo.") List<PuntoTendenciaDto> tendencia,
        @Schema(description = "Espacios deportivos más reservados en el periodo.") List<EspacioPopularDto> espaciosTop) {
}
