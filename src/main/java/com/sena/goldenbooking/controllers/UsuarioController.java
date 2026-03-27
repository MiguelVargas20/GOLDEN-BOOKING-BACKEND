package com.sena.goldenbooking.controllers;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.dtos.UsuarioRegistroDto;
import com.sena.goldenbooking.services.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*") // Ajusta según tus necesidades de CORS
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // --- ENDPOINTS DE CREACIÓN ---

    @PostMapping
    public ResponseEntity<UsuarioDto> crear(@RequestBody UsuarioDto usuarioDto) {
        return new ResponseEntity<>(usuarioService.crearUsuario(usuarioDto), HttpStatus.CREATED);
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioRegistroDto> registrar(@RequestBody UsuarioRegistroDto registroDto) {
        return new ResponseEntity<>(usuarioService.registrarUsuario(registroDto), HttpStatus.CREATED);
    }

    // --- ENDPOINTS DE LECTURA ---

    @GetMapping
    public ResponseEntity<List<UsuarioDto>> listarTodos() {
        return ResponseEntity.ok(usuarioService.ListUsuarios());
    }

    @GetMapping("/documento/{docnum}")
    public ResponseEntity<UsuarioDto> buscarPorDocumento(@PathVariable String docnum) {
        return ResponseEntity.ok(usuarioService.UsuarioByDocNum(docnum));
    }

    // --- ENDPOINTS DE ACTUALIZACIÓN ---

@PutMapping("/documento/{docnum}")
public ResponseEntity<UsuarioDto> actualizarPorDocumento(
        @PathVariable String docnum, 
        @RequestBody UsuarioDto usuarioDto) {
    return ResponseEntity.ok(usuarioService.actualizarUsuarios(docnum, usuarioDto));
}
    // --- ENDPOINTS DE ELIMINACIÓN ---

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        usuarioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}