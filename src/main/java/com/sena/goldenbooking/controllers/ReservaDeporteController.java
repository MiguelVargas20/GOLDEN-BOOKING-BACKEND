package com.sena.goldenbooking.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.security.AutenticacionUtils;
import com.sena.goldenbooking.services.ReservaDeporteService;
import com.sena.goldenbooking.services.UsuarioService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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
    // Si quien reserva es CLIENTE, se ignora cualquier docUsuario que venga en el
    // body y se fuerza el del usuario autenticado — así nadie puede crear una
    // reserva "a nombre de" otro documento con solo cambiar el JSON.
    // Un ADMIN sí puede reservar a nombre de otra persona (ej. recepción con un usuario presencial).
    @PostMapping
    public ResponseEntity<ReservaDeporteDto> crear(@Valid @RequestBody ReservaDeporteDto dto, Authentication authentication) {
        boolean esAdmin = AutenticacionUtils.esAdmin(authentication);
        if (!esAdmin) {
            dto.setDocUsuario(usuarioService.obtenerDocumentoPorUsername(authentication.getName()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }
    
    // GET /api/reservas/deporte — SOLO ADMIN.
    // Antes el comentario decía "uso exclusivo de ADMIN en el front", pero el
    // backend no lo exigía: cualquier CLIENTE podía llamarlo directo y ver
    // las reservas de todos. Ahora se valida también aquí.
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarTodas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        if (!AutenticacionUtils.esAdmin(authentication)) {
            throw new com.sena.goldenbooking.exception.AccesoDenegadoException(
                    "Solo un administrador puede listar todas las reservas.");
        }

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
    // Solo el dueño de la reserva o un ADMIN pueden verla (fix IDOR)
    @GetMapping("/{id}")
    public ResponseEntity<ReservaDeporteDto> obtenerPorId(@PathVariable String id, Authentication authentication) {
        boolean esAdmin = AutenticacionUtils.esAdmin(authentication);
        String docUsuario = usuarioService.obtenerDocumentoPorUsername(authentication.getName());

        return ResponseEntity.ok(service.obtenerPorId(id, docUsuario, esAdmin));
    }

    // GET /api/reservas/deporte/reserva/{idReserva}
    @GetMapping("/reserva/{idReserva}")
    public ResponseEntity<List<ReservaDeporteDto>> obtenerPorReserva(@PathVariable String idReserva) {
        return ResponseEntity.ok(service.obtenerPorReserva(idReserva));
    }

    // PUT /api/reservas/deporte/{id}
    // Solo el dueño de la reserva o un ADMIN pueden actualizarla (fix IDOR)
    @PutMapping("/{id}")
    public ResponseEntity<ReservaDeporteDto> actualizar(
            @PathVariable String id,
            @Valid @RequestBody ReservaDeporteDto dto,
            Authentication authentication) {
        boolean esAdmin = AutenticacionUtils.esAdmin(authentication);
        String docUsuario = usuarioService.obtenerDocumentoPorUsername(authentication.getName());

        return ResponseEntity.ok(service.actualizar(id, dto, docUsuario, esAdmin));
    }

    // PATCH /api/reservas/deporte/{id}/cancelar
    // Solo el dueño de la reserva o un ADMIN pueden cancelarla (fix IDOR)
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable String id, Authentication authentication) {
        boolean esAdmin = AutenticacionUtils.esAdmin(authentication);
        String docUsuario = usuarioService.obtenerDocumentoPorUsername(authentication.getName());

        service.cancelar(id, docUsuario, esAdmin);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/reservas/deporte/{id}/confirmar — SOLO ADMIN
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmar(@PathVariable String id, Authentication authentication) {
        if (!AutenticacionUtils.esAdmin(authentication)) {
            throw new com.sena.goldenbooking.exception.AccesoDenegadoException(
                    "Solo un administrador puede confirmar reservas.");
        }
        service.confirmar(id);
        return ResponseEntity.noContent().build();
    }
}