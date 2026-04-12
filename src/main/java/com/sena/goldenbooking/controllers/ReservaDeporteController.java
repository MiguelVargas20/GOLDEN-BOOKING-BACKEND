package com.sena.goldenbooking.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.services.ReservaDeporteService;

import jakarta.validation.Valid;

// Controlador REST para gestionar las reservas de deporte
@RestController

// Base URL para todas las operaciones relacionadas con reservas de deporte
@RequestMapping("/api/reservas/deporte")
@CrossOrigin(origins = "*")
public class ReservaDeporteController {

    // Inyección de la capa de servicio para manejar la lógica de negocio
    private final ReservaDeporteService service;

    // Constructor para inyectar la dependencia del servicio
    public ReservaDeporteController(ReservaDeporteService service) {
        this.service = service;
    }

    // POST /api/reservas/deporte
    @PostMapping
    public ResponseEntity<ReservaDeporteDto> crear(@Valid @RequestBody ReservaDeporteDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }
    
    // GET /api/reservas/deporte
    @GetMapping
    public ResponseEntity<List<ReservaDeporteDto>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    // GET /api/reservas/deporte/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ReservaDeporteDto> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    // GET /api/reservas/deporte/reserva/{idReserva}
    @GetMapping("/reserva/{idReserva}")
    public ResponseEntity<List<ReservaDeporteDto>> obtenerPorReserva(@PathVariable String idReserva) {
        return ResponseEntity.ok(service.obtenerPorReserva(idReserva));
    }

    // PUT /api/reservas/deporte/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ReservaDeporteDto> actualizar(
            @PathVariable String id,
            @RequestBody ReservaDeporteDto dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    // PATCH /api/reservas/deporte/{id}/cancelar
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable String id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}