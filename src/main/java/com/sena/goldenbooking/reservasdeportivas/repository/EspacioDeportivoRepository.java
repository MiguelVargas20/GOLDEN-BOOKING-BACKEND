package com.sena.goldenbooking.reservasdeportivas.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.reservasdeportivas.model.EspacioDeportivo;
import com.sena.goldenbooking.reservasdeportivas.model.EstadoEspacio;

public interface EspacioDeportivoRepository extends MongoRepository<EspacioDeportivo, String> {

    /** Espacios en alguno de los estados dados (el catálogo del cliente excluye INACTIVO). */
    List<EspacioDeportivo> findByEstadoIn(Collection<EstadoEspacio> estados, Sort sort);

    boolean existsByNombreIgnoreCase(String nombre);

    /** Para validar nombre único al editar (otro espacio con el mismo nombre). */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, String id);
}
