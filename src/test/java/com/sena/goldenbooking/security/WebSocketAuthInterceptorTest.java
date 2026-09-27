package com.sena.goldenbooking.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Permisos del WebSocket: quién puede conectarse, suscribirse y publicar. */
class WebSocketAuthInterceptorTest {

    private JwtFilter jwtFilter;
    private WebSocketAuthInterceptor interceptor;
    private final MessageChannel canal = mock(MessageChannel.class);

    @BeforeEach
    void setUp() {
        jwtFilter = mock(JwtFilter.class);
        interceptor = new WebSocketAuthInterceptor(jwtFilter);
    }

    private static UsernamePasswordAuthenticationToken usuario(String rol) {
        return new UsernamePasswordAuthenticationToken("u@test.com", null, List.of(new SimpleGrantedAuthority(rol)));
    }

    private static Message<byte[]> mensaje(StompCommand comando, String destino, String token,
                                           UsernamePasswordAuthenticationToken usuario) {
        StompHeaderAccessor acc = StompHeaderAccessor.create(comando);
        if (destino != null) acc.setDestination(destino);
        if (token != null) acc.addNativeHeader("Authorization", "Bearer " + token);
        if (usuario != null) acc.setUser(usuario);
        acc.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());
    }

    @Test
    void conectarConTokenValidoAsignaElUsuario() {
        when(jwtFilter.validarToken(eq("ok"), anyString(), anyString()))
                .thenReturn(new JwtFilter.ResultadoToken(usuario("ROL_ADMIN"), null));
        Message<?> m = interceptor.preSend(mensaje(StompCommand.CONNECT, null, "ok", null), canal);
        assertEquals("u@test.com", StompHeaderAccessor.wrap(m).getUser().getName());
    }

    @Test
    void conectarConTokenInvalidoSeRechaza() {
        when(jwtFilter.validarToken(eq("malo"), anyString(), anyString()))
                .thenReturn(new JwtFilter.ResultadoToken(null, JwtFilter.MOTIVO_TOKEN_INVALIDO));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(mensaje(StompCommand.CONNECT, null, "malo", null), canal));
    }

    @Test
    void canalPublicoSigueAbiertoSinToken() {
        assertDoesNotThrow(() -> interceptor.preSend(mensaje(StompCommand.CONNECT, null, null, null), canal));
        assertDoesNotThrow(() -> interceptor.preSend(mensaje(StompCommand.SUBSCRIBE, "/topic/reservas-deporte", null, null), canal));
    }

    @Test
    void soloElAdminSeSuscribeAlCanalDeAdmin() {
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(mensaje(StompCommand.SUBSCRIBE, "/topic/admin/reservas", null, null), canal));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(mensaje(StompCommand.SUBSCRIBE, "/topic/admin/reservas", null, usuario("ROL_CLIENTE")), canal));
        assertDoesNotThrow(
                () -> interceptor.preSend(mensaje(StompCommand.SUBSCRIBE, "/topic/admin/reservas", null, usuario("ROL_ADMIN")), canal));
    }

    @Test
    void nadiePublicaDirectoEnLosTopicos() {
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(mensaje(StompCommand.SEND, "/topic/admin/reservas", null, usuario("ROL_ADMIN")), canal));
    }
}
