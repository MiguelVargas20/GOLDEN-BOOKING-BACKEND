package com.sena.goldenbooking.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sena.goldenbooking.auth.repository.TokenInvalidadoRepository;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import com.sena.goldenbooking.usuarios.model.Usuario;
import com.sena.goldenbooking.usuarios.model.UsuarioAuth;
import com.sena.goldenbooking.usuarios.repository.UsuarioAuthRepository;
import com.sena.goldenbooking.usuarios.repository.UsuarioRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Autentica cada petición a partir del JWT de la cabecera Authorization.
 *
 * Cambios importantes respecto a la versión anterior:
 *
 * 1. PÉRDIDA DE ACCESO INMEDIATA: antes los roles salían del propio token y
 *    nunca se consultaba la base de datos, así que un usuario desactivado o
 *    eliminado por el admin seguía entrando hasta que su token expirara
 *    (hasta 1 hora). Ahora en cada petición se verifica en Mongo que el
 *    usuario exista y esté ACTIVO, y los roles se leen de la base (si el
 *    admin le quita un rol, también aplica de inmediato).
 *
 * 2. Si el token no sirve (expirado, alterado, en lista negra, usuario
 *    inactivo) ya NO se corta la petición aquí con un 401 vacío: se continúa
 *    sin autenticar y se guarda el motivo en la petición. Así los endpoints
 *    públicos (login, registro...) funcionan aunque el navegador mande un
 *    token viejo, y los protegidos responden 401 con un JSON claro desde el
 *    AuthenticationEntryPoint de SecurityConfig.
 */
@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    /** Atributo de la petición donde se guarda por qué no se autenticó. */
    public static final String ATRIBUTO_MOTIVO = "motivoAutenticacion";

    public static final String MOTIVO_SESION_EXPIRADA = "SESION_EXPIRADA";
    public static final String MOTIVO_TOKEN_INVALIDO = "TOKEN_INVALIDO";
    public static final String MOTIVO_CUENTA_INACTIVA = "CUENTA_INACTIVA";

    private final JwtService jwtService;
    private final TokenInvalidadoRepository tokenInvalidadoRepo;
    private final UsuarioAuthRepository authRepo;
    private final UsuarioRepository usuarioRepo;

    public JwtFilter(JwtService jwtService, TokenInvalidadoRepository tokenInvalidadoRepo,
                     UsuarioAuthRepository authRepo, UsuarioRepository usuarioRepo) {
        this.jwtService = jwtService;
        this.tokenInvalidadoRepo = tokenInvalidadoRepo;
        this.authRepo = authRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Sin cabecera Bearer: se continúa sin autenticar
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String motivo = autenticar(header.substring(7), request);
        if (motivo != null) {
            SecurityContextHolder.clearContext();
            request.setAttribute(ATRIBUTO_MOTIVO, motivo);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Intenta autenticar la petición con el token.
     * @return null si se autenticó; si no, el motivo (ver constantes MOTIVO_*).
     */
    private String autenticar(String token, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        try {
            // 1. Firma y expiración: obtenerClaims lanza ExpiredJwtException si
            //    expiró o JwtException si fue alterado (se manejan abajo, así el
            //    frontend puede distinguir "tu sesión expiró" de "token inválido").
            jwtService.obtenerClaims(token);

            // 2. Lista negra (logout)
            if (tokenInvalidadoRepo.existsById(token)) {
                log.warn("Token en lista negra (sesión cerrada). IP: {} | Endpoint: {}", ip, uri);
                return MOTIVO_SESION_EXPIRADA;
            }

            // 3. El usuario debe seguir existiendo y estar ACTIVO (consulta a la BD)
            String username = jwtService.extraerEmail(token);
            Optional<UsuarioAuth> auth = authRepo.findByUser(username);
            Optional<Usuario> perfil = auth.flatMap(a -> usuarioRepo.findById(a.getId()));
            if (auth.isEmpty() || perfil.isEmpty() || perfil.get().getEstado() == EstadoUsuario.INACTIVO) {
                log.warn("Token de usuario eliminado o inactivo: {}. IP: {} | Endpoint: {}", username, ip, uri);
                return MOTIVO_CUENTA_INACTIVA;
            }

            // 4. Roles desde la BD, no desde el token
            List<SimpleGrantedAuthority> authorities = auth.get().getRls().stream()
                    .map(rol -> new SimpleGrantedAuthority(rol.name()))
                    .toList();

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(username, null, authorities));
            log.debug("Usuario {} autenticado. IP: {} | Endpoint: {}", username, ip, uri);
            return null;

        } catch (ExpiredJwtException ex) {
            log.warn("JWT expirado. IP: {} | Endpoint: {}", ip, uri);
            return MOTIVO_SESION_EXPIRADA;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token JWT inválido o alterado. IP: {} | Endpoint: {}", ip, uri);
            return MOTIVO_TOKEN_INVALIDO;
        } catch (Exception ex) {
            log.error("Error inesperado procesando el JWT. IP: {} | Mensaje: {}", ip, ex.getMessage());
            return MOTIVO_TOKEN_INVALIDO;
        }
    }
}
