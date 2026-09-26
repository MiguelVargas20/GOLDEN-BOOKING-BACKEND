package com.sena.goldenbooking.reservasdeportivas.service;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.reservas.service.ReglasMiembros;
import com.sena.goldenbooking.reservas.model.MiembroReserva;
import com.sena.goldenbooking.membresias.service.MembresiaService;
import com.sena.goldenbooking.membresias.dto.BeneficioVigente;
import com.sena.goldenbooking.compartido.email.EmailService;
import com.sena.goldenbooking.compartido.exception.AccesoDenegadoException;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.ReservaNoEncontradaException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.service.NotificacionService;
import com.sena.goldenbooking.reservas.model.AccionReserva;
import com.sena.goldenbooking.reservas.model.CanceladaPor;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.Reserva;
import com.sena.goldenbooking.reservas.model.TipoReserva;
import com.sena.goldenbooking.reservas.repository.ReservaRepository;
import com.sena.goldenbooking.reservas.service.PlantillasCorreoReserva;
import com.sena.goldenbooking.reservas.service.AvisosAdminService;
import com.sena.goldenbooking.reservas.service.HistorialReserva;
import com.sena.goldenbooking.reservas.service.ReglasEstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.dto.RangoOcupadoDeporteDto;
import com.sena.goldenbooking.reservasdeportivas.dto.ReservaDeporteDto;
import com.sena.goldenbooking.reservasdeportivas.dto.ReservaDeporteEventDto;
import com.sena.goldenbooking.reservasdeportivas.mapper.ReservaDeporteMapper;
import com.sena.goldenbooking.reservasdeportivas.model.EspacioDeportivo;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

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
    private final AvisosAdminService avisosAdmin;
    private final NotificacionService notificaciones;
    private final MembresiaService membresias;

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
            EspacioDeportivoService espacioService,
            AvisosAdminService avisosAdmin,
            NotificacionService notificaciones,
            MembresiaService membresias) {
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaRepo = reservaRepo;
        this.mapper = mapper;
        this.messagingTemplate = messagingTemplate;
        this.emailService = emailService;
        this.usuarioService = usuarioService;
        this.espacioService = espacioService;
        this.avisosAdmin = avisosAdmin;
        this.notificaciones = notificaciones;
        this.membresias = membresias;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Crear
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public ReservaDeporteDto crear(ReservaDeporteDto dto, boolean registradaPorAdmin, boolean confirmarDeInmediato) {
        log.info("Solicitud de reserva deportiva. Usuario: {}, Espacio: {}", dto.getDocUsuario(), dto.getEspacioId());

        if (dto.getDocUsuario() == null || dto.getDocUsuario().isBlank()) {
            throw new SolicitudInvalidaException("El documento del usuario es obligatorio.");
        }
        if (dto.getFInicioReserva() == null || dto.getFFinReserva() == null) {
            throw new SolicitudInvalidaException("Las fechas de inicio y fin son obligatorias.");
        }

        // El titular debe ser un cliente registrado y activo (importa cuando el
        // ADMIN reserva a nombre de otra persona escribiendo su documento).
        ReglasEstadoReserva.validarCliente(usuarioService, dto.getDocUsuario());

        // El espacio debe existir y estar ACTIVO. Antes se aceptaba cualquier
        // nombre de cancha como texto libre ("Futbol" y "Fútbol" eran canchas
        // distintas que se podían reservar a la misma hora).
        EspacioDeportivo espacio = espacioService.obtenerReservable(dto.getEspacioId());

        LocalDateTime inicio = dto.getFInicioReserva();
        LocalDateTime fin = dto.getFFinReserva();
        validarFechas(inicio, fin, espacio);

        // Beneficios de socio: más días de anticipación (solo si reserva el cliente) y descuento
        BeneficioVigente beneficio = beneficioDe(dto.getDocUsuario());
        if (!registradaPorAdmin) beneficio.validarAnticipacion(inicio.toLocalDate());
        List<MiembroReserva> miembros = ReglasMiembros.validar(dto.getMiembros(), dto.getDocUsuario(), espacio.getCapacidad());

        // Precio proporcional a los minutos, con la tarifa de ESTE espacio (y el descuento de socio)
        long minutos = ChronoUnit.MINUTES.between(inicio, fin);
        double precioTotal = beneficio.aplicar(minutos / 60.0 * espacio.getTarifaHora());

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
            boolean confirmada = registradaPorAdmin && confirmarDeInmediato;
            EstadoReserva estadoInicial = confirmada ? EstadoReserva.CONFIRMADA : EstadoReserva.PENDIENTE;
            Reserva reserva = reservaRepo.save(Reserva.builder()
                    .documentoUsuario(dto.getDocUsuario())
                    .tipo(TipoReserva.DEPORTE)
                    .estado(estadoInicial)
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
                    .estado(estadoInicial)
                    .registradaPorAdministrador(registradaPorAdmin)
                    .fechaSolicitud(ahora)
                    .fechaConfirmacion(confirmada ? ahora : null)
                    .miembros(miembros)
                    .descuento(beneficio.descuentoParaGuardar())
                    .historial(HistorialReserva.agregar(null, HistorialReserva.evento(AccionReserva.CREADA,
                            registradaPorAdmin ? (confirmada ? "Registrada en recepción y confirmada" : "Registrada en recepción") : null)))
                    .build());
        } finally {
            lock.unlock();
        }

        notificarWebSocket(guardada, "OCUPADO", "El espacio " + espacio.getNombre() + " acaba de ser reservado.");
        enviarCorreo(guardada, guardada.getEstado() == EstadoReserva.CONFIRMADA ? Correo.CONFIRMADA : Correo.SOLICITUD_RECIBIDA, null);
        if (!registradaPorAdmin) {
            avisosAdmin.nuevaReserva("DEPORTE", guardada.getIdReservaDeporte(), guardada.getDocUsuario(),
                    guardada.getTipoCancha(), guardada.getFechaReserva(), guardada.getFechaFinReserva());
        }

        log.info("Reserva deportiva creada ({}{}). ID: {}", guardada.getEstado(),
                registradaPorAdmin ? ", registrada por el administrador" : "", guardada.getIdReservaDeporte());
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
        rd.setHistorial(HistorialReserva.agregar(rd.getHistorial(), HistorialReserva.evento(AccionReserva.CONFIRMADA, null)));
        ReservaDeporte guardada = reservaDeporteRepo.save(rd);
        sincronizarPadre(rd.getIdReserva(), EstadoReserva.CONFIRMADA);

        enviarCorreo(guardada, Correo.CONFIRMADA, null);
        notificaciones.notificar(guardada.getDocUsuario(), TipoNotificacion.RESERVA_APROBADA, TipoReserva.DEPORTE, id,
                "Reserva aprobada", "Tu reserva de " + guardada.getTipoCancha() + " para el "
                        + PlantillasCorreoReserva.fecha(guardada.getFechaReserva()) + " fue aprobada. ¡Te esperamos!");
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
        rd.setHistorial(HistorialReserva.agregar(rd.getHistorial(), HistorialReserva.evento(AccionReserva.CANCELADA, motivoLimpio)));
        ReservaDeporte guardada = reservaDeporteRepo.save(rd);
        sincronizarPadre(rd.getIdReserva(), EstadoReserva.CANCELADA);

        notificarWebSocket(guardada, "DISPONIBLE", "El espacio " + rd.getTipoCancha() + " quedó disponible.");
        enviarCorreo(guardada, Correo.CANCELADA, motivoLimpio);
        if (esAdmin) {
            notificaciones.notificar(guardada.getDocUsuario(), TipoNotificacion.RESERVA_CANCELADA, TipoReserva.DEPORTE, id,
                    "Reserva cancelada", "La administración canceló tu reserva de " + guardada.getTipoCancha() + " del "
                            + PlantillasCorreoReserva.fecha(guardada.getFechaReserva()) + ". Motivo: " + motivoLimpio);
        } else {
            avisosAdmin.reservaCanceladaPorCliente("DEPORTE", id, guardada.getDocUsuario(),
                    guardada.getTipoCancha(), guardada.getFechaReserva(), guardada.getFechaFinReserva());
        }

        log.info("Reserva deportiva {} CANCELADA por {}.", id, guardada.getCanceladaPor());
        return mapper.toDto(guardada);
    }

    @Override
    public ReservaDeporteDto reprogramar(String id, LocalDateTime inicio, LocalDateTime fin,
                                         String docUsuarioSolicitante, boolean esAdmin) {
        if (inicio == null || fin == null) {
            throw new SolicitudInvalidaException("Indica el nuevo horario.");
        }
        ReservaDeporte rd = buscar(id);
        validarDuenoOAdmin(rd, docUsuarioSolicitante, esAdmin, "reprogramar");
        ReglasEstadoReserva.validarReprogramable(rd.getEstado());
        if (!esAdmin && rd.getFechaReserva().isBefore(ZonaHoraria.ahora().plusHours(HORAS_MINIMAS_CANCELACION))) {
            throw new ConflictoDeNegocioException("No se puede reprogramar con menos de 24 horas de anticipación.");
        }
        if (inicio.equals(rd.getFechaReserva()) && fin.equals(rd.getFechaFinReserva())) {
            throw new SolicitudInvalidaException("Elige un horario distinto al actual.");
        }

        // Mismas reglas que al crear: espacio activo, horario de apertura, 1 hora mínima
        EspacioDeportivo espacio = espacioService.obtenerReservable(rd.getEspacioId());
        validarFechas(inicio, fin, espacio);
        if (!esAdmin) beneficioDe(rd.getDocUsuario()).validarAnticipacion(inicio.toLocalDate());
        double precioTotal = BeneficioVigente.aplicar(
                ChronoUnit.MINUTES.between(inicio, fin) / 60.0 * espacio.getTarifaHora(), rd.getDescuento());
        String anterior = PlantillasCorreoReserva.fecha(rd.getFechaReserva()) + " – "
                + PlantillasCorreoReserva.fecha(rd.getFechaFinReserva());

        Lock lock = locksPorEspacio.computeIfAbsent(espacio.getId(), k -> new ReentrantLock());
        ReservaDeporte guardada;
        boolean vuelveAPendiente;
        lock.lock();
        try {
            boolean ocupado = reservaDeporteRepo.findSolapadasEnEspacio(espacio.getId(), inicio, fin).stream()
                    .anyMatch(otra -> !id.equals(otra.getIdReservaDeporte()));
            if (ocupado) {
                throw new ConflictoDeNegocioException(
                        "El espacio " + espacio.getNombre() + " ya está reservado en ese horario. Elige otro horario.");
            }

            // Si el cliente cambia una reserva ya aprobada, la administración debe aprobar el nuevo horario
            vuelveAPendiente = !esAdmin && rd.getEstado() == EstadoReserva.CONFIRMADA;
            rd.setFechaReserva(inicio);
            rd.setFechaFinReserva(fin);
            rd.setPrecio(precioTotal);
            rd.setRecordatorio24hEnviado(false);
            rd.setRecordatorio2hEnviado(false);
            if (vuelveAPendiente) {
                rd.setEstado(EstadoReserva.PENDIENTE);
                rd.setFechaConfirmacion(null);
            }
            rd.setHistorial(HistorialReserva.agregar(rd.getHistorial(), HistorialReserva.evento(AccionReserva.REPROGRAMADA,
                    "Horario anterior: " + anterior + (vuelveAPendiente ? ". Vuelve a quedar pendiente de aprobación." : ""))));
            guardada = reservaDeporteRepo.save(rd);
        } finally {
            lock.unlock();
        }
        sincronizarPadre(guardada.getIdReserva(), guardada.getEstado(), inicio, fin, precioTotal);

        notificarWebSocket(guardada, "OCUPADO", "El espacio " + guardada.getTipoCancha() + " cambió de horario.");
        enviarCorreo(guardada, Correo.REPROGRAMADA, null);
        if (esAdmin) {
            notificaciones.notificar(guardada.getDocUsuario(), TipoNotificacion.RESERVA_REPROGRAMADA, TipoReserva.DEPORTE, id,
                    "Reserva reprogramada", "La administración cambió tu reserva de " + guardada.getTipoCancha()
                            + " al " + PlantillasCorreoReserva.fecha(inicio) + ".");
        } else {
            avisosAdmin.reservaReprogramadaPorCliente("DEPORTE", id, guardada.getDocUsuario(),
                    guardada.getTipoCancha(), inicio, fin);
        }
        log.info("Reserva deportiva {} reprogramada ({} → {}).", id, anterior, inicio);
        return mapper.toDto(guardada);
    }

    @Override
    public ReservaDeporteDto actualizarMiembros(String id, List<MiembroReserva> miembros, String docUsuarioSolicitante, boolean esAdmin) {
        ReservaDeporte rd = buscar(id);
        validarDuenoOAdmin(rd, docUsuarioSolicitante, esAdmin, "modificar");
        ReglasEstadoReserva.validarReprogramable(rd.getEstado());
        Integer capacidad = espacioService.obtenerReservable(rd.getEspacioId()).getCapacidad();
        List<MiembroReserva> limpios = ReglasMiembros.validar(miembros, rd.getDocUsuario(), capacidad);
        rd.setMiembros(limpios);
        rd.setHistorial(HistorialReserva.agregar(rd.getHistorial(), HistorialReserva.evento(AccionReserva.ACOMPANANTES,
                limpios.isEmpty() ? "Sin acompañantes" : limpios.size() + (limpios.size() == 1 ? " acompañante" : " acompañantes"))));
        return mapper.toDto(reservaDeporteRepo.save(rd));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Utilidades
    // ═══════════════════════════════════════════════════════════════════════

    /** Beneficios de socio del cliente. Si no se pueden consultar, se reserva sin beneficios ni límite. */
    private BeneficioVigente beneficioDe(String docUsuario) {
        try {
            BeneficioVigente b = membresias.beneficiosDe(docUsuario);
            return b != null ? b : BeneficioVigente.sinBeneficios(3650);
        } catch (Exception e) {
            log.warn("No se pudieron consultar los beneficios de socio de {}: {}", docUsuario, e.getMessage());
            return BeneficioVigente.sinBeneficios(3650);
        }
    }

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

    /** Reprogramación: la Reserva "padre" también cambia de fechas, precio y (quizá) estado. */
    private void sincronizarPadre(String idReserva, EstadoReserva estado, LocalDateTime inicio, LocalDateTime fin, double precio) {
        if (idReserva == null) return;
        reservaRepo.findById(idReserva).ifPresent(padre -> {
            padre.setEstado(estado);
            padre.setFechaInicio(inicio);
            padre.setFechaFin(fin);
            padre.setPrecioTotal(precio);
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

    private enum Correo { SOLICITUD_RECIBIDA, CONFIRMADA, CANCELADA, REPROGRAMADA }

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
                case REPROGRAMADA -> emailService.enviarCorreoHtml(cliente.getEmail(),
                        "Tu reserva cambió de fecha - " + rd.getTipoCancha(),
                        PlantillasCorreoReserva.reservaReprogramada(cliente.getNombre(), detalles,
                                rd.getEstado() == EstadoReserva.PENDIENTE));
            }
        } catch (Exception e) {
            // El correo no debe impedir la operación (EmailService además es @Async)
            log.warn("No se pudo enviar el correo ({}) de la reserva deportiva {}: {}",
                    tipo, rd.getIdReservaDeporte(), e.getMessage());
        }
    }
}
