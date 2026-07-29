package com.sena.goldenbooking.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sena.goldenbooking.dtos.MensajeDto;

public interface MensajeService {
    MensajeDto enviar(MensajeDto dto);
    Page<MensajeDto> listarPaginados(Pageable pageable);
    Page<MensajeDto> buscarPorNombre(String nombre, Pageable pageable);
    MensajeDto marcarLeido(String id);
    MensajeDto responder(String id, String textoRespuesta);

    // Total de mensajes sin leer, para el badge/banner del admin
    long contarNoLeidos();

    // ── Lado del usuario (dueño del mensaje) ────────────────────
    Page<MensajeDto> misMensajes(String correo, Pageable pageable);
    long contarRespuestasNoVistas(String correo);
    MensajeDto marcarRespuestaVista(String id, String correo);
}