package com.sena.goldenbooking.controllers;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.sena.goldenbooking.dtos.MensajeDto;
import com.sena.goldenbooking.dtos.ResponderMensajeDto;
import com.sena.goldenbooking.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.models.UsuarioAuth;
import com.sena.goldenbooking.repositories.UsuarioAuthRepository;
import com.sena.goldenbooking.repositories.UsuarioRepository;
import com.sena.goldenbooking.services.MensajeService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Contacto", description = "Mensajes enviados desde el formulario de contacto.")
@RestController
@RequestMapping("/api/contacto")
public class MensajeController {

    private final MensajeService service;
    private final UsuarioAuthRepository authRepo;
    private final UsuarioRepository usuarioRepo;

    public MensajeController(MensajeService service, UsuarioAuthRepository authRepo, UsuarioRepository usuarioRepo) {
        this.service = service;
        this.authRepo = authRepo;
        this.usuarioRepo = usuarioRepo;
    }

    // Resuelve el correo real del usuario autenticado a partir del username (subject del JWT)
    private String correoDe(Authentication authentication) {
        UsuarioAuth auth = authRepo.findByUser(authentication.getName())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado."));
        return usuarioRepo.findById(auth.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Perfil no encontrado."))
                .getCorreo();
    }

    // POST /api/contacto — cualquier usuario autenticado puede enviar un mensaje
    @PostMapping
    public ResponseEntity<MensajeDto> enviar(@Valid @RequestBody MensajeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.enviar(dto));
    }

    // GET /api/contacto — solo ADMIN, para revisar los mensajes recibidos
    // Si se envía "nombre", filtra por remitente (búsqueda parcial, sin mayúsculas/minúsculas)
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String nombre) {

        Pageable pageable = PageRequest.of(page, size);
        var pagina = (nombre == null || nombre.isBlank())
                ? service.listarPaginados(pageable)
                : service.buscarPorNombre(nombre.trim(), pageable);

        return ResponseEntity.ok(Map.of(
            "contenido",      pagina.getContent(),
            "paginaActual",   pagina.getNumber(),
            "totalPaginas",   pagina.getTotalPages(),
            "totalElementos", pagina.getTotalElements()
        ));
    }

    // GET /api/contacto/no-leidos/count — solo ADMIN, para el badge del banner
    @GetMapping("/no-leidos/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidos() {
        return ResponseEntity.ok(Map.of("noLeidos", service.contarNoLeidos()));
    }

    // PATCH /api/contacto/{id}/leido — solo ADMIN
    @PatchMapping("/{id}/leido")
    public ResponseEntity<MensajeDto> marcarLeido(@PathVariable String id) {
        return ResponseEntity.ok(service.marcarLeido(id));
    }

    // PATCH /api/contacto/{id}/responder — solo ADMIN, envía la respuesta por correo al usuario
    @PatchMapping("/{id}/responder")
    public ResponseEntity<MensajeDto> responder(@PathVariable String id,
                                                 @Valid @RequestBody ResponderMensajeDto dto) {
        return ResponseEntity.ok(service.responder(id, dto.getRespuesta()));
    }

    // ── Lado del usuario (ADMIN o CLIENTE, solo ve SUS mensajes) ──────

    // GET /api/contacto/mios — el historial de mensajes que el usuario envió, con las respuestas del admin
    @GetMapping("/mios")
    public ResponseEntity<Map<String, Object>> misMensajes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        var pagina = service.misMensajes(correoDe(authentication), pageable);

        return ResponseEntity.ok(Map.of(
            "contenido",      pagina.getContent(),
            "paginaActual",   pagina.getNumber(),
            "totalPaginas",   pagina.getTotalPages(),
            "totalElementos", pagina.getTotalElements()
        ));
    }

    // GET /api/contacto/mios/no-vistas/count — para el badge de notificaciones del usuario
    @GetMapping("/mios/no-vistas/count")
    public ResponseEntity<Map<String, Long>> contarRespuestasNoVistas(Authentication authentication) {
        return ResponseEntity.ok(Map.of("noVistas", service.contarRespuestasNoVistas(correoDe(authentication))));
    }

    // PATCH /api/contacto/{id}/respuesta-vista — el usuario marca que ya leyó la respuesta del admin
    @PatchMapping("/{id}/respuesta-vista")
    public ResponseEntity<MensajeDto> marcarRespuestaVista(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(service.marcarRespuestaVista(id, correoDe(authentication)));
    }
}