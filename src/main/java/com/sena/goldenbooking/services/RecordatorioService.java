package com.sena.goldenbooking.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.dtos.UsuarioDto;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.ReservaDeporte;
import com.sena.goldenbooking.models.ReservaHotel;
import com.sena.goldenbooking.repositories.ReservaDeporteRepository;
import com.sena.goldenbooking.repositories.ReservaHotelRepository;
import com.sena.goldenbooking.repositories.ReservaRepository;

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
        LocalDateTime ahora = LocalDateTime.now();
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
        LocalDateTime ahora = LocalDateTime.now();
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
        LocalDateTime ahora = LocalDateTime.now();

        // Ventana de 24h: reservas que empiezan entre 23h45 y 24h15 desde ahora
        List<ReservaDeporte> proximas24h = reservaDeporteRepo
                .findByEstadoNotAndRecordatorio24hEnviadoFalseAndFechaReservaBetween(
                        EstadoReserva.CANCELADA, ahora.plusHours(23).plusMinutes(45), ahora.plusHours(24).plusMinutes(15));
        proximas24h.forEach(r -> enviarRecordatorioDeporte(r, "24 horas"));
        proximas24h.forEach(r -> { r.setRecordatorio24hEnviado(true); reservaDeporteRepo.save(r); });

        // Ventana de 2h
        List<ReservaDeporte> proximas2h = reservaDeporteRepo
                .findByEstadoNotAndRecordatorio2hEnviadoFalseAndFechaReservaBetween(
                        EstadoReserva.CANCELADA, ahora.plusMinutes(105), ahora.plusMinutes(135));
        proximas2h.forEach(r -> enviarRecordatorioDeporte(r, "2 horas"));
        proximas2h.forEach(r -> { r.setRecordatorio2hEnviado(true); reservaDeporteRepo.save(r); });
    }

    private void enviarRecordatorioDeporte(ReservaDeporte r, String tiempoAntes) {
        try {
            UsuarioDto usuario = usuarioService.obtenerPorDocNum(r.getDocUsuario());
            String html = """
                    <div style="font-family: 'Poppins', sans-serif; max-width: 500px; margin: auto; padding: 30px; border-radius: 12px; border: 1px solid #eee;">
                        <h2 style="color: #1a1a2e;">Recordatorio de tu reserva</h2>
                        <p style="color: #4a5568;">Tu reserva de <strong>%s</strong> es en aproximadamente <strong>%s</strong>.</p>
                        <p style="color: #4a5568;">Fecha: %s</p>
                    </div>
                    """.formatted(r.getTipoCancha(), tiempoAntes, r.getFechaReserva());

            emailService.enviarCorreoHtml(usuario.getEmail(), "Recordatorio: " + r.getTipoCancha(), html);
            log.info("Recordatorio ({}) enviado para reserva deportiva {}", tiempoAntes, r.getIdReservaDeporte());
        } catch (Exception e) {
            log.warn("No se pudo enviar recordatorio para reserva deportiva {}: {}", r.getIdReservaDeporte(), e.getMessage());
        }
    }

    private void revisarRecordatoriosHotel() {
        LocalDateTime ahora = LocalDateTime.now();

        List<ReservaHotel> proximas24h = reservaHotelRepo
                .findByEstadoNotAndRecordatorio24hEnviadoFalseAndFechaCheckInBetween(
                        EstadoReserva.CANCELADA, ahora.plusHours(23).plusMinutes(45), ahora.plusHours(24).plusMinutes(15));
        proximas24h.forEach(r -> enviarRecordatorioHotel(r, "24 horas"));
        proximas24h.forEach(r -> { r.setRecordatorio24hEnviado(true); reservaHotelRepo.save(r); });

        List<ReservaHotel> proximas2h = reservaHotelRepo
                .findByEstadoNotAndRecordatorio2hEnviadoFalseAndFechaCheckInBetween(
                        EstadoReserva.CANCELADA, ahora.plusMinutes(105), ahora.plusMinutes(135));
        proximas2h.forEach(r -> enviarRecordatorioHotel(r, "2 horas"));
        proximas2h.forEach(r -> { r.setRecordatorio2hEnviado(true); reservaHotelRepo.save(r); });
    }

    private void enviarRecordatorioHotel(ReservaHotel r, String tiempoAntes) {
        try {
            UsuarioDto usuario = usuarioService.obtenerPorDocNum(r.getDocUsuario());
            String html = """
                    <div style="font-family: 'Poppins', sans-serif; max-width: 500px; margin: auto; padding: 30px; border-radius: 12px; border: 1px solid #eee;">
                        <h2 style="color: #1a1a2e;">Recordatorio de tu reserva</h2>
                        <p style="color: #4a5568;">Tu check-in en la habitación <strong>%s</strong> es en aproximadamente <strong>%s</strong>.</p>
                        <p style="color: #4a5568;">Check-in: %s</p>
                    </div>
                """.formatted(r.getDatosH().getNumHab(), tiempoAntes, r.getFechaCheckIn());

            emailService.enviarCorreoHtml(usuario.getEmail(), "Recordatorio: Habitación " + r.getDatosH().getNumHab(), html);
            log.info("Recordatorio ({}) enviado para reserva hotel {}", tiempoAntes, r.getIdHotelReserva());
        } catch (Exception e) {
            log.warn("No se pudo enviar recordatorio para reserva hotel {}: {}", r.getIdHotelReserva(), e.getMessage());
        }
    }
}