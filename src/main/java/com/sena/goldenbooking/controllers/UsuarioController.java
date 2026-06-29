package com.sena.goldenbooking.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.dtos.UsuarioRegistroDto;
import com.sena.goldenbooking.services.UsuarioService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // POST /api/usuarios/registro
    @PostMapping("/registro")
    public ResponseEntity<UsuarioRegistroDto> registrar(@Valid @RequestBody UsuarioRegistroDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrarUsuario(dto));
    }

    // GET /api/usuarios
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        var paginaUsuarios = usuarioService.listarUsuariosPaginados(pageable);

        Map<String, Object> respuesta = Map.of(
            "contenido",       paginaUsuarios.getContent(),
            "paginaActual",    paginaUsuarios.getNumber(),
            "totalPaginas",    paginaUsuarios.getTotalPages(),
            "totalElementos",  paginaUsuarios.getTotalElements()
        );
        return ResponseEntity.ok(respuesta);
    }

    // GET /api/usuarios/{id}
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDto> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    // GET /api/usuarios/doc/{docNum}
    @GetMapping("/doc/{docNum}")
    public ResponseEntity<UsuarioDto> obtenerPorDoc(@PathVariable String docNum) {
        return ResponseEntity.ok(usuarioService.obtenerPorDocNum(docNum));
    }

    // PUT /api/usuarios/{id}
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDto> actualizar(@PathVariable String id,@RequestBody UsuarioDto dto) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto));
    }

    // DELETE /api/usuarios/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}