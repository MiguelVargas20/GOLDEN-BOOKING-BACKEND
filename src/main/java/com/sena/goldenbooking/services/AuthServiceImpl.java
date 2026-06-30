package com.sena.goldenbooking.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.models.TokenInvalidado;
import com.sena.goldenbooking.repositories.TokenInvalidadoRepository;
import com.sena.goldenbooking.security.JwtService;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final JwtService jwtService;
    private final TokenInvalidadoRepository tokenInvalidadoRepo;

    public AuthServiceImpl(
            JwtService jwtService,
            TokenInvalidadoRepository tokenInvalidadoRepo) {
        this.jwtService = jwtService;
        this.tokenInvalidadoRepo = tokenInvalidadoRepo;
    }

    @Override
    public void logout(String token) {
        try {
            TokenInvalidado tokenInvalidado = TokenInvalidado.builder()
                    .token(token)
                    .expiracion(jwtService.extraerExpiracion(token))
                    .build();
            tokenInvalidadoRepo.save(tokenInvalidado);

            log.info("Logout exitoso: Token invalidado correctamente hasta su expiración");

        } catch (Exception e) {
            log.error("Error al intentar invalidar el token durante el logout", e);
            throw e;
        }
    }
}