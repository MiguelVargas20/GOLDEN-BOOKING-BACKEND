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

    // Repositorios para acceder a los datos de reservas de hotel, reservas generales y habitaciones, y mapper para convertir entre entidades y DTOs
    private final ReservaHotelRepository reservaHotelRepo;
    private final ReservaRepository reservaRepo;

    // Repositorio para acceder a los datos de habitaciones, necesario para validar la existencia de la habitación y calcular el precio total de la reserva
    private final HabitacionRepository habitacionRepo;
    private final ReservaHotelMapper mapper;

    
    // Constructor que inyecta las dependencias necesarias para el servicio de reservas de hotel
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

    // Método para crear una nueva reserva de hotel a partir de un DTO, validando los datos, calculando el precio total y estableciendo la fecha y estado inicial
    @Override

    // Método para crear una nueva reserva de hotel a partir de un DTO, validando los datos, calculando el precio total y estableciendo la fecha y estado inicial
    public ReservaHotelDto crear(ReservaHotelDto dto) {

        // Validaciones básicas
        if (dto.getDocUsuario() == null || dto.getDocUsuario().isBlank())
            throw new RuntimeException("El documento del usuario es obligatorio.");

        // Validación adicional para el tipo de reserva
        if (dto.getIdHabitacion() == null || dto.getIdHabitacion().isBlank())
            throw new RuntimeException("El ID de la habitación es obligatorio.");

        // Validación de fechas
        if (dto.getFCheckIn() == null || dto.getFCheckOut() == null)
            throw new RuntimeException("Las fechas de check-in y check-out son obligatorias.");

        // Buscar habitación
        Habitacion habitacion = habitacionRepo.findById(dto.getIdHabitacion())
                .orElseThrow(() -> new RuntimeException("Habitación no encontrada: " + dto.getIdHabitacion()));

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
                .docUsuario(dto.getDocUsuario())      // ← esta línea faltaba
                .datosH(habitacion)
                .fechaCheckIn(dto.getFCheckIn())
                .fechaCheckOut(dto.getFCheckOut())
                .noches((int) noches)
                .precioTotal(precioTotal)
                .build();
                
        return mapper.toDto(reservaHotelRepo.save(reservaHotel));
    }


    // Método para listar todas las reservas de hotel, convirtiendo las entidades a DTOs antes de devolver la lista
    @Override
    public List<ReservaHotelDto> listarTodas() {
        return mapper.toDtoList(reservaHotelRepo.findAll());
    }

    // Método para obtener una reserva de hotel por su ID, lanzando una excepción si no se encuentra la reserva
    @Override
    public ReservaHotelDto obtenerPorId(String id) {
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva hotel no encontrada: " + id));
        return mapper.toDto(rh);
    }


    // Método para obtener reservas de hotel por el ID de reserva padre, devolviendo una lista de DTOs correspondientes a las reservas encontradas
    @Override
    public List<ReservaHotelDto> obtenerPorReserva(String idReserva) {
        return mapper.toDtoList(reservaHotelRepo.findByIdReserva(idReserva));
    }

    // Método para actualizar una reserva de hotel existente, buscando la reserva por ID, actualizando sus campos con los datos del DTO y guardando los cambios
    @Override
    public ReservaHotelDto actualizar(String id, ReservaHotelDto dto) {
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva hotel no encontrada: " + id));
        mapper.actualizarReservaHotel(dto, rh);
        return mapper.toDto(reservaHotelRepo.save(rh));
    }


    // Método para cancelar una reserva de hotel, buscando la reserva por ID, verificando su estado actual y actualizando el estado a CANCELADA si no está ya cancelada, además de cancelar la reserva padre
    @Override
    public void cancelar(String id) {
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva hotel no encontrada: " + id));

        // Cancelar también la Reserva padre
        Reserva reserva = reservaRepo.findById(rh.getIdReserva())
                .orElseThrow(() -> new RuntimeException("Reserva padre no encontrada."));
        if (reserva.getEstado() == EstadoReserva.CANCELADA)
            throw new RuntimeException("La reserva ya está cancelada.");

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepo.save(reserva);
    }
}