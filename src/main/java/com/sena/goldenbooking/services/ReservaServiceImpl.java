package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.exception.AccesoDenegadoException;
import com.sena.goldenbooking.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.exception.ReservaNoEncontradaException;
import com.sena.goldenbooking.mapper.ReservaMapper;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.TipoReserva;
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
            throw new IllegalArgumentException("El documento del usuario es obligatorio.");
        }
        // Validación adicional para el tipo de reserva
        if (dto.getTp() == null) {
            throw new IllegalArgumentException("El tipo de reserva es obligatorio.");
        }


        // Conversión del DTO a la entidad Reserva, estableciendo la fecha de reserva y el estado inicial como PENDIENTE
        Reserva reserva = reservaMapper.toReserva(dto);
        reserva.setFechaReserva(LocalDateTime.now());
        reserva.setEstado(EstadoReserva.PENDIENTE);

        return reservaMapper.toDto(reservaRepo.save(reserva));
    }

    // Método para obtener una lista de reservas filtradas por estado, devolviendo una lista de DTOs correspondientes a las reservas encontradas
    @Override
    public List<ReservaDto> obtenerPorEstado(EstadoReserva estado) {
        return reservaMapper.toDtoList(reservaRepo.findByEstado(estado));
    }

    // Método para listar todas las reservas, convirtiendo las entidades a DTOs antes de devolver la lista
    @Override
    public List<ReservaDto> listarReservas() {
        return reservaMapper.toDtoList(reservaRepo.findAll());
    }


    // Método para obtener una reserva por su ID.
    // Fix IDOR: si quien pide no es ADMIN, solo puede ver su propia reserva.
    @Override
    public ReservaDto obtenerPorId(String id, String docUsuarioSolicitante, boolean esAdmin) {
        Reserva reserva = reservaRepo.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException("Reserva no encontrada con ID: " + id));
        validarPropietarioOAdmin(reserva.getDocumentoUsuario(), docUsuarioSolicitante, esAdmin);
        return reservaMapper.toDto(reserva);
    }


    // Método para obtener reservas por el documento del usuario.
    // Fix IDOR: un CLIENTE solo puede pedir SUS propias reservas (documentoUsuario == docUsuarioSolicitante).
    @Override
    public List<ReservaDto> obtenerPorUsuario(String documentoUsuario, String docUsuarioSolicitante, boolean esAdmin) {
        validarPropietarioOAdmin(documentoUsuario, docUsuarioSolicitante, esAdmin);
        return reservaMapper.toDtoList(
            reservaRepo.findByDocumentoUsuario(documentoUsuario));
    }

    // Método para actualizar una reserva existente.
    // Fix IDOR: solo el dueño de la reserva o un ADMIN pueden modificarla.
    @Override
    public ReservaDto actualizarReserva(String id, ReservaDto dto, String docUsuarioSolicitante, boolean esAdmin) {
        Reserva reservaExistente = reservaRepo.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException("Reserva no encontrada con ID: " + id));
        validarPropietarioOAdmin(reservaExistente.getDocumentoUsuario(), docUsuarioSolicitante, esAdmin);
        reservaMapper.actualizarReserva(dto, reservaExistente);
        return reservaMapper.toDto(reservaRepo.save(reservaExistente));
    }

    // Método para obtener reservas por usuario y tipo.
    // Fix IDOR: mismo criterio que obtenerPorUsuario.
    @Override
    public List<ReservaDto> obtenerPorUsuarioYTipo(String documentoUsuario, TipoReserva tipo, String docUsuarioSolicitante, boolean esAdmin) {
        validarPropietarioOAdmin(documentoUsuario, docUsuarioSolicitante, esAdmin);
        return reservaMapper.toDtoList(
            reservaRepo.findByDocumentoUsuarioAndTipo(documentoUsuario, tipo));
    }

    // Método para cancelar una reserva.
    // Fix IDOR: solo el dueño de la reserva o un ADMIN pueden cancelarla.
    @Override
    public void cancelarReserva(String id, String docUsuarioSolicitante, boolean esAdmin) {
        Reserva reserva = reservaRepo.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException("Reserva no encontrada con ID: " + id));
        validarPropietarioOAdmin(reserva.getDocumentoUsuario(), docUsuarioSolicitante, esAdmin);
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ConflictoDeNegocioException("La reserva ya está cancelada.");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepo.save(reserva);
    }

    // Utilidad centralizada: lanza AccesoDenegadoException si quien solicita
    // no es ni el dueño del recurso ni un ADMIN. Mismo patrón que ya se usa
    // en ReservaHotelServiceImpl / ReservaDeporteServiceImpl.
    private void validarPropietarioOAdmin(String documentoUsuarioDelRecurso, String docUsuarioSolicitante, boolean esAdmin) {
        if (esAdmin) {
            return;
        }
        if (documentoUsuarioDelRecurso == null || !documentoUsuarioDelRecurso.equals(docUsuarioSolicitante)) {
            throw new AccesoDenegadoException("No tienes permiso para acceder a esta reserva.");
        }
    }
}