package com.sena.goldenbooking.compartido.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/** Envío por la API de Brevo contra un servidor HTTP local que imita a Brevo. */
class BrevoClientTest {

    private HttpServer servidor;
    private final AtomicReference<String> ruta = new AtomicReference<>();
    private final AtomicReference<String> clave = new AtomicReference<>();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private final AtomicReference<String> longitud = new AtomicReference<>();
    private int estadoRespuesta = 201;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", intercambio -> {
            ruta.set(intercambio.getRequestMethod() + " " + intercambio.getRequestURI().getPath());
            clave.set(intercambio.getRequestHeaders().getFirst("api-key"));
            longitud.set(intercambio.getRequestHeaders().getFirst("Content-Length"));
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] respuesta = "{\"messageId\":\"<1@brevo>\"}".getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estadoRespuesta, respuesta.length);
            intercambio.getResponseBody().write(respuesta);
            intercambio.close();
        });
        servidor.start();
    }

    @AfterEach
    void apagarServidor() {
        servidor.stop(0);
    }

    private BrevoClient cliente(String apiKey, String remitente) {
        return new BrevoClient(apiKey, "http://localhost:" + servidor.getAddress().getPort(), remitente, "Golden Booking");
    }

    @Test
    void sinClaveNoSeActiva() {
        assertThat(cliente("", "a@b.com").activo()).isFalse();
        assertThat(cliente("   ", "a@b.com").activo()).isFalse();
        assertThat(cliente("xkeysib-123", "a@b.com").activo()).isTrue();
    }

    @Test
    void enviaHtmlConRemitenteClaveYAdjuntoIcs() {
        byte[] ics = "BEGIN:VCALENDAR".getBytes(StandardCharsets.UTF_8);

        cliente("xkeysib-123", "reservas@golden.com")
                .enviar("laura@correo.com", "Confirmación de tu reserva", "<b>Hola</b>", true, Map.of("reserva.ics", ics));

        assertThat(ruta.get()).isEqualTo("POST /v3/smtp/email");
        assertThat(clave.get()).isEqualTo("xkeysib-123");
        // El cuerpo va completo con Content-Length (no "chunked")
        assertThat(longitud.get()).isEqualTo(String.valueOf(cuerpo.get().getBytes(StandardCharsets.UTF_8).length));
        assertThat(cuerpo.get())
                .contains("\"sender\":{")
                .contains("\"email\":\"reservas@golden.com\"")
                .contains("\"name\":\"Golden Booking\"")
                .contains("\"to\":[{\"email\":\"laura@correo.com\"}]")
                .contains("\"subject\":\"Confirmación de tu reserva\"")
                .contains("\"htmlContent\":\"<b>Hola</b>\"")
                .contains("\"name\":\"reserva.ics\"")
                .contains("\"content\":\"" + Base64.getEncoder().encodeToString(ics) + "\"")
                .doesNotContain("textContent");
    }

    @Test
    void textoPlanoSinAdjuntos() {
        cliente("xkeysib-123", "reservas@golden.com")
                .enviar("laura@correo.com", "Aviso", "Hola Laura", false, Map.of());

        assertThat(cuerpo.get())
                .contains("\"textContent\":\"Hola Laura\"")
                .doesNotContain("htmlContent")
                .doesNotContain("attachment");
    }

    @Test
    void unErrorDeBrevoSeReportaComoExcepcion() {
        estadoRespuesta = 401;

        assertThatThrownBy(() -> cliente("clave-mala", "reservas@golden.com")
                .enviar("laura@correo.com", "Aviso", "Hola", false, Map.of()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void sinRemitenteNoIntentaEnviar() {
        assertThatThrownBy(() -> cliente("xkeysib-123", "")
                .enviar("laura@correo.com", "Aviso", "Hola", false, Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_USERNAME");
        assertThat(ruta.get()).isNull();
    }
}
