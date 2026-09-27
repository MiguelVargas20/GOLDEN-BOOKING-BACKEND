package com.sena.goldenbooking.reservas.model;

/** Quién canceló una reserva (se muestra en el historial y en los paneles). */
public enum CanceladaPor {
    CLIENTE,
    ADMINISTRADOR,
    /** Vencida: seguía PENDIENTE cuando llegó su fecha (nadie la aprobó a tiempo). */
    SISTEMA
}
