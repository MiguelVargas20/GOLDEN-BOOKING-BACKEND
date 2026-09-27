package com.sena.goldenbooking.cargos.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.cargos.model.Cargo;
import com.sena.goldenbooking.cargos.model.EstadoCargo;

public interface CargoRepository extends MongoRepository<Cargo, String> {

    List<Cargo> findByDocUsuarioOrderByFechaDesc(String docUsuario);

    List<Cargo> findByDocUsuarioAndEstado(String docUsuario, EstadoCargo estado);

    List<Cargo> findByEstadoOrderByFechaDesc(EstadoCargo estado);
}
