package com.sena.goldenbooking.membresias.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sena.goldenbooking.membresias.dto.MiMembresiaDto;
import com.sena.goldenbooking.membresias.dto.SocioDto;
import com.sena.goldenbooking.membresias.model.ConfigMembresia;
import com.sena.goldenbooking.membresias.service.MembresiaService;
import com.sena.goldenbooking.usuarios.model.TipoMembresia;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Membresías", description = "Programa de socios: categorías Ocasional y Miembro con sus beneficios.")
@RestController
@RequestMapping("/api/membresias")
public class MembresiaController {

    private final MembresiaService service;
    private final UsuarioService usuarioService;

    public MembresiaController(MembresiaService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Mi membresía", description = "Categoría, beneficios y reservas realizadas del usuario autenticado.")
    @GetMapping("/mia")
    public ResponseEntity<MiMembresiaDto> mia(Authentication authentication) {
        return ResponseEntity.ok(service.miMembresia(usuarioService.obtenerDocumentoPorUsername(authentication.getName())));
    }

    @Operation(summary = "Configuración del programa de socios (ADMIN)")
    @GetMapping("/config")
    public ResponseEntity<ConfigMembresia> config() {
        return ResponseEntity.ok(service.obtenerConfig());
    }

    @Operation(summary = "Guardar la configuración (ADMIN)",
            description = "Reservas para sugerir la membresía, anticipación general y beneficios de cada categoría.")
    @PutMapping("/config")
    public ResponseEntity<ConfigMembresia> guardarConfig(@Valid @RequestBody ConfigMembresia config) {
        return ResponseEntity.ok(service.guardarConfig(config));
    }

    @Operation(summary = "Clientes con su categoría y reservas (ADMIN)", description = "Los sugeridos para hacerse socios van primero.")
    @GetMapping("/socios")
    public ResponseEntity<List<SocioDto>> socios() {
        return ResponseEntity.ok(service.listarClientes());
    }

    @Operation(summary = "Asignar o quitar la membresía de un cliente (ADMIN)")
    @PatchMapping("/socios/{idUsuario}")
    public ResponseEntity<SocioDto> asignar(@PathVariable String idUsuario, @RequestParam TipoMembresia tipo) {
        return ResponseEntity.ok(service.asignar(idUsuario, tipo));
    }
}
