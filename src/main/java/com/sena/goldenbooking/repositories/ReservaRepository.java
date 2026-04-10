package com.sena.goldenbooking.repositories;

import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.TipoReserva;
import com.sena.goldenbooking.models.EstadoReserva;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ReservaRepository extends MongoRepository<Reserva, String> {

    // Todas las reservas de un usuario por su documento
    List<Reserva> findByDocUsuario(String docUsuario);

    // Filtrar por tipo: HOTELERA o DEPORTIVA
    List<Reserva> findByTipR(TipoReserva tipo);

    // Filtrar por estado: PENDIENTE, CONFIRMADA, CANCELADA
    List<Reserva> findByEstR(EstadoReserva estado);

    // Reservas deportivas de un usuario específico
    @Query("{ 'docUsuario': ?0, 'tipR': 'DEPORTIVA' }")
    List<Reserva> findReservasDeportivasByUsuario(String docUsuario);

    // Reservas hoteleras de un usuario específico
    @Query("{ 'docUsuario': ?0, 'tipR': 'HOTELERA' }")
    List<Reserva> findReservasHotelarasByUsuario(String docUsuario);
}