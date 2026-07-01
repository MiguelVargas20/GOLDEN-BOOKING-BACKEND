package com.sena.goldenbooking.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.services.ReservaDeporteService;
import com.sena.goldenbooking.services.UsuarioService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.Map;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;

// Controlador REST para gestionar las reservas de deporte
@Tag(name = "Reservas - Deporte", description = "Reservas de canchas e instalaciones deportivas.")
@RestController

// Base URL para todas las operaciones relacionadas con reservas de deporte
@RequestMapping("/api/reservas/deporte")

public class ReservaDeporteController {

    // Inyección de la capa de servicio para manejar la lógica de negocio
    private final ReservaDeporteService service;
    // Se usa para resolver el docUsuario del usuario autenticado a partir del JWT
    private final UsuarioService usuarioService;

    // Constructor para inyectar la dependencia del servicio
    public ReservaDeporteController(ReservaDeporteService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    // POST /api/reservas/deporte
    @PostMapping
    public ResponseEntity<ReservaDeporteDto> crear(@Valid @RequestBody ReservaDeporteDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }
    
    // GET /api/reservas/deporte
    // Endpoint para listar todas las reservas de deporte con paginación (uso exclusivo de ADMIN en el front)
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarTodas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        var pagina = service.listarTodasPaginadas(pageable);

        return ResponseEntity.ok(Map.of(
            "contenido",      pagina.getContent(),
            "paginaActual",   pagina.getNumber(),
            "totalPaginas",   pagina.getTotalPages(),
            "totalElementos", pagina.getTotalElements()
        ));
    }

    // GET /api/reservas/deporte/mis-reservas
    // Endpoint dedicado para que el usuario autenticado obtenga SOLO sus propias reservas.
    // Reemplaza el patrón inseguro de traer 100 reservas y filtrar en el frontend.
    @GetMapping("/mis-reservas")
    public ResponseEntity<List<ReservaDeporteDto>> misReservas(Authentication authentication) {
        String docUsuario = usuarioService.obtenerDocumentoPorUsername(authentication.getName());
        return ResponseEntity.ok(service.obtenerPorUsuario(docUsuario));
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