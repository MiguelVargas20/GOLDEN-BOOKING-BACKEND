package com.sena.goldenbooking.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.dtos.UsuarioRegistroDto;
import com.sena.goldenbooking.dtos.LoginDto;
import com.sena.goldenbooking.dtos.LoginResponsiveDto;
import com.sena.goldenbooking.services.UsuarioService;
import com.sena.goldenbooking.services.AuthService;


/* Controlador REST para gestionar usuarios (registro, login, listado) */
@RestController

// Base URL para todas las operaciones relacionadas con usuarios
@RequestMapping("/api/usuarios")

// Permitir peticiones desde cualquier origen para pruebas
@CrossOrigin(origins = "*") // Permitir peticiones desde cualquier origen para pruebas
public class UsuarioController {

    // Inyectamos el servicio de usuario y el servicio de autenticación a través del constructor
    private final UsuarioService usuarioService;
    private final AuthService authService;

    
    // Constructor para inyección de dependencias
    public UsuarioController(UsuarioService usuarioService, AuthService authService) {
        this.usuarioService = usuarioService;
        this.authService = authService;
    }

    // 1. REGISTRO (Crea Perfil + Auth)
    @PostMapping("/registro")
    public ResponseEntity<UsuarioRegistroDto> registrar(@RequestBody UsuarioRegistroDto registroDto) {
        return new ResponseEntity<>(usuarioService.registrarUsuario(registroDto), HttpStatus.CREATED);
    }

    
    // 2. LOGIN
    @PostMapping("/login")
    public ResponseEntity<LoginResponsiveDto> login(@RequestBody LoginDto loginDto) {
        return ResponseEntity.ok(authService.login(loginDto));
    }

    // 3. LISTAR TODOS
    @GetMapping
    public ResponseEntity<List<UsuarioDto>> listar() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    // 4. BUSCAR POR DOCUMENTO
    @GetMapping("/documento/{doc}")
    public ResponseEntity<UsuarioDto> obtenerPorDocumento(@PathVariable String doc) {
        return ResponseEntity.ok(usuarioService.obtenerPorDocNum(doc));
    }

    // 5. ELIMINAR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}