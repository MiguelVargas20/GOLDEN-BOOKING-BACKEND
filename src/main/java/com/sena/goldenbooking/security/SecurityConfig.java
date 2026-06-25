package com.sena.goldenbooking.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    // Inyectamos el filtro de JWT para que se pueda usar en la configuración de seguridad
    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    // Configura el AuthenticationManager para manejar la autenticación
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Configura la cadena de filtros de seguridad, definiendo las reglas de acceso a las rutas

    @Bean
    
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> {})
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            // ── Públicas ────────────────────────────────────────────
            .requestMatchers(
                "/auth/**",
                "/auth/recuperar-password",
                "/api/usuarios/registro",
                "/test/**"
            ).permitAll()

            // ── Solo ADMIN ──────────────────────────────────────────
            // Gestión de habitaciones y tipos
            .requestMatchers(HttpMethod.POST,   "/api/habitaciones/**").hasAuthority("ROL_ADMIN")
            .requestMatchers(HttpMethod.PUT,    "/api/habitaciones/**").hasAuthority("ROL_ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/habitaciones/**").hasAuthority("ROL_ADMIN")
            .requestMatchers(HttpMethod.POST,   "/api/tipohabitaciones/**").hasAuthority("ROL_ADMIN")
            .requestMatchers(HttpMethod.PUT,    "/api/tipohabitaciones/**").hasAuthority("ROL_ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/tipohabitaciones/**").hasAuthority("ROL_ADMIN")

            // Gestión de usuarios (ADMIN ve y edita todo)
            .requestMatchers(HttpMethod.GET,    "/api/usuarios/**").hasAuthority("ROL_ADMIN")
            .requestMatchers(HttpMethod.PUT,    "/api/usuarios/**").hasAuthority("ROL_ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasAuthority("ROL_ADMIN")

            // Cancelar y eliminar reservas (solo ADMIN)
            .requestMatchers(HttpMethod.DELETE, "/api/reservas/**").hasAuthority("ROL_ADMIN")

            // ── ADMIN o CLIENTE autenticado ─────────────────────────
            // Ver habitaciones disponibles
            .requestMatchers(HttpMethod.GET, "/api/habitaciones/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")
            .requestMatchers(HttpMethod.GET, "/api/tipohabitaciones/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")

            // Reservas — cualquier usuario logueado puede crear y ver las suyas
            .requestMatchers("/api/reservas/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")
            .requestMatchers("/api/reservas/hotel/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")
            .requestMatchers("/api/reservas/deporte/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")

            // ── Todo lo demás requiere JWT ──────────────────────────
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
    // Configura CORS para permitir solicitudes desde el frontend (ej. localhost:5173)
    @Bean
    
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:56083"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        // Aplica esta configuración a todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}