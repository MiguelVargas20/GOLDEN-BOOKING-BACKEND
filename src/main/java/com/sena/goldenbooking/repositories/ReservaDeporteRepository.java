package com.sena.goldenbooking.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.ReservaDeporte;

public interface ReservaDeporteRepository extends MongoRepository<ReservaDeporte, String> {

    /** Lista reservas de un espacio específico */
    List<ReservaDeporte> findByIdEsp(String idEsp);

}
