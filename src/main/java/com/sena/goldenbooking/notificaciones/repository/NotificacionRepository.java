package com.sena.goldenbooking.notificaciones.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.notificaciones.model.Notificacion;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {

    /** Las más recientes del cliente. */
    List<Notificacion> findByDocUsuarioOrderByFechaDesc(String docUsuario, Pageable pageable);

    long countByDocUsuarioAndLeidaFalse(String docUsuario);

    List<Notificacion> findByDocUsuarioAndLeidaFalse(String docUsuario);
}
