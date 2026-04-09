package com.sena.goldenbooking.repositories;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.Reserva;

public interface ReservaRepository extends MongoRepository<Reserva, String> {

    /** Lista reservas de un cliente específico */
    List<Reserva> findByDocUsuario(String docUsuario);

    /** Lista reservas por estado */

    List<Reserva> findByEstadoRes(EstadoReserva estR); // Cambiado String por el Enum
}