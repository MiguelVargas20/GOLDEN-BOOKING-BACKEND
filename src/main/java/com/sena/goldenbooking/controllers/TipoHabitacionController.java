package com.sena.goldenbooking.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sena.goldenbooking.dtos.TipoHabitacionDto;
import com.sena.goldenbooking.services.TipoHabitacionService;
import io.swagger.v3.oas.annotations.tags.Tag;

// Controlador REST para manejar las solicitudes relacionadas con los tipos de habitación
@Tag(name = "Tipos de Habitación", description = "Gestión de los tipos/categorías de habitación.")
@RestController

// Define la ruta base para las operaciones de tipo de habitación y permite solicitudes desde cualquier origen
@RequestMapping("/api/tipohabitaciones")

// Permite solicitudes desde cualquier origen para evitar problemas de CORS
@CrossOrigin(origins = "*")
public class TipoHabitacionController {

    // Inyección de dependencia del servicio para manejar la lógica de negocio
    private final TipoHabitacionService service;

    // Constructor para inyectar el servicio
    public TipoHabitacionController(TipoHabitacionService service) {
        this.service = service;
    }


    // Define los endpoints para las operaciones CRUD utilizando los métodos del servicio y devolviendo respuestas HTTP adecuadas
    @PostMapping
    public ResponseEntity<TipoHabitacionDto> crear(@RequestBody TipoHabitacionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    // Endpoint para listar todos los tipos de habitación, devolviendo una lista de DTOs
    @GetMapping
    public ResponseEntity<List<TipoHabitacionDto>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    // Endpoint para obtener un tipo de habitación por su ID, devolviendo un DTO o una respuesta de error si no se encuentra
    @GetMapping("/{id}")
    public ResponseEntity<TipoHabitacionDto> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    // Endpoint para actualizar un tipo de habitación existente, devolviendo el DTO actualizado o una respuesta de error si no se encuentra
    @PutMapping("/{id}")
    public ResponseEntity<TipoHabitacionDto> actualizar(
            @PathVariable String id, @RequestBody TipoHabitacionDto dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    // Endpoint para eliminar un tipo de habitación por su ID, devolviendo una respuesta sin contenido o una respuesta de error si no se encuentra
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}