package com.sena.goldenbooking.controllers;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sena.goldenbooking.dtos.MensajeDto;
import com.sena.goldenbooking.services.MensajeService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Contacto", description = "Mensajes enviados desde el formulario de contacto.")
@RestController
@RequestMapping("/api/contacto")
public class MensajeController {

    private final MensajeService service;

    public MensajeController(MensajeService service) {
        this.service = service;
    }

    // POST /api/contacto — cualquier usuario autenticado puede enviar un mensaje
    @PostMapping
    public ResponseEntity<MensajeDto> enviar(@Valid @RequestBody MensajeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.enviar(dto));
    }

    // GET /api/contacto — solo ADMIN, para revisar los mensajes recibidos
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        var pagina = service.listarPaginados(pageable);

        return ResponseEntity.ok(Map.of(
            "contenido",      pagina.getContent(),
            "paginaActual",   pagina.getNumber(),
            "totalPaginas",   pagina.getTotalPages(),
            "totalElementos", pagina.getTotalElements()
        ));
    }

    // PATCH /api/contacto/{id}/leido — solo ADMIN
    @PatchMapping("/{id}/leido")
    public ResponseEntity<MensajeDto> marcarLeido(@PathVariable String id) {
        return ResponseEntity.ok(service.marcarLeido(id));
    }
}