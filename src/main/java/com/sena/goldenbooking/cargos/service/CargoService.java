package com.sena.goldenbooking.cargos.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.sena.goldenbooking.cargos.dto.CargoDto;
import com.sena.goldenbooking.cargos.dto.CuentaClienteDto;
import com.sena.goldenbooking.cargos.dto.DestinoDisponibleDto;
import com.sena.goldenbooking.cargos.model.Cargo;
import com.sena.goldenbooking.cargos.model.DestinoCargo;
import com.sena.goldenbooking.cargos.model.EstadoCargo;
import com.sena.goldenbooking.cargos.repository.CargoRepository;
import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.membresias.service.MembresiaService;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.service.NotificacionService;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.service.PlantillasCorreoReserva;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import com.sena.goldenbooking.usuarios.model.TipoMembresia;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

import lombok.extern.slf4j.Slf4j;

/**
 * Cargos a la habitación o a la cuenta de socio: el personal registra lo que
 * el cliente consume en el club (restaurante, bar, tienda, spa...) y se carga a
 * una reserva CONFIRMADA que aún no termina, o a su cuenta de socio. Todo se
 * paga junto al hacer el check-out o a fin de mes.
 */
@Slf4j
@Service
public class CargoService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("d MMM h:mm a", Locale.forLanguageTag("es-CO"));

    private final CargoRepository repo;
    private final ReservaHotelRepository reservaHotelRepo;
    private final ReservaDeporteRepository reservaDeporteRepo;
    private final UsuarioService usuarioService;
    private final MembresiaService membresias;
    private final NotificacionService notificaciones;

    public CargoService(CargoRepository repo, ReservaHotelRepository reservaHotelRepo,
                        ReservaDeporteRepository reservaDeporteRepo, UsuarioService usuarioService,
                        MembresiaService membresias, NotificacionService notificaciones) {
        this.repo = repo;
        this.reservaHotelRepo = reservaHotelRepo;
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.usuarioService = usuarioService;
        this.membresias = membresias;
        this.notificaciones = notificaciones;
    }

    // ── Consultas ──────────────────────────────────────────────────────────

    /** Reservas confirmadas que aún no terminan y la cuenta de socio (si lo es). */
    public List<DestinoDisponibleDto> destinos(String docUsuario) {
        LocalDateTime ahora = ZonaHoraria.ahora();
        List<DestinoDisponibleDto> lista = new ArrayList<>();
        reservaHotelRepo.findByDocUsuarioAndEstado(docUsuario, EstadoReserva.CONFIRMADA).stream()
                .filter(r -> r.getFechaCheckOut() != null && r.getFechaCheckOut().isAfter(ahora))
                .forEach(r -> lista.add(new DestinoDisponibleDto(DestinoCargo.RESERVA_HOTEL, r.getIdHotelReserva(), describir(r))));
        reservaDeporteRepo.findByDocUsuarioAndEstado(docUsuario, EstadoReserva.CONFIRMADA).stream()
                .filter(r -> r.getFechaFinReserva() != null && r.getFechaFinReserva().isAfter(ahora))
                .forEach(r -> lista.add(new DestinoDisponibleDto(DestinoCargo.RESERVA_DEPORTE, r.getIdReservaDeporte(),
                        r.getTipoCancha() + " · " + r.getFechaReserva().format(DIA_HORA))));
        if (membresias.membresiaDe(docUsuario) != TipoMembresia.NINGUNA) {
            lista.add(new DestinoDisponibleDto(DestinoCargo.CUENTA_SOCIO, null, "Cuenta de socio (pago a fin de mes)"));
        }
        return lista;
    }

    /** Estado de cuenta completo de un cliente (para el admin). */
    public CuentaClienteDto cuenta(String docUsuario) {
        UsuarioDto cliente = cliente(docUsuario);
        List<CargoDto> cargos = repo.findByDocUsuarioOrderByFechaDesc(docUsuario).stream().map(c -> aDto(c, nombre(cliente))).toList();
        return new CuentaClienteDto(docUsuario, nombre(cliente), membresias.membresiaDe(docUsuario), totalPendiente(cargos),
                destinos(docUsuario), cargos);
    }

    /** Los consumos del propio cliente. */
    public CuentaClienteDto miCuenta(String docUsuario) {
        List<CargoDto> cargos = repo.findByDocUsuarioOrderByFechaDesc(docUsuario).stream().map(c -> aDto(c, null)).toList();
        return new CuentaClienteDto(docUsuario, null, membresias.membresiaDe(docUsuario), totalPendiente(cargos), List.of(), cargos);
    }

    /** Todos los cargos pendientes de pago (vista general del admin). */
    public List<CargoDto> pendientes() {
        return repo.findByEstadoOrderByFechaDesc(EstadoCargo.PENDIENTE).stream().map(c -> aDto(c, null)).toList();
    }

    // ── Registrar y pagar ──────────────────────────────────────────────────

    public CargoDto registrar(CargoDto dto, String usuarioAdmin) {
        UsuarioDto cliente = cliente(dto.getDocUsuario());
        if (cliente.getEstado() == EstadoUsuario.INACTIVO) {
            throw new ConflictoDeNegocioException("La cuenta de este cliente está inactiva.");
        }
        String descripcion = destinos(dto.getDocUsuario()).stream()
                .filter(d -> d.destino() == dto.getDestino()
                        && (dto.getDestino() == DestinoCargo.CUENTA_SOCIO || d.idReserva().equals(dto.getIdReserva())))
                .map(DestinoDisponibleDto::descripcion)
                .findFirst()
                .orElseThrow(() -> new SolicitudInvalidaException(dto.getDestino() == DestinoCargo.CUENTA_SOCIO
                        ? "Este cliente no es socio: carga el consumo a una de sus reservas activas."
                        : "Esa reserva no está activa (debe estar confirmada y sin terminar) o no es de este cliente."));

        double total = Math.round(dto.getCantidad() * dto.getValorUnitario());
        Cargo cargo = repo.save(Cargo.builder()
                .docUsuario(dto.getDocUsuario())
                .concepto(dto.getConcepto().trim())
                .categoria(dto.getCategoria())
                .cantidad(dto.getCantidad())
                .valorUnitario(dto.getValorUnitario())
                .total(total)
                .destino(dto.getDestino())
                .idReserva(dto.getDestino() == DestinoCargo.CUENTA_SOCIO ? null : dto.getIdReserva())
                .descripcionDestino(descripcion)
                .estado(EstadoCargo.PENDIENTE)
                .fecha(ZonaHoraria.ahora())
                .registradoPor(usuarioAdmin)
                .build());

        notificaciones.notificar(dto.getDocUsuario(), TipoNotificacion.CARGO, null, null, "Nuevo consumo cargado",
                cargo.getConcepto() + " x" + cargo.getCantidad() + " por " + PlantillasCorreoReserva.pesos(total) + " → " + descripcion + ".");
        log.info("Cargo de {} a {} ({}) registrado por {}.", total, dto.getDocUsuario(), descripcion, usuarioAdmin);
        return aDto(cargo, nombre(cliente));
    }

    public CargoDto pagar(String id, String usuarioAdmin) {
        Cargo c = buscar(id);
        if (c.getEstado() == EstadoCargo.PAGADO) throw new ConflictoDeNegocioException("Este consumo ya está pagado.");
        marcarPagado(c, usuarioAdmin);
        return aDto(repo.save(c), null);
    }

    /**
     * Paga de una vez los pendientes del cliente: los de una reserva (check-out),
     * los de la cuenta de socio (fin de mes) o todos.
     * @return cuántos consumos se pagaron y por cuánto
     */
    public ResumenPago pagarPendientes(String docUsuario, String idReserva, boolean soloCuentaSocio, String usuarioAdmin) {
        List<Cargo> pendientes = repo.findByDocUsuarioAndEstado(docUsuario, EstadoCargo.PENDIENTE).stream()
                .filter(c -> idReserva == null || idReserva.equals(c.getIdReserva()))
                .filter(c -> !soloCuentaSocio || c.getDestino() == DestinoCargo.CUENTA_SOCIO)
                .toList();
        if (pendientes.isEmpty()) throw new ConflictoDeNegocioException("No hay consumos pendientes para pagar.");
        pendientes.forEach(c -> marcarPagado(c, usuarioAdmin));
        repo.saveAll(pendientes);
        double total = pendientes.stream().mapToDouble(Cargo::getTotal).sum();
        notificaciones.notificar(docUsuario, TipoNotificacion.CARGO, null, null, "Pago registrado",
                "Registramos el pago de " + pendientes.size() + (pendientes.size() == 1 ? " consumo" : " consumos")
                        + " por " + PlantillasCorreoReserva.pesos(total) + ". ¡Gracias!");
        return new ResumenPago(pendientes.size(), total);
    }

    public void eliminar(String id) {
        Cargo c = buscar(id);
        if (c.getEstado() == EstadoCargo.PAGADO) {
            throw new ConflictoDeNegocioException("No se puede eliminar un consumo que ya fue pagado.");
        }
        repo.delete(c);
    }

    /** Resultado de pagar varios consumos. */
    public record ResumenPago(int cantidad, double total) {
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private static void marcarPagado(Cargo c, String usuarioAdmin) {
        c.setEstado(EstadoCargo.PAGADO);
        c.setFechaPago(ZonaHoraria.ahora());
        c.setPagoRegistradoPor(usuarioAdmin);
    }

    private Cargo buscar(String id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El consumo no existe."));
    }

    private UsuarioDto cliente(String docUsuario) {
        try {
            return usuarioService.obtenerPorDocNum(docUsuario);
        } catch (RecursoNoEncontradoException e) {
            throw new RecursoNoEncontradoException("No hay ningún cliente registrado con el documento " + docUsuario + ".");
        }
    }

    private static String nombre(UsuarioDto c) {
        return c == null ? null : ((c.getNombre() != null ? c.getNombre() : "") + " " + (c.getApellido() != null ? c.getApellido() : "")).trim();
    }

    private static double totalPendiente(List<CargoDto> cargos) {
        return cargos.stream().filter(c -> c.getEstado() == EstadoCargo.PENDIENTE).mapToDouble(CargoDto::getTotal).sum();
    }

    private static String describir(ReservaHotel r) {
        String numero = r.getDatosH() != null && r.getDatosH().getNumHab() != null ? r.getDatosH().getNumHab() : "—";
        return "Habitación " + numero + " · " + r.getFechaCheckIn().format(DIA) + " → " + r.getFechaCheckOut().format(DIA);
    }

    private static CargoDto aDto(Cargo c, String nombreCliente) {
        return CargoDto.builder()
                .id(c.getId())
                .docUsuario(c.getDocUsuario())
                .nombreCliente(nombreCliente)
                .concepto(c.getConcepto())
                .categoria(c.getCategoria())
                .cantidad(c.getCantidad())
                .valorUnitario(c.getValorUnitario())
                .total(c.getTotal())
                .destino(c.getDestino())
                .idReserva(c.getIdReserva())
                .descripcionDestino(c.getDescripcionDestino())
                .estado(c.getEstado())
                .fecha(c.getFecha())
                .registradoPor(c.getRegistradoPor())
                .fechaPago(c.getFechaPago())
                .build();
    }
}
