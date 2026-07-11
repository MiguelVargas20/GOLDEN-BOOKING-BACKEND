package com.sena.goldenbooking.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.sena.goldenbooking.dtos.ReservaHotelDto;
import com.sena.goldenbooking.dtos.RangoOcupadoDto;
import com.sena.goldenbooking.services.ReservaHotelService;
import com.sena.goldenbooking.services.UsuarioService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;

// Controlador REST para gestionar las reservas de hotel
@Tag(name = "Reservas - Hotel", description = "Reservas de habitaciones de hotel.")
@RestController

// Base URL para todas las operaciones relacionadas con reservas de hotel
@RequestMapping("/api/reservas/hotel")

// Controlador para manejar las operaciones CRUD y de cancelación de reservas de hotel
public class ReservaHotelController {

    // Inyección de la capa de servicio para manejar la lógica de negocio
    private final ReservaHotelService service;
    // Se usa para resolver el docUsuario del usuario autenticado a partir del JWT
    private final UsuarioService usuarioService;


    // Constructor para inyectar la dependencia del servicio
    public ReservaHotelController(ReservaHotelService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
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

    // GET /api/reservas/hotel/mis-reservas
    // Endpoint dedicado para que el usuario autenticado obtenga SOLO sus propias reservas.
    // Reemplaza /usuario/{docUsuario}, que permitía a cualquiera ver reservas ajenas
    // con solo cambiar el documento en la URL.
    @GetMapping("/mis-reservas")
    public ResponseEntity<List<ReservaHotelDto>> misReservas(Authentication authentication) {
        String docUsuario = usuarioService.obtenerDocumentoPorUsername(authentication.getName());
        return ResponseEntity.ok(service.obtenerPorUsuario(docUsuario));
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

    // GET /api/reservas/hotel/habitacion/{idHabitacion}/ocupadas
    // Devuelve los rangos de fechas donde ESA habitación ya tiene reservas activas.
    // El frontend lo usa para bloquear esas fechas en el datepicker antes de
    // que el usuario intente reservar (evita el viaje redondo al backend
    // solo para enterarse del conflicto).
    @GetMapping("/habitacion/{idHabitacion}/ocupadas")
    public ResponseEntity<List<RangoOcupadoDto>> obtenerFechasOcupadas(@PathVariable String idHabitacion) {
        return ResponseEntity.ok(service.obtenerFechasOcupadas(idHabitacion));
    }

    // PUT /api/reservas/hotel/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ReservaHotelDto> actualizar(
            @PathVariable String id,
            @RequestBody ReservaHotelDto dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    // PATCH /api/reservas/hotel/{id}/cancelar
    // Solo el dueño de la reserva o un ADMIN pueden cancelarla (fix IDOR)
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable String id, Authentication authentication) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROL_ADMIN"));
        String docUsuario = usuarioService.obtenerDocumentoPorUsername(authentication.getName());

        service.cancelar(id, docUsuario, esAdmin);
        return ResponseEntity.noContent().build();
    }
}