package com.sena.goldenbooking.reservasdeportivas.model;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementos que se sugieren al reservar, según el deporte, cuando el admin
 * no configuró una lista propia para el espacio.
 */
public final class ImplementosSugeridos {

    private static final Map<String, List<String>> POR_DEPORTE = Map.ofEntries(
            Map.entry("futbol", List.of("Balón", "Petos", "Conos", "Arco portátil")),
            Map.entry("baloncesto", List.of("Balón", "Petos", "Cronómetro")),
            Map.entry("voleibol", List.of("Balón", "Rodilleras", "Petos")),
            Map.entry("tenis", List.of("Raquetas", "Pelotas", "Grips")),
            Map.entry("padel", List.of("Palas", "Pelotas", "Muñequeras")),
            Map.entry("squash", List.of("Raquetas", "Pelotas", "Gafas protectoras")),
            Map.entry("natacion", List.of("Gorro", "Gafas", "Tabla", "Toalla")),
            Map.entry("piscina", List.of("Gorro", "Gafas", "Tabla", "Toalla")),
            Map.entry("golf", List.of("Palos", "Pelotas", "Tees", "Carrito")),
            Map.entry("ping pong", List.of("Raquetas", "Pelotas")),
            Map.entry("gimnasio", List.of("Toalla", "Guantes", "Colchoneta")));

    private static final List<String> GENERALES = List.of("Balón", "Petos", "Toalla", "Hidratación");

    private ImplementosSugeridos() {
        // Clase de utilidades: no se instancia
    }

    /** Sugeridos para el deporte (sin tildes ni mayúsculas); si no se reconoce, unos generales. */
    public static List<String> para(String deporte) {
        String clave = normalizar(deporte);
        return POR_DEPORTE.entrySet().stream()
                .filter(e -> clave.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(GENERALES);
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
