package com.sena.goldenbooking.habitaciones.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.sena.goldenbooking.compartido.web.Paginacion;
import com.sena.goldenbooking.habitaciones.dto.HabitacionDto;
import com.sena.goldenbooking.habitaciones.model.EstadoHabitacion;
import com.sena.goldenbooking.habitaciones.service.HabitacionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Habitaciones", description = "Gestión de habitaciones del hotel.")
@RestController
@RequestMapping("/api/habitaciones")

public class HabitacionController {

    private final HabitacionService service;

    public HabitacionController(HabitacionService service) {
        this.service = service;
    }

    // POST /api/habitaciones
    @PostMapping
    public ResponseEntity<HabitacionDto> crear(@RequestBody HabitacionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    // GET /api/habitaciones
    // Listar todas las habitaciones con paginación
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarTodas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = Paginacion.de(page, size);
        var pagina = service.listarTodasPaginadas(pageable);

        return ResponseEntity.ok(Map.of(
            "contenido",      pagina.getContent(),
            "paginaActual",   pagina.getNumber(),
            "totalPaginas",   pagina.getTotalPages(),
            "totalElementos", pagina.getTotalElements()
        ));
    }

    // GET /api/habitaciones/disponibles
    @GetMapping("/disponibles")
    public ResponseEntity<List<HabitacionDto>> listarDisponibles() {
        return ResponseEntity.ok(service.listarPorEstado(EstadoHabitacion.DISPONIBLE));
    }

    // GET /api/habitaciones/estado/{estado}
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<HabitacionDto>> listarPorEstado(@PathVariable EstadoHabitacion estado) {
        return ResponseEntity.ok(service.listarPorEstado(estado));
    }

    // GET /api/habitaciones/tipo/{idTipo}
    @GetMapping("/tipo/{idTipo}")
    public ResponseEntity<List<HabitacionDto>> listarPorTipo(@PathVariable String idTipo) {
        return ResponseEntity.ok(service.listarPorTipo(idTipo));
    }

    // GET /api/habitaciones/{id}
    @GetMapping("/{id}")
    public ResponseEntity<HabitacionDto> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    // PUT /api/habitaciones/{id}
    @PutMapping("/{id}")
    public ResponseEntity<HabitacionDto> actualizar(
            @PathVariable String id,
            @RequestBody HabitacionDto dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    // PATCH /api/habitaciones/{id}/estado
    @PatchMapping("/{id}/estado")
    public ResponseEntity<HabitacionDto> cambiarEstado(
            @PathVariable String id,
            @RequestParam EstadoHabitacion nuevoEstado) {
        return ResponseEntity.ok(service.cambiarEstado(id, nuevoEstado));
    }

    // DELETE /api/habitaciones/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subir o reemplazar la imagen de la habitación (ADMIN)",
            description = "multipart/form-data con el campo 'archivo'. JPG, PNG o WEBP de hasta 5 MB y mínimo 400×300 px.")
    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HabitacionDto> subirImagen(@PathVariable String id,
                                                     @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(service.subirImagen(id, archivo));
    }

    @Operation(summary = "Quitar la imagen subida (ADMIN)", description = "La habitación vuelve a la imagen por defecto.")
    @DeleteMapping("/{id}/imagen")
    public ResponseEntity<HabitacionDto> eliminarImagen(@PathVariable String id) {
        return ResponseEntity.ok(service.eliminarImagen(id));
    }

    @Operation(summary = "Ver la imagen de la habitación (público)",
            description = "Para usarlo directo en <img src> (el navegador no envía el token). 404 si no tiene imagen propia.")
    @SecurityRequirements
    @GetMapping("/{id}/imagen")
    public ResponseEntity<InputStreamResource> verImagen(@PathVariable String id) throws IOException {
        var imagen = service.obtenerImagen(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getContentType()))
                .contentLength(imagen.contentLength())
                // la URL lleva ?v=<id de la imagen>: al subir otra cambia, así que cachear un día es seguro
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(imagen.getInputStream()));
    }

    // ── Galería (hasta 5 imágenes; la primera es la portada) ───────────────

    @Operation(summary = "Agregar una imagen a la galería (ADMIN)", description = "Máximo 5 por habitación. Mismas reglas de formato y tamaño.")
    @PostMapping(value = "/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HabitacionDto> agregarImagen(@PathVariable String id, @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(service.agregarImagen(id, archivo));
    }

    @Operation(summary = "Quitar una imagen de la galería (ADMIN)")
    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<HabitacionDto> quitarImagen(@PathVariable String id, @PathVariable String imagenId) {
        return ResponseEntity.ok(service.quitarImagen(id, imagenId));
    }

    @Operation(summary = "Usar una imagen como portada (ADMIN)")
    @PatchMapping("/{id}/imagenes/{imagenId}/portada")
    public ResponseEntity<HabitacionDto> elegirPortada(@PathVariable String id, @PathVariable String imagenId) {
        return ResponseEntity.ok(service.elegirPortada(id, imagenId));
    }

    @Operation(summary = "Ver una imagen de la galería (público)", description = "Para <img src> (el navegador no envía el token).")
    @GetMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<InputStreamResource> verImagenGaleria(@PathVariable String id, @PathVariable String imagenId) throws IOException {
        var imagen = service.obtenerImagenGaleria(id, imagenId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getContentType()))
                .contentLength(imagen.contentLength())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic()) // el id de la imagen no cambia
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(imagen.getInputStream()));
    }
}
