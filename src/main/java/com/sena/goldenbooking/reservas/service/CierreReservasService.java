package com.sena.goldenbooking.reservas.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.email.EmailService;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.service.NotificacionService;
import com.sena.goldenbooking.reservas.model.AccionReserva;
import com.sena.goldenbooking.reservas.model.CanceladaPor;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.TipoReserva;
import com.sena.goldenbooking.reservas.repository.ReservaRepository;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

import lombok.extern.slf4j.Slf4j;

/**
 * Cierre automático de reservas (antes estaba dentro de RecordatorioService
 * y solo corría a las 3 a. m.; si el servidor estaba apagado a esa hora, ese
 * día no se cerraba nada).
 *
 * Cada 30 minutos (configurable con APP_RESERVAS_CIERRE_MS):
 *   - CONFIRMADA cuyo horario / estadía ya terminó      → FINALIZADA
 *   - PENDIENTE que nadie aprobó y ya llegó su fecha     → CANCELADA (por SISTEMA)
 *       · deporte: en cuanto empieza el horario reservado
 *       · hotel:   al terminar el día del check-in (margen para aprobar
 *                  una llegada del mismo día)
 *     El cliente recibe un correo avisando que su solicitud venció.
 *
 * Sin esto, las pendientes viejas se acumulaban para siempre en el
 * dashboard y en los contadores de la Navbar.
 */
@Slf4j
@Service
public class CierreReservasService {

    static final String MOTIVO_VENCIDA = "La solicitud no fue aprobada antes de la fecha reservada.";

    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaHotelRepository reservaHotelRepo;
    private final ReservaRepository reservaRepo;
    private final EmailService emailService;
    private final UsuarioService usuarioService;
    private final NotificacionService notificaciones;

    public CierreReservasService(ReservaDeporteRepository reservaDeporteRepo,
                                 ReservaHotelRepository reservaHotelRepo,
                                 ReservaRepository reservaRepo,
                                 EmailService emailService,
                                 UsuarioService usuarioService,
                                 NotificacionService notificaciones) {
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaHotelRepo = reservaHotelRepo;
        this.reservaRepo = reservaRepo;
        this.emailService = emailService;
        this.usuarioService = usuarioService;
        this.notificaciones = notificaciones;
    }

    /** Primera corrida un minuto después de arrancar (así se pone al día tras un reinicio). */
    @Scheduled(initialDelay = 60_000, fixedDelayString = "${APP_RESERVAS_CIERRE_MS:1800000}")
    public void cerrarReservas() {
        LocalDateTime ahora = ZonaHoraria.ahora();
        int finalizadas = finalizarConfirmadas(ahora);
        int vencidas = vencerPendientes(ahora);
        if (finalizadas + vencidas > 0) {
            log.info("Cierre automático: {} reservas finalizadas, {} pendientes vencidas.", finalizadas, vencidas);
        }
    }

    // ── CONFIRMADA → FINALIZADA ────────────────────────────────────────────

    int finalizarConfirmadas(LocalDateTime ahora) {
        List<ReservaHotel> hotel = reservaHotelRepo.findByEstadoAndFechaCheckOutBefore(EstadoReserva.CONFIRMADA, ahora);
        hotel.forEach(rh -> {
            rh.setEstado(EstadoReserva.FINALIZADA);
            rh.setHistorial(HistorialReserva.agregar(rh.getHistorial(), HistorialReserva.delSistema(AccionReserva.FINALIZADA, null)));
            reservaHotelRepo.save(rh);
            sincronizarPadre(rh.getIdReserva(), EstadoReserva.FINALIZADA);
            notificaciones.notificar(rh.getDocUsuario(), TipoNotificacion.CALIFICAR, TipoReserva.HOTEL, rh.getIdHotelReserva(),
                    "¿Qué tal tu estadía?", "Califica la habitación " + numeroHabitacion(rh) + ": tu opinión nos ayuda a mejorar.");
        });

        List<ReservaDeporte> deporte = reservaDeporteRepo.findByEstadoAndFechaFinReservaBefore(EstadoReserva.CONFIRMADA, ahora);
        deporte.forEach(rd -> {
            rd.setEstado(EstadoReserva.FINALIZADA);
            rd.setHistorial(HistorialReserva.agregar(rd.getHistorial(), HistorialReserva.delSistema(AccionReserva.FINALIZADA, null)));
            reservaDeporteRepo.save(rd);
            sincronizarPadre(rd.getIdReserva(), EstadoReserva.FINALIZADA);
            notificaciones.notificar(rd.getDocUsuario(), TipoNotificacion.CALIFICAR, TipoReserva.DEPORTE, rd.getIdReservaDeporte(),
                    "¿Cómo te fue?", "Califica " + rd.getTipoCancha() + ": tu opinión nos ayuda a mejorar.");
        });
        return hotel.size() + deporte.size();
    }

    // ── PENDIENTE vencida → CANCELADA (por SISTEMA) ────────────────────────

    int vencerPendientes(LocalDateTime ahora) {
        List<ReservaDeporte> deporte = reservaDeporteRepo.findByEstadoAndFechaReservaBefore(EstadoReserva.PENDIENTE, ahora);
        List<ReservaHotel> hotel = reservaHotelRepo.findByEstadoAndFechaCheckInBefore(
                EstadoReserva.PENDIENTE, ahora.toLocalDate().atStartOfDay());
        if (deporte.isEmpty() && hotel.isEmpty()) return 0;

        // Una sola consulta para los correos de todos los clientes afectados
        Map<String, UsuarioDto> clientes = usuarioService.obtenerMapaPorDocNums(Stream.concat(
                        deporte.stream().map(ReservaDeporte::getDocUsuario),
                        hotel.stream().map(ReservaHotel::getDocUsuario))
                .distinct().toList());

        deporte.forEach(rd -> {
            rd.setEstado(EstadoReserva.CANCELADA);
            rd.setCanceladaPor(CanceladaPor.SISTEMA);
            rd.setMotivoCancelacion(MOTIVO_VENCIDA);
            rd.setFechaCancelacion(ahora);
            rd.setHistorial(HistorialReserva.agregar(rd.getHistorial(), HistorialReserva.delSistema(AccionReserva.VENCIDA, MOTIVO_VENCIDA)));
            reservaDeporteRepo.save(rd);
            notificaciones.notificar(rd.getDocUsuario(), TipoNotificacion.RESERVA_VENCIDA, TipoReserva.DEPORTE, rd.getIdReservaDeporte(),
                    "Tu solicitud venció", "Tu reserva de " + rd.getTipoCancha() + " no alcanzó a ser aprobada a tiempo.");
            sincronizarPadre(rd.getIdReserva(), EstadoReserva.CANCELADA);

            Map<String, String> detalles = new LinkedHashMap<>();
            detalles.put("Espacio", rd.getTipoCancha());
            detalles.put("Inicio", PlantillasCorreoReserva.fecha(rd.getFechaReserva()));
            detalles.put("Fin", PlantillasCorreoReserva.fecha(rd.getFechaFinReserva()));
            avisarCliente(clientes.get(rd.getDocUsuario()), "Tu solicitud de reserva venció - " + rd.getTipoCancha(), detalles);
        });

        hotel.forEach(rh -> {
            rh.setEstado(EstadoReserva.CANCELADA);
            rh.setCanceladaPor(CanceladaPor.SISTEMA);
            rh.setMotivoCancelacion(MOTIVO_VENCIDA);
            rh.setFechaCancelacion(ahora);
            rh.setHistorial(HistorialReserva.agregar(rh.getHistorial(), HistorialReserva.delSistema(AccionReserva.VENCIDA, MOTIVO_VENCIDA)));
            reservaHotelRepo.save(rh);
            notificaciones.notificar(rh.getDocUsuario(), TipoNotificacion.RESERVA_VENCIDA, TipoReserva.HOTEL, rh.getIdHotelReserva(),
                    "Tu solicitud venció", "Tu reserva de la habitación " + numeroHabitacion(rh) + " no alcanzó a ser aprobada a tiempo.");
            sincronizarPadre(rh.getIdReserva(), EstadoReserva.CANCELADA);

            String habitacion = numeroHabitacion(rh);
            Map<String, String> detalles = new LinkedHashMap<>();
            detalles.put("Habitación", habitacion);
            detalles.put("Check-in", PlantillasCorreoReserva.fecha(rh.getFechaCheckIn()));
            detalles.put("Check-out", PlantillasCorreoReserva.fecha(rh.getFechaCheckOut()));
            avisarCliente(clientes.get(rh.getDocUsuario()), "Tu solicitud de reserva venció - Habitación " + habitacion, detalles);
        });
        return deporte.size() + hotel.size();
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    /** Mantiene la Reserva "padre" con el mismo estado (las reservas antiguas pueden no tenerla). */
    private void sincronizarPadre(String idReserva, EstadoReserva estado) {
        if (idReserva == null) return;
        reservaRepo.findById(idReserva).ifPresent(padre -> {
            padre.setEstado(estado);
            reservaRepo.save(padre);
        });
    }

    private static String numeroHabitacion(ReservaHotel rh) {
        return rh.getDatosH() != null && rh.getDatosH().getNumHab() != null ? rh.getDatosH().getNumHab() : "—";
    }

    private void avisarCliente(UsuarioDto cliente, String asunto, Map<String, String> detalles) {
        if (cliente == null || cliente.getEmail() == null) return;
        try {
            emailService.enviarCorreoHtml(cliente.getEmail(), asunto,
                    PlantillasCorreoReserva.reservaVencida(cliente.getNombre(), detalles, MOTIVO_VENCIDA));
        } catch (Exception e) {
            // Un correo fallido no debe frenar el cierre del resto
            log.warn("No se pudo avisar a {} que su reserva venció: {}", cliente.getEmail(), e.getMessage());
        }
    }
}
