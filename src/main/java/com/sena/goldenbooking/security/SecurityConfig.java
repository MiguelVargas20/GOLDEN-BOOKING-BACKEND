package com.sena.goldenbooking.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    // Configura el AuthenticationManager para manejar la autenticación
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Configura la cadena de filtros de seguridad, definiendo las reglas de acceso a las rutas
    @Bean

    // Configura la seguridad HTTP, incluyendo CORS, CSRF y las reglas de autorización
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                // Preflight siempre libre
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── Rutas públicas ──────────────────────────────────────
                .requestMatchers(
                    // Auth
                    "/auth/**",

                    // Usuarios
                    "/api/usuarios/registrar",
                    "/api/usuarios",
                    "/api/usuarios/**",

                    // Habitaciones — público para testear
                    "/api/habitaciones/**",

                    // Reservas general — público para testear
                    "/api/reservas",
                    "/api/reservas/**",

                    // Reservas hotel — público para testear
                    "/api/reservas/hotel",
                    "/api/reservas/hotel/",
                    "/api/reservas/hotel/**",

                    // Reservas deporte — público para testear
                    "/api/reservas/deporte",
                    "/api/reservas/deporte/",
                    "/api/reservas/deporte/**",

                    // Test JWT
                    "/test/**"
                ).permitAll()

                // ── Todo lo demás requiere JWT ──────────────────────────
                .anyRequest().authenticated()
            );

        return http.build();
    }


    // Configura CORS para permitir solicitudes desde el frontend (ej. localhost:5173)
    @Bean
    
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        // Aplica esta configuración a todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}