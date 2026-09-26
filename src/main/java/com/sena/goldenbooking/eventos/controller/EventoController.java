package com.sena.goldenbooking.eventos.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sena.goldenbooking.eventos.dto.EventoDto;
import com.sena.goldenbooking.eventos.service.EventoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Eventos", description = "Bailes, festivales y actividades del club.")
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService service;

    public EventoController(EventoService service) {
        this.service = service;
    }

    @Operation(summary = "Próximos eventos publicados", description = "Para la portada del cliente, el más próximo primero.")
    @GetMapping("/proximos")
    public ResponseEntity<List<EventoDto>> proximos() {
        return ResponseEntity.ok(service.proximos());
    }

    @Operation(summary = "Todos los eventos, incluidos borradores (ADMIN)")
    @GetMapping
    public ResponseEntity<List<EventoDto>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @Operation(summary = "Crear un evento (ADMIN)", description = "Si se crea publicado, los clientes reciben una notificación.")
    @PostMapping
    public ResponseEntity<EventoDto> crear(@Valid @RequestBody EventoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @Operation(summary = "Editar un evento (ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<EventoDto> actualizar(@PathVariable String id, @Valid @RequestBody EventoDto dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @Operation(summary = "Eliminar un evento (ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subir o reemplazar la imagen del evento (ADMIN)")
    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventoDto> subirImagen(@PathVariable String id, @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(service.subirImagen(id, archivo));
    }

    @Operation(summary = "Quitar la imagen del evento (ADMIN)")
    @DeleteMapping("/{id}/imagen")
    public ResponseEntity<EventoDto> eliminarImagen(@PathVariable String id) {
        return ResponseEntity.ok(service.eliminarImagen(id));
    }

    @Operation(summary = "Ver la imagen del evento (público)")
    @GetMapping("/{id}/imagen")
    public ResponseEntity<InputStreamResource> verImagen(@PathVariable String id) throws IOException {
        var imagen = service.obtenerImagen(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getContentType()))
                .contentLength(imagen.contentLength())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(imagen.getInputStream()));
    }
}
