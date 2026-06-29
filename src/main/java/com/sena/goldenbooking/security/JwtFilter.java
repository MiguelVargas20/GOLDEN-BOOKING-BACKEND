package com.sena.goldenbooking.security;

import java.io.IOException;
import java.util.List;

// Importamos el Sl4j para el manejo y auditoría de logs
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sena.goldenbooking.repositories.TokenInvalidadoRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenInvalidadoRepository tokenInvalidadoRepo;

    public JwtFilter(JwtService jwtService, TokenInvalidadoRepository tokenInvalidadoRepo) {
        this.jwtService = jwtService;
        this.tokenInvalidadoRepo = tokenInvalidadoRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();

        // Si no hay cabecera válida, se continúa el filtro sin autenticar
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            // 1. Validar firma y expiración mediante el servicio de JWT
            if (!jwtService.tokenValido(token)) {
                log.warn("Token JWT inválido o con firma alterada. IP: {} | Endpoint: {}", ip, uri);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 2. Verificar en BD que el token no haya sido invalidado por logout
            if (tokenInvalidadoRepo.existsById(token)) {
                log.warn("Intento de acceso con Token en lista negra (Usuario deslogueado). IP: {} | Endpoint: {}", ip, uri);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 3. Si pasó las validaciones, extraemos la info y autenticamos
            String username = jwtService.extraerEmail(token);
            List<String> roles = jwtService.extraerRoles(token);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Log de éxito en auditoría
            log.info("Usuario {} autenticado correctamente. IP: {} | Endpoint: {}", username, ip, uri);

        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("JWT expirado. IP: {} | Endpoint: {}", ip, uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception ex) {
            // Captura cualquier otro fallo imprevisto al procesar el token sin romper el hilo
            log.error("Error inesperado procesando el JWT. IP: {} | Mensaje: {}", ip, ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}