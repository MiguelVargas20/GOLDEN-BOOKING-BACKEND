package com.sena.goldenbooking.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.data.CalendarOutputter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // URL base del frontend — configurable vía app.frontend.url
    // (antes hardcodeada como "http://localhost:5173" en cada método)
    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCorreoSimple(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        mailSender.send(mensaje);
    }

    /**
     * Envía un correo con contenido HTML (necesario para botones,
     * colores de marca, etc. — un correo de texto plano no soporta eso).
     */
    public void enviarCorreoHtml(String destinatario, String asunto, String html) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(html, true); // true = el contenido es HTML
            mailSender.send(mensaje);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo enviar el correo: " + e.getMessage());
        }
    }

    public void enviarCorreoVerificacion(String destinatario, String token) {
        String urlVerificacion = frontendUrl + "/verificar-cuenta?token=" + token;
        String html = """
                <div style="font-family: 'Poppins', sans-serif; max-width: 500px; margin: auto; padding: 30px; border-radius: 12px; border: 1px solid #eee;">
                    <h2 style="color: #1a1a2e;">Bienvenido a <span style="color:#f68b1e;">Golden Booking</span></h2>
                    <p style="color: #4a5568;">Gracias por registrarte. Para activar tu cuenta, confirma tu correo haciendo clic en el siguiente botón:</p>
                    <a href="%s" style="display:inline-block; background:#f68b1e; color:#fff; padding:12px 28px; border-radius:8px; text-decoration:none; font-weight:600; margin-top:15px;">
                        Verificar mi cuenta
                    </a>
                    <p style="color: #a0aec0; font-size: 0.85rem; margin-top: 25px;">Si no creaste esta cuenta, ignora este correo.</p>
                </div>
                """.formatted(urlVerificacion);

        enviarCorreoHtml(destinatario, "Verifica tu cuenta - Golden Booking", html);
    }

    public void enviarCorreoRecuperacion(String destinatario, String token) {
        String urlRecuperacion = frontendUrl + "/restablecer-password?token=" + token;
        String html = """
                <div style="font-family: 'Poppins', sans-serif; max-width: 500px; margin: auto; padding: 30px; border-radius: 12px; border: 1px solid #eee;">
                    <h2 style="color: #1a1a2e;">Recupera tu contraseña</h2>
                    <p style="color: #4a5568;">Recibimos una solicitud para restablecer tu contraseña. Este enlace es válido por 1 hora:</p>
                    <a href="%s" style="display:inline-block; background:#f68b1e; color:#fff; padding:12px 28px; border-radius:8px; text-decoration:none; font-weight:600; margin-top:15px;">
                        Restablecer contraseña
                    </a>
                    <p style="color: #a0aec0; font-size: 0.85rem; margin-top: 25px;">Si no solicitaste esto, ignora este correo — tu contraseña actual sigue siendo válida.</p>
                </div>
                """.formatted(urlRecuperacion);

        enviarCorreoHtml(destinatario, "Recupera tu contraseña - Golden Booking", html);
    }

    /**
     * Envía la confirmación de una reserva con un archivo .ics adjunto,
     * que el usuario puede abrir para agregar el evento directo a su
     * Google Calendar, Outlook, etc.
     */
    public void enviarConfirmacionReserva(String destinatario, String tituloEvento,
            String descripcionHtml, LocalDateTime inicio, LocalDateTime fin) {
        try {
            // 1. Construir el .ics en memoria
            byte[] icsBytes = generarIcs(tituloEvento, inicio, fin);

            // 2. Armar el correo con adjunto
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8"); // true = multipart, permite adjuntos
            helper.setTo(destinatario);
            helper.setSubject("Confirmación de tu reserva - " + tituloEvento);
            helper.setText(descripcionHtml, true);
            helper.addAttachment("reserva.ics", () -> new java.io.ByteArrayInputStream(icsBytes), "text/calendar");

            mailSender.send(mensaje);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo enviar la confirmación: " + e.getMessage());
        }
    }

    private byte[] generarIcs(String titulo, LocalDateTime inicio, LocalDateTime fin) throws Exception {
        Calendar calendario = new Calendar();
        calendario.getProperties().add(new ProdId("-//Golden Booking//Reservas//ES"));
        calendario.getProperties().add(Version.VERSION_2_0);
        calendario.getProperties().add(CalScale.GREGORIAN);

        Date fechaInicio = Date.from(inicio.atZone(ZoneId.systemDefault()).toInstant());
        Date fechaFin = Date.from(fin.atZone(ZoneId.systemDefault()).toInstant());

        VEvent evento = new VEvent(
                new net.fortuna.ical4j.model.DateTime(fechaInicio),
                new net.fortuna.ical4j.model.DateTime(fechaFin),
                titulo
        );
        evento.getProperties().add(new Uid(java.util.UUID.randomUUID().toString()));

        calendario.getComponents().add(evento);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new CalendarOutputter().output(calendario, out);
        return out.toByteArray();
    }

    public void enviarAvisoCancelacion(String destinatario, String tituloEvento, String detalleHtml) {
    String html = """
            <div style="font-family: 'Poppins', sans-serif; max-width: 500px; margin: auto; padding: 30px; border-radius: 12px; border: 1px solid #eee;">
                <h2 style="color: #1a1a2e;">Reserva cancelada — <span style="color:#e53e3e;">Golden Booking</span></h2>
                <p style="color: #4a5568;">Tu reserva fue cancelada. Estos son los detalles:</p>
                %s
                <p style="color: #a0aec0; font-size: 0.85rem; margin-top: 20px;">Si crees que esto es un error, contáctanos o realiza una nueva reserva desde la app.</p>
            </div>
            """.formatted(detalleHtml);

    enviarCorreoHtml(destinatario, "Cancelación de reserva - " + tituloEvento, html);
}
}