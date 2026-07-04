package com.sena.goldenbooking.controllers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.sena.goldenbooking.dtos.LoginDto;
import com.sena.goldenbooking.exception.RefreshTokenInvalidoException;
import com.sena.goldenbooking.models.Usuario;
import com.sena.goldenbooking.models.UsuarioAuth;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;
import com.sena.goldenbooking.repositories.UsuarioRepository;
import com.sena.goldenbooking.security.JwtService;
import com.sena.goldenbooking.services.AuthService;  // ← NUEVO import
import com.sena.goldenbooking.services.RefreshTokenService;
import com.sena.goldenbooking.services.RefreshTokenService.RefreshTokenPair;

import jakarta.servlet.http.HttpServletResponse;
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
    private final RefreshTokenService refreshTokenService;

    private static final String COOKIE_REFRESH = "refreshToken";

    public AuthController(
            JwtService jwtService,
            AuthenticationManager authManager,
            UsuarioAuthRepository authRepo,
            UsuarioRepository usuarioRepo,
            PasswordEncoder passwordEncoder,
            AuthService authService,                   // ← NUEVO parámetro
            RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.authManager = authManager;
        this.authRepo = authRepo;
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;                // ← NUEVO
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginDto dto,
                                                       HttpServletResponse response) {

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

        // Nueva sesión = nueva familia de refresh tokens (un dispositivo/login distinto)
        RefreshTokenPair refresh = refreshTokenService.crearNuevoRefreshToken(perfil.getId());
        setRefreshCookie(response, refresh.rawToken(), refresh.expiracion());

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

    // ── REFRESH ──────────────────────────────────────────────────
    // Emite un access token nuevo sin pedir contraseña, usando el refresh
    // token (cookie httpOnly). Rota el refresh token en cada uso.
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshCookie,
            HttpServletResponse response) {

        if (refreshCookie == null || refreshCookie.isBlank()) {
            throw new RefreshTokenInvalidoException("No se encontró refresh token, inicia sesión de nuevo");
        }

        RefreshTokenPair nuevoRefresh = refreshTokenService.rotar(refreshCookie);

        UsuarioAuth auth = authRepo.findById(nuevoRefresh.userId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Usuario perfil = usuarioRepo.findById(nuevoRefresh.userId())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        List<String> roles = auth.getRls().stream()
                .map(Enum::name)
                .toList();

        String nuevoAccessToken = jwtService.generarToken(
                auth.getUser(),
                roles,
                perfil.getNomUsr(),
                perfil.getApellUsr(),
                perfil.getId()
        );

        setRefreshCookie(response, nuevoRefresh.rawToken(), nuevoRefresh.expiracion());

        Map<String, Object> respuesta = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 200,
                "mensaje", "Token renovado",
                "token", nuevoAccessToken
        );

        return ResponseEntity.ok(respuesta);
    }

    private void setRefreshCookie(HttpServletResponse response, String rawToken, Date expiracion) {
        long maxAgeSegundos = Math.max(0, Duration.between(java.time.Instant.now(), expiracion.toInstant()).getSeconds());

        ResponseCookie cookie = ResponseCookie.from(COOKIE_REFRESH, rawToken)
                .httpOnly(true)
                .secure(true)              // requiere HTTPS en producción (en dev con localhost los navegadores lo permiten igual)
                .sameSite("Lax")           // mismo "site" (localhost:5173 -> localhost:8080) viaja igual; en prod cross-domain usar "None"+secure
                .path("/auth")             // solo se envía a endpoints de auth, reduce superficie de exposición
                .maxAge(maxAgeSegundos)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_REFRESH, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/auth")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshCookie,
            HttpServletResponse response) {

        String token = authHeader.substring(7); // quita el "Bearer "
        authService.logout(token);

        if (refreshCookie != null && !refreshCookie.isBlank()) {
            refreshTokenService.revocarPorToken(refreshCookie);
        }
        clearRefreshCookie(response);

        return ResponseEntity.noContent().build(); // 204
    }
}