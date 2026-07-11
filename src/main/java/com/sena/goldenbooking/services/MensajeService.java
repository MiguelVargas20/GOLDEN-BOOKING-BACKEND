package com.sena.goldenbooking.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sena.goldenbooking.dtos.MensajeDto;

public interface MensajeService {
    MensajeDto enviar(MensajeDto dto);
    Page<MensajeDto> listarPaginados(Pageable pageable);
    MensajeDto marcarLeido(String id);

    // Total de mensajes sin leer, para el badge/banner del admin
    long contarNoLeidos();
}