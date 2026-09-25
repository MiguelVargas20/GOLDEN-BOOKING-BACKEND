package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.config.ZonaHoraria;
import com.sena.goldenbooking.dtos.RangoOcupadoDeporteDto;
import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.dtos.ReservaDeporteEventDto;
import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.exception.AccesoDenegadoException;
import com.sena.goldenbooking.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.exception.ReservaNoEncontradaException;
import com.sena.goldenbooking.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.mapper.ReservaDeporteMapper;
import com.sena.goldenbooking.models.CanceladaPor;
import com.sena.goldenbooking.models.EspacioDeportivo;
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

    // Duración mínima de una reserva deportiva
    private static final long DURACION_MINIMA_MINUTOS = 60;

    // El cliente puede cancelar hasta 24 h antes del inicio (el admin siempre puede)
    private static final long HORAS_MINIMAS_CANCELACION = 24;

    private static final String TOPICO_WEBSOCKET = "/topic/reservas-deporte";

    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaRepository reservaRepo;
    private final ReservaDeporteMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final UsuarioService usuarioService;
    private final EspacioDeportivoService espacioService;

    // ── Lock por espacio (evita dos reservas simultáneas del mismo horario) ──
    // Entre "consultar solapamientos" y "guardar", ningún otro hilo puede
    // reservar el MISMO espacio. Sincroniza dentro de esta instancia de la JVM:
    // con varias instancias del backend haría falta un lock distribuido.
    private final ConcurrentHashMap<String, Lock> locksPorEspacio = new ConcurrentHashMap<>();

    public ReservaDeporteServiceImpl(
            ReservaDeporteRepository reservaDeporteRepo,
            ReservaRepository reservaRepo,
            ReservaDeporteMapper mapper,
            SimpMessagingTemplate messagingTemplate,
            EmailService emailService,
            UsuarioService usuarioService,
            EspacioDeportivoService espacioService) {
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaRepo = reservaRepo;
        this.mapper = mapper;
        this.messagingTemplate = messagingTemplate;
        this.emailService = emailService;
        this.usuarioService = usuarioService;
        this.espacioService = espacioService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Crear
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public ReservaDeporteDto crear(ReservaDeporteDto dto) {
        log.info("Solicitud de reserva deportiva. Usuario: {}, Espacio: {}", dto.getDocUsuario(), dto.getEspacioId());

        if (dto.getDocUsuario() == null || dto.getDocUsuario().isBlank()) {
            throw new SolicitudInvalidaException("El documento del usuario es obligatorio.");
        }
        if (dto.getFInicioReserva() == null || dto.getFFinReserva() == null) {
            throw new SolicitudInvalidaException("Las fechas de inicio y fin son obligatorias.");
        }

        // El espacio debe existir y estar ACTIVO. Antes se aceptaba cualquier
        // nombre de cancha como texto libre ("Futbol" y "Fútbol" eran canchas
        // distintas que se podían reservar a la misma hora).
        EspacioDeportivo espacio = espacioService.obtenerReservable(dto.getEspacioId());

        LocalDateTime inicio = dto.getFInicioReserva();
        LocalDateTime fin = dto.getFFinReserva();
        validarFechas(inicio, fin, espacio);

        // Precio proporcional a los minutos, con la tarifa de ESTE espacio
        long minutos = ChronoUnit.MINUTES.between(inicio, fin);
        double precioTotal = Math.round(minutos / 60.0 * espacio.getTarifaHora());

        Lock lock = locksPorEspacio.computeIfAbsent(espacio.getId(), k -> new ReentrantLock());
        ReservaDeporte guardada;
        lock.lock();
        try {
            if (!reservaDeporteRepo.findSolapadasEnEspacio(espacio.getId(), inicio, fin).isEmpty()) {
                log.warn("Horario ocupado en el espacio {} ({} - {})", espacio.getNombre(), inicio, fin);
                throw new ConflictoDeNegocioException(
                        "El espacio " + espacio.getNombre() + " ya está reservado en ese horario. Elige otro horario.");
            }

            LocalDateTime ahora = ZonaHoraria.ahora();
            Reserva reserva = reservaRepo.save(Reserva.builder()
                    .documentoUsuario(dto.getDocUsuario())
                    .tipo(TipoReserva.DEPORTE)
                    .estado(EstadoReserva.PENDIENTE)
                    .fechaReserva(ahora)
                    .fechaInicio(inicio)
                    .fechaFin(fin)
                    .precioTotal(precioTotal)
                    .build());

            guardada = reservaDeporteRepo.save(ReservaDeporte.builder()
                    .idReserva(reserva.getId())
                    .docUsuario(dto.getDocUsuario())
                    .espacioId(espacio.getId())
                    .tipoCancha(espacio.getNombre())
                    .implementosAlquilados(dto.getImplAlquilados())
                    .requiereEntrenador(dto.isRqrEntrenador())
                    .fechaReserva(inicio)
                    .fechaFinReserva(fin)
                    .precio(precioTotal)
                    .estado(EstadoReserva.PENDIENTE)
                    .fechaSolicitud(ahora)
                    .build());
        } finally {
            lock.unlock();
        }

        notificarWebSocket(guardada, "OCUPADO", "El espacio " + espacio.getNombre() + " acaba de ser reservado.");
        enviarCorreo(guardada, Correo.SOLICITUD_RECIBIDA, null);

        log.info("Reserva deportiva creada (PENDIENTE). ID: {}", guardada.getIdReservaDeporte());
        return mapper.toDto(guardada);
    }

    /** No en el pasado, mismo día, mínimo 1 hora y dentro del horario del espacio. */
    private void validarFechas(LocalDateTime inicio, LocalDateTime fin, EspacioDeportivo espacio) {
        if (inicio.isBefore(ZonaHoraria.ahora())) {
            throw new SolicitudInvalidaException("La fecha de inicio no puede estar en el pasado.");
        }
        long minutos = ChronoUnit.MINUTES.between(inicio, fin);
        if (minutos <= 0) {
            throw new SolicitudInvalidaException("La fecha de fin debe ser posterior al inicio.");
        }
        if (minutos < DURACION_MINIMA_MINUTOS) {
            throw new SolicitudInvalidaException("La reserva debe durar al menos una hora.");
        }
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new SolicitudInvalidaException("La reserva debe empezar y terminar el mismo día.");
        }
        if (espacio.getHoraApertura() != null && espacio.getHoraCierre() != null
                && (inicio.toLocalTime().isBefore(espacio.getHoraApertura())
                    || fin.toLocalTime().isAfter(espacio.getHoraCierre()))) {
            throw new SolicitudInvalidaException("El horario de " + espacio.getNombre() + " es de "
                    + espacio.getHoraApertura() + " a " + espacio.getHoraCierre() + ".");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Consultas
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public Page<ReservaDeporteDto> listarAdmin(EstadoReserva estado, Pageable pageable) {
        // Más recientes primero (por fecha de la reserva)
        Pageable ordenado = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by("fechaReserva").descending());
        Page<ReservaDeporte> pagina = estado == null
                ? reservaDeporteRepo.findAll(ordenado)
                : reservaDeporteRepo.findByEstado(estado, ordenado);

        // Nombre y correo del cliente en UNA sola consulta para toda la página
        Map<String, UsuarioDto> clientes = usuarioService.obtenerMapaPorDocNums(
                pagina.getContent().stream().map(ReservaDeporte::getDocUsuario).distinct().toList());

        return pagina.map(rd -> {
            ReservaDeporteDto dto = mapper.toDto(rd);
            UsuarioDto cliente = clientes.get(rd.getDocUsuario());
            if (cliente != null) {
                dto.setNombreCliente(cliente.getNombre() + " " + cliente.getApellido());
                dto.setCorreoCliente(cliente.getEmail());
            }
            return dto;
        });
    }

    @Override
    public Map<EstadoReserva, Long> resumenPorEstado() {
        Map<EstadoReserva, Long> resumen = new EnumMap<>(EstadoReserva.class);
        for (EstadoReserva estado : EstadoReserva.values()) {
            resumen.put(estado, reservaDeporteRepo.countByEstado(estado));
        }
        return resumen;
    }

    @Override
    public ReservaDeporteDto obtenerPorId(String id, String docUsuarioSolicitante, boolean esAdmin) {
        ReservaDeporte rd = buscar(id);
        validarDuenoOAdmin(rd, docUsuarioSolicitante, esAdmin, "ver");
        return mapper.toDto(rd);
    }

    @Override
    public List<ReservaDeporteDto> obtenerPorReserva(String idReserva, String docUsuarioSolicitante, boolean esAdmin) {
        List<ReservaDeporte> resultado = reservaDeporteRepo.findByIdReserva(idReserva);
        if (!esAdmin) {
            resultado = resultado.stream()
                    .filter(rd -> docUsuarioSolicitante != null && docUsuarioSolicitante.equals(rd.getDocUsuario()))
                    .toList();
        }
        return mapper.toDtoList(resultado);
    }

    @Override
    public List<ReservaDeporteDto> obtenerPorUsuario(String docUsuario) {
        return reservaDeporteRepo.findByDocUsuario(docUsuario).stream()
                .sorted((a, b) -> b.getFechaReserva().compareTo(a.getFechaReserva()))
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<RangoOcupadoDeporteDto> obtenerFechasOcupadas() {
        // Solo las que aún no terminan: antes devolvía TODO el historial, que
        // crecía sin límite y el calendario del cliente descargaba completo.
        return reservaDeporteRepo.findByEstadoNotAndFechaFinReservaAfter(EstadoReserva.CANCELADA, ZonaHoraria.ahora())
                .stream()
                .map(r -> RangoOcupadoDeporteDto.builder()
                        .espacioId(r.getEspacioId())
                        .tipoCancha(r.getTipoCancha())
                        .inicio(r.getFechaReserva())
                        .fin(r.getFechaFinReserva())
                        .build())
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Modificar estado
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public ReservaDeporteDto actualizar(String id, ReservaDeporteDto dto, String docUsuarioSolicitante, boolean esAdmin) {
        ReservaDeporte rd = buscar(id);
        validarDuenoOAdmin(rd, docUsuarioSolicitante, esAdmin, "modificar");
        if (rd.getEstado() == EstadoReserva.CANCELADA || rd.getEstado() == EstadoReserva.FINALIZADA) {
            throw new ConflictoDeNegocioException("No se puede modificar una reserva cancelada o finalizada.");
        }
        // Solo extras (implementos, entrenador): fechas, espacio y precio no se
        // cambian aquí porque no pasarían por la validación de disponibilidad.
        mapper.actualizarReservaDeporte(dto, rd);
        return mapper.toDto(reservaDeporteRepo.save(rd));
    }

    @Override
    public ReservaDeporteDto confirmar(String id) {
        ReservaDeporte rd = buscar(id);
        ReglasEstadoReserva.validarConfirmable(rd.getEstado());

        rd.setEstado(EstadoReserva.CONFIRMADA);
        rd.setFechaConfirmacion(ZonaHoraria.ahora());
        ReservaDeporte guardada = reservaDeporteRepo.save(rd);
        sincronizarPadre(rd.getIdReserva(), EstadoReserva.CONFIRMADA);

        enviarCorreo(guardada, Correo.CONFIRMADA, null);
        log.info("Reserva deportiva {} CONFIRMADA por el administrador.", id);
        return mapper.toDto(guardada);
    }

    @Override
    public ReservaDeporteDto cancelar(String id, String docUsuarioSolicitante, boolean esAdmin, String motivo) {
        ReservaDeporte rd = buscar(id);
        validarDuenoOAdmin(rd, docUsuarioSolicitante, esAdmin, "cancelar");
        ReglasEstadoReserva.validarCancelable(rd.getEstado());
        String motivoLimpio = ReglasEstadoReserva.validarMotivo(motivo, esAdmin);

        if (!esAdmin && rd.getFechaReserva().isBefore(ZonaHoraria.ahora().plusHours(HORAS_MINIMAS_CANCELACION))) {
            throw new ConflictoDeNegocioException("No se puede cancelar con menos de 24 horas de anticipación.");
        }

        rd.setEstado(EstadoReserva.CANCELADA);
        rd.setFechaCancelacion(ZonaHoraria.ahora());
        rd.setCanceladaPor(esAdmin ? CanceladaPor.ADMINISTRADOR : CanceladaPor.CLIENTE);
        rd.setMotivoCancelacion(motivoLimpio);
        ReservaDeporte guardada = reservaDeporteRepo.save(rd);
        sincronizarPadre(rd.getIdReserva(), EstadoReserva.CANCELADA);

        notificarWebSocket(guardada, "DISPONIBLE", "El espacio " + rd.getTipoCancha() + " quedó disponible.");
        enviarCorreo(guardada, Correo.CANCELADA, motivoLimpio);

        log.info("Reserva deportiva {} CANCELADA por {}.", id, guardada.getCanceladaPor());
        return mapper.toDto(guardada);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Utilidades
    // ═══════════════════════════════════════════════════════════════════════

    private ReservaDeporte buscar(String id) {
        return reservaDeporteRepo.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException("La reserva no existe."));
    }

    private void validarDuenoOAdmin(ReservaDeporte rd, String docUsuarioSolicitante, boolean esAdmin, String accion) {
        if (!esAdmin && !rd.getDocUsuario().equals(docUsuarioSolicitante)) {
            log.warn("Usuario {} intentó {} la reserva {} de otro usuario.", docUsuarioSolicitante, accion, rd.getIdReservaDeporte());
            throw new AccesoDenegadoException("No tienes permiso para " + accion + " esta reserva.");
        }
    }

    /** Mantiene la Reserva "padre" con el mismo estado que la reserva deportiva. */
    private void sincronizarPadre(String idReserva, EstadoReserva estado) {
        if (idReserva == null) return;
        reservaRepo.findById(idReserva).ifPresent(padre -> {
            padre.setEstado(estado);
            reservaRepo.save(padre);
        });
    }

    private void notificarWebSocket(ReservaDeporte rd, String estado, String mensaje) {
        try {
            messagingTemplate.convertAndSend(TOPICO_WEBSOCKET, ReservaDeporteEventDto.builder()
                    .espacioId(rd.getEspacioId())
                    .nombreEspacio(rd.getTipoCancha())
                    .fecha(rd.getFechaReserva().toLocalDate().toString())
                    .horaInicio(rd.getFechaReserva().toString())
                    .horaFin(rd.getFechaFinReserva().toString())
                    .estado(estado)
                    .mensaje(mensaje)
                    .build());
        } catch (Exception e) {
            // El aviso en vivo es un extra: si falla, la reserva ya quedó guardada
            log.warn("No se pudo notificar por WebSocket la reserva {}: {}", rd.getIdReservaDeporte(), e.getMessage());
        }
    }

    private enum Correo { SOLICITUD_RECIBIDA, CONFIRMADA, CANCELADA }

    private void enviarCorreo(ReservaDeporte rd, Correo tipo, String motivo) {
        try {
            UsuarioDto cliente = usuarioService.obtenerPorDocNum(rd.getDocUsuario());
            Map<String, String> detalles = new LinkedHashMap<>();
            detalles.put("Espacio", rd.getTipoCancha());
            detalles.put("Inicio", PlantillasCorreoReserva.fecha(rd.getFechaReserva()));
            detalles.put("Fin", PlantillasCorreoReserva.fecha(rd.getFechaFinReserva()));
            detalles.put("Total", PlantillasCorreoReserva.pesos(rd.getPrecio()));

            String titulo = "Reserva: " + rd.getTipoCancha();
            switch (tipo) {
                case SOLICITUD_RECIBIDA -> emailService.enviarCorreoHtml(cliente.getEmail(),
                        "Recibimos tu solicitud de reserva - " + rd.getTipoCancha(),
                        PlantillasCorreoReserva.solicitudRecibida(cliente.getNombre(), detalles));
                case CONFIRMADA -> emailService.enviarConfirmacionReserva(cliente.getEmail(), titulo,
                        PlantillasCorreoReserva.reservaConfirmada(cliente.getNombre(), detalles),
                        rd.getFechaReserva(), rd.getFechaFinReserva());
                case CANCELADA -> emailService.enviarCorreoHtml(cliente.getEmail(),
                        "Reserva cancelada - " + rd.getTipoCancha(),
                        PlantillasCorreoReserva.reservaCancelada(cliente.getNombre(), detalles, motivo,
                                rd.getCanceladaPor() == CanceladaPor.ADMINISTRADOR));
            }
        } catch (Exception e) {
            // El correo no debe impedir la operación (EmailService además es @Async)
            log.warn("No se pudo enviar el correo ({}) de la reserva deportiva {}: {}",
                    tipo, rd.getIdReservaDeporte(), e.getMessage());
        }
    }
}
