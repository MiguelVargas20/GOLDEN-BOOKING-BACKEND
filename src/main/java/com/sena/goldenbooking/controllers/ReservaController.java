package com.sena.goldenbooking.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sena.goldenbooking.dtos.ReservaDto;
import com.sena.goldenbooking.models.EstadoReserva;
import com.sena.goldenbooking.services.ReservaService;

/**
 * Controlador REST para gestionar reservas de hoteles y deportes.
 * Proporciona endpoints para crear, listar, actualizar y eliminar reservas.
 * Soporta filtrado por cliente, estado y obtención de detalles por ID.
 */
@RestController


// Permitir solicitudes desde cualquier origen (CORS)
@RequestMapping("/api/reservas")

// Permitir solicitudes desde cualquier origen (CORS)
@CrossOrigin(origins = "*")

// Anotación para marcar esta clase como un controlador REST
public class ReservaController {

    // Inyección de la capa de servicio para manejar la lógica de negocio relacionada con reservas
    private final ReservaService reservaService;


    // Constructor para inyectar la dependencia del servicio de reservas
    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    /**
     * 1. CREAR RESERVA
     * El JSON debe llevar detalleHotel o detalleDeporte (el otro en null).
     */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ReservaDto reservaDto) {
        try {

            // Intentar crear la reserva utilizando el servicio. Si hay un error (e.g., datos inválidos), se lanzará una RuntimeException.
            ReservaDto nueva = reservaService.crearReserva(reservaDto);
            return new ResponseEntity<>(nueva, HttpStatus.CREATED);

            // Si ocurre una RuntimeException, capturamos el mensaje de error y devolvemos un BAD_REQUEST con ese mensaje.
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 2. BUSCAR POR DOCUMENTO DEL CLIENTE
     * Ideal para que el cliente vea "Mis Reservas" sin saber el ID de MongoDB.
     */
    @GetMapping("/cliente/{documento}")
    public ResponseEntity<List<ReservaDto>> listarPorDocumentoCliente(@PathVariable String documento) {
        List<ReservaDto> reservas = reservaService.listarPorCliente(documento);
        if (reservas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(reservas);
    }

    /**
     * 3. LISTAR TODAS (Vista de ADMIN)
     */
    @GetMapping
    public ResponseEntity<List<ReservaDto>> listarTodas() {
        return ResponseEntity.ok(reservaService.listarTodas());
    }

    /**
     * 4. OBTENER DETALLE POR ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservaDto> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    /**
     * 5. FILTRAR POR ESTADO
     * Soporta: PENDIENTE, CONFIRMADA, CANCELADA, FINALIZADA.
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ReservaDto>> listarPorEstado(@PathVariable EstadoReserva estado) {
        return ResponseEntity.ok(reservaService.listarPorEstado(estado));
    }

    /**
     * 6. ACTUALIZAR TOTAL (PUT)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReservaDto> actualizar(@PathVariable String id, @RequestBody ReservaDto dto) {
        return ResponseEntity.ok(reservaService.actualizarReserva(id, dto));
    }

    /**
     * 7. CAMBIO DE ESTADO RÁPIDO (PATCH)
     * Ejemplo: /api/reservas/ID_AQUÍ/estado?nuevoEstado=CONFIRMADA
     */
    @PatchMapping("/{id}/estado")

    // El nuevo estado se recibe como un parámetro de consulta (query parameter) llamado "nuevoEstado".
    public ResponseEntity<ReservaDto> cambiarEstado(

        // El ID de la reserva se recibe como parte de la URL, y el nuevo estado se recibe como un parámetro de consulta.
            @PathVariable String id, 
            @RequestParam EstadoReserva nuevoEstado) {
        return ResponseEntity.ok(reservaService.cambiarEstado(id, nuevoEstado));
    }

    /**
     * 8. ELIMINAR RESERVA
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        reservaService.eliminarReserva(id);
        return ResponseEntity.noContent().build();
    }
}