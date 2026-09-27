package com.sena.goldenbooking.reportes.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.reportes.dto.FilaReporteDto;
import com.sena.goldenbooking.reportes.dto.ReporteDto;
import com.sena.goldenbooking.reportes.dto.ResumenReporteDto;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

/**
 * Reporte de reservas e ingresos por rango de fechas (ADMIN). Una reserva
 * entra en el rango por su fecha de inicio (deporte) o de check-in (hotel).
 */
@Service
public class ReporteService {

    /** Rango máximo: un año (el reporte se arma en memoria). */
    static final long MAXIMO_DIAS = 366;
    private static final Set<EstadoReserva> COBRADAS = EnumSet.of(EstadoReserva.CONFIRMADA, EstadoReserva.FINALIZADA);

    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaHotelRepository reservaHotelRepo;
    private final UsuarioService usuarioService;

    public ReporteService(ReservaDeporteRepository reservaDeporteRepo, ReservaHotelRepository reservaHotelRepo,
                          UsuarioService usuarioService) {
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaHotelRepo = reservaHotelRepo;
        this.usuarioService = usuarioService;
    }

    public ReporteDto generar(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new SolicitudInvalidaException("Indica la fecha inicial y la final del reporte.");
        }
        if (hasta.isBefore(desde)) {
            throw new SolicitudInvalidaException("La fecha final no puede ser anterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) >= MAXIMO_DIAS) {
            throw new SolicitudInvalidaException("El reporte puede abarcar como máximo un año.");
        }
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay(); // "hasta" incluido

        List<ReservaDeporte> deporte = reservaDeporteRepo.findByFechaReservaGreaterThanEqualAndFechaReservaLessThan(inicio, fin);
        List<ReservaHotel> hotel = reservaHotelRepo.findByFechaCheckInGreaterThanEqualAndFechaCheckInLessThan(inicio, fin);

        Map<String, UsuarioDto> clientes = usuarioService.obtenerMapaPorDocNums(Stream.concat(
                deporte.stream().map(ReservaDeporte::getDocUsuario),
                hotel.stream().map(ReservaHotel::getDocUsuario)).distinct().toList());

        List<FilaReporteDto> filas = Stream.concat(
                        deporte.stream().map(r -> new FilaReporteDto("DEPORTE", r.getIdReservaDeporte(),
                                nombre(clientes, r.getDocUsuario()), r.getDocUsuario(), r.getTipoCancha(),
                                r.getFechaReserva(), r.getFechaFinReserva(), estado(r.getEstado()), r.getPrecio(),
                                r.getFechaSolicitud(), r.isRegistradaPorAdministrador())),
                        hotel.stream().map(r -> new FilaReporteDto("HOTEL", r.getIdHotelReserva(),
                                nombre(clientes, r.getDocUsuario()), r.getDocUsuario(), "Habitación " + numero(r),
                                r.getFechaCheckIn(), r.getFechaCheckOut(), estado(r.getEstado()), r.getPrecioTotal(),
                                r.getFechaSolicitud(), r.isRegistradaPorAdministrador())))
                .sorted(Comparator.comparing(FilaReporteDto::inicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return new ReporteDto(desde, hasta, ZonaHoraria.ahora(), resumir(filas), filas);
    }

    static ResumenReporteDto resumir(List<FilaReporteDto> filas) {
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoReserva e : EstadoReserva.values()) {
            porEstado.put(e.name(), filas.stream().filter(f -> e.name().equals(f.estado())).count());
        }
        double ingresosDeporte = ingresos(filas, "DEPORTE");
        double ingresosHotel = ingresos(filas, "HOTEL");
        return new ResumenReporteDto(filas.size(),
                filas.stream().filter(f -> "DEPORTE".equals(f.categoria())).count(),
                filas.stream().filter(f -> "HOTEL".equals(f.categoria())).count(),
                porEstado, ingresosDeporte + ingresosHotel, ingresosDeporte, ingresosHotel);
    }

    private static double ingresos(List<FilaReporteDto> filas, String categoria) {
        return filas.stream()
                .filter(f -> categoria.equals(f.categoria()))
                .filter(f -> COBRADAS.stream().anyMatch(e -> e.name().equals(f.estado())))
                .mapToDouble(f -> f.total() != null ? f.total() : 0)
                .sum();
    }

    private static String numero(ReservaHotel r) {
        return r.getDatosH() != null && r.getDatosH().getNumHab() != null ? r.getDatosH().getNumHab() : "—";
    }

    private static String nombre(Map<String, UsuarioDto> clientes, String documento) {
        UsuarioDto c = documento != null ? clientes.get(documento) : null;
        if (c == null) return documento != null ? "Doc. " + documento : "Cliente";
        return ((c.getNombre() != null ? c.getNombre() : "") + " " + (c.getApellido() != null ? c.getApellido() : "")).trim();
    }

    private static String estado(EstadoReserva estado) {
        return estado != null ? estado.name() : EstadoReserva.PENDIENTE.name();
    }
}
