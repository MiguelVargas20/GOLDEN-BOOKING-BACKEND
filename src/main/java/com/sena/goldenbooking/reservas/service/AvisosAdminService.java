package com.sena.goldenbooking.reservas.service;

import java.time.LocalDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.sena.goldenbooking.reservas.dto.AvisoReservaDto;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

import lombok.extern.slf4j.Slf4j;

/**
 * Avisa en vivo a los administradores conectados cuando un CLIENTE crea o
 * cancela una reserva (así el contador de pendientes y el dashboard se
 * actualizan al instante, sin esperar el sondeo de cada minuto).
 *
 * El canal "/topic/admin/**" está protegido: solo un ADMIN autenticado
 * puede suscribirse (ver security.WebSocketAuthInterceptor).
 */
@Slf4j
@Service
public class AvisosAdminService {

    public static final String TOPICO_ADMIN_RESERVAS = "/topic/admin/reservas";

    private final SimpMessagingTemplate messagingTemplate;
    private final UsuarioService usuarioService;

    public AvisosAdminService(SimpMessagingTemplate messagingTemplate, UsuarioService usuarioService) {
        this.messagingTemplate = messagingTemplate;
        this.usuarioService = usuarioService;
    }

    public void nuevaReserva(String categoria, String idReserva, String docCliente, String lugar,
                             LocalDateTime inicio, LocalDateTime fin) {
        enviar(new AvisoReservaDto(categoria, "NUEVA", idReserva, nombreCliente(docCliente), lugar, inicio, fin));
    }

    public void reservaCanceladaPorCliente(String categoria, String idReserva, String docCliente, String lugar,
                                           LocalDateTime inicio, LocalDateTime fin) {
        enviar(new AvisoReservaDto(categoria, "CANCELADA", idReserva, nombreCliente(docCliente), lugar, inicio, fin));
    }

    private void enviar(AvisoReservaDto aviso) {
        try {
            messagingTemplate.convertAndSend(TOPICO_ADMIN_RESERVAS, aviso);
        } catch (Exception e) {
            // El aviso en vivo es un extra: la reserva ya quedó guardada
            log.warn("No se pudo enviar el aviso en vivo al admin ({} {}): {}", aviso.accion(), aviso.idReserva(), e.getMessage());
        }
    }

    private String nombreCliente(String documento) {
        try {
            UsuarioDto c = usuarioService.obtenerPorDocNum(documento);
            return (c.getNombre() + " " + (c.getApellido() != null ? c.getApellido() : "")).trim();
        } catch (Exception e) {
            return "Doc. " + documento;
        }
    }
}
