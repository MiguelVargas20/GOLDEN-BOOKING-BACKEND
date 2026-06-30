package com.sena.goldenbooking.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.sena.goldenbooking.dtos.LoginDto;
import com.sena.goldenbooking.models.Usuario;
import com.sena.goldenbooking.models.UsuarioAuth;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;
import com.sena.goldenbooking.repositories.UsuarioRepository;
import com.sena.goldenbooking.security.JwtService;
import com.sena.goldenbooking.services.AuthService;  // ← NUEVO import

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Autenticación", description = "Login, recuperación de contraseña y generación de tokens JWT.")
@RestController
@RequestMapping("/auth")

public class AuthController {

    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final UsuarioAuthRepository authRepo;
    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;             // ← NUEVO campo

    public AuthController(
            JwtService jwtService,
            AuthenticationManager authManager,
            UsuarioAuthRepository authRepo,
            UsuarioRepository usuarioRepo,
            PasswordEncoder passwordEncoder,
            AuthService authService) {                 // ← NUEVO parámetro
        this.jwtService = jwtService;
        this.authManager = authManager;
        this.authRepo = authRepo;
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;                // ← NUEVO
    }

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

    @PostMapping("/recuperar-password")
    public ResponseEntity<Map<String, Object>> recuperarPassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String passwordAntigua = body.get("passwordAntigua");
        String nuevaPassword = body.get("nuevaPassword");

        if (username == null || passwordAntigua == null || nuevaPassword == null || nuevaPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Todos los campos son obligatorios"
            ));
        }

        UsuarioAuth auth = authRepo.findByUser(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(passwordAntigua, auth.getPwd())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "La contraseña actual es incorrecta"
            ));
        }

        if (nuevaPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "La nueva contraseña debe tener mínimo 6 caracteres"
            ));
        }

        auth.setPwd(passwordEncoder.encode(nuevaPassword));
        authRepo.save(auth);

        return ResponseEntity.ok(Map.of(
            "mensaje", "Contraseña actualizada correctamente"
        ));
    }

    // ── LOGOUT ───────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // quita el "Bearer "
        authService.logout(token);
        return ResponseEntity.noContent().build(); // 204
    }
}