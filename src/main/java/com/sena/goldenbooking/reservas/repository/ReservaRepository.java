package com.sena.goldenbooking.reservas.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.Reserva;

public interface ReservaRepository extends MongoRepository<Reserva, String> {

    // Filtrar por estado: PENDIENTE, CONFIRMADA, CANCELADA
    List<Reserva> findByEstado(EstadoReserva estado);

    // ── Dashboard del administrador ──────────────────────────────────────

    /** Reservas creadas desde una fecha (tendencia de solicitudes por día). */
    List<Reserva> findByFechaReservaGreaterThanEqual(LocalDateTime desde);

    // Rango sobre un mismo campo: va con @Query porque Spring Data MongoDB no
    // admite dos condiciones del mismo campo en un nombre de método derivado
    // ("findByXGreaterThanEqualAndXLessThan" lanza InvalidMongoDbApiUsageException).
    /** Reservas cuyo uso empieza en [desde, hasta) y están en alguno de los estados (ingresos del mes). */
    @Query("{ 'fechaInicio': { $gte: ?0, $lt: ?1 }, 'estado': { $in: ?2 } }")
    List<Reserva> findByFechaInicioGreaterThanEqualAndFechaInicioLessThanAndEstadoIn(
            LocalDateTime desde, LocalDateTime hasta, Collection<EstadoReserva> estados);
}
