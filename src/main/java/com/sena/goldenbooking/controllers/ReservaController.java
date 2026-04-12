package com.sena.goldenbooking.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.TipoReserva;
import com.sena.goldenbooking.services.ReservaService;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    // POST /api/reservas
    @PostMapping
    public ResponseEntity<ReservaDto> crear(@RequestBody ReservaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crearReserva(dto));
    }

    // GET /api/reservas
    @GetMapping
    public ResponseEntity<List<ReservaDto>> listar() {
        return ResponseEntity.ok(reservaService.listarReservas());
    }

    // GET /api/reservas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ReservaDto> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    // GET /api/reservas/usuario/{docUsuario}
    @GetMapping("/usuario/{docUsuario}")
    public ResponseEntity<List<ReservaDto>> obtenerPorUsuario(@PathVariable String docUsuario) {
        return ResponseEntity.ok(reservaService.obtenerPorUsuario(docUsuario));
    }

    // PUT /api/reservas/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ReservaDto> actualizar(@PathVariable String id,@RequestBody ReservaDto dto) {
        return ResponseEntity.ok(reservaService.actualizarReserva(id, dto));
    }
    
    // GET /api/reservas/estado/{estado}
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ReservaDto>> obtenerPorEstado(@PathVariable EstadoReserva estado) {
        return ResponseEntity.ok(reservaService.obtenerPorEstado(estado));
    }

    // GET /api/reservas/usuario/{docUsuario}/tipo/{tipo}
    @GetMapping("/usuario/{docUsuario}/tipo/{tipo}")
    public ResponseEntity<List<ReservaDto>> obtenerPorUsuarioYTipo(
            @PathVariable String docUsuario,
            @PathVariable TipoReserva tipo) {
        return ResponseEntity.ok(reservaService.obtenerPorUsuarioYTipo(docUsuario, tipo));
}

    // PATCH /api/reservas/{id}/cancelar
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable String id) {
        reservaService.cancelarReserva(id);
        return ResponseEntity.noContent().build();
    }
}