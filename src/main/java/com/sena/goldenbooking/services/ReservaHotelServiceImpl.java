package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.dtos.ReservaHotelDto;
import com.sena.goldenbooking.mapper.ReservaHotelMapper;
import com.sena.goldenbooking.models.*;
import com.sena.goldenbooking.repositories.*;

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
            throw new RuntimeException("Datos obligatorios faltantes.");
        }

        // 2. Búsqueda y disponibilidad
        Habitacion habitacion = habitacionRepo.findById(dto.getIdHabitacion())
                .orElseThrow(() -> new RuntimeException("Habitación no encontrada."));

        if (!"disponible".equalsIgnoreCase(habitacion.getEstado())) {
            log.warn("Intento de reserva en habitación no disponible: ID {}", habitacion.getId());
            throw new RuntimeException("Habitación no disponible.");
        }

        // 3. Cálculos
        long noches = ChronoUnit.DAYS.between(dto.getFCheckIn().toLocalDate(), dto.getFCheckOut().toLocalDate());
        if (noches <= 0) throw new RuntimeException("Fechas inválidas.");
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
                    .build();

            ReservaHotel guardada = reservaHotelRepo.save(reservaHotel);

            habitacion.setEstado("ocupada");
            habitacionRepo.save(habitacion);

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
                    return new RuntimeException("No encontrada.");
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
                .orElseThrow(() -> new RuntimeException("No encontrada."));
        mapper.actualizarReservaHotel(dto, rh);
        return mapper.toDto(reservaHotelRepo.save(rh));
    }

    @Override
    public void cancelar(String id) {
        log.info("Iniciando cancelación de reserva hotel ID: {}", id);
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("No encontrada."));

        Reserva reserva = reservaRepo.findById(rh.getIdReserva())
                .orElseThrow(() -> new RuntimeException("Reserva padre no encontrada."));
        
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            log.warn("Intento de cancelar una reserva ya cancelada: {}", id);
            throw new RuntimeException("Ya está cancelada.");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepo.save(reserva);

        Habitacion habitacion = habitacionRepo.findById(rh.getIdHabitacion())
                .orElseThrow(() -> new RuntimeException("Habitación no encontrada."));
        habitacion.setEstado("disponible");
        habitacionRepo.save(habitacion);

        log.info("Cancelación exitosa. Reserva {} liberada.", id);
    }
}