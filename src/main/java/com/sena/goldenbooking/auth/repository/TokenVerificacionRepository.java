package com.sena.goldenbooking.auth.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.auth.model.TipoToken;
import com.sena.goldenbooking.auth.model.TokenVerificacion;

public interface TokenVerificacionRepository extends MongoRepository<TokenVerificacion, String> {
    Optional<TokenVerificacion> findByToken(String token);
    void deleteByCorreoAndTipo(String correo, TipoToken tipo);
}