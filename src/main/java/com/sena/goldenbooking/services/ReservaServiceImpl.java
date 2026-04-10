package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.mapper.ReservaMapper;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.repositories.ReservaRepository;

@Service
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepo;
    private final ReservaMapper reservaMapper;

    public ReservaServiceImpl(ReservaRepository reservaRepo, ReservaMapper reservaMapper) {
        this.reservaRepo = reservaRepo;
        this.reservaMapper = reservaMapper;
    }

    @Override
    public ReservaDto crearReserva(ReservaDto dto) {
        if (dto.getDocumentoUsuario() == null || dto.getDocumentoUsuario().isBlank()) {
            throw new RuntimeException("El documento del usuario es obligatorio.");
        }
        if (dto.getTipo() == null) {
            throw new RuntimeException("El tipo de reserva es obligatorio.");
        }

        Reserva reserva = reservaMapper.toReserva(dto);
        reserva.setFechaR(LocalDateTime.now());
        reserva.setEstR(EstadoReserva.PENDIENTE); // Estado inicial siempre

        return reservaMapper.toDto(reservaRepo.save(reserva));
    }

    @Override
    public List<ReservaDto> listarReservas() {
        return reservaMapper.toDtoList(reservaRepo.findAll());
    }

    @Override
    public ReservaDto obtenerPorId(String id) {
        Reserva reserva = reservaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));
        return reservaMapper.toDto(reserva);
    }

    @Override
    public List<ReservaDto> obtenerPorUsuario(String docUsuario) {
        return reservaMapper.toDtoList(reservaRepo.findByDocUsuario(docUsuario));
    }

    @Override
    public ReservaDto actualizarReserva(String id, ReservaDto dto) {
        Reserva reservaExistente = reservaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));

        reservaMapper.actualizarReserva(dto, reservaExistente);

        return reservaMapper.toDto(reservaRepo.save(reservaExistente));
    }

    @Override
    public void cancelarReserva(String id) {
        Reserva reserva = reservaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));

        if (reserva.getEstR() == EstadoReserva.CANCELADA) {
            throw new RuntimeException("La reserva ya está cancelada.");
        }

        reserva.setEstR(EstadoReserva.CANCELADA);
        reservaRepo.save(reserva);
    }
}