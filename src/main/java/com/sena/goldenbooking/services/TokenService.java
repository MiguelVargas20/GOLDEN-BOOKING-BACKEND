package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sena.goldenbooking.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.models.TipoToken;
import com.sena.goldenbooking.models.TokenVerificacion;
import com.sena.goldenbooking.repositories.TokenVerificacionRepository;

@Service
public class TokenService {

    private final TokenVerificacionRepository tokenRepository;

    public TokenService(TokenVerificacionRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Genera un token nuevo para el correo dado, eliminando primero
     * cualquier token anterior del mismo tipo para evitar duplicados.
     */
    public String generarToken(String correo, TipoToken tipo) {
        tokenRepository.deleteByCorreoAndTipo(correo, tipo);

        String codigo = UUID.randomUUID().toString();

        TokenVerificacion nuevoToken = TokenVerificacion.builder()
                .token(codigo)
                .correo(correo)
                .tipo(tipo)
                .fechaCreacion(LocalDateTime.now())
                .build();

        tokenRepository.save(nuevoToken);
        return codigo;
    }

    /**
     * Valida que el token exista y corresponda al tipo esperado.
     * Devuelve el correo asociado si es válido.
     * Lanza excepción si no existe (inválido o ya expiró y MongoDB lo borró).
     */
    public String validarYObtenerCorreo(String token, TipoToken tipoEsperado) {
        TokenVerificacion tokenEncontrado = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNoEncontradoException("El enlace no es válido o ya expiró."));

        if (tokenEncontrado.getTipo() != tipoEsperado) {
            throw new IllegalArgumentException("Este enlace no corresponde a esta acción.");
        }

        return tokenEncontrado.getCorreo();
    }

    /** Elimina el token una vez usado, para que no se pueda reutilizar. */
    public void invalidarToken(String token) {
        tokenRepository.findByToken(token).ifPresent(tokenRepository::delete);
    }
}