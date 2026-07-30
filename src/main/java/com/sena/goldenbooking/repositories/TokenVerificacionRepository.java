package com.sena.goldenbooking.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.TokenVerificacion;

public interface TokenVerificacionRepository extends MongoRepository<TokenVerificacion, String> {
    Optional<TokenVerificacion> findByToken(String token);
    void deleteByCorreoAndTipo(String correo, com.sena.goldenbooking.models.TipoToken tipo);
}