package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.mapper.ReservaDeporteMapper;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.ReservaDeporte;
import com.sena.goldenbooking.models.TipoReserva;
import com.sena.goldenbooking.repositories.ReservaDeporteRepository;
import com.sena.goldenbooking.repositories.ReservaRepository;

// Implementación de la interfaz ReservaDeporteService que maneja la lógica de negocio relacionada con las reservas de deporte
@Service
public class ReservaDeporteServiceImpl implements ReservaDeporteService {

    // Repositorios para acceder a los datos de reservas de deporte y reservas generales, y mapper para convertir entre entidades y DTOs
    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaRepository reservaRepo;
    private final ReservaDeporteMapper mapper;


    // Constructor que inyecta las dependencias necesarias para el servicio de reservas de deporte
    public ReservaDeporteServiceImpl(
            ReservaDeporteRepository reservaDeporteRepo,
            ReservaRepository reservaRepo,
            ReservaDeporteMapper mapper) {
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaRepo = reservaRepo;
        this.mapper = mapper;
    }

    // Método para crear una nueva reserva de deporte a partir de un DTO, validando los datos, calculando el precio total y estableciendo la fecha y estado inicial
    @Override
    public ReservaDeporteDto crear(ReservaDeporteDto dto) {
        
        // Validaciones básicas
        if (dto.getDocUsuario() == null || dto.getDocUsuario().isBlank())
            throw new RuntimeException("El documento del usuario es obligatorio.");

        // Validación adicional para el tipo de reserva
        if (dto.getTCancha() == null || dto.getTCancha().isBlank())
            throw new RuntimeException("El tipo de cancha es obligatorio.");

        // Validación de fechas
        if (dto.getFInicioReserva() == null || dto.getFFinReserva() == null)
            throw new RuntimeException("Las fechas de inicio y fin son obligatorias.");

        // Calcular horas y precio (tarifa fija por hora, ajusta según tu lógica)
        long horas = ChronoUnit.HOURS.between(dto.getFInicioReserva(), dto.getFFinReserva());
        if (horas <= 0)
            throw new RuntimeException("La fecha de fin debe ser posterior al inicio.");

        double tarifaHora = 50000.0; // ajusta o recibe desde configuración
        double precioTotal = horas * tarifaHora;

        // Crear Reserva padre
        Reserva reserva = Reserva.builder()
                .documentoUsuario(dto.getDocUsuario())
                .tipo(TipoReserva.DEPORTE)
                .estado(EstadoReserva.PENDIENTE)
                .fechaReserva(LocalDateTime.now())
                .fechaInicio(dto.getFInicioReserva())
                .fechaFin(dto.getFFinReserva())
                .precioTotal(precioTotal)
                .build();
        Reserva reservaGuardada = reservaRepo.save(reserva);

        // Crear ReservaDeporte
        ReservaDeporte reservaDeporte = ReservaDeporte.builder()
                .idReserva(reservaGuardada.getId())
                .docUsuario(dto.getDocUsuario())      // ← agregar
                .tipoCancha(dto.getTCancha())
                .implementosAlquilados(dto.getImplAlquilados())
                .requiereEntrenador(dto.isRqrEntrenador())
                .fechaReserva(dto.getFInicioReserva())
                .fechaFinReserva(dto.getFFinReserva())
                .precio(precioTotal)
                .build();

        return mapper.toDto(reservaDeporteRepo.save(reservaDeporte));
    }

    //  Método para listar todas las reservas de deporte, convirtiendo las entidades a DTOs antes de devolver la lista
    @Override
    public List<ReservaDeporteDto> listarTodas() {
        return mapper.toDtoList(reservaDeporteRepo.findAll());
    }

    // Método para obtener una reserva de deporte por su ID, lanzando una excepción si no se encuentra la reserva
    @Override
    public ReservaDeporteDto obtenerPorId(String id) {
        ReservaDeporte rd = reservaDeporteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva deporte no encontrada: " + id));
        return mapper.toDto(rd);
    }

    // Método para obtener reservas de deporte por el ID de reserva padre, devolviendo una lista de DTOs correspondientes a las reservas encontradas
    @Override
    public List<ReservaDeporteDto> obtenerPorReserva(String idReserva) {
        return mapper.toDtoList(reservaDeporteRepo.findByIdReserva(idReserva));
    }

    // Método para actualizar una reserva de deporte existente, buscando la reserva por ID, actualizando sus campos con los datos del DTO y guardando los cambios
    @Override
    public ReservaDeporteDto actualizar(String id, ReservaDeporteDto dto) {
        ReservaDeporte rd = reservaDeporteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva deporte no encontrada: " + id));
        mapper.actualizarReservaDeporte(dto, rd);
        return mapper.toDto(reservaDeporteRepo.save(rd));
    }

    // Método para cancelar una reserva de deporte, buscando la reserva por ID, verificando su estado actual y actualizando el estado a CANCELADA si no está ya cancelada, además de cancelar la reserva padre
    @Override
    public void cancelar(String id) {
        ReservaDeporte rd = reservaDeporteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva deporte no encontrada: " + id));

        // Verificar si la reserva de deporte ya está cancelada
        Reserva reserva = reservaRepo.findById(rd.getIdReserva())
                .orElseThrow(() -> new RuntimeException("Reserva padre no encontrada."));
        if (reserva.getEstado() == EstadoReserva.CANCELADA)
            throw new RuntimeException("La reserva ya está cancelada.");

        // Actualizar estado de la reserva de deporte a CANCELADA
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepo.save(reserva);
    }
}