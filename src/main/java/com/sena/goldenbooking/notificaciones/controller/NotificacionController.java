package com.sena.goldenbooking.notificaciones.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sena.goldenbooking.notificaciones.model.Notificacion;
import com.sena.goldenbooking.notificaciones.service.NotificacionService;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Campana del cliente: sus notificaciones (siempre las del usuario autenticado). */
@Tag(name = "Notificaciones", description = "Avisos de la campana del cliente sobre sus reservas.")
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService service;
    private final UsuarioService usuarioService;

    public NotificacionController(NotificacionService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Mis notificaciones", description = "Las 30 más recientes, la más nueva primero.")
    @GetMapping
    public ResponseEntity<List<Notificacion>> listar(Authentication authentication) {
        return ResponseEntity.ok(service.listarMias(documentoDe(authentication)));
    }

    @Operation(summary = "Cuántas notificaciones sin leer tengo")
    @GetMapping("/no-leidas/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(Authentication authentication) {
        return ResponseEntity.ok(Map.of("noLeidas", service.contarNoLeidas(documentoDe(authentication))));
    }

    @Operation(summary = "Marcar una notificación como leída")
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Notificacion> marcarLeida(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(service.marcarLeida(id, documentoDe(authentication)));
    }

    @Operation(summary = "Marcar todas mis notificaciones como leídas")
    @PatchMapping("/leidas")
    public ResponseEntity<Map<String, Integer>> marcarTodas(Authentication authentication) {
        return ResponseEntity.ok(Map.of("marcadas", service.marcarTodasLeidas(documentoDe(authentication))));
    }

    private String documentoDe(Authentication authentication) {
        return usuarioService.obtenerDocumentoPorUsername(authentication.getName());
    }
}
