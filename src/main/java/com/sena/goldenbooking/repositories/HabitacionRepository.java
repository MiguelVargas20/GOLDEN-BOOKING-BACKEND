package com.sena.goldenbooking.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.Habitacion;
import com.sena.goldenbooking.models.EstadoHabitacion;

public interface HabitacionRepository extends MongoRepository<Habitacion, String> {

     /** Lista habitaciones según su estado (DISPONIBLE, OCUPADA, MANTENIMIENTO) */
    List<Habitacion> findByEstado(EstadoHabitacion estado);

    /*Para buscar por tipo en MongoDB, buscamos por el ID del objeto TipoHabitacion que está dentro de la habitación*/
    List<Habitacion> findByTipoHabitacion_Id(String idTipoHabitacion);
}