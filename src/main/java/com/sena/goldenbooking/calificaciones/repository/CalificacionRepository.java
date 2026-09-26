package com.sena.goldenbooking.calificaciones.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.calificaciones.model.Calificacion;
import com.sena.goldenbooking.reservas.model.TipoReserva;

public interface CalificacionRepository extends MongoRepository<Calificacion, String> {

    boolean existsByIdReserva(String idReserva);

    List<Calificacion> findByCategoria(TipoReserva categoria);

    List<Calificacion> findByCategoriaAndIdRecursoOrderByFechaDesc(TipoReserva categoria, String idRecurso, Pageable pageable);

    List<Calificacion> findByDocUsuario(String docUsuario);
}
