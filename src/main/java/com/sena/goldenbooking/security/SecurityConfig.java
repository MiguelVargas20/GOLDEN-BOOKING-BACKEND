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

    // Exponer AuthenticationManager para que pueda ser inyectado en AuthService
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Configuración de seguridad HTTP
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // Configuración CORS y CSRF
        http
            .cors(cors -> {}) // usa el bean corsConfigurationSource automáticamente
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Preflight del navegador — siempre debe pasar libre
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── Rutas públicas ──────────────────────────────────────
                .requestMatchers(
                    "/auth/**",                      // login / registro de tokens
                    "/api/usuarios/registrar",       // registro nuevo usuario
                    "/api/usuarios",                 // listar (si quieres que sea público)
                    "/api/usuarios/**",              // detalle usuario
                    "/api/servicios/crear",          // crear servicio (ajusta si debe ser privado)
                    "/api/servicios/categorias"      // catálogo público
                ).permitAll()

                // ── Todo lo demás requiere JWT ──────────────────────────
                .anyRequest().authenticated()
            );

        return http.build();
    }

    // Configuración CORS para permitir solicitudes desde el frontend React
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Cambia el puerto si tu React corre en otro
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