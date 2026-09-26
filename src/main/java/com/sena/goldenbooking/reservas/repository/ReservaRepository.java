package com.sena.goldenbooking.reservas.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.Reserva;

public interface ReservaRepository extends MongoRepository<Reserva, String> {

    // Filtrar por estado: PENDIENTE, CONFIRMADA, CANCELADA
    List<Reserva> findByEstado(EstadoReserva estado);

    // ── Dashboard del administrador ──────────────────────────────────────

    /** Reservas creadas desde una fecha (tendencia de solicitudes por día). */
    List<Reserva> findByFechaReservaGreaterThanEqual(LocalDateTime desde);

    /** Reservas cuyo uso empieza en [desde, hasta) y están en alguno de los estados (ingresos del mes). */
    List<Reserva> findByFechaInicioGreaterThanEqualAndFechaInicioLessThanAndEstadoIn(
            LocalDateTime desde, LocalDateTime hasta, Collection<EstadoReserva> estados);
}
