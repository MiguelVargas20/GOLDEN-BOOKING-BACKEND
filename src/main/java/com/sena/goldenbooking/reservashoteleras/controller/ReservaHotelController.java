package com.sena.goldenbooking.reservashoteleras.controller;

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

import com.sena.goldenbooking.compartido.exception.AccesoDenegadoException;
import com.sena.goldenbooking.compartido.web.Paginacion;
import com.sena.goldenbooking.reservas.dto.CancelacionReservaDto;
import com.sena.goldenbooking.reservas.dto.MiembrosDto;
import com.sena.goldenbooking.reservas.dto.ReprogramacionDto;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservashoteleras.dto.RangoOcupadoDto;
import com.sena.goldenbooking.reservashoteleras.dto.ReservaHotelDto;
import com.sena.goldenbooking.reservashoteleras.service.ReservaHotelService;
import com.sena.goldenbooking.security.AutenticacionUtils;
import com.sena.goldenbooking.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Reservas de habitaciones de hotel.
 *
 * Flujo: el cliente crea la reserva (PENDIENTE) → el admin la aprueba
 * (CONFIRMADA) o la cancela con un motivo (CANCELADA). El cliente también
 * puede cancelar la suya con 24 h de anticipación.
 */
@Tag(name = "Reservas - Hotel", description = "Reservas de habitaciones y su flujo de aprobación.")
@RestController
@RequestMapping("/api/reservas/hotel")
public class ReservaHotelController {

    private final ReservaHotelService service;
    private final UsuarioService usuarioService;

    public ReservaHotelController(ReservaHotelService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Solicitar una reserva",
            description = "Queda PENDIENTE hasta que el admin la apruebe. Si reserva un CLIENTE se usa su documento; "
                    + "un ADMIN puede reservar a nombre de un cliente registrado (ej. recepción) y, con "
                    + "?confirmar=true, dejarla CONFIRMADA de una vez.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reserva creada en estado PENDIENTE"),
        @ApiResponse(responseCode = "400", description = "Fechas inválidas o en el pasado"),
        @ApiResponse(responseCode = "404", description = "La habitación o el cliente no existen"),
        @ApiResponse(responseCode = "409", description = "Habitación ocupada en esas fechas o en mantenimiento")
    })
    @PostMapping
    public ResponseEntity<ReservaHotelDto> crear(
            @Valid @RequestBody ReservaHotelDto dto,
            @Parameter(description = "Solo ADMIN: registrar la reserva ya CONFIRMADA (huésped presente en recepción). "
                    + "Para un CLIENTE se ignora.")
            @RequestParam(defaultValue = "false") boolean confirmar,
            Authentication authentication) {
        if (!AutenticacionUtils.esAdmin(authentication)) {
            dto.setDocUsuario(documentoDe(authentication));
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto, true, confirmar));
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
    public ResponseEntity<List<ReservaHotelDto>> misReservas(Authentication authentication) {
        return ResponseEntity.ok(service.obtenerPorUsuario(documentoDe(authentication)));
    }

    @Operation(summary = "Obtener una reserva", description = "Solo el titular o un ADMIN.")
    @GetMapping("/{id}")
    public ResponseEntity<ReservaHotelDto> obtenerPorId(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(service.obtenerPorId(id, documentoDe(authentication), AutenticacionUtils.esAdmin(authentication)));
    }

    @Operation(summary = "Reservas de hotel de una reserva padre", description = "Un CLIENTE solo ve las suyas.")
    @GetMapping("/reserva/{idReserva}")
    public ResponseEntity<List<ReservaHotelDto>> obtenerPorReserva(@PathVariable String idReserva, Authentication authentication) {
        return ResponseEntity.ok(service.obtenerPorReserva(idReserva, documentoDe(authentication), AutenticacionUtils.esAdmin(authentication)));
    }

    @Operation(summary = "Fechas ocupadas de una habitación",
            description = "Rangos con reservas no canceladas, para bloquearlos en el calendario.")
    @GetMapping("/habitacion/{idHabitacion}/ocupadas")
    public ResponseEntity<List<RangoOcupadoDto>> obtenerFechasOcupadas(@PathVariable String idHabitacion) {
        return ResponseEntity.ok(service.obtenerFechasOcupadas(idHabitacion));
    }

    @Operation(summary = "Actualizar reserva",
            description = "Sin campos editables por ahora: fechas y precio no se cambian sin revalidar disponibilidad.")
    @PutMapping("/{id}")
    public ResponseEntity<ReservaHotelDto> actualizar(@PathVariable String id,
                                                      @Valid @RequestBody ReservaHotelDto dto,
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
    public ResponseEntity<ReservaHotelDto> confirmar(@PathVariable String id, Authentication authentication) {
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
    public ResponseEntity<ReservaHotelDto> cancelar(@PathVariable String id,
                                                    @Valid @RequestBody(required = false) CancelacionReservaDto cancelacion,
                                                    Authentication authentication) {
        String motivo = cancelacion != null ? cancelacion.getMotivo() : null;
        return ResponseEntity.ok(service.cancelar(id, documentoDe(authentication),
                AutenticacionUtils.esAdmin(authentication), motivo));
    }

    @Operation(summary = "Reprogramar (cambiar la fecha sin cancelar)",
            description = "Body: día de check-in (inicio) y de check-out (fin); la hora se ajusta a 3:00 p. m. y 12:00 m.. Se validan las mismas reglas que al reservar y se recalcula el precio. "
                    + "CLIENTE: solo la suya, con 24 h de anticipación; si estaba CONFIRMADA vuelve a PENDIENTE. "
                    + "ADMIN: cualquiera; el cliente recibe un correo y una notificación.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva reprogramada"),
        @ApiResponse(responseCode = "400", description = "Fechas inválidas o iguales a las actuales"),
        @ApiResponse(responseCode = "403", description = "La reserva es de otro usuario"),
        @ApiResponse(responseCode = "409", description = "Fechas ocupadas, reserva cancelada/finalizada o menos de 24 h (CLIENTE)")
    })
    @PatchMapping("/{id}/reprogramar")
    public ResponseEntity<ReservaHotelDto> reprogramar(@PathVariable String id,
                                                      @Valid @RequestBody ReprogramacionDto fechas,
                                                      Authentication authentication) {
        return ResponseEntity.ok(service.reprogramar(id, fechas.getInicio(), fechas.getFin(),
                documentoDe(authentication), AutenticacionUtils.esAdmin(authentication)));
    }

    @Operation(summary = "Actualizar los acompañantes de la reserva",
            description = "Reemplaza la lista completa (nombre, tipo y número de documento; los menores con TI). "
                    + "Dueño o ADMIN, en reservas pendientes o confirmadas y sin superar la capacidad.")
    @PatchMapping("/{id}/miembros")
    public ResponseEntity<ReservaHotelDto> actualizarMiembros(@PathVariable String id, @Valid @RequestBody MiembrosDto cuerpo,
                                                          Authentication authentication) {
        return ResponseEntity.ok(service.actualizarMiembros(id, cuerpo.getMiembros(),
                documentoDe(authentication), AutenticacionUtils.esAdmin(authentication)));
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
