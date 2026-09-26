package com.sena.goldenbooking.reservas.model;

/** Qué le pasó a una reserva (cada cambio queda en su historial). */
public enum AccionReserva {
    CREADA,
    CONFIRMADA,
    CANCELADA,
    REPROGRAMADA,
    FINALIZADA,
    VENCIDA,
    CALIFICADA,
    ACOMPANANTES
}
