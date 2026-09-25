package com.sena.goldenbooking.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.auth.model.RefreshToken;

// No necesita @Repository: Spring Data detecta solo las interfaces MongoRepository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByFamilyId(String familyId);

    List<RefreshToken> findByUserIdAndRevocadoFalse(String userId);
}