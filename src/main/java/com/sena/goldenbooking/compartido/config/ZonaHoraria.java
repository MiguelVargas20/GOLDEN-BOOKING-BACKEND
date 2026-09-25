package com.sena.goldenbooking.compartido.config;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Zona horaria del negocio (Colombia).
 *
 * Las fechas de las reservas (check-in, inicio de cancha...) llegan del
 * frontend como hora local de Colombia, sin zona. Pero el servidor en AWS
 * corre en UTC, así que LocalDateTime.now() daba una hora 5h adelantada:
 * la regla de "cancelar con 24h de anticipación", los recordatorios de
 * 24h/2h y la finalización automática se calculaban contra un "ahora"
 * equivocado, y el .ics adjunto al correo marcaba el evento 5h corrido.
 *
 * Se usa esta zona SOLO para comparar "ahora" contra fechas elegidas por el
 * usuario, en vez de cambiar la zona por defecto de toda la JVM: eso haría
 * que Spring Data convirtiera distinto las fechas ya guardadas en Mongo y
 * todas las reservas existentes se verían desplazadas 5 horas.
 *
 * Configurable con la variable de entorno APP_ZONA_HORARIA.
 */
public final class ZonaHoraria {

    public static final ZoneId ZONA = ZoneId.of(
            System.getenv().getOrDefault("APP_ZONA_HORARIA", "America/Bogota"));

    private ZonaHoraria() {
        // Clase de utilidades: no se instancia
    }

    /** Fecha y hora actual en la zona del negocio (no la del servidor). */
    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }
}
