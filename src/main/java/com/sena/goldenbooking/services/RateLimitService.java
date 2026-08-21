package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.sena.goldenbooking.exception.DemasiadosIntentosException;
import com.sena.goldenbooking.models.IntentoFallido;
import com.sena.goldenbooking.repositories.IntentoFallidoRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Rate limiting simple basado en un contador con TTL en Mongo (mismo patrón
 * que TokenInvalidado). Se usa en AuthController para login y para
 * solicitar-recuperación, donde antes no había ningún límite de intentos.
 *
 * No es un lock atómico a nivel de Mongo (es lectura + escritura, como el
 * resto del proyecto) — para el volumen de tráfico de este backend es
 * suficiente. Si el día de mañana esto corre con mucho tráfico concurrente
 * real, la mejora natural es usar findAndModify con $inc para que el
 * incremento sea atómico.
 */
@Slf4j
@Service
public class RateLimitService {

    private final IntentoFallidoRepository repo;

    public RateLimitService(IntentoFallidoRepository repo) {
        this.repo = repo;
    }

    /**
     * Lanza DemasiadosIntentosException si 'clave' ya alcanzó 'maxIntentos'
     * dentro de su ventana vigente. Se llama ANTES de intentar la operación
     * protegida (login, envío de correo de recuperación, etc.).
     */
    public void verificarNoBloqueado(String clave, int maxIntentos) {
        repo.findById(clave)
                .filter(i -> i.getExpiracion().after(new Date())) // ignora entradas ya vencidas
                .ifPresent(i -> {
                    if (i.getContador() >= maxIntentos) {
                        log.warn("Bloqueado por rate limit: {} ({} intentos)", clave, i.getContador());
                        throw new DemasiadosIntentosException(
                                "Demasiados intentos. Intenta de nuevo en unos minutos.");
                    }
                });
    }

    /**
     * Suma un intento a 'clave'. Si no existe o su ventana ya venció, arranca
     * un contador nuevo con 'ventanaMinutos' de vigencia.
     */
    public void registrarIntento(String clave, int ventanaMinutos) {
        IntentoFallido intento = repo.findById(clave).orElse(null);
        Date ahora = new Date();

        if (intento == null || intento.getExpiracion().before(ahora)) {
            intento = IntentoFallido.builder()
                    .clave(clave)
                    .contador(1)
                    .expiracion(Date.from(LocalDateTime.now().plusMinutes(ventanaMinutos)
                            .atZone(ZoneId.systemDefault()).toInstant()))
                    .build();
        } else {
            intento.setContador(intento.getContador() + 1);
        }

        repo.save(intento);
    }

    /** Se llama tras una operación exitosa (ej. login correcto) para resetear el contador. */
    public void limpiar(String clave) {
        repo.deleteById(clave);
    }
}