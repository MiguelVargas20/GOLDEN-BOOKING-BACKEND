package com.sena.goldenbooking.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// Importamos el Sl4j para manejo de logs
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Errores de validación @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));

        log.warn("Error de validación en la petición: {}", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "errores", errores
        ));
    }

    // 2. Peticiones inválidas / Errores de negocio controlados (IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        // Para errores controlados de negocio, un WARN en los logs basta
        log.warn("Petición inválida: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", ex.getMessage()
        ));
    }

    // 3. Recurso no encontrado (reserva, usuario, etc.)
    @ExceptionHandler(com.sena.goldenbooking.exception.ReservaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrada(com.sena.goldenbooking.exception.ReservaNoEncontradaException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.NOT_FOUND.value(),
                "error", ex.getMessage()
        ));
    }

    // 4. Conflictos de negocio (horario ocupado, reserva ya cancelada, etc.)
    @ExceptionHandler(com.sena.goldenbooking.exception.ConflictoDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleConflicto(com.sena.goldenbooking.exception.ConflictoDeNegocioException ex) {
        log.warn("Conflicto de negocio: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.CONFLICT.value(),
                "error", ex.getMessage()
        ));
    }

    // 5. Acceso denegado por no ser dueño del recurso (protección IDOR)
    @ExceptionHandler(com.sena.goldenbooking.exception.AccesoDenegadoException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoDenegado(com.sena.goldenbooking.exception.AccesoDenegadoException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.FORBIDDEN.value(),
                "error", ex.getMessage()
        ));
    }

    // 6. Otros errores de negocio o tiempo de ejecución (RuntimeException genérica, ya no debería usarse en Reserva Deporte)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.warn("Error de ejecución (RuntimeException): {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", ex.getMessage()
        ));
    }

    // 7. Cualquier otro error no controlado (La magia del error crítico)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        // AQUÍ ESTÁ LA MAGIA: El usuario recibe una respuesta limpia sin exponer tripas del sistema,
        // pero tú guardas todo el StackTrace en tus logs (ej. errors.log) para debuguear.
        log.error("Error crítico inesperado procesando la petición", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error", "Ocurrió un error interno en el servidor."
        ));
    }
}