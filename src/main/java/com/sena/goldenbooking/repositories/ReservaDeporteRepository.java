package com.sena.goldenbooking.repositories;

import com.sena.goldenbooking.models.ReservaDeporte;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReservaDeporteRepository extends MongoRepository<ReservaDeporte, String> {

    // Todas las reservas deporte ligadas a una Reserva padre
    List<ReservaDeporte> findByIdReserva(String idReserva);

    // Filtrar por tipo de cancha
    List<ReservaDeporte> findByTipoCancha(String tipoCancha);

    // Reservas que requieren entrenador
    List<ReservaDeporte> findByRequiereEntrenador(boolean requiereEntrenador);
}