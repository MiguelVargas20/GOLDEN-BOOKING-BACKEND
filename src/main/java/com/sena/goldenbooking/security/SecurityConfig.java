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

                        // ── Públicas ─────────────────────────────────────────
                    .requestMatchers(
                        "/auth/login",
                        "/auth/refresh",
                        "/auth/recuperar-password",
                        "/api/usuarios/registro",
                        "/ws/**"
                    ).permitAll()

                    // ── Documentación Swagger / OpenAPI ───────────────────
                    .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml"
                    ).permitAll()

                    // ── Perfil propio — VA PRIMERO antes que restricciones de ADMIN ──
                    .requestMatchers(HttpMethod.GET,   "/api/usuarios/perfil/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")
                    .requestMatchers(HttpMethod.PATCH, "/api/usuarios/perfil/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")

                    // ── Solo ADMIN — Usuarios ─────────────────────────────
                    .requestMatchers(HttpMethod.GET,    "/api/usuarios/**").hasAuthority("ROL_ADMIN")
                    .requestMatchers(HttpMethod.PUT,    "/api/usuarios/**").hasAuthority("ROL_ADMIN")
                    .requestMatchers(HttpMethod.PATCH,  "/api/usuarios/**").hasAuthority("ROL_ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasAuthority("ROL_ADMIN")

                    // ── Solo ADMIN — Habitaciones ─────────────────────────
                    .requestMatchers(HttpMethod.POST,   "/api/habitaciones/**").hasAuthority("ROL_ADMIN")
                    .requestMatchers(HttpMethod.PUT,    "/api/habitaciones/**").hasAuthority("ROL_ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/habitaciones/**").hasAuthority("ROL_ADMIN")

                    // ── Solo ADMIN — Tipos de Habitación ─────────────────
                    .requestMatchers(HttpMethod.POST,   "/api/tipohabitaciones/**").hasAuthority("ROL_ADMIN")
                    .requestMatchers(HttpMethod.PUT,    "/api/tipohabitaciones/**").hasAuthority("ROL_ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/tipohabitaciones/**").hasAuthority("ROL_ADMIN")

                    // ── Solo ADMIN — Eliminar reservas ────────────────────
                    .requestMatchers(HttpMethod.DELETE, "/api/reservas/**").hasAuthority("ROL_ADMIN")

                    // ── ADMIN o CLIENTE — Reservas ────────────────────────
                    .requestMatchers(HttpMethod.GET,   "/api/reservas/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")
                    .requestMatchers(HttpMethod.POST,  "/api/reservas/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")
                    .requestMatchers(HttpMethod.PATCH, "/api/reservas/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")

                    // ── ADMIN o CLIENTE — Habitaciones lectura ────────────
                    .requestMatchers(HttpMethod.GET, "/api/habitaciones/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")
                    .requestMatchers(HttpMethod.GET, "/api/tipohabitaciones/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")

                    // ── ADMIN o CLIENTE — Mensajes de contacto ─────────────
                    .requestMatchers(HttpMethod.POST, "/api/contacto").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")

                    // ── Solo ADMIN — Mensajes de contacto ───────────────────
                    .requestMatchers(HttpMethod.GET,   "/api/contacto/**").hasAuthority("ROL_ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/contacto/**").hasAuthority("ROL_ADMIN")


                    // ── Todo lo demás requiere JWT ────────────────────────
                    .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }
    // Configura CORS para permitir solicitudes desde el frontend (ej. localhost:5173)
    @Bean
    
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:56155"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        // Aplica esta configuración a todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}