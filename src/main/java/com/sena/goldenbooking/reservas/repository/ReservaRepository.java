package com.sena.goldenbooking.reservas.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.Reserva;
import com.sena.goldenbooking.reservas.model.TipoReserva;

public interface ReservaRepository extends MongoRepository<Reserva, String> {

    // Todas las reservas de un usuario por su documento
    List<Reserva> findByDocumentoUsuario(String documentoUsuario);

    // Filtrar por tipo: HOTEL o DEPORTE
    List<Reserva> findByTipo(TipoReserva tipo);

    // Filtrar por estado: PENDIENTE, CONFIRMADA, CANCELADA
    List<Reserva> findByEstado(EstadoReserva estado);

    // Reservas hoteleras de un usuario específico
    @Query("{ 'documentoUsuario': ?0, 'tipo': 'HOTEL' }")
    List<Reserva> findReservasHotelByUsuario(String documentoUsuario);

    // Reservas deportivas de un usuario específico
    @Query("{ 'documentoUsuario': ?0, 'tipo': 'DEPORTE' }")
    List<Reserva> findReservasDeporteByUsuario(String documentoUsuario);

    // Filtrar por usuario y tipo
    List<Reserva> findByDocumentoUsuarioAndTipo(String documentoUsuario, TipoReserva tipo);

    // ── Dashboard del administrador ──────────────────────────────────────

    /** Reservas creadas desde una fecha (tendencia de solicitudes por día). */
    List<Reserva> findByFechaReservaGreaterThanEqual(LocalDateTime desde);

    /** Reservas cuyo uso empieza en [desde, hasta) y están en alguno de los estados (ingresos del mes). */
    List<Reserva> findByFechaInicioGreaterThanEqualAndFechaInicioLessThanAndEstadoIn(
            LocalDateTime desde, LocalDateTime hasta, Collection<EstadoReserva> estados);
}
