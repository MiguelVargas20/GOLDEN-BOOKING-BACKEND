package com.sena.goldenbooking.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.IntentoFallido;

public interface IntentoFallidoRepository extends MongoRepository<IntentoFallido, String> {
    // El ID es la clave (ej. "login:admin1"), existsById/findById/deleteById
    // que ya trae MongoRepository son suficientes.
}