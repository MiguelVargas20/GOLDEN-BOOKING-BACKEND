package com.sena.goldenbooking.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.Mensaje;

public interface MensajeRepository extends MongoRepository<Mensaje, String> {
    Page<Mensaje> findAllByOrderByFechaEnvioDesc(Pageable pageable);

    // Búsqueda por nombre de usuario que envió el mensaje (case-insensitive, coincidencia parcial)
    Page<Mensaje> findByNombreContainingIgnoreCaseOrderByFechaEnvioDesc(String nombre, Pageable pageable);

    // Mensajes enviados por un usuario específico (para que él vea sus propios mensajes/respuestas)
    Page<Mensaje> findByCorreoOrderByFechaEnvioDesc(String correo, Pageable pageable);

    // Cuántas respuestas tiene el usuario sin haber visto todavía (para el badge de notificaciones)
    long countByCorreoAndRespuestaIsNotNullAndRespuestaVistaFalse(String correo);

    // Cuenta cuántos mensajes siguen sin marcar como leídos.
    // Lo usa el endpoint del badge/banner del admin.
    long countByLeidoFalse();
}