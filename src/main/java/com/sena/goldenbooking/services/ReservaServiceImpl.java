package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.mapper.ReservaMapper;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.TipoReserva;
import com.sena.goldenbooking.repositories.ReservaRepository;

@Service
public class ReservaServiceImpl implements ReservaService {

    // Inyectamos el repositorio y el mapper a través del constructor
    private final ReservaRepository reservaRepo;
    private final ReservaMapper reservaMapper;

    // Constructor para inyección de dependencias
    public ReservaServiceImpl(ReservaRepository reservaRepo, ReservaMapper reservaMapper) {
        this.reservaRepo = reservaRepo;
        this.reservaMapper = reservaMapper;
    }

    /* Implementación de los métodos definidos en la interfaz ReservaService */
    @Override

    /* Crea una reserva (Hotel o Deporte) y asigna fecha de registro */
    @Transactional
    public ReservaDto crearReserva(ReservaDto dto) {

        // 1. Validaciones de integridad según el tipo
        validarDatosReserva(dto);

        // 2. Mapeo a entidad
        Reserva reserva = reservaMapper.toReserva(dto);

        // 3. Seteo de valores automáticos
        reserva.setFechaR(LocalDateTime.now()); // Fecha de creación

        if (reserva.getEstR() == null) {
            reserva.setEstR(EstadoReserva.PENDIENTE); // Estado inicial por defecto
        }

        // 4. Guardar en MongoDB
        Reserva guardada = reservaRepo.save(reserva);
        return reservaMapper.toDto(guardada);
    }


    /* Método para listar todas las reservas del sistema (ADMIN) */
    @Override
    public List<ReservaDto> listarTodas() {
        return reservaMapper.toDtoList(reservaRepo.findAll());
    }

    /* Método para obtener el historial de reservas de un cliente por su documento */
    @Override
    public List<ReservaDto> listarPorCliente(String docUsuario) {
        return reservaMapper.toDtoList(reservaRepo.findByDocUsuario(docUsuario));
    }

    /* Método para filtrar reservas por estado (PENDIENTE, CONFIRMADA, CANCELADA) */
    @Override
    public List<ReservaDto> listarPorEstado(EstadoReserva estado) {

        // Convertimos el Enum a String para el repositorio si es necesario
        return reservaMapper.toDtoList(reservaRepo.findByEstadoRes(estado.name()));
    }

    /* Método para obtener detalle de una reserva por su ID único */
    @Override
    public ReservaDto obtenerPorId(String id) {
        
        // Busca la reserva por ID, lanza excepción si no existe
        Reserva reserva = reservaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));
        return reservaMapper.toDto(reserva);
    }

    /* Método para actualizar datos de la reserva (fechas, estado, precio) */
    @Override

    /* El método es @Transactional para asegurar que la actualización sea atómica */
    @Transactional
    public ReservaDto actualizarReserva(String id, ReservaDto dto) {
        Reserva existente = reservaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la reserva para actualizar"));

        // Usamos el mapper para actualizar los campos permitidos
        reservaMapper.actualizarReserva(dto, existente);

        return reservaMapper.toDto(reservaRepo.save(existente));
    }


    /* Método para cambiar el estado de la reserva (ej: de PENDIENTE a CONFIRMADA) */
    @Override
    public ReservaDto cambiarEstado(String id, EstadoReserva nuevoEstado) {
        Reserva reserva = reservaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        
        reserva.setEstR(nuevoEstado);
        return reservaMapper.toDto(reservaRepo.save(reserva));
    }

    /* Método para eliminar una reserva del sistema */
    @Override
    public void eliminarReserva(String id) {
        if (!reservaRepo.existsById(id)) {
            throw new RuntimeException("No se puede eliminar, ID no existe");
        }
        reservaRepo.deleteById(id);
    }

    /*Lógica privada para validar que los detalles coincidan con el tipo de reserva*/

    private void validarDatosReserva(ReservaDto dto) {

        // Validaciones básicas de integridad según el tipo de reserva
        if (dto.getTipo() == TipoReserva.HOTEL && dto.getDetalleHotel() == null) {
            throw new RuntimeException("Una reserva de tipo HOTEL debe incluir detalles de habitación.");
        }

        // Para reservas de deporte, el detalleDeporte es obligatorio
        if (dto.getTipo() == TipoReserva.DEPORTE && dto.getDetalleDeporte() == null) {
            throw new RuntimeException("Una reserva de tipo DEPORTE debe incluir detalles de la cancha.");
            
        }

        // Validación común: el documento del usuario es obligatorio para cualquier reserva
        if (dto.getDocumentoUsuario() == null || dto.getDocumentoUsuario().isEmpty()) {
            throw new RuntimeException("El documento del usuario es obligatorio para la reserva.");
        }
    }
}