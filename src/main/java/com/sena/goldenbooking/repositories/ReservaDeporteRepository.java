package com.sena.goldenbooking.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.ReservaDeporte;

public interface ReservaDeporteRepository extends MongoRepository<ReservaDeporte, String> {

    // Todas las reservas deporte ligadas a una Reserva padre
    List<ReservaDeporte> findByIdReserva(String idReserva);

    // Reservas de un usuario específico — usado por el endpoint /mis-reservas
    List<ReservaDeporte> findByDocUsuario(String docUsuario);

    // Filtrar por tipo de cancha
    List<ReservaDeporte> findByTipoCancha(String tipoCancha);

    // Reservas que requieren entrenador
    List<ReservaDeporte> findByRequiereEntrenador(boolean requiereEntrenador);

// ── NUEVO: detecta solapamiento de horarios para una cancha ──
    // Busca reservas que se solapen con el rango (inicio, fin) pedido
    // Una reserva solapa si: su inicio < finNueva Y su fin > inicioNueva
    @Query("{ 'tipoCancha': ?0, " +
           "  'fechaReserva':    { $lt: ?2 }, " +
           "  'fechaFinReserva': { $gt: ?1 } }")
    List<ReservaDeporte> findSolapadas(
            String tipoCancha,
            LocalDateTime inicioNuevo,
            LocalDateTime finNuevo
    );
    
    // En ReservaDeporteRepository
    List<ReservaDeporte> findByEstadoNotAndRecordatorio24hEnviadoFalseAndFechaReservaBetween(
            EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta);
    List<ReservaDeporte> findByEstadoNotAndRecordatorio2hEnviadoFalseAndFechaReservaBetween(
            EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta);

    // Reservas CONFIRMADAS cuyo horario ya terminó — usadas por el job que las cierra como FINALIZADA
    List<ReservaDeporte> findByEstadoAndFechaFinReservaBefore(EstadoReserva estado, LocalDateTime fecha);
}