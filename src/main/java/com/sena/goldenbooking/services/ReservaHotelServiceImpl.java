package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.dtos.ReservaHotelDto;
import com.sena.goldenbooking.mapper.ReservaHotelMapper;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.Habitacion;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.ReservaHotel;
import com.sena.goldenbooking.models.TipoReserva;
import com.sena.goldenbooking.repositories.HabitacionRepository;
import com.sena.goldenbooking.repositories.ReservaHotelRepository;
import com.sena.goldenbooking.repositories.ReservaRepository;

@Service
public class ReservaHotelServiceImpl implements ReservaHotelService {

    // Repositorios y mapper necesarios para la lógica de negocio
    private final ReservaHotelRepository reservaHotelRepo;
    private final ReservaRepository reservaRepo;
    private final HabitacionRepository habitacionRepo;
    private final ReservaHotelMapper mapper;

    // Inyección de dependencias a través del constructor
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

    // Implementación de métodos del servicio
    @Override
    public ReservaHotelDto crear(ReservaHotelDto dto) {

        // Validaciones básicas
        if (dto.getDocUsuario() == null || dto.getDocUsuario().isBlank())
            throw new RuntimeException("El documento del usuario es obligatorio.");
        if (dto.getIdHabitacion() == null || dto.getIdHabitacion().isBlank())
            throw new RuntimeException("El ID de la habitación es obligatorio.");
        if (dto.getFCheckIn() == null || dto.getFCheckOut() == null)
            throw new RuntimeException("Las fechas de check-in y check-out son obligatorias.");

        // Buscar habitación
        Habitacion habitacion = habitacionRepo.findById(dto.getIdHabitacion())
                .orElseThrow(() -> new RuntimeException("Habitación no encontrada: " + dto.getIdHabitacion()));

        // ← VALIDAR DISPONIBILIDAD
        if (!"disponible".equalsIgnoreCase(habitacion.getEstado()))
            throw new RuntimeException("La habitación no está disponible para reservar.");

        // Calcular noches y precio total
        long noches = ChronoUnit.DAYS.between(
                dto.getFCheckIn().toLocalDate(),
                dto.getFCheckOut().toLocalDate());
        if (noches <= 0)
            throw new RuntimeException("La fecha de check-out debe ser posterior al check-in.");

        double precioTotal = noches * habitacion.getPrecNoche();

        // Crear Reserva padre
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

        // Crear ReservaHotel con todos los datos
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

        // ← CAMBIAR ESTADO HABITACIÓN A OCUPADA
        habitacion.setEstado("ocupada");
        habitacionRepo.save(habitacion);

        return mapper.toDto(guardada);
    }
    

    // Implementación de métodos para listar, obtener por ID, actualizar y cancelar reservas hotel
    @Override
    public List<ReservaHotelDto> listarTodas() {
        return mapper.toDtoList(reservaHotelRepo.findAll());
    }


    // Obtener reserva hotel por ID, lanzando excepción si no se encuentra
    @Override
    public ReservaHotelDto obtenerPorId(String id) {
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva hotel no encontrada: " + id));
        return mapper.toDto(rh);
    }

    // Obtener todas las reservas hotel asociadas a una reserva padre específica
    @Override
    public List<ReservaHotelDto> obtenerPorReserva(String idReserva) {
        return mapper.toDtoList(reservaHotelRepo.findByIdReserva(idReserva));
    }

    // Actualizar reserva hotel por ID, lanzando excepción si no se encuentra
    @Override
    public ReservaHotelDto actualizar(String id, ReservaHotelDto dto) {
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva hotel no encontrada: " + id));
        mapper.actualizarReservaHotel(dto, rh);
        return mapper.toDto(reservaHotelRepo.save(rh));
    }

    // Cancelar reserva hotel por ID, lanzando excepción si no se encuentra
    @Override
    public void cancelar(String id) {
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva hotel no encontrada: " + id));

        // Cancelar Reserva padre
        Reserva reserva = reservaRepo.findById(rh.getIdReserva())
                .orElseThrow(() -> new RuntimeException("Reserva padre no encontrada."));
        if (reserva.getEstado() == EstadoReserva.CANCELADA)
            throw new RuntimeException("La reserva ya está cancelada.");

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepo.save(reserva);

        // ← LIBERAR HABITACIÓN al cancelar
        Habitacion habitacion = habitacionRepo.findById(rh.getIdHabitacion())
                .orElseThrow(() -> new RuntimeException("Habitación no encontrada al cancelar."));
        habitacion.setEstado("disponible");
        habitacionRepo.save(habitacion);
    }
}