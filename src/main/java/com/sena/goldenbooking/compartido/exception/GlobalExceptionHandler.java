package com.sena.goldenbooking.compartido.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * Manejo centralizado de errores de la API.
 *
 * Reglas:
 * 1. Todas las respuestas de error tienen el mismo formato (ver RespuestaError):
 *    timestamp, status, codigo, error (mensaje para el usuario) y path.
 * 2. Al usuario NUNCA le llegan detalles técnicos (mensajes de Mongo, de
 *    Jackson, nombres de clases, stack traces). Esos van solo al log.
 * 3. Solo se muestran tal cual los mensajes de NUESTRAS excepciones de
 *    negocio (SolicitudInvalida, ConflictoDeNegocio, RecursoNoEncontrado...),
 *    que están escritos pensando en el usuario.
 * 4. Cada tipo de error usa su código HTTP correcto (antes todo lo que fuera
 *    RuntimeException respondía 400, incluso fallos internos, y una contraseña
 *    incorrecta respondía 400 en vez de 401).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String codigo,
                                                                 String mensaje, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(RespuestaError.cuerpo(status, codigo, mensaje, request.getRequestURI()));
    }

    // ── 400: datos de entrada ──────────────────────────────────────────────

    /** @Valid falló: se devuelve el detalle por campo en "errores". */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));

        log.warn("Validación fallida en {}: {}", request.getRequestURI(), errores);

        // "error" lleva los mensajes unidos para que el front los muestre directo
        String mensaje = errores.isEmpty()
                ? "Revisa los datos ingresados."
                : String.join(" ", errores.values());
        Map<String, Object> cuerpo = RespuestaError.cuerpo(HttpStatus.BAD_REQUEST, "VALIDACION", mensaje, request.getRequestURI());
        cuerpo.put("errores", errores);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    /** Regla de negocio incumplida (fechas en el pasado, etc.). Mensaje escrito para el usuario. */
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, Object>> solicitudInvalida(SolicitudInvalidaException ex, HttpServletRequest request) {
        log.warn("Solicitud inválida en {}: {}", request.getRequestURI(), ex.getMessage());
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", ex.getMessage(), request);
    }

    /** JSON mal formado, fecha con formato incorrecto, valor de enum inexistente... */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> cuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Cuerpo ilegible en {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return respuesta(HttpStatus.BAD_REQUEST, "FORMATO_INVALIDO",
                "El formato de los datos enviados no es válido. Revisa fechas, números y opciones seleccionadas.", request);
    }

    /** Parámetro de URL con tipo incorrecto (ej. /estado/ABC en vez de un estado válido). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipoParametro(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Parámetro '{}' inválido en {}: {}", ex.getName(), request.getRequestURI(), ex.getValue());
        return respuesta(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "El valor del parámetro '" + ex.getName() + "' no es válido.", request);
    }

    /** Falta un parámetro, cabecera o cookie obligatoria. */
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Map<String, Object>> faltaDato(ServletRequestBindingException ex, HttpServletRequest request) {
        log.warn("Falta información en {}: {}", request.getRequestURI(), ex.getMessage());
        return respuesta(HttpStatus.BAD_REQUEST, "FALTAN_DATOS",
                "Falta información obligatoria en la solicitud.", request);
    }

    /**
     * IllegalArgumentException genérica: la lanzan librerías con mensajes
     * técnicos, así que NO se muestra su mensaje (para reglas de negocio se
     * usa SolicitudInvalidaException).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentoInvalido(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Argumento inválido en {}: {}", request.getRequestURI(), ex.getMessage());
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud contiene datos no válidos.", request);
    }

    // ── 401 / 403 / 429: autenticación y permisos ──────────────────────────

    /** Usuario o contraseña incorrectos (antes respondía 400 con "Bad credentials"). */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> autenticacion(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Autenticación fallida en {}: {}", request.getRequestURI(), ex.getClass().getSimpleName());
        return respuesta(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                "Usuario o contraseña incorrectos.", request);
    }

    /** Refresh token inválido, expirado o reusado: hay que iniciar sesión de nuevo. */
    @ExceptionHandler(RefreshTokenInvalidoException.class)
    public ResponseEntity<Map<String, Object>> refreshInvalido(RefreshTokenInvalidoException ex, HttpServletRequest request) {
        log.warn("Refresh token rechazado: {}", ex.getMessage());
        return respuesta(HttpStatus.UNAUTHORIZED, "SESION_EXPIRADA", ex.getMessage(), request);
    }

    /** No es dueño del recurso (protección IDOR) o no tiene el rol necesario. */
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, Object>> accesoDenegado(AccesoDenegadoException ex, HttpServletRequest request) {
        log.warn("Acceso denegado en {}: {}", request.getRequestURI(), ex.getMessage());
        return respuesta(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accesoDenegadoSpring(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado (Spring Security) en {}", request.getRequestURI());
        return respuesta(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                "No tienes permiso para realizar esta acción.", request);
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<Map<String, Object>> demasiadosIntentos(DemasiadosIntentosException ex, HttpServletRequest request) {
        log.warn("Rate limit alcanzado en {}: {}", request.getRequestURI(), ex.getMessage());
        return respuesta(HttpStatus.TOO_MANY_REQUESTS, "DEMASIADOS_INTENTOS", ex.getMessage(), request);
    }

    // ── 404 / 405 / 409 / 415: recursos y conflictos ───────────────────────

    @ExceptionHandler({ RecursoNoEncontradoException.class, ReservaNoEncontradaException.class })
    public ResponseEntity<Map<String, Object>> noEncontrado(RuntimeException ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado en {}: {}", request.getRequestURI(), ex.getMessage());
        return respuesta(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", ex.getMessage(), request);
    }

    /** Ruta inexistente (antes caía en el manejador genérico y respondía 500). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> rutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "El recurso solicitado no existe.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> metodoNoPermitido(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO",
                "Esta operación no está permitida en este recurso.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> formatoNoSoportado(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FORMATO_NO_SOPORTADO",
                "El formato de la solicitud no es compatible. Envía los datos como JSON.", request);
    }

    /** Conflicto de negocio (horario ocupado, reserva ya cancelada...). */
    @ExceptionHandler(ConflictoDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoDeNegocioException ex, HttpServletRequest request) {
        log.warn("Conflicto de negocio en {}: {}", request.getRequestURI(), ex.getMessage());
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO", ex.getMessage(), request);
    }

    /**
     * Índice único de Mongo violado (correo, usuario, documento repetido...).
     * El mensaje original de Mongo incluye la colección, el índice y el valor
     * duplicado ("E11000 duplicate key ... correo: x@y.com"): solo va al log.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> duplicado(DuplicateKeyException ex, HttpServletRequest request) {
        log.warn("Registro duplicado en {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return respuesta(HttpStatus.CONFLICT, "DATO_DUPLICADO",
                "No fue posible guardar la información porque algunos datos ya están en uso. Verifica e intenta de nuevo.", request);
    }

    /** Archivo más grande que spring.servlet.multipart.max-file-size (imágenes de espacios). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> archivoMuyGrande(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Archivo demasiado grande en {}", request.getRequestURI());
        return respuesta(HttpStatus.CONTENT_TOO_LARGE, "ARCHIVO_MUY_GRANDE",
                "El archivo es demasiado grande. El tamaño máximo es 5 MB.", request);
    }

    // ── 5xx: fallos internos ───────────────────────────────────────────────

    /** Base de datos caída, timeout, etc. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> baseDeDatos(DataAccessException ex, HttpServletRequest request) {
        log.error("Error de acceso a datos en {}", request.getRequestURI(), ex);
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE",
                "No pudimos procesar tu solicitud en este momento. Intenta de nuevo en unos minutos.", request);
    }

    /**
     * Cualquier otro error no previsto. Antes existía un manejador de
     * RuntimeException que respondía 400 con ex.getMessage(): escondía bugs
     * reales como "error del usuario" y podía mostrarle mensajes técnicos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado procesando {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error inesperado. Intenta de nuevo más tarde.", request);
    }
}
