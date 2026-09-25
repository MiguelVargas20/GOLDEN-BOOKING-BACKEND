package com.sena.goldenbooking.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicadores principales del dashboard.")
public record IndicadoresDto(
        @Schema(description = "Reservas deportivas pendientes de aprobación.") long pendientesDeporte,
        @Schema(description = "Reservas hoteleras pendientes de aprobación.") long pendientesHotel,
        @Schema(description = "Reservas deportivas (no canceladas) que se usan hoy.") long reservasDeporteHoy,
        @Schema(description = "Huéspedes que llegan hoy.") long checkInsHoy,
        @Schema(description = "Huéspedes que se van hoy.") long checkOutsHoy,
        @Schema(description = "Habitaciones en total.") long habitacionesTotal,
        @Schema(description = "Habitaciones ocupadas hoy por una reserva confirmada.") long habitacionesOcupadas,
        @Schema(description = "Habitaciones reservadas hoy con la reserva aún pendiente.") long habitacionesReservadasPendientes,
        @Schema(description = "Habitaciones en mantenimiento.") long habitacionesMantenimiento,
        @Schema(description = "Ingresos del mes en curso (reservas confirmadas y finalizadas que se usan este mes).") double ingresosMes,
        @Schema(description = "Ingresos del mes anterior, para comparar.") double ingresosMesAnterior,
        @Schema(description = "Mensajes de contacto sin leer.") long mensajesNoLeidos) {
}
