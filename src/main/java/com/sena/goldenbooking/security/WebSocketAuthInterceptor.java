package com.sena.goldenbooking.security;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Seguridad del WebSocket (STOMP). El endpoint /ws es público porque el
 * canal de disponibilidad de espacios ("/topic/reservas-deporte") lo usa
 * cualquiera, pero:
 *
 *  - CONNECT: si trae "Authorization: Bearer ...", se valida con las mismas
 *    reglas que la API (JwtFilter.validarToken). Un token inválido se rechaza.
 *  - SUBSCRIBE a "/topic/admin/**": solo usuarios con ROL_ADMIN (los avisos
 *    llevan nombres de clientes).
 *  - SEND a "/topic/**": prohibido. Solo el servidor publica; si no, un
 *    cliente podría enviar avisos falsos a los demás.
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    static final String PREFIJO_ADMIN = "/topic/admin";
    private static final String ROL_ADMIN = "ROL_ADMIN";

    private final JwtFilter jwtFilter;

    public WebSocketAuthInterceptor(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        StompCommand comando = accessor.getCommand();
        String destino = accessor.getDestination();

        if (comando == StompCommand.CONNECT) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                JwtFilter.ResultadoToken r = jwtFilter.validarToken(header.substring(7), "websocket", "/ws");
                if (r.autenticacion() == null) {
                    throw new AccessDeniedException("Token inválido para el WebSocket (" + r.motivo() + ").");
                }
                accessor.setUser(r.autenticacion());
            }
        } else if (comando == StompCommand.SUBSCRIBE && destino != null && destino.startsWith(PREFIJO_ADMIN)) {
            if (!esAdmin(accessor.getUser())) {
                log.warn("Suscripción a {} rechazada (no es ADMIN).", destino);
                throw new AccessDeniedException("No tienes permiso para suscribirte a este canal.");
            }
        } else if (comando == StompCommand.SEND && destino != null && destino.startsWith("/topic")) {
            throw new AccessDeniedException("Solo el servidor puede publicar en este canal.");
        }
        return message;
    }

    private static boolean esAdmin(Principal usuario) {
        return usuario instanceof Authentication auth
                && auth.getAuthorities().stream().anyMatch(a -> ROL_ADMIN.equals(a.getAuthority()));
    }
}
