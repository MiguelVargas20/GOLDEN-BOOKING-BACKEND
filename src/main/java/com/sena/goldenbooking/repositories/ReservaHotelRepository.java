package com.sena.goldenbooking.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.ReservaHotel;

public interface ReservaHotelRepository extends MongoRepository<ReservaHotel, String> {

    // Todas las reservas hotel ligadas a una Reserva padre
    List<ReservaHotel> findByIdReserva(String idReserva);

    // Reservas hotel por habitación específica
    List<ReservaHotel> findByDatosH_Id(String idHabitacion);

    // Reservas hotel por documento de usuario
    List<ReservaHotel> findByDocUsuario(String docUsuario);

    // Todas las reservas ACTIVAS (no canceladas) de una habitación puntual.
    // Es la base para validar disponibilidad por rango de fechas: si una
    // reserva nueva se solapa con alguna de estas, la habitación NO está
    // libre para esas fechas (aunque sí lo esté para otras).
    List<ReservaHotel> findByIdHabitacionAndEstadoNot(String idHabitacion, EstadoReserva estado);

        // FIX hallazgo #12: antes eran findByEstadoNot...(CANCELADA, ...), es decir,
        // "cualquier estado que no sea CANCELADA" — eso incluía PENDIENTE, así que una
        // reserva que un admin nunca confirmó igual disparaba el correo "tu reserva es
        // en 24 horas", lo cual confunde al cliente si al final no fue confirmada.
        // Ahora se filtra explícitamente por CONFIRMADA.
    List<ReservaHotel> findByEstadoAndRecordatorio24hEnviadoFalseAndFechaCheckInBetween(
            EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta);
    List<ReservaHotel> findByEstadoAndRecordatorio2hEnviadoFalseAndFechaCheckInBetween(
            EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta);

    // Reservas CONFIRMADAS cuyo check-out ya pasó — usadas por el job que las cierra como FINALIZADA
    List<ReservaHotel> findByEstadoAndFechaCheckOutBefore(EstadoReserva estado, LocalDateTime fecha);
}