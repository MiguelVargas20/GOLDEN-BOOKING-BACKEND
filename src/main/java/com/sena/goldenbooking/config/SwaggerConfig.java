package com.sena.goldenbooking.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * Configuración central de OpenAPI/Swagger para Golden Booking.
 *
 * - @OpenAPIDefinition: define la metadata general (título, versión, servidores)
 *   y aplica el requisito de seguridad "bearerAuth" a TODOS los endpoints por defecto.
 * - @SecurityScheme: declara cómo se autentica la API (JWT tipo Bearer), lo que hace
 *   aparecer el botón "Authorize" en Swagger UI.
 *
 * No se necesita ningún bean adicional: springdoc-openapi escanea esta clase
 * automáticamente al iniciar la aplicación.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Golden Booking API",
        version = "1.0.0",
        description = """
            API REST para la gestión de reservas hoteleras y deportivas de Golden Booking.

            Incluye autenticación basada en JWT, gestión de usuarios, habitaciones,
            tipos de habitación y reservas (hotel y deporte).

            Para probar los endpoints protegidos:
            1. Ejecuta `POST /auth/login` con tus credenciales.
            2. Copia el `token` recibido en la respuesta.
            3. Haz clic en el botón "Authorize" (arriba a la derecha) y pega el token
               (sin el prefijo "Bearer ", Swagger lo agrega automáticamente).
            """,
        contact = @Contact(
            name = "Miguel Andrés Vargas León",
            url = "https://www.linkedin.com/in/miguel-andres-vargas-leon-a4a3a0330"
        ),
        license = @License(
            name = "Uso académico - SENA"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Servidor local de desarrollo")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "Autenticación mediante JWT. Pega únicamente el token (sin 'Bearer ')."
)
public class SwaggerConfig {

}