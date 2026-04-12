package com.sena.goldenbooking.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    // Inyectamos el servicio de JWT para validar y extraer información del token
    private final JwtService jwtService;


    // Constructor para inyectar el JwtService
    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }


    // Método principal del filtro que se ejecuta para cada solicitud
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Leer el header Authorization
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 2. Si no hay token o no empieza con Bearer, dejar pasar (rutas públicas)
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraer el token sin el prefijo "Bearer "
        String token = header.substring(7);

        // 4. Validar token
        if (!jwtService.tokenValido(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 5. Extraer usuario y roles del token
        String email = jwtService.extraerEmail(token);
        List<String> roles = jwtService.extraerRoles(token);

        // 6. Construir authorities para Spring Security
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        // 7. Crear autenticación y meterla en el contexto de Spring
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 8. Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}