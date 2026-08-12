package com.sena.goldenbooking.security;

import org.springframework.security.core.Authentication;

/**
 * Utilidad centralizada para leer el rol del usuario autenticado a partir
 * del objeto Authentication que Spring Security inyecta en los controllers.
 *
 * Antes esta misma expresión (authentication.getAuthorities().stream()...)
 * estaba copiada en cada controller que necesitaba distinguir ADMIN de
 * CLIENTE. El problema de esa duplicación no es solo estético: si mañana
 * cambia el nombre del rol o la forma de representarlo, hay que acordarse
 * de actualizarlo en cada copia — y si se te olvida una, queda un endpoint
 * con una autorización inconsistente (el mismo tipo de descuido que causó
 * el bug de compilación y los IDOR que ya corregimos).
 */
public final class AutenticacionUtils {

    private AutenticacionUtils() {
        // Clase de utilidades: no se instancia
    }

    public static boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROL_ADMIN"));
    }
}