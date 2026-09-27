package com.sena.goldenbooking.habitaciones.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.habitaciones.model.EstadoHabitacion;
import com.sena.goldenbooking.habitaciones.model.Habitacion;

public interface HabitacionRepository extends MongoRepository<Habitacion, String> {

     /** Lista habitaciones según su estado (DISPONIBLE, OCUPADA, MANTENIMIENTO) */
    List<Habitacion> findByEstado(EstadoHabitacion estado);

    /*Para buscar por tipo en MongoDB, buscamos por el ID del objeto TipoHabitacion que está dentro de la habitación*/
    List<Habitacion> findByTipoHabitacion_Id(String idTipoHabitacion);

    /** Para no repetir el número de habitación (lista: la base podría tener repetidos antiguos). */
    List<Habitacion> findAllByNumHab(String numHab);
}
