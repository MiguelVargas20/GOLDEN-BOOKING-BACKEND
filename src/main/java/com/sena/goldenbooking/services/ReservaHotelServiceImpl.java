package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.dtos.ReservaHotelDto;
import com.sena.goldenbooking.dtos.RangoOcupadoDto;
import com.sena.goldenbooking.mapper.ReservaHotelMapper;
import com.sena.goldenbooking.models.*;
import com.sena.goldenbooking.repositories.*;
import com.sena.goldenbooking.exception.ReservaNoEncontradaException;
import com.sena.goldenbooking.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.exception.AccesoDenegadoException;

@Slf4j
@Service
public class ReservaHotelServiceImpl implements ReservaHotelService {

    private final ReservaHotelRepository reservaHotelRepo;
    private final ReservaRepository reservaRepo;
    private final HabitacionRepository habitacionRepo;
    private final ReservaHotelMapper mapper;

    public ReservaHotelServiceImpl(
            ReservaHotelRepository reservaHotelRepo,
            ReservaRepository reservaRepo,
            HabitacionRepository habitacionRepo,
            ReservaHotelMapper mapper) {
        this.reservaHotelRepo = reservaHotelRepo;
        this.reservaRepo = reservaRepo;
        this.habitacionRepo = habitacionRepo;
        this.mapper = mapper;
    }

    @Override
    public ReservaHotelDto crear(ReservaHotelDto dto) {
        log.info("Iniciando creación de reserva hotel para usuario: {}", dto.getDocUsuario());

        // 1. Validaciones
        if (dto.getDocUsuario() == null || dto.getIdHabitacion() == null) {
            log.warn("Intento de creación fallido: Datos incompletos.");
            throw new IllegalArgumentException("Datos obligatorios faltantes.");
        }

        // 2. Búsqueda de la habitación
        Habitacion habitacion = habitacionRepo.findById(dto.getIdHabitacion())
                .orElseThrow(() -> new ReservaNoEncontradaException("Habitación no encontrada."));

        // 2.1 "mantenimiento" sigue siendo un bloqueo total decidido por el ADMIN,
        //      independiente de fechas (ej: la habitación está dañada).
        if ("mantenimiento".equalsIgnoreCase(habitacion.getEstado())) {
            log.warn("Intento de reserva en habitación en mantenimiento: ID {}", habitacion.getId());
            throw new ConflictoDeNegocioException("Esta habitación está en mantenimiento.");
        }

        // 2.2 Validación de fechas antes de comparar solapamientos
        long noches = ChronoUnit.DAYS.between(dto.getFCheckIn().toLocalDate(), dto.getFCheckOut().toLocalDate());
        if (noches <= 0) throw new IllegalArgumentException("Fechas inválidas.");

        // 2.3 Disponibilidad REAL: ya no depende de un campo global "ocupada",
        //     sino de si el rango pedido se cruza con alguna reserva activa
        //     (no cancelada) de ESTA habitación puntual.
        List<ReservaHotel> reservasActivas = reservaHotelRepo
                .findByIdHabitacionAndEstadoNot(habitacion.getId(), EstadoReserva.CANCELADA);

        boolean haySolapamiento = reservasActivas.stream()
                .anyMatch(r -> seSolapan(r.getFechaCheckIn(), r.getFechaCheckOut(),
                                          dto.getFCheckIn(), dto.getFCheckOut()));

        if (haySolapamiento) {
            log.warn("Intento de reserva solapada en habitación {} para fechas {} - {}",
                    habitacion.getId(), dto.getFCheckIn(), dto.getFCheckOut());
            throw new ConflictoDeNegocioException(
                    "Esta habitación ya está reservada para esas fechas. Elige otro rango u otra habitación.");
        }

        // 3. Cálculos
        double precioTotal = noches * habitacion.getPrecNoche();

        // 4. Persistencia
        try {
            Reserva reserva = Reserva.builder()
                    .documentoUsuario(dto.getDocUsuario())
                    .tipo(TipoReserva.HOTEL)
                    .estado(EstadoReserva.PENDIENTE)
                    .fechaReserva(LocalDateTime.now())
                    .fechaInicio(dto.getFCheckIn())
                    .fechaFin(dto.getFCheckOut())
                    .precioTotal(precioTotal)
                    .build();
            Reserva reservaGuardada = reservaRepo.save(reserva);

            ReservaHotel reservaHotel = ReservaHotel.builder()
                    .idReserva(reservaGuardada.getId())
                    .idHabitacion(habitacion.getId())
                    .docUsuario(dto.getDocUsuario())
                    .datosH(habitacion)
                    .fechaCheckIn(dto.getFCheckIn())
                    .fechaCheckOut(dto.getFCheckOut())
                    .noches((int) noches)
                    .precioTotal(precioTotal)
                    .estado(EstadoReserva.PENDIENTE)
                    .build();

            ReservaHotel guardada = reservaHotelRepo.save(reservaHotel);

            // Ya NO tocamos habitacion.estado aquí: la disponibilidad ahora se calcula
            // dinámicamente por fecha (ver findByIdHabitacionAndEstadoNot arriba),
            // así la misma habitación puede tener reservas distintas en fechas distintas.

            log.info("Reserva hotel creada con éxito. ID: {}, Usuario: {}", guardada.getIdHotelReserva(), dto.getDocUsuario());
            return mapper.toDto(guardada);

        } catch (Exception e) { 
            log.error("Error crítico al persistir reserva hotel para usuario {}: {}", dto.getDocUsuario(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<ReservaHotelDto> listarTodas() {
        return mapper.toDtoList(reservaHotelRepo.findAll());
    }

    @Override
    public ReservaHotelDto obtenerPorId(String id) {
        return reservaHotelRepo.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> {
                    log.warn("Consulta fallida: Reserva hotel {} no encontrada.", id);
                    return new ReservaNoEncontradaException("No encontrada.");
                });
    }

    @Override
    public List<ReservaHotelDto> obtenerPorReserva(String idReserva) {
        return mapper.toDtoList(reservaHotelRepo.findByIdReserva(idReserva));
    }

    @Override
    public ReservaHotelDto actualizar(String id, ReservaHotelDto dto) {
        log.info("Actualizando reserva hotel ID: {}", id);
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException("No encontrada."));
        mapper.actualizarReservaHotel(dto, rh);
        return mapper.toDto(reservaHotelRepo.save(rh));
    }

    @Override
    public void cancelar(String id, String docUsuarioSolicitante, boolean esAdmin) {
        log.info("Iniciando cancelación de reserva hotel ID: {}", id);
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException("No encontrada."));

        // ── FIX IDOR: solo el dueño de la reserva o un ADMIN pueden cancelarla ──
        if (!esAdmin && !rh.getDocUsuario().equals(docUsuarioSolicitante)) {
            log.warn("Intento de cancelación no autorizado. Usuario {} intentó cancelar la reserva {} del usuario {}.",
                    docUsuarioSolicitante, id, rh.getDocUsuario());
            throw new AccesoDenegadoException("No tienes permiso para cancelar esta reserva.");
        }

        Reserva reserva = reservaRepo.findById(rh.getIdReserva())
                .orElseThrow(() -> new ReservaNoEncontradaException("Reserva padre no encontrada."));
        
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            log.warn("Intento de cancelar una reserva ya cancelada: {}", id);
            throw new ConflictoDeNegocioException("Ya está cancelada.");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepo.save(reserva);

        // ── FIX: sincronizar el estado también en ReservaHotel,
        // que es la colección que realmente se lee en las vistas de reservas ──
        rh.setEstado(EstadoReserva.CANCELADA);
        reservaHotelRepo.save(rh);

        // Al cancelar, la reserva pasa a CANCELADA y por eso deja de contar en
        // findByIdHabitacionAndEstadoNot(...): esas fechas quedan libres
        // automáticamente, sin necesidad de tocar Habitacion.estado.

        log.info("Cancelación exitosa. Reserva {} liberada.", id);
    }

    // Método adicional para obtener reservas por documento de usuario
    @Override
        public List<ReservaHotelDto> obtenerPorUsuario(String docUsuario) {
            log.info("Listando reservas hotel para usuario: {}", docUsuario);
            return mapper.toDtoList(reservaHotelRepo.findByDocUsuario(docUsuario));
        }

    @Override
    public List<RangoOcupadoDto> obtenerFechasOcupadas(String idHabitacion) {
        return reservaHotelRepo.findByIdHabitacionAndEstadoNot(idHabitacion, EstadoReserva.CANCELADA)
                .stream()
                .map(r -> RangoOcupadoDto.builder()
                        .checkIn(r.getFechaCheckIn())
                        .checkOut(r.getFechaCheckOut())
                        .build())
                .toList();
    }

    // Dos rangos de fechas [inicioA, finA) y [inicioB, finB) se solapan si
    // uno empieza ANTES de que el otro termine, en ambos sentidos.
    // Ejemplo: reserva existente 10-15 julio, nueva reserva 14-18 julio →
    // 10 < 18 (true) Y 15 > 14 (true) → SE SOLAPAN.
    // Nueva reserva 15-20 julio (empieza justo cuando la otra termina) →
    // 10 < 20 (true) Y 15 > 15 (false) → NO se solapan (check-out y check-in
    // el mismo día se permite, como en cualquier hotel real).
    private boolean seSolapan(LocalDateTime inicioA, LocalDateTime finA,
                               LocalDateTime inicioB, LocalDateTime finB) {
        return inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }
}