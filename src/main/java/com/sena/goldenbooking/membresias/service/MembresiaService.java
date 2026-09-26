package com.sena.goldenbooking.membresias.service;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.membresias.dto.BeneficioVigente;
import com.sena.goldenbooking.membresias.dto.MiMembresiaDto;
import com.sena.goldenbooking.membresias.dto.SocioDto;
import com.sena.goldenbooking.membresias.model.BeneficiosMembresia;
import com.sena.goldenbooking.membresias.model.ConfigMembresia;
import com.sena.goldenbooking.membresias.repository.ConfigMembresiaRepository;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.service.NotificacionService;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.model.Rol;
import com.sena.goldenbooking.usuarios.model.TipoMembresia;
import com.sena.goldenbooking.usuarios.model.Usuario;
import com.sena.goldenbooking.usuarios.model.UsuarioAuth;
import com.sena.goldenbooking.usuarios.repository.UsuarioAuthRepository;
import com.sena.goldenbooking.usuarios.repository.UsuarioRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Programa de socios: dos categorías (Ocasional y Miembro) con descuento en
 * las reservas y más días de anticipación para reservar. Cuando un cliente
 * alcanza el número de reservas configurado, el panel sugiere hacerlo socio.
 */
@Slf4j
@Service
public class MembresiaService {

    /** Estados que cuentan como "reserva realizada" para sugerir la membresía. */
    static final Set<EstadoReserva> CUENTAN = EnumSet.of(EstadoReserva.CONFIRMADA, EstadoReserva.FINALIZADA);

    private final ConfigMembresiaRepository configRepo;
    private final UsuarioRepository usuarioRepo;
    private final UsuarioAuthRepository authRepo;
    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaHotelRepository reservaHotelRepo;
    private final NotificacionService notificaciones;

    public MembresiaService(ConfigMembresiaRepository configRepo, UsuarioRepository usuarioRepo,
                            UsuarioAuthRepository authRepo, ReservaDeporteRepository reservaDeporteRepo,
                            ReservaHotelRepository reservaHotelRepo, NotificacionService notificaciones) {
        this.configRepo = configRepo;
        this.usuarioRepo = usuarioRepo;
        this.authRepo = authRepo;
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaHotelRepo = reservaHotelRepo;
        this.notificaciones = notificaciones;
    }

    // ── Configuración ──────────────────────────────────────────────────────

    public ConfigMembresia obtenerConfig() {
        return configRepo.findById(ConfigMembresia.ID).orElseGet(ConfigMembresia::porDefecto);
    }

    public ConfigMembresia guardarConfig(ConfigMembresia config) {
        if (config.getOcasional().getDiasAnticipacion() < config.getDiasAnticipacionGeneral()
                || config.getMiembro().getDiasAnticipacion() < config.getDiasAnticipacionGeneral()) {
            throw new SolicitudInvalidaException(
                    "Los socios deben poder reservar con igual o más anticipación que un cliente sin membresía.");
        }
        config.setId(ConfigMembresia.ID);
        config.setFechaActualizacion(ZonaHoraria.ahora());
        return configRepo.save(config);
    }

    // ── Beneficios de un cliente ───────────────────────────────────────────

    /** Descuento y anticipación que le aplican hoy al cliente con ese documento. */
    public BeneficioVigente beneficiosDe(String docUsuario) {
        ConfigMembresia config = obtenerConfig();
        TipoMembresia tipo = usuarioRepo.findByDocNum(docUsuario)
                .map(Usuario::getMembresia).orElse(TipoMembresia.NINGUNA);
        BeneficiosMembresia b = beneficios(config, tipo);
        return b == null
                ? BeneficioVigente.sinBeneficios(config.getDiasAnticipacionGeneral())
                : new BeneficioVigente(tipo, b.getDescuento(), b.getDiasAnticipacion());
    }

    public TipoMembresia membresiaDe(String docUsuario) {
        return usuarioRepo.findByDocNum(docUsuario).map(Usuario::getMembresia)
                .filter(t -> t != null).orElse(TipoMembresia.NINGUNA);
    }

    public MiMembresiaDto miMembresia(String docUsuario) {
        ConfigMembresia config = obtenerConfig();
        BeneficioVigente vigente = beneficiosDe(docUsuario);
        BeneficiosMembresia b = beneficios(config, vigente.membresia());
        long reservas = reservaDeporteRepo.countByDocUsuarioAndEstadoIn(docUsuario, CUENTAN)
                + reservaHotelRepo.countByDocUsuarioAndEstadoIn(docUsuario, CUENTAN);
        return new MiMembresiaDto(vigente.membresia(), vigente.descuento(), vigente.diasAnticipacion(),
                b != null ? b.getOtrosBeneficios() : null, reservas, config.getReservasParaSugerir(),
                config.getOcasional(), config.getMiembro());
    }

    // ── Panel de socios ────────────────────────────────────────────────────

    /** Clientes (sin administradores) con su categoría y cuántas reservas han hecho; los sugeridos primero. */
    public List<SocioDto> listarClientes() {
        int umbral = obtenerConfig().getReservasParaSugerir();
        Set<String> admins = authRepo.findAll().stream()
                .filter(a -> a.getRls() != null && a.getRls().contains(Rol.ROL_ADMIN))
                .map(UsuarioAuth::getId).collect(Collectors.toSet());
        Map<String, Long> reservasPorDoc = Stream.concat(
                        reservaDeporteRepo.findByEstadoIn(CUENTAN).stream().map(ReservaDeporte::getDocUsuario),
                        reservaHotelRepo.findByEstadoIn(CUENTAN).stream().map(ReservaHotel::getDocUsuario))
                .filter(d -> d != null)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return usuarioRepo.findAll().stream()
                .filter(u -> !admins.contains(u.getId()))
                .map(u -> aSocio(u, reservasPorDoc, umbral))
                .sorted(Comparator.comparing(SocioDto::sugerido).reversed()
                        .thenComparing(Comparator.comparingLong(SocioDto::reservas).reversed()))
                .toList();
    }

    /** Asigna o quita la categoría de socio y se le avisa al cliente en su campana. */
    public SocioDto asignar(String idUsuario, TipoMembresia tipo) {
        if (tipo == null) throw new SolicitudInvalidaException("Indica la categoría: NINGUNA, OCASIONAL o MIEMBRO.");
        Usuario u = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
        u.setMembresia(tipo);
        u.setFechaMembresia(tipo == TipoMembresia.NINGUNA ? null : ZonaHoraria.ahora());
        usuarioRepo.save(u);

        String doc = u.getDocId() != null ? u.getDocId().getNumeroD() : null;
        ConfigMembresia config = obtenerConfig();
        BeneficiosMembresia b = beneficios(config, tipo);
        notificaciones.notificar(doc, TipoNotificacion.MEMBRESIA, null, null,
                tipo == TipoMembresia.NINGUNA ? "Tu membresía terminó" : "¡Ahora eres socio " + nombre(tipo) + "!",
                b == null ? "Ya no tienes beneficios de socio."
                        : "Tienes " + (int) b.getDescuento() + " % de descuento en tus reservas y puedes reservar con "
                                + b.getDiasAnticipacion() + " días de anticipación.");
        log.info("Membresía de {} cambiada a {}.", doc, tipo);

        long reservas = doc == null ? 0 : reservaDeporteRepo.countByDocUsuarioAndEstadoIn(doc, CUENTAN)
                + reservaHotelRepo.countByDocUsuarioAndEstadoIn(doc, CUENTAN);
        return aSocio(u, doc == null ? Map.of() : Map.of(doc, reservas), config.getReservasParaSugerir());
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private static BeneficiosMembresia beneficios(ConfigMembresia config, TipoMembresia tipo) {
        if (tipo == TipoMembresia.OCASIONAL) return config.getOcasional();
        if (tipo == TipoMembresia.MIEMBRO) return config.getMiembro();
        return null;
    }

    public static String nombre(TipoMembresia tipo) {
        return tipo == TipoMembresia.MIEMBRO ? "Miembro" : tipo == TipoMembresia.OCASIONAL ? "Ocasional" : "Sin membresía";
    }

    private static SocioDto aSocio(Usuario u, Map<String, Long> reservasPorDoc, int umbral) {
        String doc = u.getDocId() != null ? u.getDocId().getNumeroD() : null;
        TipoMembresia tipo = u.getMembresia() != null ? u.getMembresia() : TipoMembresia.NINGUNA;
        long reservas = doc != null ? reservasPorDoc.getOrDefault(doc, 0L) : 0;
        String nombre = ((u.getNomUsr() != null ? u.getNomUsr() : "") + " " + (u.getApellUsr() != null ? u.getApellUsr() : "")).trim();
        return new SocioDto(u.getId(), nombre, doc, u.getCorreo(), tipo, u.getFechaMembresia(), reservas,
                tipo == TipoMembresia.NINGUNA && reservas >= umbral);
    }
}
