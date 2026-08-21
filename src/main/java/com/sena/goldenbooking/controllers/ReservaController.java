package com.sena.goldenbooking.controllers;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.exception.AccesoDenegadoException;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.TipoReserva;
import com.sena.goldenbooking.security.AutenticacionUtils;
import com.sena.goldenbooking.services.ReservaService;
import com.sena.goldenbooking.services.UsuarioService;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Reservas", description = "Operaciones generales sobre reservas.")
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    // Se usa para resolver el docUsuario del usuario autenticado a partir del JWT,
    // igual que ya se hace en ReservaHotelController/ReservaDeporteController.
    private final UsuarioService usuarioService;

    public ReservaController(ReservaService reservaService, UsuarioService usuarioService) {
        this.reservaService = reservaService;
        this.usuarioService = usuarioService;
    }

    // POST /api/reservas
    // Mismo criterio que ReservaHotelController.crear(): si quien reserva es
    // CLIENTE, se ignora cualquier docUsuario que venga en el body y se fuerza
    // el del usuario autenticado — evita crear una reserva "a nombre de" otro
    // documento con solo cambiar el JSON. Un ADMIN sí puede reservar por otro.
    @PostMapping
    public ResponseEntity<ReservaDto> crear(@RequestBody ReservaDto dto, Authentication authentication) {
        boolean esAdmin = AutenticacionUtils.esAdmin(authentication);
        if (!esAdmin) {
            dto.setDocUsuario(usuarioService.obtenerDocumentoPorUsername(authentication.getName()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crearReserva(dto));
    }

    // GET /api/reservas — SOLO ADMIN (fix: antes cualquier CLIENTE veía las reservas de todos)
    @GetMapping
    public ResponseEntity<List<ReservaDto>> listar(Authentication authentication) {
        exigirAdmin(authentication);
        return ResponseEntity.ok(reservaService.listarReservas());
    }

    // GET /api/reservas/{id} — fix IDOR: solo el dueño de la reserva o un ADMIN
    @GetMapping("/{id}")
    public ResponseEntity<ReservaDto> obtenerPorId(@PathVariable String id, Authentication authentication) {
        boolean esAdmin = AutenticacionUtils.esAdmin(authentication);
        String docUsuario = usuarioService.obtenerDocumentoPorUsername(authentication.getName());
        return ResponseEntity.ok(reservaService.obtenerPorId(id, docUsuario, esAdmin));
    }

    // GET /api/reservas/usuario/{docUsuario} — fix IDOR: un CLIENTE solo puede
    // pedir su propio documento; un ADMIN puede pedir cualquiera.
    @GetMapping("/usuario/{docUsuario}")
    public ResponseEntity<List<ReservaDto>> obtenerPorUsuario(@PathVariable String docUsuario, Authentication authentication) {
        boolean esAdmin = AutenticacionUtils.esAdmin(authentication);
        String docUsuarioSolicitante = usuarioService.obtenerDocumentoPorUsername(authentication.getName());
        return ResponseEntity.ok(reservaService.obtenerPorUsuario(docUsuario, docUsuarioSolicitante, esAdmin));
    }

    // PUT /api/reservas/{id} — fix IDOR: solo el dueño de la reserva o un ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<ReservaDto> actualizar(@PathVariable String id, @RequestBody ReservaDto dto, Authentication authentication) {
        boolean esAdmin = AutenticacionUtils.esAdmin(authentication);
        String docUsuario = usuarioService.obtenerDocumentoPorUsername(authentication.getName());
        return ResponseEntity.ok(reservaService.actualizarReserva(id, dto, docUsuario, esAdmin));
    }
    
    // GET /api/reservas/estado/{estado} — SOLO ADMIN (lista reservas de todos por estado)
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ReservaDto>> obtenerPorEstado(@PathVariable EstadoReserva estado, Authentication authentication) {
        exigirAdmin(authentication);
        return ResponseEntity.ok(reservaService.obtenerPorEstado(estado));
    }

    // GET /api/reservas/usuario/{docUsuario}/tipo/{tipo} — fix IDOR: mismo criterio que obtenerPorUsuario
    @GetMapping("/usuario/{docUsuario}/tipo/{tipo}")
    public ResponseEntity<List<ReservaDto>> obtenerPorUsuarioYTipo(
            @PathVariable String docUsuario,
            @PathVariable TipoReserva tipo,
            Authentication authentication) {
        boolean esAdmin = AutenticacionUtils.esAdmin(authentication);
        String docUsuarioSolicitante = usuarioService.obtenerDocumentoPorUsername(authentication.getName());
        return ResponseEntity.ok(reservaService.obtenerPorUsuarioYTipo(docUsuario, tipo, docUsuarioSolicitante, esAdmin));
    }

    // PATCH /api/reservas/{id}/cancelar — fix IDOR: solo el dueño de la reserva o un ADMIN
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable String id, Authentication authentication) {
        boolean esAdmin = AutenticacionUtils.esAdmin(authentication);
        String docUsuario = usuarioService.obtenerDocumentoPorUsername(authentication.getName());
        reservaService.cancelarReserva(id, docUsuario, esAdmin);
        return ResponseEntity.noContent().build();
    }

    // Utilidad local: corta la petición si quien llama no es ADMIN.
    private void exigirAdmin(Authentication authentication) {
        if (!AutenticacionUtils.esAdmin(authentication)) {
            throw new AccesoDenegadoException("Solo un administrador puede realizar esta acción.");
        }
    }
}