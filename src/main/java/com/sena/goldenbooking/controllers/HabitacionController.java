package com.sena.goldenbooking.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sena.goldenbooking.dtos.HabitacionDto;
import com.sena.goldenbooking.services.HabitacionService;

/* Controlador REST para gestionar habitaciones (CRUD, filtros) */
@RestController

// Base URL para todas las operaciones relacionadas con habitaciones
@RequestMapping("/api/habitaciones")

// Permitir peticiones desde cualquier origen para pruebas
@CrossOrigin(origins = "*")
public class HabitacionController {

    // Inyectamos el servicio de habitaciones a través del constructor
    private final HabitacionService habitacionService;

    // Constructor para inyección de dependencias
    public HabitacionController(HabitacionService habitacionService) {
        this.habitacionService = habitacionService;
    }

    // 1. CREAR UNA HABITACIÓN (Solo ADMIN)
    @PostMapping
    public ResponseEntity<HabitacionDto> crear(@RequestBody HabitacionDto habitacionDto) {
        HabitacionDto nueva = habitacionService.crear(habitacionDto);
        return new ResponseEntity<>(nueva, HttpStatus.CREATED);
    }

    // 2. LISTAR TODAS LAS HABITACIONES
    @GetMapping
    public ResponseEntity<List<HabitacionDto>> listarTodas() {
        return ResponseEntity.ok(habitacionService.listarTodas());
    }

    // 3. OBTENER UNA POR ID
    @GetMapping("/{id}")
    public ResponseEntity<HabitacionDto> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(habitacionService.obtenerPorId(id));
    }

    // 4. FILTRAR POR ESTADO (Disponible, Ocupada, Mantenimiento)
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<HabitacionDto>> listarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(habitacionService.listarPorEstado(estado));
    }

    // 5. FILTRAR POR TIPO DE HABITACIÓN (Usando el ID del Tipo)
    @GetMapping("/tipo/{idTipo}")
    public ResponseEntity<List<HabitacionDto>> listarPorTipo(@PathVariable String idTipo) {
        return ResponseEntity.ok(habitacionService.listarPorTipo(idTipo));
    }

    // 6. ACTUALIZAR DATOS DE HABITACIÓN
    @PutMapping("/{id}")

    // Para actualizar toda la información de la habitación (excepto el ID)
    public ResponseEntity<HabitacionDto> actualizar(@PathVariable String id, @RequestBody HabitacionDto dto) {
        return ResponseEntity.ok(habitacionService.actualizar(id, dto));
    }

    // 7. CAMBIAR SOLO EL ESTADO (Útil para recepcionistas)
    @PatchMapping("/{id}/estado")
    public ResponseEntity<HabitacionDto> cambiarEstado(

        // Cambia solo el estado de la habitación, el nuevo estado se pasa como query param
            @PathVariable String id, 
            @RequestParam String nuevoEstado) {
        return ResponseEntity.ok(habitacionService.cambiarEstado(id, nuevoEstado));
    }

    // 8. ELIMINAR HABITACIÓN
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        habitacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}