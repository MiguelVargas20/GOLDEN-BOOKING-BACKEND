package com.sena.goldenbooking.compartido.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.sena.goldenbooking.security.WebSocketAuthInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Mismos orígenes permitidos que usa SecurityConfig para el resto de la API
    // (app.cors.allowed-origins). Antes estaban hardcodeados a localhost aquí,
    // así que al desplegar a un dominio real el WebSocket se habría quedado
    // roto en silencio aunque el resto de la app funcionara bien.
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /** Autenticación con el JWT al conectar y permisos por canal (ver WebSocketAuthInterceptor). */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefijo para mensajes que el servidor ENVÍA a los clientes
        // El front se suscribe a "/topic/reservas-deporte"
        config.enableSimpleBroker("/topic");

        // Prefijo para mensajes que el cliente ENVÍA al servidor
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint al que el front se conecta para iniciar WebSocket
        // SockJS es un fallback para navegadores que no soportan WebSocket nativo
        registry.addEndpoint("/ws")
                .setAllowedOrigins(java.util.Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origen -> !origen.isEmpty())
                        .toArray(String[]::new))
                .withSockJS();
    }
}