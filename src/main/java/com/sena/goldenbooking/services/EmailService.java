package com.sena.goldenbooking.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

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
        String urlVerificacion = "http://localhost:5173/verificar-cuenta?token=" + token;
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
        String urlRecuperacion = "http://localhost:5173/restablecer-password?token=" + token;
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
}