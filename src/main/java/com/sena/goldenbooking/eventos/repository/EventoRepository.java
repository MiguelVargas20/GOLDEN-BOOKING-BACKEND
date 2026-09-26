package com.sena.goldenbooking.eventos.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.eventos.model.Evento;

public interface EventoRepository extends MongoRepository<Evento, String> {

    /** Publicados que aún no terminan (portada del cliente). */
    List<Evento> findByPublicadoTrueAndFechaFinAfter(LocalDateTime ahora, Sort sort);
}
