package com.sena.goldenbooking.calificaciones.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sena.goldenbooking.calificaciones.dto.CalificacionDto;
import com.sena.goldenbooking.calificaciones.dto.ResumenCalificacionDto;
import com.sena.goldenbooking.calificaciones.service.CalificacionService;
import com.sena.goldenbooking.reservas.model.TipoReserva;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Calificaciones", description = "Estrellas y opiniones de espacios y habitaciones.")
@RestController
@RequestMapping("/api/calificaciones")
public class CalificacionController {

    private final CalificacionService service;
    private final UsuarioService usuarioService;

    public CalificacionController(CalificacionService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Calificar una reserva finalizada",
            description = "Solo el dueño de la reserva, cuando ya está FINALIZADA, y una sola vez.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Calificación guardada"),
        @ApiResponse(responseCode = "400", description = "Puntuación fuera de 1 a 5 o comentario muy largo"),
        @ApiResponse(responseCode = "403", description = "La reserva es de otro usuario"),
        @ApiResponse(responseCode = "409", description = "La reserva no ha finalizado o ya fue calificada")
    })
    @PostMapping
    public ResponseEntity<CalificacionDto> calificar(@Valid @RequestBody CalificacionDto dto, Authentication authentication) {
        String documento = usuarioService.obtenerDocumentoPorUsername(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.calificar(dto, documento));
    }

    @Operation(summary = "Promedio de estrellas por espacio o habitación")
    @GetMapping("/resumen")
    public ResponseEntity<List<ResumenCalificacionDto>> resumen(@RequestParam TipoReserva categoria) {
        return ResponseEntity.ok(service.resumen(categoria));
    }

    @Operation(summary = "Opiniones de un espacio o habitación", description = "Las 20 más recientes.")
    @GetMapping
    public ResponseEntity<List<CalificacionDto>> listar(@RequestParam TipoReserva categoria, @RequestParam String idRecurso) {
        return ResponseEntity.ok(service.listarPorRecurso(categoria, idRecurso));
    }

    @Operation(summary = "Mis calificaciones")
    @GetMapping("/mias")
    public ResponseEntity<List<CalificacionDto>> mias(Authentication authentication) {
        return ResponseEntity.ok(service.listarMias(usuarioService.obtenerDocumentoPorUsername(authentication.getName())));
    }
}
