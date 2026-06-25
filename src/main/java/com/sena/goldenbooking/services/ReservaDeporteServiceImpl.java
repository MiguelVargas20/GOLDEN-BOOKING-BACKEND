package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;  // ← NUEVO
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.dtos.ReservaDeporteEventDto;        // ← NUEVO
import com.sena.goldenbooking.mapper.ReservaDeporteMapper;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.ReservaDeporte;
import com.sena.goldenbooking.models.TipoReserva;
import com.sena.goldenbooking.repositories.ReservaDeporteRepository;
import com.sena.goldenbooking.repositories.ReservaRepository;

@Service
public class ReservaDeporteServiceImpl implements ReservaDeporteService {

    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaRepository reservaRepo;
    private final ReservaDeporteMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;         // ← NUEVO

    public ReservaDeporteServiceImpl(
            ReservaDeporteRepository reservaDeporteRepo,
            ReservaRepository reservaRepo,
            ReservaDeporteMapper mapper,
            SimpMessagingTemplate messagingTemplate) {             // ← NUEVO
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaRepo = reservaRepo;
        this.mapper = mapper;
        this.messagingTemplate = messagingTemplate;                // ← NUEVO
    }

    @Override
    public ReservaDeporteDto crear(ReservaDeporteDto dto) {

        if (dto.getDocUsuario() == null || dto.getDocUsuario().isBlank())
            throw new RuntimeException("El documento del usuario es obligatorio.");

        if (dto.getTCancha() == null || dto.getTCancha().isBlank())
            throw new RuntimeException("El tipo de cancha es obligatorio.");

        if (dto.getFInicioReserva() == null || dto.getFFinReserva() == null)
            throw new RuntimeException("Las fechas de inicio y fin son obligatorias.");

        long horas = ChronoUnit.HOURS.between(dto.getFInicioReserva(), dto.getFFinReserva());
        if (horas <= 0)
            throw new RuntimeException("La fecha de fin debe ser posterior al inicio.");

        double tarifaHora = 50000.0;
        double precioTotal = horas * tarifaHora;

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

        ReservaDeporte reservaDeporte = ReservaDeporte.builder()
                .idReserva(reservaGuardada.getId())
                .docUsuario(dto.getDocUsuario())
                .tipoCancha(dto.getTCancha())
                .implementosAlquilados(dto.getImplAlquilados())
                .requiereEntrenador(dto.isRqrEntrenador())
                .fechaReserva(dto.getFInicioReserva())
                .fechaFinReserva(dto.getFFinReserva())
                .precio(precioTotal)
                .build();

        ReservaDeporteDto resultado = mapper.toDto(reservaDeporteRepo.save(reservaDeporte));

        // ── NUEVO: Notificar a todos los clientes conectados ─────
        // Se ejecuta DESPUÉS de guardar exitosamente en MongoDB
        ReservaDeporteEventDto evento = ReservaDeporteEventDto.builder()
                .espacioId(dto.getTCancha())
                .fecha(dto.getFInicioReserva().toLocalDate().toString())
                .horaInicio(dto.getFInicioReserva().toString())
                .horaFin(dto.getFFinReserva().toString())
                .estado("OCUPADO")
                .mensaje("La cancha " + dto.getTCancha() + " acaba de ser reservada.")
                .build();

        // Envía el evento a todos los suscritos en /topic/reservas-deporte
        messagingTemplate.convertAndSend("/topic/reservas-deporte", evento);
        // ─────────────────────────────────────────────────────────

        return resultado;
    }

    @Override
    public List<ReservaDeporteDto> listarTodas() {
        return mapper.toDtoList(reservaDeporteRepo.findAll());
    }

    @Override
    public ReservaDeporteDto obtenerPorId(String id) {
        ReservaDeporte rd = reservaDeporteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva deporte no encontrada: " + id));
        return mapper.toDto(rd);
    }

    @Override
    public List<ReservaDeporteDto> obtenerPorReserva(String idReserva) {
        return mapper.toDtoList(reservaDeporteRepo.findByIdReserva(idReserva));
    }

    @Override
    public ReservaDeporteDto actualizar(String id, ReservaDeporteDto dto) {
        ReservaDeporte rd = reservaDeporteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva deporte no encontrada: " + id));
        mapper.actualizarReservaDeporte(dto, rd);
        return mapper.toDto(reservaDeporteRepo.save(rd));
    }

    @Override
    public void cancelar(String id) {
        ReservaDeporte rd = reservaDeporteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva deporte no encontrada: " + id));

        Reserva reserva = reservaRepo.findById(rd.getIdReserva())
                .orElseThrow(() -> new RuntimeException("Reserva padre no encontrada."));

        if (reserva.getEstado() == EstadoReserva.CANCELADA)
            throw new RuntimeException("La reserva ya está cancelada.");

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepo.save(reserva);

        // ── NUEVO: Notificar cancelación a todos los clientes ────
        ReservaDeporteEventDto evento = ReservaDeporteEventDto.builder()
                .espacioId(rd.getTipoCancha())
                .fecha(rd.getFechaReserva().toLocalDate().toString())
                .horaInicio(rd.getFechaReserva().toString())
                .horaFin(rd.getFechaFinReserva().toString())
                .estado("DISPONIBLE")
                .mensaje("La cancha " + rd.getTipoCancha() + " quedó disponible.")
                .build();

        messagingTemplate.convertAndSend("/topic/reservas-deporte", evento);
        // ─────────────────────────────────────────────────────────
    }
}