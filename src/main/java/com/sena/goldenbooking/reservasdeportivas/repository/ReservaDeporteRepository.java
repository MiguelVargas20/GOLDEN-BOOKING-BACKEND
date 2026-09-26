package com.sena.goldenbooking.reservasdeportivas.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;

public interface ReservaDeporteRepository extends MongoRepository<ReservaDeporte, String> {

    // Todas las reservas deporte ligadas a una Reserva padre
    List<ReservaDeporte> findByIdReserva(String idReserva);

    // Reservas de un usuario específico — usado por el endpoint /mis-reservas
    List<ReservaDeporte> findByDocUsuario(String docUsuario);

    
    // FIX hallazgo #12: antes eran findByEstadoNot...(CANCELADA, ...), o sea
    // "cualquier estado que no sea CANCELADA" — eso incluía PENDIENTE, así que
    // una reserva que nunca fue confirmada por el admin igual disparaba el
    // correo "tu reserva es en 24 horas". Ahora filtra explícitamente por
    // CONFIRMADA.
    List<ReservaDeporte> findByEstadoAndRecordatorio24hEnviadoFalseAndFechaReservaBetween(
            EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta);
    List<ReservaDeporte> findByEstadoAndRecordatorio2hEnviadoFalseAndFechaReservaBetween(
            EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta);

    // Reservas CONFIRMADAS cuyo horario ya terminó — usadas por el job que las cierra como FINALIZADA
    List<ReservaDeporte> findByEstadoAndFechaFinReservaBefore(EstadoReserva estado, LocalDateTime fecha);

    // PENDIENTES cuyo horario ya empezó sin que nadie las aprobara — el job las vence
    List<ReservaDeporte> findByEstadoAndFechaReservaBefore(EstadoReserva estado, LocalDateTime fecha);

    /**
     * Reservas NO canceladas del espacio que se cruzan con [inicio, fin).
     * Antes se comparaba por el nombre de la cancha como texto libre; ahora
     * cada reserva apunta a un espacio real por su id.
     */
    @Query("{ 'espacioId': ?0, " +
           "  'estado':          { $ne: 'CANCELADA' }, " +
           "  'fechaReserva':    { $lt: ?2 }, " +
           "  'fechaFinReserva': { $gt: ?1 } }")
    List<ReservaDeporte> findSolapadasEnEspacio(String espacioId, LocalDateTime inicioNuevo, LocalDateTime finNuevo);

    /** Listado del admin filtrado por estado. */
    Page<ReservaDeporte> findByEstado(EstadoReserva estado, Pageable pageable);

    /** Para el resumen del panel (cuántas pendientes, confirmadas...). */
    long countByEstado(EstadoReserva estado);

    /** Horarios ocupados que aún no terminan (para el calendario del cliente). */
    List<ReservaDeporte> findByEstadoNotAndFechaFinReservaAfter(EstadoReserva estado, LocalDateTime fecha);

    /** ¿El espacio tiene reservas vigentes (no canceladas y que aún no terminan)? Se usa antes de eliminarlo. */
    boolean existsByEspacioIdAndEstadoNotAndFechaFinReservaAfter(String espacioId, EstadoReserva estado, LocalDateTime fecha);

    /** ¿El espacio tiene alguna reserva en su historial? */
    boolean existsByEspacioId(String espacioId);

    // ── Dashboard del administrador ──────────────────────────────────────

    // Rango sobre un mismo campo: va con @Query porque Spring Data MongoDB no
    // admite dos condiciones del mismo campo en un nombre de método derivado
    // ("findByXGreaterThanEqualAndXLessThan" lanza InvalidMongoDbApiUsageException).
    /** Reservas no canceladas que empiezan en [desde, hasta) (agenda del día / espacios más reservados). */
    @Query("{ 'fechaReserva': { $gte: ?0, $lt: ?1 }, 'estado': { $ne: ?2 } }")
    List<ReservaDeporte> findByFechaReservaGreaterThanEqualAndFechaReservaLessThanAndEstadoNot(
            LocalDateTime desde, LocalDateTime hasta, EstadoReserva estado);

    /** Todas las reservas (cualquier estado) que empiezan en [desde, hasta): reportes. */
    @Query("{ 'fechaReserva': { $gte: ?0, $lt: ?1 } }")
    List<ReservaDeporte> findByFechaReservaGreaterThanEqualAndFechaReservaLessThan(LocalDateTime desde, LocalDateTime hasta);

    /** Pendientes de aprobación, la más próxima primero. */
    List<ReservaDeporte> findByEstadoOrderByFechaReservaAsc(EstadoReserva estado, Pageable pageable);

    // ── Socios y cargos ──────────────────────────────────────────────────

    /** Reservas en alguno de los estados (conteo de reservas por cliente en el panel de socios). */
    List<ReservaDeporte> findByEstadoIn(Collection<EstadoReserva> estados);

    long countByDocUsuarioAndEstadoIn(String docUsuario, Collection<EstadoReserva> estados);

    /** Reservas del cliente en un estado (reservas activas a las que se cargan consumos). */
    List<ReservaDeporte> findByDocUsuarioAndEstado(String docUsuario, EstadoReserva estado);
}
