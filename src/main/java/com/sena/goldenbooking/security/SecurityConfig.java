package com.sena.goldenbooking.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

    // Orígenes permitidos por CORS — configurable vía app.cors.allowed-origins
    // (application.properties / variable de entorno CORS_ALLOWED_ORIGINS),
    // así en producción no hay que tocar código para apuntar al dominio real.
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

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
                        "/auth/verificar-cuenta",        // ← nuevo
                        "/auth/solicitar-recuperacion",  // ← nuevo
                        "/auth/restablecer-password",    // ← nuevo
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
                    // Igual que en reservas: esta regla solo exige ADMIN o CLIENTE.
                    // El chequeo de "es TU perfil" (comparando el id del JWT contra
                    // el {id} de la URL) vive en UsuarioController/validarPropioPerfil,
                    // no aquí.
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
                    // OJO al agregar una ruta nueva aquí: esta regla solo exige
                    // estar autenticado como ADMIN o CLIENTE — NO valida dueño.
                    // La validación de "solo el dueño o un admin" vive en el
                    // CONTROLLER de cada recurso (ReservaController,
                    // ReservaHotelController, ReservaDeporteController), comparando
                    // el docUsuario del JWT contra el dueño real de la reserva.
                    // Si agregas un endpoint nuevo bajo /api/reservas/** y se te
                    // olvida ese chequeo en el controller, cualquier CLIENTE
                    // autenticado podría ver/editar reservas ajenas (fue justo
                    // el Hallazgo 2 de la auditoría — ver ReservaController).
                    .requestMatchers(HttpMethod.GET,   "/api/reservas/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")
                    .requestMatchers(HttpMethod.POST,  "/api/reservas/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")
                    .requestMatchers(HttpMethod.PATCH, "/api/reservas/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")

                    // ── ADMIN o CLIENTE — Habitaciones lectura ────────────
                    .requestMatchers(HttpMethod.GET, "/api/habitaciones/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")
                    .requestMatchers(HttpMethod.GET, "/api/tipohabitaciones/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")

                    // ── ADMIN o CLIENTE — Mensajes de contacto ─────────────
                    .requestMatchers(HttpMethod.POST, "/api/contacto").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")

                    // ── ADMIN o CLIENTE — el usuario ve y gestiona SUS PROPIOS mensajes/respuestas
                    // (deben ir ANTES que la regla general de abajo, que restringe todo /api/contacto/** a ADMIN)
                    .requestMatchers(HttpMethod.GET,   "/api/contacto/mios/**").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")
                    .requestMatchers(HttpMethod.PATCH, "/api/contacto/*/respuesta-vista").hasAnyAuthority("ROL_ADMIN", "ROL_CLIENTE")

                    // ── Solo ADMIN — Mensajes de contacto ───────────────────
                    .requestMatchers(HttpMethod.GET,   "/api/contacto/**").hasAuthority("ROL_ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/contacto/**").hasAuthority("ROL_ADMIN")


                    // ── Todo lo demás requiere JWT ────────────────────────
                    .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }
    // Configura CORS para permitir solicitudes desde el frontend.
    // Los orígenes permitidos se leen de app.cors.allowed-origins (variable
    // de entorno CORS_ALLOWED_ORIGINS), separados por coma. Antes este bean
    // ignoraba esa variable y usaba una lista fija — funcionaba en local
    // pero iba a fallar en producción con un dominio distinto.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        // Aplica esta configuración a todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}