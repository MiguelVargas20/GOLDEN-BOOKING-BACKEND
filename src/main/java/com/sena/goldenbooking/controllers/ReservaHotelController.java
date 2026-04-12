package com.sena.goldenbooking.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sena.goldenbooking.dtos.ReservaHotelDto;
import com.sena.goldenbooking.services.ReservaHotelService;

import jakarta.validation.Valid;

// Controlador REST para gestionar las reservas de hotel
@RestController

// Base URL para todas las operaciones relacionadas con reservas de hotel
@RequestMapping("/api/reservas/hotel")
@CrossOrigin(origins = "*")

// Controlador para manejar las operaciones CRUD y de cancelación de reservas de hotel
public class ReservaHotelController {

    // Inyección de la capa de servicio para manejar la lógica de negocio
    private final ReservaHotelService service;


    // Constructor para inyectar la dependencia del servicio
    public ReservaHotelController(ReservaHotelService service) {
        this.service = service;
    }

    // POST /api/reservas/hotel
    // ReservaHotelController
    @PostMapping
    public ResponseEntity<ReservaHotelDto> crear(@Valid @RequestBody ReservaHotelDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    // GET /api/reservas/hotel
    @GetMapping
    public ResponseEntity<List<ReservaHotelDto>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    // GET /api/reservas/hotel/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ReservaHotelDto> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    // GET /api/reservas/hotel/reserva/{idReserva}
    @GetMapping("/reserva/{idReserva}")
    public ResponseEntity<List<ReservaHotelDto>> obtenerPorReserva(@PathVariable String idReserva) {
        return ResponseEntity.ok(service.obtenerPorReserva(idReserva));
    }

    // PUT /api/reservas/hotel/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ReservaHotelDto> actualizar(
            @PathVariable String id,
            @RequestBody ReservaHotelDto dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    // PATCH /api/reservas/hotel/{id}/cancelar
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable String id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}