package com.sena.goldenbooking.repositories;

import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.TipoReserva;
import com.sena.goldenbooking.models.EstadoReserva;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface ReservaRepository extends MongoRepository<Reserva, String> {

    // Todas las reservas de un usuario por su documento
    List<Reserva> findByDocumentoUsuario(String documentoUsuario);

    // Filtrar por tipo: HOTEL o DEPORTE
    List<Reserva> findByTipo(TipoReserva tipo);

    // Filtrar por estado: PENDIENTE, CONFIRMADA, CANCELADA
    List<Reserva> findByEstado(EstadoReserva estado);

    // Reservas hoteleras de un usuario específico
    @Query("{ 'documentoUsuario': ?0, 'tipo': 'HOTEL' }")
    List<Reserva> findReservasHotelByUsuario(String documentoUsuario);

    // Reservas deportivas de un usuario específico
    @Query("{ 'documentoUsuario': ?0, 'tipo': 'DEPORTE' }")
    List<Reserva> findReservasDeporteByUsuario(String documentoUsuario);
}