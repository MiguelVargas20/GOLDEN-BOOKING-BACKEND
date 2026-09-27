package com.sena.goldenbooking.compartido.email;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import jakarta.mail.internet.MimeMessage;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    // FIX hallazgo #10: @Async saca el envío del hilo de la petición HTTP. El
    // llamador ya no espera a que Gmail responda para poder devolver su propia
    // respuesta al frontend.
    @Async
    public void enviarCorreoSimple(String destinatario, String asunto, String cuerpo) {
        // Al ser @Async, un error aquí ya no le llega a quien llamó: se
        // registra en el log para que no se pierda en silencio.
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
            log.info("Correo enviado a {} ({})", destinatario, asunto);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {} ({}): {}", destinatario, asunto, e.getMessage());
        }
    }

    /**
     * Envía un correo con contenido HTML (necesario para botones,
     * colores de marca, etc. — un correo de texto plano no soporta eso).
     */
    @Async
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

    @Async
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

    @Async
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
    @Async
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

        Date fechaInicio = Date.from(inicio.atZone(ZonaHoraria.ZONA).toInstant());
        Date fechaFin = Date.from(fin.atZone(ZonaHoraria.ZONA).toInstant());

        // En UTC ("...Z"): sin esto ical4j escribe una hora "flotante" formateada
        // con la zona del servidor (UTC en AWS) y el calendario del usuario la
        // interpreta como hora local de Colombia, corriendo el evento 5h.
        net.fortuna.ical4j.model.DateTime dtInicio = new net.fortuna.ical4j.model.DateTime(fechaInicio);
        net.fortuna.ical4j.model.DateTime dtFin = new net.fortuna.ical4j.model.DateTime(fechaFin);
        dtInicio.setUtc(true);
        dtFin.setUtc(true);

        VEvent evento = new VEvent(dtInicio, dtFin, titulo);
        evento.getProperties().add(new Uid(java.util.UUID.randomUUID().toString()));

        calendario.getComponents().add(evento);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new CalendarOutputter().output(calendario, out);
        return out.toByteArray();
    }
}
