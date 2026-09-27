package com.sena.goldenbooking.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.auth.model.IntentoFallido;

public interface IntentoFallidoRepository extends MongoRepository<IntentoFallido, String> {
    // El ID es la clave (ej. "login:admin1"), existsById/findById/deleteById
    // que ya trae MongoRepository son suficientes.
}