package com.sena.goldenbooking.reservasdeportivas.model;

/**
 * Estado de un espacio deportivo.
 * - ACTIVO: aparece en el catálogo y se puede reservar.
 * - MANTENIMIENTO: aparece en el catálogo como "no disponible" y no se puede reservar.
 * - INACTIVO: oculto para los clientes (dado de baja sin perder su historial de reservas).
 */
public enum EstadoEspacio {
    ACTIVO,
    MANTENIMIENTO,
    INACTIVO
}
