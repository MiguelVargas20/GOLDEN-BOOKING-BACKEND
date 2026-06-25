package com.sena.goldenbooking.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sena.goldenbooking.dtos.HabitacionDto;
import com.sena.goldenbooking.services.HabitacionService;

@RestController
@RequestMapping("/api/habitaciones")
@CrossOrigin(origins = "*")
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
    @GetMapping
    public ResponseEntity<List<HabitacionDto>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    // GET /api/habitaciones/disponibles
    @GetMapping("/disponibles")
    public ResponseEntity<List<HabitacionDto>> listarDisponibles() {
        return ResponseEntity.ok(service.listarPorEstado("disponible"));
    }

    // GET /api/habitaciones/estado/{estado}
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<HabitacionDto>> listarPorEstado(@PathVariable String estado) {
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
            @RequestParam String nuevoEstado) {
        return ResponseEntity.ok(service.cambiarEstado(id, nuevoEstado));
    }

    // DELETE /api/habitaciones/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}