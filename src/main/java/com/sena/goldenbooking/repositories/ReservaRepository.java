package com.sena.goldenbooking.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.Reserva;

public interface ReservaRepository extends MongoRepository<Reserva, String> {

     /** Lista reservas de un cliente específico */
    List<Reserva> findByDocUsuario(String docUsuario);

    //  /** Lista reservas de una reserva específica */
    List<Reserva> findByIdReserv(String id);

        /** Lista reservas por estado */
    List<Reserva> findByEstadoRes(String estR);

}
