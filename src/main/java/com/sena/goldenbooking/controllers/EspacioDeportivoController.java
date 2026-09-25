package com.sena.goldenbooking.controllers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sena.goldenbooking.dtos.EspacioDeportivoDto;
import com.sena.goldenbooking.models.EstadoEspacio;
import com.sena.goldenbooking.security.AutenticacionUtils;
import com.sena.goldenbooking.services.EspacioDeportivoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Espacios deportivos (canchas, piscinas, pistas...).
 *
 * Permisos (ver SecurityConfig):
 * - GET: ADMIN o CLIENTE. El cliente no ve los espacios INACTIVOS.
 * - GET /{id}/imagen: público (lo carga el navegador con <img>, que no manda el token).
 * - POST / PUT / PATCH / DELETE: solo ADMIN.
 */
@Tag(name = "Espacios deportivos", description = "Administración de los espacios deportivos reservables y sus imágenes.")
@RestController
@RequestMapping("/api/espacios-deportivos")
public class EspacioDeportivoController {

    private final EspacioDeportivoService service;

    public EspacioDeportivoController(EspacioDeportivoService service) {
        this.service = service;
    }

    @Operation(summary = "Listar espacios deportivos",
            description = "ADMIN: todos los espacios. CLIENTE: solo ACTIVOS y en MANTENIMIENTO (estos últimos se muestran como no disponibles).")
    @GetMapping
    public ResponseEntity<List<EspacioDeportivoDto>> listar(Authentication authentication) {
        return ResponseEntity.ok(service.listar(AutenticacionUtils.esAdmin(authentication)));
    }

    @Operation(summary = "Obtener un espacio deportivo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Espacio encontrado"),
        @ApiResponse(responseCode = "404", description = "El espacio no existe")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EspacioDeportivoDto> obtener(@PathVariable String id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @Operation(summary = "Crear espacio deportivo (ADMIN)",
            description = "Si no se envía estado, queda ACTIVO. El nombre debe ser único.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Espacio creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos (horario, tarifa, capacidad...)"),
        @ApiResponse(responseCode = "409", description = "Ya existe un espacio con ese nombre")
    })
    @PostMapping
    public ResponseEntity<EspacioDeportivoDto> crear(@Valid @RequestBody EspacioDeportivoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @Operation(summary = "Actualizar espacio deportivo (ADMIN)",
            description = "Las reservas ya hechas conservan el precio con el que se crearon.")
    @PutMapping("/{id}")
    public ResponseEntity<EspacioDeportivoDto> actualizar(@PathVariable String id,
                                                          @Valid @RequestBody EspacioDeportivoDto dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @Operation(summary = "Cambiar estado del espacio (ADMIN)",
            description = "ACTIVO: reservable. MANTENIMIENTO: visible pero no reservable. INACTIVO: oculto para los clientes.")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<EspacioDeportivoDto> cambiarEstado(
            @PathVariable String id,
            @Parameter(description = "Nuevo estado", example = "MANTENIMIENTO") @RequestParam EstadoEspacio estado) {
        return ResponseEntity.ok(service.cambiarEstado(id, estado));
    }

    @Operation(summary = "Eliminar espacio deportivo (ADMIN)",
            description = "Solo si nunca tuvo reservas. Si tiene historial, se debe marcar como INACTIVO.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Espacio eliminado"),
        @ApiResponse(responseCode = "409", description = "Tiene reservas; usar estado INACTIVO")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subir o reemplazar la imagen del espacio (ADMIN)",
            description = "multipart/form-data con el campo 'archivo'. JPG, PNG o WEBP de hasta 5 MB.")
    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EspacioDeportivoDto> subirImagen(@PathVariable String id,
                                                           @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(service.subirImagen(id, archivo));
    }

    @Operation(summary = "Quitar la imagen subida (ADMIN)",
            description = "El espacio vuelve a mostrar la imagen por defecto de su deporte.")
    @DeleteMapping("/{id}/imagen")
    public ResponseEntity<EspacioDeportivoDto> eliminarImagen(@PathVariable String id) {
        return ResponseEntity.ok(service.eliminarImagen(id));
    }

    @Operation(summary = "Ver la imagen del espacio (público)",
            description = "Endpoint público para usarlo directo en <img src>. 404 si el espacio usa la imagen por defecto.")
    @SecurityRequirements // sin candado en Swagger: no requiere token
    @GetMapping("/{id}/imagen")
    public ResponseEntity<InputStreamResource> verImagen(@PathVariable String id) throws IOException {
        var imagen = service.obtenerImagen(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getContentType()))
                .contentLength(imagen.contentLength())
                // La URL incluye ?v=<id de la imagen> (ver EspacioDeportivoMapperImpl):
                // si el admin sube otra, la URL cambia. Por eso se puede cachear un
                // día sin riesgo de mostrar una imagen vieja.
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(imagen.getInputStream()));
    }
}
