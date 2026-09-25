package com.sena.goldenbooking.reservas.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.email.EmailService;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.repository.ReservaRepository;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RecordatorioService {

    private final ReservaDeporteRepository reservaDeporteRepo;
    private final ReservaHotelRepository reservaHotelRepo;
    private final ReservaRepository reservaRepo;
    private final EmailService emailService;
    private final UsuarioService usuarioService;

    public RecordatorioService(
            ReservaDeporteRepository reservaDeporteRepo,
            ReservaHotelRepository reservaHotelRepo,
            ReservaRepository reservaRepo,
            EmailService emailService,
            UsuarioService usuarioService) {
        this.reservaDeporteRepo = reservaDeporteRepo;
        this.reservaHotelRepo = reservaHotelRepo;
        this.reservaRepo = reservaRepo;
        this.emailService = emailService;
        this.usuarioService = usuarioService;
    }

    // Corre cada 15 minutos (900,000 ms)
    @Scheduled(fixedRate = 900000)
    public void revisarRecordatorios() {
        log.info("Ejecutando revisión de recordatorios...");
        revisarRecordatoriosDeporte();
        revisarRecordatoriosHotel();
    }

    // Corre todos los días a las 3am. Cierra como FINALIZADA cualquier
    // reserva CONFIRMADA cuya fecha de check-out / fin de reserva ya pasó.
    // Solo toca reservas CONFIRMADAS a propósito: una PENDIENTE que nunca
    // se confirmó no debe "finalizarse sola" — eso es una decisión de
    // negocio aparte (ej. limpiar reservas abandonadas), no de este job.
    @Scheduled(cron = "0 0 3 * * *")
    public void finalizarReservasVencidas() {
        log.info("Ejecutando finalización automática de reservas vencidas...");
        finalizarReservasHotelVencidas();
        finalizarReservasDeporteVencidas();
    }

    private void finalizarReservasHotelVencidas() {
        LocalDateTime ahora = ZonaHoraria.ahora();
        List<ReservaHotel> vencidas = reservaHotelRepo
                .findByEstadoAndFechaCheckOutBefore(EstadoReserva.CONFIRMADA, ahora);

        for (ReservaHotel rh : vencidas) {
            rh.setEstado(EstadoReserva.FINALIZADA);
            reservaHotelRepo.save(rh);

            reservaRepo.findById(rh.getIdReserva()).ifPresent(reserva -> {
                reserva.setEstado(EstadoReserva.FINALIZADA);
                reservaRepo.save(reserva);
            });
        }
        if (!vencidas.isEmpty()) {
            log.info("Reservas de hotel finalizadas automáticamente: {}", vencidas.size());
        }
    }

    private void finalizarReservasDeporteVencidas() {
        LocalDateTime ahora = ZonaHoraria.ahora();
        List<ReservaDeporte> vencidas = reservaDeporteRepo
                .findByEstadoAndFechaFinReservaBefore(EstadoReserva.CONFIRMADA, ahora);

        for (ReservaDeporte rd : vencidas) {
            rd.setEstado(EstadoReserva.FINALIZADA);
            reservaDeporteRepo.save(rd);

            reservaRepo.findById(rd.getIdReserva()).ifPresent(reserva -> {
                reserva.setEstado(EstadoReserva.FINALIZADA);
                reservaRepo.save(reserva);
            });
        }
        if (!vencidas.isEmpty()) {
            log.info("Reservas deportivas finalizadas automáticamente: {}", vencidas.size());
        }
    }

    private void revisarRecordatoriosDeporte() {
        LocalDateTime ahora = ZonaHoraria.ahora();

        // FIX hallazgo #12: se filtra explícitamente por CONFIRMADA en vez de
        // "estado distinto de CANCELADA" (que antes incluía PENDIENTE).
        // Ventana de 24h: reservas que empiezan entre 23h45 y 24h15 desde ahora
        List<ReservaDeporte> proximas24h = reservaDeporteRepo
                .findByEstadoAndRecordatorio24hEnviadoFalseAndFechaReservaBetween(
                        EstadoReserva.CONFIRMADA, ahora.plusHours(23).plusMinutes(45), ahora.plusHours(24).plusMinutes(15));

        // Ventana de 2h
        List<ReservaDeporte> proximas2h = reservaDeporteRepo
                .findByEstadoAndRecordatorio2hEnviadoFalseAndFechaReservaBetween(
                        EstadoReserva.CONFIRMADA, ahora.plusMinutes(105), ahora.plusMinutes(135));

        // FIX hallazgo #11 (N+1): antes cada enviarRecordatorioDeporte() hacía su
        // propia consulta a Mongo por el usuario dueño de la reserva. Ahora se
        // arma UN solo mapa docUsuario -> UsuarioDto para todas las reservas de
        // ambas ventanas, con una sola consulta $in.
        List<String> docs = java.util.stream.Stream.concat(proximas24h.stream(), proximas2h.stream())
                .map(ReservaDeporte::getDocUsuario)
                .distinct()
                .toList();
        Map<String, UsuarioDto> usuariosPorDoc = usuarioService.obtenerMapaPorDocNums(docs);

        proximas24h.forEach(r -> enviarRecordatorioDeporte(r, "24 horas", usuariosPorDoc));
        proximas24h.forEach(r -> { r.setRecordatorio24hEnviado(true); reservaDeporteRepo.save(r); });

        proximas2h.forEach(r -> enviarRecordatorioDeporte(r, "2 horas", usuariosPorDoc));
        proximas2h.forEach(r -> { r.setRecordatorio2hEnviado(true); reservaDeporteRepo.save(r); });
    }

    private void enviarRecordatorioDeporte(ReservaDeporte r, String tiempoAntes, Map<String, UsuarioDto> usuariosPorDoc) {
        try {
            UsuarioDto usuario = usuariosPorDoc.get(r.getDocUsuario());
            if (usuario == null) {
                log.warn("No se encontró usuario {} para el recordatorio de la reserva deportiva {}",
                        r.getDocUsuario(), r.getIdReservaDeporte());
                return;
            }
            String html = """
                    <div style="font-family: 'Poppins', sans-serif; max-width: 500px; margin: auto; padding: 30px; border-radius: 12px; border: 1px solid #eee;">
                        <h2 style="color: #1a1a2e;">Recordatorio de tu reserva</h2>
                        <p style="color: #4a5568;">Tu reserva de <strong>%s</strong> es en aproximadamente <strong>%s</strong>.</p>
                        <p style="color: #4a5568;">Fecha: %s</p>
                    </div>
                    """.formatted(HtmlUtils.htmlEscape(r.getTipoCancha()), tiempoAntes, r.getFechaReserva());

            emailService.enviarCorreoHtml(usuario.getEmail(), "Recordatorio: " + r.getTipoCancha(), html);
            log.info("Recordatorio ({}) enviado para reserva deportiva {}", tiempoAntes, r.getIdReservaDeporte());
        } catch (Exception e) {
            log.warn("No se pudo enviar recordatorio para reserva deportiva {}: {}", r.getIdReservaDeporte(), e.getMessage());
        }
    }

    private void revisarRecordatoriosHotel() {
        LocalDateTime ahora = ZonaHoraria.ahora();

        // FIX hallazgo #12: mismo criterio que en deporte — solo CONFIRMADA.
        List<ReservaHotel> proximas24h = reservaHotelRepo
                .findByEstadoAndRecordatorio24hEnviadoFalseAndFechaCheckInBetween(
                        EstadoReserva.CONFIRMADA, ahora.plusHours(23).plusMinutes(45), ahora.plusHours(24).plusMinutes(15));

        List<ReservaHotel> proximas2h = reservaHotelRepo
                .findByEstadoAndRecordatorio2hEnviadoFalseAndFechaCheckInBetween(
                        EstadoReserva.CONFIRMADA, ahora.plusMinutes(105), ahora.plusMinutes(135));

        // FIX hallazgo #11 (N+1): un solo mapa docUsuario -> UsuarioDto para
        // todas las reservas de ambas ventanas, con una sola consulta $in.
        List<String> docs = java.util.stream.Stream.concat(proximas24h.stream(), proximas2h.stream())
                .map(ReservaHotel::getDocUsuario)
                .distinct()
                .toList();
        Map<String, UsuarioDto> usuariosPorDoc = usuarioService.obtenerMapaPorDocNums(docs);

        proximas24h.forEach(r -> enviarRecordatorioHotel(r, "24 horas", usuariosPorDoc));
        proximas24h.forEach(r -> { r.setRecordatorio24hEnviado(true); reservaHotelRepo.save(r); });

        proximas2h.forEach(r -> enviarRecordatorioHotel(r, "2 horas", usuariosPorDoc));
        proximas2h.forEach(r -> { r.setRecordatorio2hEnviado(true); reservaHotelRepo.save(r); });
    }

    private void enviarRecordatorioHotel(ReservaHotel r, String tiempoAntes, Map<String, UsuarioDto> usuariosPorDoc) {
        try {
            UsuarioDto usuario = usuariosPorDoc.get(r.getDocUsuario());
            if (usuario == null) {
                log.warn("No se encontró usuario {} para el recordatorio de la reserva hotel {}",
                        r.getDocUsuario(), r.getIdHotelReserva());
                return;
            }
            String html = """
                    <div style="font-family: 'Poppins', sans-serif; max-width: 500px; margin: auto; padding: 30px; border-radius: 12px; border: 1px solid #eee;">
                        <h2 style="color: #1a1a2e;">Recordatorio de tu reserva</h2>
                        <p style="color: #4a5568;">Tu check-in en la habitación <strong>%s</strong> es en aproximadamente <strong>%s</strong>.</p>
                        <p style="color: #4a5568;">Check-in: %s</p>
                    </div>
                """.formatted(HtmlUtils.htmlEscape(r.getDatosH().getNumHab()), tiempoAntes, r.getFechaCheckIn());

            emailService.enviarCorreoHtml(usuario.getEmail(), "Recordatorio: Habitación " + r.getDatosH().getNumHab(), html);
            log.info("Recordatorio ({}) enviado para reserva hotel {}", tiempoAntes, r.getIdHotelReserva());
        } catch (Exception e) {
            log.warn("No se pudo enviar recordatorio para reserva hotel {}: {}", r.getIdHotelReserva(), e.getMessage());
        }
    }
}