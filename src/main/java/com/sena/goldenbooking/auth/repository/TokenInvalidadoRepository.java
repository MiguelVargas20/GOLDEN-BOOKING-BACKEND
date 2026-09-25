package com.sena.goldenbooking.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.auth.model.TokenInvalidado;

public interface TokenInvalidadoRepository extends MongoRepository<TokenInvalidado, String> {

    // El ID es el token mismo, así que existsById(token) es suficiente
    // No necesitamos métodos extra — MongoRepository ya trae existsById()
}