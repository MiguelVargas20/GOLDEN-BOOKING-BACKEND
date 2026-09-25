package com.sena.goldenbooking.habitaciones.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.habitaciones.model.TipoHabitacion;

public interface TipoHabitacionRepository extends MongoRepository<TipoHabitacion, String>{

}
