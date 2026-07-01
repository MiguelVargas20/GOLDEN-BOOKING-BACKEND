package com.sena.goldenbooking.repositories;

import com.sena.goldenbooking.models.ReservaDeporte;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

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
}