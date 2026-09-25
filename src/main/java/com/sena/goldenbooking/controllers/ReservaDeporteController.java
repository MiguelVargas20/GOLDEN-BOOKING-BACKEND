package com.sena.goldenbooking.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sena.goldenbooking.dtos.CancelacionReservaDto;
import com.sena.goldenbooking.dtos.RangoOcupadoDeporteDto;
import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.exception.AccesoDenegadoException;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.security.AutenticacionUtils;
import com.sena.goldenbooking.services.ReservaDeporteService;
import com.sena.goldenbooking.services.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Reservas de espacios deportivos.
 *
 * Flujo: el cliente crea la reserva (PENDIENTE) → el admin la aprueba
 * (CONFIRMADA) o la cancela con un motivo (CANCELADA). El cliente también
 * puede cancelar la suya con 24 h de anticipación.
 */
@Tag(name = "Reservas - Deporte", description = "Reservas de espacios deportivos y su flujo de aprobación.")
@RestController
@RequestMapping("/api/reservas/deporte")
public class ReservaDeporteController {

    private final ReservaDeporteService service;
    private final UsuarioService usuarioService;

    public ReservaDeporteController(ReservaDeporteService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Solicitar una reserva",
            description = "Queda PENDIENTE hasta que el admin la apruebe. Si reserva un CLIENTE se usa su documento "
                    + "(se ignora docUsuario del body); un ADMIN puede reservar a nombre de otra persona.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reserva creada en estado PENDIENTE"),
        @ApiResponse(responseCode = "400", description = "Fechas inválidas, fuera del horario del espacio o menos de 1 hora"),
        @ApiResponse(responseCode = "404", description = "El espacio no existe"),
        @ApiResponse(responseCode = "409", description = "Horario ocupado o espacio no disponible")
    })
    @PostMapping
    public ResponseEntity<ReservaDeporteDto> crear(@Valid @RequestBody ReservaDeporteDto dto, Authentication authentication) {
        if (!AutenticacionUtils.esAdmin(authentication)) {
            dto.setDocUsuario(usuarioService.obtenerDocumentoPorUsername(authentication.getName()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @Operation(summary = "Listar todas las reservas (ADMIN)",
            description = "Paginado y filtrable por estado. Incluye nombre y correo del cliente.")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            @Parameter(description = "Filtrar por estado (opcional)") @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        exigirAdmin(authentication, "listar todas las reservas");
        var pagina = service.listarAdmin(estado, Paginacion.de(page, size));
        return ResponseEntity.ok(Map.of(
            "contenido",      pagina.getContent(),
            "paginaActual",   pagina.getNumber(),
            "totalPaginas",   pagina.getTotalPages(),
            "totalElementos", pagina.getTotalElements()
        ));
    }

    @Operation(summary = "Resumen por estado (ADMIN)",
            description = "Cantidad de reservas PENDIENTE, CONFIRMADA, CANCELADA y FINALIZADA para los indicadores del panel.")
    @GetMapping("/resumen")
    public ResponseEntity<Map<EstadoReserva, Long>> resumen(Authentication authentication) {
        exigirAdmin(authentication, "ver el resumen de reservas");
        return ResponseEntity.ok(service.resumenPorEstado());
    }

    @Operation(summary = "Mis reservas", description = "Reservas del usuario autenticado, más recientes primero.")
    @GetMapping("/mis-reservas")
    public ResponseEntity<List<ReservaDeporteDto>> misReservas(Authentication authentication) {
        return ResponseEntity.ok(service.obtenerPorUsuario(documentoDe(authentication)));
    }

    @Operation(summary = "Horarios ocupados",
            description = "Reservas no canceladas que aún no terminan, sin datos del titular. Lo usa el calendario del cliente.")
    @GetMapping("/ocupadas")
    public ResponseEntity<List<RangoOcupadoDeporteDto>> ocupadas() {
        return ResponseEntity.ok(service.obtenerFechasOcupadas());
    }

    @Operation(summary = "Obtener una reserva", description = "Solo el titular o un ADMIN.")
    @GetMapping("/{id}")
    public ResponseEntity<ReservaDeporteDto> obtenerPorId(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(service.obtenerPorId(id, documentoDe(authentication), AutenticacionUtils.esAdmin(authentication)));
    }

    @Operation(summary = "Reservas deportivas de una reserva padre", description = "Un CLIENTE solo ve las suyas.")
    @GetMapping("/reserva/{idReserva}")
    public ResponseEntity<List<ReservaDeporteDto>> obtenerPorReserva(@PathVariable String idReserva, Authentication authentication) {
        return ResponseEntity.ok(service.obtenerPorReserva(idReserva, documentoDe(authentication), AutenticacionUtils.esAdmin(authentication)));
    }

    @Operation(summary = "Actualizar extras de la reserva",
            description = "Solo implementos y entrenador. Fechas, espacio y precio no se modifican. Titular o ADMIN.")
    @PutMapping("/{id}")
    public ResponseEntity<ReservaDeporteDto> actualizar(@PathVariable String id,
                                                        @Valid @RequestBody ReservaDeporteDto dto,
                                                        Authentication authentication) {
        return ResponseEntity.ok(service.actualizar(id, dto, documentoDe(authentication), AutenticacionUtils.esAdmin(authentication)));
    }

    @Operation(summary = "Aprobar reserva (ADMIN)",
            description = "PENDIENTE → CONFIRMADA. Se envía al cliente un correo de confirmación con archivo de calendario.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva confirmada"),
        @ApiResponse(responseCode = "409", description = "La reserva no está PENDIENTE")
    })
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<ReservaDeporteDto> confirmar(@PathVariable String id, Authentication authentication) {
        exigirAdmin(authentication, "confirmar reservas");
        return ResponseEntity.ok(service.confirmar(id));
    }

    @Operation(summary = "Cancelar reserva",
            description = "CLIENTE: solo la suya y con 24 h de anticipación (motivo opcional). "
                    + "ADMIN: cualquiera, con motivo obligatorio que se envía al cliente por correo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva cancelada"),
        @ApiResponse(responseCode = "400", description = "Falta el motivo (ADMIN)"),
        @ApiResponse(responseCode = "409", description = "Ya cancelada/finalizada o menos de 24 h (CLIENTE)")
    })
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReservaDeporteDto> cancelar(@PathVariable String id,
                                                      @Valid @RequestBody(required = false) CancelacionReservaDto cancelacion,
                                                      Authentication authentication) {
        String motivo = cancelacion != null ? cancelacion.getMotivo() : null;
        return ResponseEntity.ok(service.cancelar(id, documentoDe(authentication),
                AutenticacionUtils.esAdmin(authentication), motivo));
    }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private String documentoDe(Authentication authentication) {
        return usuarioService.obtenerDocumentoPorUsername(authentication.getName());
    }

    private void exigirAdmin(Authentication authentication, String accion) {
        if (!AutenticacionUtils.esAdmin(authentication)) {
            throw new AccesoDenegadoException("Solo un administrador puede " + accion + ".");
        }
    }
}
