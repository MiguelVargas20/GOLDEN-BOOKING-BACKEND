package com.sena.goldenbooking.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository; // <-- Importar

import com.sena.goldenbooking.models.RefreshToken;

@Repository // <-- Agregar esta anotación
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByFamilyId(String familyId);
}