package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.mapper.ReservaMapper;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.repositories.ReservaRepository;

// Implementación de la interfaz ReservaService que maneja la lógica de negocio relacionada con las reservas
@Service
public class ReservaServiceImpl implements ReservaService {

    // Repositorio para acceder a los datos de las reservas y mapper para convertir entre entidades y DTOs
    private final ReservaRepository reservaRepo;
    private final ReservaMapper reservaMapper;


    // Constructor que inyecta las dependencias necesarias para el servicio
    public ReservaServiceImpl(ReservaRepository reservaRepo, ReservaMapper reservaMapper) {
        this.reservaRepo = reservaRepo;
        this.reservaMapper = reservaMapper;
    }


    // Método para crear una nueva reserva a partir de un DTO, validando los datos y estableciendo la fecha y estado inicial
    @Override
    public ReservaDto crearReserva(ReservaDto dto) {
        // Validación de campos obligatorios en el DTO
        if (dto.getDocUsuario() == null || dto.getDocUsuario().isBlank()) {
            throw new RuntimeException("El documento del usuario es obligatorio.");
        }
        // Validación adicional para el tipo de reserva
        if (dto.getTp() == null) {
            throw new RuntimeException("El tipo de reserva es obligatorio.");
        }


        // Conversión del DTO a la entidad Reserva, estableciendo la fecha de reserva y el estado inicial como PENDIENTE
        Reserva reserva = reservaMapper.toReserva(dto);
        reserva.setFechaReserva(LocalDateTime.now());
        reserva.setEstado(EstadoReserva.PENDIENTE);

        return reservaMapper.toDto(reservaRepo.save(reserva));
    }


    // Método para listar todas las reservas, convirtiendo las entidades a DTOs antes de devolver la lista
    @Override
    public List<ReservaDto> listarReservas() {
        return reservaMapper.toDtoList(reservaRepo.findAll());
    }


    // Método para obtener una reserva por su ID, lanzando una excepción si no se encuentra la reserva
    @Override
    public ReservaDto obtenerPorId(String id) {
        Reserva reserva = reservaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));
        return reservaMapper.toDto(reserva);
    }


    // Método para obtener reservas por el documento del usuario, devolviendo una lista de DTOs correspondientes a las reservas encontradas
    @Override
    public List<ReservaDto> obtenerPorUsuario(String documentoUsuario) {
        return reservaMapper.toDtoList(
            reservaRepo.findByDocumentoUsuario(documentoUsuario));
    }

    // Método para actualizar una reserva existente, buscando la reserva por ID, actualizando sus campos con los datos del DTO y guardando los cambios
    @Override
    public ReservaDto actualizarReserva(String id, ReservaDto dto) {
        Reserva reservaExistente = reservaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));
        reservaMapper.actualizarReserva(dto, reservaExistente);
        return reservaMapper.toDto(reservaRepo.save(reservaExistente));
    }


    // Método para cancelar una reserva, buscando la reserva por ID, verificando su estado actual y actualizando el estado a CANCELADA si no está ya cancelada
    @Override
    public void cancelarReserva(String id) {
        Reserva reserva = reservaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new RuntimeException("La reserva ya está cancelada.");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepo.save(reserva);
    }
}