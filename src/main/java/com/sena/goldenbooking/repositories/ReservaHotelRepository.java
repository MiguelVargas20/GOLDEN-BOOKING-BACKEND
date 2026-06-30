package com.sena.goldenbooking.repositories;

import com.sena.goldenbooking.models.ReservaHotel;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReservaHotelRepository extends MongoRepository<ReservaHotel, String> {

    // Todas las reservas hotel ligadas a una Reserva padre
    List<ReservaHotel> findByIdReserva(String idReserva);

    // Reservas hotel por habitación específica
    List<ReservaHotel> findByDatosH_Id(String idHabitacion);

    // Reservas hotel por documento de usuario
    List<ReservaHotel> findByDocUsuario(String docUsuario);

}