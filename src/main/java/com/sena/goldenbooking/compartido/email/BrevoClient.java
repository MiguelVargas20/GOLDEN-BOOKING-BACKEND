package com.sena.goldenbooking.compartido.email;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Envío de correos por la API HTTP de Brevo (plan gratis: 300 correos al día).
 *
 * Existe porque algunos hostings gratuitos (p. ej. Render) bloquean la salida
 * SMTP (puertos 25/465/587): ahí Gmail por SMTP no funciona, pero una llamada
 * HTTPS sí. Se activa solo si BREVO_API_KEY está configurada; si no, el backend
 * sigue enviando por SMTP como siempre.
 *
 * El remitente (MAIL_USERNAME o MAIL_REMITENTE) debe estar verificado en Brevo
 * (Senders, domains & dedicated IPs → Senders).
 */
@Component
public class BrevoClient {

    private final String apiKey;
    private final String remitente;
    private final String nombreRemitente;
    private final RestClient http;

    public BrevoClient(
            @Value("${app.mail.brevo.api-key:}") String apiKey,
            @Value("${app.mail.brevo.url:https://api.brevo.com}") String url,
            @Value("${app.mail.remitente:}") String remitente,
            @Value("${app.mail.nombre-remitente:Golden Booking}") String nombreRemitente) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.remitente = remitente == null ? "" : remitente.trim();
        this.nombreRemitente = nombreRemitente;

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(10));
        fabrica.setReadTimeout(Duration.ofSeconds(20));
        // Buffering: arma el JSON completo antes de enviarlo para mandar
        // Content-Length (sin esto va por partes, "chunked", y hay APIs que lo rechazan)
        this.http = RestClient.builder().baseUrl(url)
                .requestFactory(new BufferingClientHttpRequestFactory(fabrica)).build();
    }

    /** true si hay clave de Brevo: el correo sale por HTTP en vez de SMTP. */
    public boolean activo() {
        return !apiKey.isEmpty();
    }

    /**
     * @param html      true = {@code contenido} es HTML; false = texto plano
     * @param adjuntos  nombre de archivo → bytes (puede ser vacío)
     */
    public void enviar(String destinatario, String asunto, String contenido, boolean html,
            Map<String, byte[]> adjuntos) {
        if (remitente.isEmpty()) {
            throw new IllegalStateException("Falta el remitente: configura MAIL_USERNAME o MAIL_REMITENTE.");
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("sender", Map.of("name", nombreRemitente, "email", remitente));
        cuerpo.put("to", List.of(Map.of("email", destinatario)));
        cuerpo.put("subject", asunto);
        cuerpo.put(html ? "htmlContent" : "textContent", contenido);
        if (adjuntos != null && !adjuntos.isEmpty()) {
            List<Map<String, String>> lista = new ArrayList<>();
            adjuntos.forEach((nombre, bytes) ->
                    lista.add(Map.of("name", nombre, "content", Base64.getEncoder().encodeToString(bytes))));
            cuerpo.put("attachment", lista);
        }

        // Un 4xx/5xx lanza excepción (p. ej. clave inválida o remitente sin verificar):
        // EmailService la registra en el log.
        http.post()
                .uri("/v3/smtp/email")
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }
}
