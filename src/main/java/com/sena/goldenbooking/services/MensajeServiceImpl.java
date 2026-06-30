package com.sena.goldenbooking.services;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.dtos.MensajeDto;
import com.sena.goldenbooking.models.Mensaje;
import com.sena.goldenbooking.repositories.MensajeRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MensajeServiceImpl implements MensajeService {

    private final MensajeRepository repo;
    private final JavaMailSender mailSender; // 1. Agregamos el enviador de correos como dependencia final

    // 2. Lo inyectamos a través del constructor (Mejor práctica que @Autowired)
    public MensajeServiceImpl(MensajeRepository repo, JavaMailSender mailSender) {
        this.repo = repo;
        this.mailSender = mailSender;
    }

    private MensajeDto toDto(Mensaje m) {
        return MensajeDto.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .correo(m.getCorreo())
                .contenido(m.getContenido())
                .fechaEnvio(m.getFechaEnvio())
                .leido(m.isLeido())
                .build();
    }

    @Override
    public MensajeDto enviar(MensajeDto dto) {
        // Tu lógica actual para construir y guardar el mensaje
        Mensaje mensaje = Mensaje.builder()
                .nombre(dto.getNombre())
                .correo(dto.getCorreo())
                .contenido(dto.getContenido())
                .fechaEnvio(LocalDateTime.now())
                .leido(false) // Aquí ya aseguras que empiece en false, reemplaza el if null del ejemplo
                .build();

        Mensaje guardado = repo.save(mensaje);
        log.info("Mensaje de contacto recibido de: {}", dto.getCorreo());

        // 3. Implementación segura del envío de correo
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo("tu-correo-admin@gmail.com"); // <-- Cambia esto por tu correo real de administrador
            email.setSubject("Golden Booking - Nuevo mensaje de: " + guardado.getNombre());
            email.setText("Has recibido un nuevo mensaje de contacto:\n\n" +
                          "Nombre: " + guardado.getNombre() + "\n" +
                          "Correo: " + guardado.getCorreo() + "\n\n" +
                          "Mensaje:\n" + guardado.getContenido() + "\n\n" +
                          "Gestiona este mensaje desde el panel de administración.");
            
            mailSender.send(email);
            log.info("Correo de notificación enviado correctamente.");
        } catch (Exception e) {
            // Usamos log.error en lugar de System.err.println aprovechando @Slf4j
            log.error("Error al enviar el correo de notificación: {}", e.getMessage());
        }

        return toDto(guardado);
    }

    @Override
    public Page<MensajeDto> listarPaginados(Pageable pageable) {
        return repo.findAllByOrderByFechaEnvioDesc(pageable).map(this::toDto);
    }

    @Override
    public MensajeDto marcarLeido(String id) {
        Mensaje mensaje = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado con ID: " + id));
        mensaje.setLeido(true);
        return toDto(repo.save(mensaje));
    }
}