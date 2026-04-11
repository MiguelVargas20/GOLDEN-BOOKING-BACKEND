package com.sena.goldenbooking.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.TipoHabitacion;

public interface TipoHabitacionRepository extends MongoRepository<TipoHabitacion, String>{

}
