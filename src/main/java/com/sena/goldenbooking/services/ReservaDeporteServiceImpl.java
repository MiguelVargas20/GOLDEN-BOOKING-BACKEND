package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j; // ← AGREGADO
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.dtos.ReservaDeporteEventDto;
import com.sena.goldenbooking.mapper.ReservaDeporteMapper;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.ReservaDeporte;
import com.sena.goldenbooking.models.TipoReserva;
import com.sena.goldenbooking.repositories.ReservaDeporteRepository;
import com.sena.goldenbooking.repositories.ReservaRepository;
import com.sena.goldenbooking.exception.ReservaNoEncontradaException;
import com.sena.goldenbooking.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.exception.AccesoDenegadoException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Slf4j // ← AGREGADO
@Service
public class ReservaDeporteServiceImpl implements ReservaDeporteService {

    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaRepository reservaRepo;
    private final ReservaDeporteMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;

    public ReservaDeporteServiceImpl(
            ReservaDeporteRepository reservaDeporteRepo,
            ReservaRepository reservaRepo,
            ReservaDeporteMapper mapper,
            SimpMessagingTemplate messagingTemplate) {
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaRepo = reservaRepo;
        this.mapper = mapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public ReservaDeporteDto crear(ReservaDeporteDto dto) {
        log.info("Iniciando creación de reserva deportiva. Usuario: {}, Cancha: {}", dto.getDocUsuario(), dto.getTCancha());

        if (dto.getDocUsuario() == null || dto.getDocUsuario().isBlank()) {
            log.warn("Reserva rechazada: Documento de usuario nulo o vacío.");
            throw new IllegalArgumentException("El documento del usuario es obligatorio.");
        }

        if (dto.getTCancha() == null || dto.getTCancha().isBlank()) {
            log.warn("Reserva rechazada: Tipo de cancha nulo o vacío para usuario {}.", dto.getDocUsuario());
            throw new IllegalArgumentException("El tipo de cancha es obligatorio.");
        }

        if (dto.getFInicioReserva() == null || dto.getFFinReserva() == null) {
            log.warn("Reserva rechazada: Fechas incompletas para usuario {}.", dto.getDocUsuario());
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias.");
        }

        long horas = ChronoUnit.HOURS.between(dto.getFInicioReserva(), dto.getFFinReserva());
        if (horas <= 0) {
            log.warn("Reserva rechazada: Fecha de fin no es posterior a inicio. Usuario {}.", dto.getDocUsuario());
            throw new IllegalArgumentException("La fecha de fin debe ser posterior al inicio.");
        }

        // ── NUEVO: validación atómica de disponibilidad ──────────
        List<ReservaDeporte> solapadas = reservaDeporteRepo.findSolapadas(
                dto.getTCancha(),
                dto.getFInicioReserva(),
                dto.getFFinReserva()
        );
        if (!solapadas.isEmpty()) {
            log.warn("Conflicto de disponibilidad: La cancha {} ya está reservada en el horario solicitado por el usuario {}.", dto.getTCancha(), dto.getDocUsuario());
            throw new ConflictoDeNegocioException(
                "La cancha " + dto.getTCancha() +
                " ya está reservada en ese horario."
            );
        }

        try {
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
                    .estado(EstadoReserva.PENDIENTE)
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

            log.info("Reserva deportiva creada y notificada con éxito. ID: {}, Usuario: {}", resultado.getIdD(), dto.getDocUsuario());
            return resultado;

        } catch (Exception e) {
            log.error("Error crítico al persistir o notificar la reserva deportiva para el usuario {}: {}", dto.getDocUsuario(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<ReservaDeporteDto> listarTodas() {
        return mapper.toDtoList(reservaDeporteRepo.findAll());
    }

    @Override
    public ReservaDeporteDto obtenerPorId(String id) {
        return reservaDeporteRepo.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> {
                    log.warn("Consulta fallida: Reserva deportiva {} no encontrada.", id);
                    return new ReservaNoEncontradaException("Reserva deporte no encontrada: " + id);
                });
    }

    @Override
    public List<ReservaDeporteDto> obtenerPorReserva(String idReserva) {
        return mapper.toDtoList(reservaDeporteRepo.findByIdReserva(idReserva));
    }

    @Override
    public ReservaDeporteDto actualizar(String id, ReservaDeporteDto dto) {
        log.info("Actualizando reserva deportiva ID: {}", id);
        ReservaDeporte rd = reservaDeporteRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Actualización fallida: Reserva deportiva {} no encontrada.", id);
                    return new ReservaNoEncontradaException("Reserva deporte no encontrada: " + id);
                });
        mapper.actualizarReservaDeporte(dto, rd);
        ReservaDeporteDto resultado = mapper.toDto(reservaDeporteRepo.save(rd));
        log.info("Reserva deportiva ID: {} actualizada con éxito", id);
        return resultado;
    }

    @Override
    public void cancelar(String id, String docUsuarioSolicitante, boolean esAdmin) {
        log.info("Iniciando cancelación de reserva deportiva ID: {}", id);
        ReservaDeporte rd = reservaDeporteRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cancelación fallida: Reserva deportiva {} no encontrada.", id);
                    return new ReservaNoEncontradaException("Reserva deporte no encontrada: " + id);
                });

        // ── FIX IDOR: solo el dueño de la reserva o un ADMIN pueden cancelarla ──
        if (!esAdmin && !rd.getDocUsuario().equals(docUsuarioSolicitante)) {
            log.warn("Intento de cancelación no autorizado. Usuario {} intentó cancelar la reserva {} del usuario {}.",
                    docUsuarioSolicitante, id, rd.getDocUsuario());
            throw new AccesoDenegadoException("No tienes permiso para cancelar esta reserva.");
        }

        Reserva reserva = reservaRepo.findById(rd.getIdReserva())
                .orElseThrow(() -> {
                    log.error("¡Inconsistencia! Reserva padre no encontrada para la reserva deportiva {}", id);
                    return new ReservaNoEncontradaException("Reserva padre no encontrada.");
                });

        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            log.warn("Intento de cancelar una reserva deportiva ya cancelada. ID: {}", id);
            throw new ConflictoDeNegocioException("La reserva ya está cancelada.");
        }

        try {
            reserva.setEstado(EstadoReserva.CANCELADA);
            reservaRepo.save(reserva);

            // ── FIX: sincronizar el estado también en ReservaDeporte,
            // que es la colección que realmente se lee en las vistas
            // "Gestionar reservas" y "Mis reservas" ────────────────
            rd.setEstado(EstadoReserva.CANCELADA);
            reservaDeporteRepo.save(rd);

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

            log.info("Cancelación exitosa y notificada para la reserva deportiva ID: {}", id);
        } catch (Exception e) {
            log.error("Error crítico al cancelar o notificar la reserva deportiva ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    // Reservas del usuario autenticado — reemplaza el filtrado inseguro del frontend
    @Override
    public List<ReservaDeporteDto> obtenerPorUsuario(String docUsuario) {
        log.info("Listando reservas deportivas del usuario: {}", docUsuario);
        return mapper.toDtoList(reservaDeporteRepo.findByDocUsuario(docUsuario));
    }

    // Método adicional para listar todas las reservas con paginación
    @Override
    public Page<ReservaDeporteDto> listarTodasPaginadas(Pageable pageable) {
        log.info("Listado paginado de reservas deportivas. Página: {}", pageable.getPageNumber());
        return reservaDeporteRepo.findAll(pageable).map(mapper::toDto);
    }
}