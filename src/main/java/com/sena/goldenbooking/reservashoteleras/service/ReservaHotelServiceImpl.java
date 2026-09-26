package com.sena.goldenbooking.reservashoteleras.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.email.EmailService;
import com.sena.goldenbooking.compartido.exception.AccesoDenegadoException;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.ReservaNoEncontradaException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.habitaciones.model.EstadoHabitacion;
import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.habitaciones.repository.HabitacionRepository;
import com.sena.goldenbooking.reservas.model.CanceladaPor;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.Reserva;
import com.sena.goldenbooking.reservas.model.TipoReserva;
import com.sena.goldenbooking.reservas.repository.ReservaRepository;
import com.sena.goldenbooking.reservas.service.AvisosAdminService;
import com.sena.goldenbooking.reservas.service.PlantillasCorreoReserva;
import com.sena.goldenbooking.reservas.service.ReglasEstadoReserva;
import com.sena.goldenbooking.reservashoteleras.dto.RangoOcupadoDto;
import com.sena.goldenbooking.reservashoteleras.dto.ReservaHotelDto;
import com.sena.goldenbooking.reservashoteleras.mapper.ReservaHotelMapper;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReservaHotelServiceImpl implements ReservaHotelService {

    private final ReservaHotelRepository reservaHotelRepo;
    private final ReservaRepository reservaRepo;
    private final HabitacionRepository habitacionRepo;
    private final ReservaHotelMapper mapper;
    private final EmailService emailService;
    private final UsuarioService usuarioService;
    private final AvisosAdminService avisosAdmin;

    // ── FIX RACE CONDITION ──────────────────────────────────────────
    // Un ReentrantLock por habitación (no uno global, para no bloquear
    // reservas de habitaciones distintas entre sí). Sirve para que, entre
    // "consultar solapamientos" y "guardar la reserva", ningún otro hilo
    // pueda colarse a reservar la MISMA habitación al mismo tiempo.
    //
    // Límite honesto: esto sincroniza dentro de esta instancia de la JVM.
    // Si el día de mañana el backend corre en más de una instancia (varios
    // pods/contenedores detrás de un balanceador), este lock deja de ser
    // suficiente y hay que migrar a un lock distribuido (ej. un documento
    // de "lock" en Mongo con índice único + TTL, o Redis con Redisson).
    // Para un solo proceso, como corre hoy este proyecto, es correcto.
    /** Horarios estándar del hotel (se guardan con cada reserva). */
    static final LocalTime HORA_CHECK_IN = LocalTime.of(15, 0);
    static final LocalTime HORA_CHECK_OUT = LocalTime.of(12, 0);

    private final ConcurrentHashMap<String, Lock> locksPorHabitacion = new ConcurrentHashMap<>();

    private Lock obtenerLock(String idHabitacion) {
        return locksPorHabitacion.computeIfAbsent(idHabitacion, k -> new ReentrantLock());
    }

public ReservaHotelServiceImpl(
        ReservaHotelRepository reservaHotelRepo,
        ReservaRepository reservaRepo,
        HabitacionRepository habitacionRepo,
        ReservaHotelMapper mapper,
        EmailService emailService,           // ← nuevo
        UsuarioService usuarioService,
        AvisosAdminService avisosAdmin) {
    this.reservaHotelRepo = reservaHotelRepo;
    this.reservaRepo = reservaRepo;
    this.habitacionRepo = habitacionRepo;
    this.mapper = mapper;
    this.emailService = emailService;
    this.usuarioService = usuarioService;
    this.avisosAdmin = avisosAdmin;
}
    @Override
    public ReservaHotelDto crear(ReservaHotelDto dto, boolean registradaPorAdmin, boolean confirmarDeInmediato) {
        log.info("Iniciando creación de reserva hotel para usuario: {}", dto.getDocUsuario());

        // 1. Validaciones
        if (dto.getDocUsuario() == null || dto.getIdHabitacion() == null) {
            log.warn("Intento de creación fallido: Datos incompletos.");
            throw new SolicitudInvalidaException("Faltan datos obligatorios de la reserva.");
        }

        // 1.1 El titular debe ser un cliente registrado y activo (importa cuando
        //     el ADMIN reserva a nombre de otra persona escribiendo su documento).
        ReglasEstadoReserva.validarCliente(usuarioService, dto.getDocUsuario());

        // 2. Búsqueda de la habitación
        Habitacion habitacion = habitacionRepo.findById(dto.getIdHabitacion())
                .orElseThrow(() -> new ReservaNoEncontradaException("Habitación no encontrada."));

        // 2.1 "mantenimiento" sigue siendo un bloqueo total decidido por el ADMIN,
        //      independiente de fechas (ej: la habitación está dañada).
        if (habitacion.getEstado() == EstadoHabitacion.MANTENIMIENTO) {
            log.warn("Intento de reserva en habitación en mantenimiento: ID {}", habitacion.getId());
            throw new ConflictoDeNegocioException("Esta habitación está en mantenimiento.");
        }

        // 2.1.1 Horarios estándar de hotel: solo importa el DÍA que elige el
        //       cliente; se guarda check-in a las 3:00 p. m. y check-out a las
        //       12:00 m. Antes llegaba la medianoche (o corrida a UTC), y la
        //       agenda mostraba "12:00 a. m.", el recordatorio de 2 h llegaba a
        //       las 10 p. m. del día anterior y el cierre finalizaba la estadía
        //       a medianoche del día de salida.
        dto.setFCheckIn(dto.getFCheckIn().toLocalDate().atTime(HORA_CHECK_IN));
        dto.setFCheckOut(dto.getFCheckOut().toLocalDate().atTime(HORA_CHECK_OUT));

        // 2.2 Validación de fechas antes de comparar solapamientos
        long noches = ChronoUnit.DAYS.between(dto.getFCheckIn().toLocalDate(), dto.getFCheckOut().toLocalDate());
        if (noches <= 0) throw new SolicitudInvalidaException("La fecha de check-out debe ser posterior a la de check-in.");

        // 2.2.1 No se puede reservar hacia atrás. Antes no había ninguna
        //       validación: vía API se podían crear reservas con check-in en
        //       el pasado. Se compara por FECHA (no por hora) porque el front
        //       manda el check-in como medianoche convertida a UTC, y un
        //       check-in para hoy mismo debe seguir siendo válido.
        if (dto.getFCheckIn().toLocalDate().isBefore(ZonaHoraria.ahora().toLocalDate())) {
            log.warn("Intento de reserva hotel con check-in en el pasado: {}", dto.getFCheckIn());
            throw new SolicitudInvalidaException("La fecha de check-in no puede estar en el pasado.");
        }

        double precioTotal = noches * habitacion.getPrecNoche();

        // ── SECCIÓN CRÍTICA (fix race condition) ──────────────────────
        // Desde acá hasta que soltamos el lock, ningún otro hilo puede estar
        // validando/guardando una reserva para ESTA MISMA habitación. Así,
        // "consultar solapamientos" + "guardar" se comportan como una sola
        // operación atómica para esta habitación puntual.
        Lock lock = obtenerLock(habitacion.getId());
        ReservaHotel guardada;
        lock.lock();
        try {
            // 2.3 Disponibilidad REAL: ya no depende de un campo global "ocupada",
            //     sino de si el rango pedido se cruza con alguna reserva activa
            //     (no cancelada) de ESTA habitación puntual.
            List<ReservaHotel> reservasActivas = reservaHotelRepo
                    .findByIdHabitacionAndEstadoNot(habitacion.getId(), EstadoReserva.CANCELADA);

            boolean haySolapamiento = reservasActivas.stream()
                    .anyMatch(r -> seSolapan(r.getFechaCheckIn(), r.getFechaCheckOut(),
                                              dto.getFCheckIn(), dto.getFCheckOut()));

            if (haySolapamiento) {
                log.warn("Intento de reserva solapada en habitación {} para fechas {} - {}",
                        habitacion.getId(), dto.getFCheckIn(), dto.getFCheckOut());
                throw new ConflictoDeNegocioException(
                        "Esta habitación ya está reservada para esas fechas. Elige otro rango u otra habitación.");
            }

            LocalDateTime ahora = ZonaHoraria.ahora();
            boolean confirmada = registradaPorAdmin && confirmarDeInmediato;
            EstadoReserva estadoInicial = confirmada ? EstadoReserva.CONFIRMADA : EstadoReserva.PENDIENTE;
            Reserva reserva = Reserva.builder()
                    .documentoUsuario(dto.getDocUsuario())
                    .tipo(TipoReserva.HOTEL)
                    .estado(estadoInicial)
                    .fechaReserva(ahora)
                    .fechaInicio(dto.getFCheckIn())
                    .fechaFin(dto.getFCheckOut())
                    .precioTotal(precioTotal)
                    .build();
            Reserva reservaGuardada = reservaRepo.save(reserva);

            ReservaHotel reservaHotel = ReservaHotel.builder()
                    .idReserva(reservaGuardada.getId())
                    .idHabitacion(habitacion.getId())
                    .docUsuario(dto.getDocUsuario())
                    .datosH(habitacion)
                    .fechaCheckIn(dto.getFCheckIn())
                    .fechaCheckOut(dto.getFCheckOut())
                    .noches((int) noches)
                    .precioTotal(precioTotal)
                    .estado(estadoInicial)
                    .registradaPorAdministrador(registradaPorAdmin)
                    .fechaSolicitud(ahora)
                    .fechaConfirmacion(confirmada ? ahora : null)
                    .build();

            guardada = reservaHotelRepo.save(reservaHotel);
        } finally {
            lock.unlock();
        }
        // ── FIN SECCIÓN CRÍTICA ────────────────────────────────────────

        // PENDIENTE: el correo avisa que se recibió la solicitud (el .ics se
        // envía al confirmarla). Si el admin la confirmó de una vez, solo se
        // envía la confirmación.
        enviarCorreo(guardada, guardada.getEstado() == EstadoReserva.CONFIRMADA ? Correo.CONFIRMADA : Correo.SOLICITUD_RECIBIDA, null);
        if (!registradaPorAdmin) {
            avisosAdmin.nuevaReserva("HOTEL", guardada.getIdHotelReserva(), guardada.getDocUsuario(),
                    "Habitación " + numeroHabitacion(guardada), guardada.getFechaCheckIn(), guardada.getFechaCheckOut());
        }
        log.info("Reserva hotel creada ({}{}). ID: {}, Usuario: {}", guardada.getEstado(),
                registradaPorAdmin ? ", registrada por el administrador" : "", guardada.getIdHotelReserva(), dto.getDocUsuario());
        return mapper.toDto(guardada);
    }

    @Override
    public Page<ReservaHotelDto> listarAdmin(EstadoReserva estado, Pageable pageable) {
        // Más recientes primero (por fecha de check-in)
        Pageable ordenado = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by("fechaCheckIn").descending());
        Page<ReservaHotel> pagina = estado == null
                ? reservaHotelRepo.findAll(ordenado)
                : reservaHotelRepo.findByEstado(estado, ordenado);

        // Nombre y correo del cliente en UNA sola consulta para toda la página
        Map<String, UsuarioDto> clientes = usuarioService.obtenerMapaPorDocNums(
                pagina.getContent().stream().map(ReservaHotel::getDocUsuario).distinct().toList());

        return pagina.map(rh -> {
            ReservaHotelDto dto = mapper.toDto(rh);
            UsuarioDto cliente = clientes.get(rh.getDocUsuario());
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
            resumen.put(estado, reservaHotelRepo.countByEstado(estado));
        }
        return resumen;
    }

    @Override
    public ReservaHotelDto obtenerPorId(String id, String docUsuarioSolicitante, boolean esAdmin) {
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Consulta fallida: Reserva hotel {} no encontrada.", id);
                    return new ReservaNoEncontradaException("No encontrada.");
                });

        // Solo el dueño de la reserva o un ADMIN pueden consultarla (fix IDOR)
        if (!esAdmin && !rh.getDocUsuario().equals(docUsuarioSolicitante)) {
            log.warn("Intento de consulta no autorizado. Usuario {} intentó ver la reserva {} del usuario {}.",
                    docUsuarioSolicitante, id, rh.getDocUsuario());
            throw new AccesoDenegadoException("No tienes permiso para ver esta reserva.");
        }

        return mapper.toDto(rh);
    }

    @Override
    public List<ReservaHotelDto> obtenerPorReserva(String idReserva, String docUsuarioSolicitante, boolean esAdmin) {
        List<ReservaHotel> resultado = reservaHotelRepo.findByIdReserva(idReserva);

        // fix IDOR: un CLIENTE solo debe ver los resultados que le pertenecen a él.
        // Si no es admin, filtramos cualquier registro que no sea suyo en vez de
        // devolver la lista completa tal cual venía de Mongo.
        if (!esAdmin) {
            resultado = resultado.stream()
                    .filter(rh -> rh.getDocUsuario() != null && rh.getDocUsuario().equals(docUsuarioSolicitante))
                    .toList();
        }

        return mapper.toDtoList(resultado);
    }

    @Override
public ReservaHotelDto actualizar(String id, ReservaHotelDto dto, String docUsuarioSolicitante, boolean esAdmin) {
    log.info("Actualizando reserva hotel ID: {}", id);
    ReservaHotel rh = reservaHotelRepo.findById(id)
            .orElseThrow(() -> new ReservaNoEncontradaException("No encontrada."));

    // Validación de permisos IDOR (misma lógica que en cancelar)
    if (!esAdmin && !rh.getDocUsuario().equals(docUsuarioSolicitante)) {
        log.warn("Intento de actualización no autorizado por el usuario {}", docUsuarioSolicitante);
        throw new AccesoDenegadoException("No tienes permiso para modificar esta reserva.");
    }

    mapper.actualizarReservaHotel(dto, rh);
    return mapper.toDto(reservaHotelRepo.save(rh));
}
    @Override
    public ReservaHotelDto confirmar(String id) {
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException("La reserva no existe."));
        ReglasEstadoReserva.validarConfirmable(rh.getEstado());

        rh.setEstado(EstadoReserva.CONFIRMADA);
        rh.setFechaConfirmacion(ZonaHoraria.ahora());
        ReservaHotel guardada = reservaHotelRepo.save(rh);
        sincronizarPadre(rh.getIdReserva(), EstadoReserva.CONFIRMADA);

        enviarCorreo(guardada, Correo.CONFIRMADA, null);
        log.info("Reserva hotel {} CONFIRMADA por el administrador.", id);
        return mapper.toDto(guardada);
    }

    @Override
    public ReservaHotelDto cancelar(String id, String docUsuarioSolicitante, boolean esAdmin, String motivo) {
        ReservaHotel rh = reservaHotelRepo.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException("La reserva no existe."));

        // Solo el dueño de la reserva o un ADMIN pueden cancelarla (IDOR)
        if (!esAdmin && !rh.getDocUsuario().equals(docUsuarioSolicitante)) {
            log.warn("Usuario {} intentó cancelar la reserva {} de otro usuario.", docUsuarioSolicitante, id);
            throw new AccesoDenegadoException("No tienes permiso para cancelar esta reserva.");
        }
        ReglasEstadoReserva.validarCancelable(rh.getEstado());
        String motivoLimpio = ReglasEstadoReserva.validarMotivo(motivo, esAdmin);

        if (!esAdmin && rh.getFechaCheckIn().isBefore(ZonaHoraria.ahora().plusHours(24))) {
            throw new ConflictoDeNegocioException("No se puede cancelar con menos de 24 horas de anticipación.");
        }

        rh.setEstado(EstadoReserva.CANCELADA);
        rh.setFechaCancelacion(ZonaHoraria.ahora());
        rh.setCanceladaPor(esAdmin ? CanceladaPor.ADMINISTRADOR : CanceladaPor.CLIENTE);
        rh.setMotivoCancelacion(motivoLimpio);
        ReservaHotel guardada = reservaHotelRepo.save(rh);
        sincronizarPadre(rh.getIdReserva(), EstadoReserva.CANCELADA);

        // Al quedar CANCELADA deja de contar en findByIdHabitacionAndEstadoNot:
        // esas fechas quedan libres automáticamente.
        enviarCorreo(guardada, Correo.CANCELADA, motivoLimpio);
        if (!esAdmin) {
            avisosAdmin.reservaCanceladaPorCliente("HOTEL", id, guardada.getDocUsuario(),
                    "Habitación " + numeroHabitacion(guardada), guardada.getFechaCheckIn(), guardada.getFechaCheckOut());
        }
        log.info("Reserva hotel {} CANCELADA por {}.", id, guardada.getCanceladaPor());
        return mapper.toDto(guardada);
    }

    // Método adicional para obtener reservas por documento de usuario
    @Override
        public List<ReservaHotelDto> obtenerPorUsuario(String docUsuario) {
            log.info("Listando reservas hotel para usuario: {}", docUsuario);
            return reservaHotelRepo.findByDocUsuario(docUsuario).stream()
                    .sorted((a, b) -> b.getFechaCheckIn().compareTo(a.getFechaCheckIn()))
                    .map(mapper::toDto)
                    .toList();
        }

    @Override
    public List<RangoOcupadoDto> obtenerFechasOcupadas(String idHabitacion) {
        return reservaHotelRepo.findByIdHabitacionAndEstadoNot(idHabitacion, EstadoReserva.CANCELADA)
                .stream()
                .map(r -> RangoOcupadoDto.builder()
                        .checkIn(r.getFechaCheckIn())
                        .checkOut(r.getFechaCheckOut())
                        .build())
                .toList();
    }

    // Dos rangos de fechas [inicioA, finA) y [inicioB, finB) se solapan si
    // uno empieza ANTES de que el otro termine, en ambos sentidos.
    // Ejemplo: reserva existente 10-15 julio, nueva reserva 14-18 julio →
    // 10 < 18 (true) Y 15 > 14 (true) → SE SOLAPAN.
    // Nueva reserva 15-20 julio (empieza justo cuando la otra termina) →
    // 10 < 20 (true) Y 15 > 15 (false) → NO se solapan (check-out y check-in
    // el mismo día se permite, como en cualquier hotel real).
    /**
     * ¿Se cruzan dos estadías? Se compara por DÍA: las reservas antiguas tienen
     * horas distintas (medianoche corrida a UTC) y, comparando la hora, una
     * entrada el mismo día en que sale otro huésped parecía un choque.
     * Salir y entrar el mismo día no es solapamiento.
     */
    static boolean seSolapan(LocalDateTime inicioA, LocalDateTime finA,
                             LocalDateTime inicioB, LocalDateTime finB) {
        return inicioA.toLocalDate().isBefore(finB.toLocalDate()) && finA.toLocalDate().isAfter(inicioB.toLocalDate());
    }

    /** Mantiene la Reserva "padre" con el mismo estado que la reserva de hotel. */
    private void sincronizarPadre(String idReserva, EstadoReserva estado) {
        if (idReserva == null) return;
        reservaRepo.findById(idReserva).ifPresent(padre -> {
            padre.setEstado(estado);
            reservaRepo.save(padre);
        });
    }

    private enum Correo { SOLICITUD_RECIBIDA, CONFIRMADA, CANCELADA }

    private void enviarCorreo(ReservaHotel rh, Correo tipo, String motivo) {
        try {
            UsuarioDto cliente = usuarioService.obtenerPorDocNum(rh.getDocUsuario());
            String habitacion = rh.getDatosH() != null ? rh.getDatosH().getNumHab() : "—";
            Map<String, String> detalles = new LinkedHashMap<>();
            detalles.put("Habitación", habitacion);
            detalles.put("Check-in", PlantillasCorreoReserva.fecha(rh.getFechaCheckIn()));
            detalles.put("Check-out", PlantillasCorreoReserva.fecha(rh.getFechaCheckOut()));
            detalles.put("Noches", String.valueOf(rh.getNoches()));
            detalles.put("Total", PlantillasCorreoReserva.pesos(rh.getPrecioTotal()));

            switch (tipo) {
                case SOLICITUD_RECIBIDA -> emailService.enviarCorreoHtml(cliente.getEmail(),
                        "Recibimos tu solicitud de reserva - Habitación " + habitacion,
                        PlantillasCorreoReserva.solicitudRecibida(cliente.getNombre(), detalles));
                case CONFIRMADA -> emailService.enviarConfirmacionReserva(cliente.getEmail(),
                        "Reserva Hotel: Habitación " + habitacion,
                        PlantillasCorreoReserva.reservaConfirmada(cliente.getNombre(), detalles),
                        rh.getFechaCheckIn(), rh.getFechaCheckOut());
                case CANCELADA -> emailService.enviarCorreoHtml(cliente.getEmail(),
                        "Reserva cancelada - Habitación " + habitacion,
                        PlantillasCorreoReserva.reservaCancelada(cliente.getNombre(), detalles, motivo,
                                rh.getCanceladaPor() == CanceladaPor.ADMINISTRADOR));
            }
        } catch (Exception e) {
            // El correo no debe impedir la operación (EmailService además es @Async)
            log.warn("No se pudo enviar el correo ({}) de la reserva hotel {}: {}", tipo, rh.getIdHotelReserva(), e.getMessage());
        }
    }

    /** Número de la habitación guardado en la reserva ("—" en reservas antiguas sin datos). */
    private static String numeroHabitacion(ReservaHotel rh) {
        return rh.getDatosH() != null && rh.getDatosH().getNumHab() != null ? rh.getDatosH().getNumHab() : "—";
    }
}
