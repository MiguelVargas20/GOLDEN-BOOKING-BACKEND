package com.sena.goldenbooking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Mismos orígenes permitidos que usa SecurityConfig para el resto de la API
    // (app.cors.allowed-origins). Antes estaban hardcodeados a localhost aquí,
    // así que al desplegar a un dominio real el WebSocket se habría quedado
    // roto en silencio aunque el resto de la app funcionara bien.
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

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
                .setAllowedOrigins(allowedOrigins.split(","))
                .withSockJS();
    }
}