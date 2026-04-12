package com.sena.goldenbooking.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import com.sena.goldenbooking.dtos.LoginDto;
import com.sena.goldenbooking.models.Usuario;
import com.sena.goldenbooking.models.UsuarioAuth;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;
import com.sena.goldenbooking.repositories.UsuarioRepository;
import com.sena.goldenbooking.security.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final UsuarioAuthRepository authRepo;
    private final UsuarioRepository usuarioRepo;

    public AuthController(
            JwtService jwtService,
            AuthenticationManager authManager,
            UsuarioAuthRepository authRepo,
            UsuarioRepository usuarioRepo) {
        this.jwtService = jwtService;
        this.authManager = authManager;
        this.authRepo = authRepo;
        this.usuarioRepo = usuarioRepo;
    }

    // POST /auth/login
    @PostMapping("/login")
        public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginDto dto) {

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));

        UsuarioAuth auth = authRepo.findByUser(dto.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Usuario perfil = usuarioRepo.findById(auth.getId())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        List<String> roles = auth.getRls().stream()
                .map(Enum::name)
                .toList();

        String token = jwtService.generarToken(
                auth.getUser(),
                roles,
                perfil.getNomUsr(),
                perfil.getApellUsr(),
                perfil.getId()
        );

        Map<String, Object> respuesta = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 200,
                "id", perfil.getId(),
                "mensaje", "Login exitoso",
                "usuario", auth.getUser(),
                "nombreCompleto", perfil.getNomUsr() + " " + perfil.getApellUsr(),
                "roles", roles,
                "token", token
        );

        return ResponseEntity.ok(respuesta);
     }
}