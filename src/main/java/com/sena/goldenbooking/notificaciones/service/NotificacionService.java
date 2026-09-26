package com.sena.goldenbooking.notificaciones.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.exception.AccesoDenegadoException;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.notificaciones.model.Notificacion;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.repository.NotificacionRepository;
import com.sena.goldenbooking.reservas.model.TipoReserva;

import lombok.extern.slf4j.Slf4j;

/**
 * Notificaciones de la campana del cliente. Se crean cuando la administración
 * aprueba, cancela o reprograma una reserva, cuando una solicitud vence y
 * cuando una reserva finaliza (para invitar a calificarla).
 */
@Slf4j
@Service
public class NotificacionService {

    /** Cuántas se muestran en la campana. */
    static final int MAXIMO_LISTADO = 30;

    private final NotificacionRepository repo;

    public NotificacionService(NotificacionRepository repo) {
        this.repo = repo;
    }

    /**
     * Crea una notificación. Nunca lanza error: el aviso es un extra y no debe
     * impedir la aprobación o cancelación que lo originó.
     */
    public void notificar(String docUsuario, TipoNotificacion tipo, TipoReserva categoria, String idReserva,
                          String titulo, String mensaje) {
        if (docUsuario == null) return;
        try {
            repo.save(Notificacion.builder()
                    .docUsuario(docUsuario)
                    .tipo(tipo)
                    .categoria(categoria)
                    .idReserva(idReserva)
                    .titulo(titulo)
                    .mensaje(mensaje)
                    .fecha(ZonaHoraria.ahora())
                    .leida(false)
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo guardar la notificación {} para {}: {}", tipo, docUsuario, e.getMessage());
        }
    }

    public List<Notificacion> listarMias(String docUsuario) {
        return repo.findByDocUsuarioOrderByFechaDesc(docUsuario, PageRequest.of(0, MAXIMO_LISTADO));
    }

    public long contarNoLeidas(String docUsuario) {
        return repo.countByDocUsuarioAndLeidaFalse(docUsuario);
    }

    public Notificacion marcarLeida(String id, String docUsuario) {
        Notificacion n = repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La notificación no existe."));
        if (!n.getDocUsuario().equals(docUsuario)) {
            throw new AccesoDenegadoException("Esta notificación no es tuya.");
        }
        if (!n.isLeida()) {
            n.setLeida(true);
            n = repo.save(n);
        }
        return n;
    }

    /** @return cuántas se marcaron. */
    public int marcarTodasLeidas(String docUsuario) {
        List<Notificacion> pendientes = repo.findByDocUsuarioAndLeidaFalse(docUsuario);
        pendientes.forEach(n -> n.setLeida(true));
        repo.saveAll(pendientes);
        return pendientes.size();
    }
}
