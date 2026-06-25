package com.sena.goldenbooking.repositories;

import com.sena.goldenbooking.models.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TokenInvalidadoRepository extends MongoRepository<TokenInvalidado, String> {

    // El ID es el token mismo, así que existsById(token) es suficiente
    // No necesitamos métodos extra — MongoRepository ya trae existsById()
}