package com.sena.goldenbooking.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.dtos.MensajeDto;
import com.sena.goldenbooking.exception.AccesoDenegadoException;
import com.sena.goldenbooking.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.models.Mensaje;
import com.sena.goldenbooking.repositories.MensajeRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MensajeServiceImpl implements MensajeService {

    private final MensajeRepository repo;
    // Los correos se envían con EmailService (@Async): antes se usaba
    // JavaMailSender directo y la petición HTTP esperaba la ida y vuelta
    // con Gmail (1-3 s) antes de responder al usuario.
    private final EmailService emailService;

    // Correo que recibe las notificaciones de contacto — configurable vía
    // app.admin.correo-notificaciones (application.properties / variable de entorno)
    @Value("${app.admin.correo-notificaciones}")
    private String correoAdminNotificaciones;

    public MensajeServiceImpl(MensajeRepository repo, EmailService emailService) {
        this.repo = repo;
        this.emailService = emailService;
    }

    private MensajeDto toDto(Mensaje m) {
        return MensajeDto.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .correo(m.getCorreo())
                .contenido(m.getContenido())
                .fechaEnvio(m.getFechaEnvio())
                .leido(m.isLeido())
                .respuesta(m.getRespuesta())
                .fechaRespuesta(m.getFechaRespuesta())
                .respuestaVista(m.isRespuestaVista())
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

        // Aviso al admin en segundo plano (@Async): la respuesta al usuario no
        // espera al servidor de correo. Si el envío falla, EmailService lo registra.
        emailService.enviarCorreoSimple(
                correoAdminNotificaciones,
                "Golden Booking - Nuevo mensaje de: " + guardado.getNombre(),
                "Has recibido un nuevo mensaje de contacto:\n\n" +
                "Nombre: " + guardado.getNombre() + "\n" +
                "Correo: " + guardado.getCorreo() + "\n\n" +
                "Mensaje:\n" + guardado.getContenido() + "\n\n" +
                "Gestiona este mensaje desde el panel de administración.");

        return toDto(guardado);
    }

    @Override
    public Page<MensajeDto> listarPaginados(Pageable pageable) {
        return repo.findAllByOrderByFechaEnvioDesc(pageable).map(this::toDto);
    }

    @Override
    public Page<MensajeDto> buscarPorNombre(String nombre, Pageable pageable) {
        return repo.findByNombreContainingIgnoreCaseOrderByFechaEnvioDesc(nombre, pageable).map(this::toDto);
    }

    @Override
    public MensajeDto marcarLeido(String id) {
        Mensaje mensaje = repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado con ID: " + id));
        mensaje.setLeido(true);
        return toDto(repo.save(mensaje));
    }

    @Override
    public MensajeDto responder(String id, String textoRespuesta) {
        Mensaje mensaje = repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado con ID: " + id));

        mensaje.setRespuesta(textoRespuesta);
        mensaje.setFechaRespuesta(LocalDateTime.now());
        mensaje.setRespuestaVista(false); // nueva respuesta => el usuario aún no la ha visto
        mensaje.setLeido(true); // responder implica que ya fue atendido
        Mensaje guardado = repo.save(mensaje);

        // Respuesta al usuario en segundo plano (@Async), igual que arriba.
        emailService.enviarCorreoSimple(
                guardado.getCorreo(),
                "Golden Booking - Respuesta a tu mensaje",
                "Hola " + guardado.getNombre() + ",\n\n" +
                "Recibimos tu mensaje:\n\"" + guardado.getContenido() + "\"\n\n" +
                "Nuestra respuesta:\n" + textoRespuesta + "\n\n" +
                "— Equipo Golden Booking");

        return toDto(guardado);
    }

    @Override
    public long contarNoLeidos() {
        return repo.countByLeidoFalse();
    }

    @Override
    public Page<MensajeDto> misMensajes(String correo, Pageable pageable) {
        return repo.findByCorreoOrderByFechaEnvioDesc(correo, pageable).map(this::toDto);
    }

    @Override
    public long contarRespuestasNoVistas(String correo) {
        return repo.countByCorreoAndRespuestaIsNotNullAndRespuestaVistaFalse(correo);
    }

    @Override
    public MensajeDto marcarRespuestaVista(String id, String correo) {
        Mensaje mensaje = repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado con ID: " + id));

        if (!mensaje.getCorreo().equalsIgnoreCase(correo)) {
            throw new AccesoDenegadoException("No puedes ver la respuesta de un mensaje que no es tuyo.");
        }

        mensaje.setRespuestaVista(true);
        return toDto(repo.save(mensaje));
    }
}