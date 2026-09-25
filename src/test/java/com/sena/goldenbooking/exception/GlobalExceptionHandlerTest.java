package com.sena.goldenbooking.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

/** Verifica códigos HTTP y que no se filtren mensajes técnicos al usuario. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/usuarios/registro");

    @Test
    void duplicadoDeMongoResponde409SinDetallesTecnicos() {
        ResponseEntity<Map<String, Object>> r = handler.duplicado(new DuplicateKeyException(
                "E11000 duplicate key error collection: goldenbooking.UsuarioPerfil index: correo dup key: { correo: \"ana@x.com\" }"),
                request);

        assertEquals(409, r.getStatusCode().value());
        assertEquals("DATO_DUPLICADO", r.getBody().get("codigo"));
        String mensaje = (String) r.getBody().get("error");
        assertFalse(mensaje.contains("E11000"));
        assertFalse(mensaje.contains("ana@x.com"));
        assertEquals("/api/usuarios/registro", r.getBody().get("path"));
    }

    @Test
    void credencialesIncorrectasResponden401() {
        ResponseEntity<Map<String, Object>> r = handler.autenticacion(new BadCredentialsException("Bad credentials"), request);

        assertEquals(401, r.getStatusCode().value());
        assertEquals("Usuario o contraseña incorrectos.", r.getBody().get("error"));
    }

    @Test
    void errorInesperadoResponde500GenericoSinExponerElMensaje() {
        ResponseEntity<Map<String, Object>> r = handler.inesperado(new NullPointerException("campo x es null"), request);

        assertEquals(500, r.getStatusCode().value());
        assertFalse(((String) r.getBody().get("error")).contains("null"));
    }

    @Test
    void illegalArgumentDeLibreriasNoMuestraSuMensaje() {
        ResponseEntity<Map<String, Object>> r = handler.argumentoInvalido(
                new IllegalArgumentException("No enum constant com.sena.EstadoReserva.XYZ"), request);

        assertEquals(400, r.getStatusCode().value());
        assertFalse(((String) r.getBody().get("error")).contains("enum"));
    }

    @Test
    void reglaDeNegocioSiMuestraSuMensaje() {
        ResponseEntity<Map<String, Object>> r = handler.solicitudInvalida(
                new SolicitudInvalidaException("La fecha de inicio no puede estar en el pasado."), request);

        assertEquals(400, r.getStatusCode().value());
        assertEquals("La fecha de inicio no puede estar en el pasado.", r.getBody().get("error"));
    }
}
