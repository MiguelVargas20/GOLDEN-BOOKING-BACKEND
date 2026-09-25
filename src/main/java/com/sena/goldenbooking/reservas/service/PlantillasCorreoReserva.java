package com.sena.goldenbooking.reservas.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.springframework.web.util.HtmlUtils;

/**
 * Plantillas HTML de los correos del flujo de reservas (solicitud recibida,
 * aprobada y cancelada), con el mismo diseño para hotel y deporte.
 *
 * Todo texto variable se escapa con HtmlUtils: nombres, espacios y motivos
 * los escriben usuarios y no deben poder inyectar HTML en el correo.
 */
public final class PlantillasCorreoReserva {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.forLanguageTag("es-CO"));

    private PlantillasCorreoReserva() {
        // Clase de utilidades: no se instancia
    }

    public static String fecha(LocalDateTime fecha) {
        return fecha == null ? "—" : fecha.format(FORMATO_FECHA);
    }

    public static String pesos(Double valor) {
        return valor == null ? "—" : String.format(Locale.forLanguageTag("es-CO"), "$%,.0f", valor);
    }

    /** Reserva creada: queda PENDIENTE hasta que el admin la apruebe. */
    public static String solicitudRecibida(String nombreCliente, Map<String, String> detalles) {
        return tarjeta("#f68b1e", "Solicitud de reserva recibida",
                "Hola " + escapar(nombreCliente) + ", recibimos tu solicitud. Está <strong>pendiente de aprobación</strong>: "
                        + "te avisaremos por este medio cuando sea confirmada.",
                detalles, null);
    }

    /** El admin aprobó la reserva. */
    public static String reservaConfirmada(String nombreCliente, Map<String, String> detalles) {
        return tarjeta("#38a169", "¡Tu reserva fue confirmada!",
                "Hola " + escapar(nombreCliente) + ", tu reserva fue aprobada. Adjuntamos un archivo de calendario "
                        + "para que la agregues a Google Calendar u Outlook.",
                detalles, null);
    }

    /** Reserva cancelada por el cliente o por el admin (con motivo). */
    public static String reservaCancelada(String nombreCliente, Map<String, String> detalles, String motivo, boolean porAdmin) {
        String intro = porAdmin
                ? "Hola " + escapar(nombreCliente) + ", lamentamos informarte que tu reserva fue cancelada por la administración."
                : "Hola " + escapar(nombreCliente) + ", confirmamos que tu reserva fue cancelada.";
        return tarjeta("#e53e3e", "Reserva cancelada", intro, detalles, motivo);
    }

    private static String tarjeta(String color, String titulo, String intro, Map<String, String> detalles, String motivo) {
        StringBuilder filas = new StringBuilder();
        detalles.forEach((etiqueta, valor) -> filas.append("""
                <tr><td style="padding:6px 0;color:#718096;width:40%%;">%s</td>
                    <td style="padding:6px 0;color:#1a1a2e;font-weight:600;">%s</td></tr>
                """.formatted(escapar(etiqueta), escapar(valor))));

        String bloqueMotivo = (motivo == null || motivo.isBlank()) ? "" : """
                <div style="margin-top:18px;padding:12px 16px;background:#fff5f5;border-left:4px solid #e53e3e;border-radius:6px;">
                    <strong style="color:#c53030;">Motivo:</strong>
                    <span style="color:#4a5568;">%s</span>
                </div>
                """.formatted(escapar(motivo));

        return """
                <div style="font-family:'Poppins',Arial,sans-serif;max-width:520px;margin:auto;padding:30px;border-radius:12px;border:1px solid #eee;">
                    <h2 style="color:#1a1a2e;margin-top:0;">Golden <span style="color:#f68b1e;">Booking</span></h2>
                    <h3 style="color:%s;margin-bottom:6px;">%s</h3>
                    <p style="color:#4a5568;">%s</p>
                    <table style="width:100%%;border-collapse:collapse;margin-top:12px;">%s</table>
                    %s
                    <p style="color:#a0aec0;font-size:0.85rem;margin-top:24px;">
                        Puedes ver el estado de tus reservas en cualquier momento desde la app, en "Mis reservas".
                    </p>
                </div>
                """.formatted(color, escapar(titulo), intro, filas, bloqueMotivo);
    }

    private static String escapar(String texto) {
        return texto == null ? "" : HtmlUtils.htmlEscape(texto);
    }
}
