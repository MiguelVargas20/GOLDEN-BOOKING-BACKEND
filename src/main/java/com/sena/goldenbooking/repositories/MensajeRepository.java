package com.sena.goldenbooking.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.Mensaje;

public interface MensajeRepository extends MongoRepository<Mensaje, String> {
    Page<Mensaje> findAllByOrderByFechaEnvioDesc(Pageable pageable);
}