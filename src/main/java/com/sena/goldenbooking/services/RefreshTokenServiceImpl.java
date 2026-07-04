package com.sena.goldenbooking.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.exception.RefreshTokenInvalidoException;
import com.sena.goldenbooking.models.RefreshToken;
import com.sena.goldenbooking.repositories.RefreshTokenRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final SecureRandom random = new SecureRandom();

    // Días de validez del refresh token (configurable por properties/env)
    @Value("${jwt.refresh.expiracion-dias:7}")
    private int diasExpiracion;

    public RefreshTokenServiceImpl(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    @Override
    public RefreshTokenPair crearNuevoRefreshToken(String userId) {
        String familyId = UUID.randomUUID().toString();
        return emitirNuevoToken(userId, familyId);
    }

    @Override
    public RefreshTokenPair rotar(String rawToken) {
        String hash = hashear(rawToken);

        RefreshToken actual = repo.findByTokenHash(hash)
                .orElseThrow(() -> new RefreshTokenInvalidoException("Refresh token inválido o desconocido"));

        if (actual.isRevocado()) {
            // El token ya fue usado antes (o cerrado sesión): esto es reuso.
            // Se asume posible robo → se revoca toda la sesión/familia.
            log.warn("Reuso de refresh token detectado. userId={} familyId={} — revocando toda la familia",
                    actual.getUserId(), actual.getFamilyId());
            revocarFamilia(actual.getFamilyId());
            throw new RefreshTokenInvalidoException(
                    "Refresh token ya utilizado. Por seguridad se cerraron todas las sesiones, vuelve a iniciar sesión.");
        }

        if (actual.getExpiracion().before(new Date())) {
            throw new RefreshTokenInvalidoException("Refresh token expirado, vuelve a iniciar sesión");
        }

        // Rotación: se consume el actual y se emite uno nuevo de la misma familia
        actual.setRevocado(true);
        actual.setRevocadoEn(new Date());
        repo.save(actual);

        return emitirNuevoToken(actual.getUserId(), actual.getFamilyId());
    }

    @Override
    public void revocarPorToken(String rawToken) {
        String hash = hashear(rawToken);
        repo.findByTokenHash(hash).ifPresent(t -> {
            t.setRevocado(true);
            t.setRevocadoEn(new Date());
            repo.save(t);
        });
    }

    private void revocarFamilia(String familyId) {
        List<RefreshToken> familia = repo.findByFamilyId(familyId);
        Date ahora = new Date();
        familia.forEach(t -> {
            t.setRevocado(true);
            t.setRevocadoEn(ahora);
        });
        repo.saveAll(familia);
    }

    private RefreshTokenPair emitirNuevoToken(String userId, String familyId) {
        String raw = generarTokenCrudo();
        Date expiracion = new Date(System.currentTimeMillis() + diasExpiracion * 24L * 60 * 60 * 1000);

        RefreshToken nuevo = RefreshToken.builder()
                .tokenHash(hashear(raw))
                .userId(userId)
                .familyId(familyId)
                .revocado(false)
                .creadoEn(new Date())
                .expiracion(expiracion)
                .build();

        repo.save(nuevo);

        return new RefreshTokenPair(raw, expiracion, userId);
    }

    // 512 bits de entropía, codificados en Base64 URL-safe (apto para cookie)
    private String generarTokenCrudo() {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashear(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre está disponible en la JVM estándar; no debería ocurrir nunca
            throw new IllegalStateException("Algoritmo de hash no disponible", e);
        }
    }
}