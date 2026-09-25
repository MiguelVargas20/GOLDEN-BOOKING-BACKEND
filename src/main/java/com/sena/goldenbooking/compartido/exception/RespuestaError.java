package com.sena.goldenbooking.compartido.exception;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Formato ÚNICO de las respuestas de error de toda la API:
 *
 * <pre>
 * {
 *   "timestamp": "2026-09-25T10:15:30",
 *   "status": 409,
 *   "codigo": "CONFLICTO",            // identificador estable para el frontend
 *   "error": "Mensaje para el usuario",
 *   "path": "/api/reservas/deporte",
 *   "errores": { "campo": "..." }       // solo en errores de validación
 * }
 * </pre>
 *
 * "error" se mantiene con ese nombre porque el frontend ya lo lee
 * (extraerMensajeError en apiUtils.js). Nunca debe contener detalles
 * técnicos (nombres de colecciones, stack traces, mensajes de Mongo...):
 * esos solo van al log.
 */
public final class RespuestaError {

    private RespuestaError() {
        // Clase de utilidades: no se instancia
    }

    public static Map<String, Object> cuerpo(HttpStatus status, String codigo, String mensaje, String path) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now());
        cuerpo.put("status", status.value());
        cuerpo.put("codigo", codigo);
        cuerpo.put("error", mensaje);
        cuerpo.put("path", path);
        return cuerpo;
    }

    /**
     * Escribe el error directamente en la respuesta HTTP. Lo usan los filtros
     * de seguridad (JwtFilter, SecurityConfig), que corren ANTES de llegar a
     * los controllers y por eso no pasan por el GlobalExceptionHandler.
     */
    public static void escribir(HttpServletResponse response, HttpStatus status,
                                String codigo, String mensaje, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":" + status.value() + ","
                + "\"codigo\":\"" + json(codigo) + "\","
                + "\"error\":\"" + json(mensaje) + "\","
                + "\"path\":\"" + json(path) + "\""
                + "}");
    }

    private static String json(String valor) {
        if (valor == null) return "";
        return valor.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
