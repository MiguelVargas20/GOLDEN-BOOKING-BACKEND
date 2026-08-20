package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.dtos.ReservaDeporteEventDto;
import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.exception.AccesoDenegadoException;
import com.sena.goldenbooking.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.exception.ReservaNoEncontradaException;
import com.sena.goldenbooking.mapper.ReservaDeporteMapper;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.ReservaDeporte;
import com.sena.goldenbooking.models.TipoReserva;
import com.sena.goldenbooking.repositories.ReservaDeporteRepository;
import com.sena.goldenbooking.repositories.ReservaRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReservaDeporteServiceImpl implements ReservaDeporteService {

    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaRepository reservaRepo;
    private final ReservaDeporteMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final UsuarioService usuarioService;

    // Tarifa por hora de reservas deportivas — configurable vía
    // app.reservas.deporte.tarifa-hora (antes hardcodeada como 50000.0 en este método)
    @Value("${app.reservas.deporte.tarifa-hora}")
    private double tarifaHora;

    // ── FIX RACE CONDITION (mismo patrón que ReservaHotelServiceImpl) ──
    // Un lock por tipo de cancha: entre "consultar solapamientos" y
    // "guardar", ningún otro hilo puede colarse a reservar la MISMA cancha.
    // Limitación honesta: solo sincroniza dentro de esta instancia de la
    // JVM — con más de una instancia del backend se necesitaría un lock
    // distribuido (Mongo con índice único + TTL, o Redis/Redisson).
    private final ConcurrentHashMap<String, Lock> locksPorCancha = new ConcurrentHashMap<>();

    private Lock obtenerLock(String tipoCancha) {
        return locksPorCancha.computeIfAbsent(tipoCancha, k -> new ReentrantLock());
    }

    public ReservaDeporteServiceImpl(
            ReservaDeporteRepository reservaDeporteRepo,
            ReservaRepository reservaRepo,
            ReservaDeporteMapper mapper,
            SimpMessagingTemplate messagingTemplate,
            EmailService emailService,
            UsuarioService usuarioService) {
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaRepo = reservaRepo;
        this.mapper = mapper;
        this.messagingTemplate = messagingTemplate;
        this.emailService = emailService;
        this.usuarioService = usuarioService;
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

        double precioTotal = horas * tarifaHora;

        // ── SECCIÓN CRÍTICA (fix race condition) ────────────────────────
        // Igual que en ReservaHotelServiceImpl: sin este lock, dos requests
        // simultáneos para la misma cancha y horario podían pasar ambos la
        // validación de solapamiento antes de que cualquiera guardara.
        Lock lock = obtenerLock(dto.getTCancha());
        ReservaDeporte reservaDeporteGuardada;
        lock.lock();
        try {
            List<ReservaDeporte> solapadas = reservaDeporteRepo.findSolapadas(
                    dto.getTCancha(),
                    dto.getFInicioReserva(),
                    dto.getFFinReserva()
            );
            if (!solapadas.isEmpty()) {
                log.warn("Conflicto de disponibilidad: La cancha {} ya está reservada en el horario solicitado por el usuario {}.", dto.getTCancha(), dto.getDocUsuario());
                throw new ConflictoDeNegocioException(
                    "La cancha " + dto.getTCancha() + " ya está reservada en ese horario."
                );
            }

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

            reservaDeporteGuardada = reservaDeporteRepo.save(reservaDeporte);
        } finally {
            lock.unlock();
        }
        // ── FIN SECCIÓN CRÍTICA ──────────────────────────────────────────

        try {
            ReservaDeporteDto resultado = mapper.toDto(reservaDeporteGuardada);

            // Notificar a todos los clientes conectados (WebSockets)
            ReservaDeporteEventDto evento = ReservaDeporteEventDto.builder()
                    .espacioId(dto.getTCancha())
                    .fecha(dto.getFInicioReserva().toLocalDate().toString())
                    .horaInicio(dto.getFInicioReserva().toString())
                    .horaFin(dto.getFFinReserva().toString())
                    .estado("OCUPADO")
                    .mensaje("La cancha " + dto.getTCancha() + " acaba de ser reservada.")
                    .build();

            messagingTemplate.convertAndSend("/topic/reservas-deporte", evento);

            // Enviar confirmación por correo con archivo .ics
            try {
                UsuarioDto usuario = usuarioService.obtenerPorDocNum(dto.getDocUsuario());
                String tituloEvento = "Reserva: " + dto.getTCancha();
                String cuerpoHtml = """
                        <div style="font-family: 'Poppins', sans-serif; max-width: 500px; margin: auto; padding: 30px; border-radius: 12px; border: 1px solid #eee;">
                            <h2 style="color: #1a1a2e;">Reserva confirmada — <span style="color:#f68b1e;">Golden Booking</span></h2>
                            <p style="color: #4a5568;">Hola %s, tu reserva quedó registrada con los siguientes detalles:</p>
                            <ul style="color: #4a5568; line-height: 1.8;">
                                <li><strong>Espacio:</strong> %s</li>
                                <li><strong>Inicio:</strong> %s</li>
                                <li><strong>Fin:</strong> %s</li>
                                <li><strong>Total:</strong> $%,.0f</li>
                            </ul>
                            <p style="color: #a0aec0; font-size: 0.85rem;">Adjuntamos un archivo de calendario para que agregues este evento directamente a Google Calendar u Outlook.</p>
                        </div>
                        """.formatted(
                        usuario.getNombre(),
                        dto.getTCancha(),
                        dto.getFInicioReserva(),
                        dto.getFFinReserva(),
                        precioTotal
                );

                emailService.enviarConfirmacionReserva(
                        usuario.getEmail(),
                        tituloEvento,
                        cuerpoHtml,
                        dto.getFInicioReserva(),
                        dto.getFFinReserva()
                );
            } catch (Exception e) {
                log.warn("No se pudo enviar la confirmación por correo para la reserva del usuario {}: {}",
                        dto.getDocUsuario(), e.getMessage());
            }

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
    public ReservaDeporteDto obtenerPorId(String id, String docUsuarioSolicitante, boolean esAdmin) {
        ReservaDeporte rd = reservaDeporteRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Consulta fallida: Reserva deportiva {} no encontrada.", id);
                    return new ReservaNoEncontradaException("Reserva deporte no encontrada: " + id);
                });

        // Solo el dueño de la reserva o un ADMIN pueden consultarla (fix IDOR)
        if (!esAdmin && !rd.getDocUsuario().equals(docUsuarioSolicitante)) {
            log.warn("Intento de consulta no autorizado. Usuario {} intentó ver la reserva {} del usuario {}.",
                    docUsuarioSolicitante, id, rd.getDocUsuario());
            throw new AccesoDenegadoException("No tienes permiso para ver esta reserva.");
        }

        return mapper.toDto(rd);
    }

    @Override
    public List<ReservaDeporteDto> obtenerPorReserva(String idReserva) {
        return mapper.toDtoList(reservaDeporteRepo.findByIdReserva(idReserva));
    }

    @Override
    public ReservaDeporteDto actualizar(String id, ReservaDeporteDto dto, String docUsuarioSolicitante, boolean esAdmin) {
        log.info("Actualizando reserva deportiva ID: {}", id);
        ReservaDeporte rd = reservaDeporteRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Actualización fallida: Reserva deportiva {} no encontrada.", id);
                    return new ReservaNoEncontradaException("Reserva deporte no encontrada: " + id);
                });

        // Solo el dueño de la reserva o un ADMIN pueden actualizarla (fix IDOR)
        if (!esAdmin && !rd.getDocUsuario().equals(docUsuarioSolicitante)) {
            log.warn("Intento de actualización no autorizado. Usuario {} intentó modificar la reserva {} del usuario {}.",
                    docUsuarioSolicitante, id, rd.getDocUsuario());
            throw new AccesoDenegadoException("No tienes permiso para modificar esta reserva.");
        }

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

        // Solo el dueño de la reserva o un ADMIN pueden cancelarla
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

            rd.setEstado(EstadoReserva.CANCELADA);
            reservaDeporteRepo.save(rd);

            // Aviso de cancelación por correo
            try {
                UsuarioDto usuario = usuarioService.obtenerPorDocNum(rd.getDocUsuario());
                String detalleHtml = """
                        <ul style="color: #4a5568; line-height: 1.8;">
                            <li><strong>Espacio:</strong> %s</li>
                            <li><strong>Fecha:</strong> %s</li>
                        </ul>
                        """.formatted(rd.getTipoCancha(), rd.getFechaReserva());

                emailService.enviarAvisoCancelacion(
                        usuario.getEmail(),
                        "Reserva: " + rd.getTipoCancha(),
                        detalleHtml
                );
            } catch (Exception e) {
                log.warn("No se pudo enviar el aviso de cancelación para la reserva {}: {}", id, e.getMessage());
            }

            // Notificar cancelación a todos los clientes mediante WebSocket
            ReservaDeporteEventDto evento = ReservaDeporteEventDto.builder()
                    .espacioId(rd.getTipoCancha())
                    .fecha(rd.getFechaReserva().toLocalDate().toString())
                    .horaInicio(rd.getFechaReserva().toString())
                    .horaFin(rd.getFechaFinReserva().toString())
                    .estado("DISPONIBLE")
                    .mensaje("La cancha " + rd.getTipoCancha() + " quedó disponible.")
                    .build();

            messagingTemplate.convertAndSend("/topic/reservas-deporte", evento);

            log.info("Cancelación exitosa y notificada para la reserva deportiva ID: {}", id);
        } catch (Exception e) {
            log.error("Error crítico al cancelar o notificar la reserva deportiva ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<ReservaDeporteDto> obtenerPorUsuario(String docUsuario) {
        log.info("Listando reservas deportivas del usuario: {}", docUsuario);
        return mapper.toDtoList(reservaDeporteRepo.findByDocUsuario(docUsuario));
    }

    @Override
    public Page<ReservaDeporteDto> listarTodasPaginadas(Pageable pageable) {
        log.info("Listado paginado de reservas deportivas. Página: {}", pageable.getPageNumber());
        return reservaDeporteRepo.findAll(pageable).map(mapper::toDto);
    }
}