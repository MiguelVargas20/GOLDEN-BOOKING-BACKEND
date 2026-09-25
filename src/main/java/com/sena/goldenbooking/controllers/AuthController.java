package com.sena.goldenbooking.controllers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sena.goldenbooking.dtos.LoginDto;
import com.sena.goldenbooking.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.exception.RefreshTokenInvalidoException;
import com.sena.goldenbooking.exception.RespuestaError;
import com.sena.goldenbooking.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.models.EstadoUsuario;
import com.sena.goldenbooking.models.TipoToken;
import com.sena.goldenbooking.models.Usuario;
import com.sena.goldenbooking.models.UsuarioAuth;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;  // ← NUEVO import
import com.sena.goldenbooking.repositories.UsuarioRepository;
import com.sena.goldenbooking.security.JwtService;
import com.sena.goldenbooking.services.AuthService;
import com.sena.goldenbooking.services.EmailService;
import com.sena.goldenbooking.services.RateLimitService;
import com.sena.goldenbooking.services.RefreshTokenService;
import com.sena.goldenbooking.services.RefreshTokenService.RefreshTokenPair;
import com.sena.goldenbooking.services.TokenService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Tag(name = "Autenticación", description = "Login, recuperación de contraseña y generación de tokens JWT.")
@RestController
@RequestMapping("/auth")

public class AuthController {

    private final EmailService emailService;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final UsuarioAuthRepository authRepo;
    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;             // ← NUEVO campo
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;
    private final RateLimitService rateLimitService;    // ← NUEVO campo (Hallazgo 6)

    private static final String COOKIE_REFRESH = "refreshToken";

    // SameSite de la cookie de refresh (app.cookie.same-site / COOKIE_SAME_SITE).
    // Con "Lax" el navegador NO envía la cookie en peticiones fetch entre
    // sitios distintos (vercel.app -> backend en AWS), así que /auth/refresh
    // nunca recibía el refresh token. "None" exige Secure, es decir, que el
    // backend se sirva por HTTPS (localhost se considera seguro en desarrollo).
    @Value("${app.cookie.same-site:None}")
    private String cookieSameSite;

    // Rate limiting: máximo de intentos antes de bloquear, y minutos que dura el bloqueo.
    private static final int MAX_INTENTOS_LOGIN = 5;
    private static final int VENTANA_LOGIN_MINUTOS = 15;
    private static final int MAX_INTENTOS_RECUPERACION = 3;
    private static final int VENTANA_RECUPERACION_MINUTOS = 15;

    // Misma longitud mínima que exige el registro (UsuarioRegistroDto). Antes
    // cambiar/restablecer aceptaba 6, así que se podía bajar a una más débil.
    private static final int LONGITUD_MINIMA_PASSWORD = 8;

    public AuthController(
            JwtService jwtService,
            AuthenticationManager authManager,
            UsuarioAuthRepository authRepo,
            UsuarioRepository usuarioRepo,
            PasswordEncoder passwordEncoder,
            AuthService authService,                   // ← NUEVO parámetro
            TokenService tokenService,   
            RefreshTokenService refreshTokenService, EmailService emailService,
            RateLimitService rateLimitService) {
        this.jwtService = jwtService;
        this.authManager = authManager;
        this.authRepo = authRepo;
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;            
        this.tokenService = tokenService;        // ← NUEVO
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginDto dto,
                                                       HttpServletResponse response) {

        // Rate limiting (Hallazgo 6): antes de gastar un intento contra el
        // AuthenticationManager, revisamos si esta cuenta ya se pasó del
        // límite de intentos fallidos en la ventana vigente.
        String claveLimite = "login:" + dto.getUsername().toLowerCase();
        rateLimitService.verificarNoBloqueado(claveLimite, MAX_INTENTOS_LOGIN);

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            // Usuario/contraseña incorrectos: contamos el intento fallido y
            // dejamos que la excepción original siga su curso normal
            // (el GlobalExceptionHandler ya sabe manejarla).
            rateLimitService.registrarIntento(claveLimite, VENTANA_LOGIN_MINUTOS);
            throw ex;
        }

        UsuarioAuth auth = authRepo.findByUser(dto.getUsername())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Usuario perfil = usuarioRepo.findById(auth.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Perfil no encontrado"));

        // ── NUEVO: bloquear login si la cuenta no está verificada ──
        if (!perfil.isVerificado()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(RespuestaError.cuerpo(
                HttpStatus.FORBIDDEN, "CUENTA_NO_VERIFICADA",
                "Debes verificar tu cuenta antes de iniciar sesión. Revisa tu correo.", "/auth/login"));
        }

        // Login exitoso: reseteamos el contador de intentos fallidos.
        rateLimitService.limpiar(claveLimite);

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
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Usuario perfil = usuarioRepo.findById(nuevoRefresh.userId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Perfil no encontrado"));

        // Cuenta desactivada por el admin: no se emite un access token nuevo
        // (antes el refresh seguía renovando la sesión indefinidamente).
        if (perfil.getEstado() == EstadoUsuario.INACTIVO) {
            refreshTokenService.revocarTodosDelUsuario(perfil.getId());
            clearRefreshCookie(response);
            throw new RefreshTokenInvalidoException("Tu cuenta está inactiva. Contacta al administrador.");
        }

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
                .sameSite(cookieSameSite)  // "None" en prod: el front (Vercel) y el back (AWS) son sitios distintos y con "Lax" la cookie no viaja
                .path("/auth")             // solo se envía a endpoints de auth, reduce superficie de exposición
                .maxAge(maxAgeSegundos)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_REFRESH, "")
                .httpOnly(true)
                .secure(true)
                .sameSite(cookieSameSite)
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

        if (username == null || username.isBlank() || passwordAntigua == null
                || nuevaPassword == null || nuevaPassword.isBlank()) {
            throw new SolicitudInvalidaException("Todos los campos son obligatorios.");
        }

        // Rate limiting: este endpoint es público y valida la contraseña
        // actual, así que sin límite servía para adivinar contraseñas
        // saltándose el bloqueo del login. Usa el MISMO contador que el login
        // ("login:<usuario>"): los intentos fallidos en cualquiera de los dos
        // suman contra el mismo límite de 5 cada 15 minutos.
        String claveLimite = "login:" + username.toLowerCase();
        rateLimitService.verificarNoBloqueado(claveLimite, MAX_INTENTOS_LOGIN);

        // Mismo mensaje exista o no el usuario: antes respondía 404 "Usuario
        // no encontrado" vs 400 "contraseña incorrecta", lo que permitía
        // averiguar qué nombres de usuario están registrados.
        UsuarioAuth auth = authRepo.findByUser(username).orElse(null);
        if (auth == null || !passwordEncoder.matches(passwordAntigua, auth.getPwd())) {
            rateLimitService.registrarIntento(claveLimite, VENTANA_LOGIN_MINUTOS);
            throw new SolicitudInvalidaException("Usuario o contraseña actual incorrectos.");
        }

        if (nuevaPassword.length() < LONGITUD_MINIMA_PASSWORD) {
            throw new SolicitudInvalidaException(
                "La nueva contraseña debe tener mínimo " + LONGITUD_MINIMA_PASSWORD + " caracteres.");
        }

        rateLimitService.limpiar(claveLimite);
        auth.setPwd(passwordEncoder.encode(nuevaPassword));
        authRepo.save(auth);

        // Cambió la contraseña: se cierran las sesiones abiertas en otros
        // dispositivos (si alguien tenía una sesión robada, deja de servirle).
        refreshTokenService.revocarTodosDelUsuario(auth.getId());

        return ResponseEntity.ok(Map.of(
            "mensaje", "Contraseña actualizada correctamente"
        ));
    }

    // ── LOGOUT ───────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshCookie,
            HttpServletResponse response) {

        // La cabecera es opcional y el endpoint es público: antes, sin cabecera
        // respondía 500, y con el access token ya expirado respondía 401, así que
        // el refresh token (cookie) nunca se revocaba. Ahora el access token se
        // manda a la lista negra solo si sigue vigente (si ya expiró no sirve
        // de nada), y la cookie de refresh se revoca y se borra SIEMPRE.
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.tokenValido(token)) {
                authService.logout(token);
            }
        }

        if (refreshCookie != null && !refreshCookie.isBlank()) {
            refreshTokenService.revocarPorToken(refreshCookie);
        }
        clearRefreshCookie(response);

        return ResponseEntity.noContent().build(); // 204
    }


    @GetMapping("/verificar-cuenta")
    public ResponseEntity<Map<String, Object>> verificarCuenta(@RequestParam String token) {
        String correo = tokenService.validarYObtenerCorreo(token, TipoToken.VERIFICACION_CUENTA);

        Usuario usuario = usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        usuario.setVerificado(true);
        usuarioRepo.save(usuario);
        tokenService.invalidarToken(token);

        return ResponseEntity.ok(Map.of(
            "mensaje", "Cuenta verificada correctamente. Ya puedes iniciar sesión."
        ));
    }

    @PostMapping("/solicitar-recuperacion")
    public ResponseEntity<Map<String, Object>> solicitarRecuperacion(@RequestBody Map<String, String> body) {
        String correo = body.get("correo");

        // Rate limiting (Hallazgo 6): sin esto, cualquiera podía golpear este
        // endpoint sin límite y bombardear de correos a una víctima. Contamos
        // POR CORREO y SIEMPRE (exista o no la cuenta) — si solo contáramos
        // cuando el correo existe, alguien podría usar el tiempo de respuesta
        // o la ausencia de bloqueo para detectar qué correos SÍ están
        // registrados, rompiendo la protección anti-enumeración que ya
        // tenías (la respuesta es igual exista o no el correo).
        String claveLimite = "recuperacion:" + (correo != null ? correo.toLowerCase() : "desconocido");
        rateLimitService.verificarNoBloqueado(claveLimite, MAX_INTENTOS_RECUPERACION);
        rateLimitService.registrarIntento(claveLimite, VENTANA_RECUPERACION_MINUTOS);

        Usuario usuario = usuarioRepo.findByCorreo(correo).orElse(null);

        // Por seguridad, respondemos igual exista o no el correo —
        // así nadie puede usar este endpoint para "adivinar" qué correos están registrados.
        if (usuario != null) {
            String token = tokenService.generarToken(correo, TipoToken.RECUPERACION_PASSWORD);
            emailService.enviarCorreoRecuperacion(correo, token);
        }

        return ResponseEntity.ok(Map.of(
            "mensaje", "Si el correo está registrado, te enviamos un enlace de recuperación."
        ));
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<Map<String, Object>> restablecerPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String nuevaPassword = body.get("nuevaPassword");

        if (nuevaPassword == null || nuevaPassword.length() < LONGITUD_MINIMA_PASSWORD) {
            throw new SolicitudInvalidaException(
                "La contraseña debe tener mínimo " + LONGITUD_MINIMA_PASSWORD + " caracteres.");
        }

        String correo = tokenService.validarYObtenerCorreo(token, TipoToken.RECUPERACION_PASSWORD);

        Usuario usuario = usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        UsuarioAuth auth = authRepo.findById(usuario.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Credenciales no encontradas"));

        auth.setPwd(passwordEncoder.encode(nuevaPassword));
        authRepo.save(auth);
        tokenService.invalidarToken(token);

        // Contraseña restablecida: se cierran todas las sesiones abiertas.
        refreshTokenService.revocarTodosDelUsuario(auth.getId());

        return ResponseEntity.ok(Map.of(
            "mensaje", "Contraseña restablecida correctamente. Ya puedes iniciar sesión."
        ));
    }
}