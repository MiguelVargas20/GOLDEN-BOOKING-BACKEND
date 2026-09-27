package com.sena.goldenbooking.security;

import java.util.Arrays;
import java.util.List;

/**
 * Lee la lista de orígenes permitidos (app.cors.allowed-origins / CORS_ALLOWED_ORIGINS),
 * la misma para CORS y para el WebSocket.
 *
 * Acepta comodines, p. ej. {@code https://goldenbooking-*.vercel.app}, para que también
 * funcionen los despliegues de vista previa de Vercel. Quita la "/" final porque el
 * navegador envía el origen sin ella y si no, nunca coincidiría.
 */
public final class OrigenesPermitidos {

    private OrigenesPermitidos() {
    }

    public static List<String> parsear(String valor) {
        if (valor == null) return List.of();
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .map(origen -> origen.replaceAll("/+$", ""))
                .filter(origen -> !origen.isEmpty())
                .toList();
    }
}
