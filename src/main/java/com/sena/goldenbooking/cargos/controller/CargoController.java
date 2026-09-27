package com.sena.goldenbooking.cargos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sena.goldenbooking.cargos.dto.CargoDto;
import com.sena.goldenbooking.cargos.dto.CuentaClienteDto;
import com.sena.goldenbooking.cargos.service.CargoService;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Cargos", description = "Consumos cargados a la reserva activa o a la cuenta de socio.")
@RestController
@RequestMapping("/api/cargos")
public class CargoController {

    private final CargoService service;
    private final UsuarioService usuarioService;

    public CargoController(CargoService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Mis consumos", description = "Consumos del cliente autenticado y su total pendiente.")
    @GetMapping("/mios")
    public ResponseEntity<CuentaClienteDto> mios(Authentication authentication) {
        return ResponseEntity.ok(service.miCuenta(usuarioService.obtenerDocumentoPorUsername(authentication.getName())));
    }

    @Operation(summary = "Estado de cuenta de un cliente (ADMIN)",
            description = "Consumos, total pendiente y las reservas activas o cuenta de socio a las que se puede cargar.")
    @GetMapping("/cuenta/{documento}")
    public ResponseEntity<CuentaClienteDto> cuenta(@PathVariable String documento) {
        return ResponseEntity.ok(service.cuenta(documento));
    }

    @Operation(summary = "Consumos pendientes de pago de todos los clientes (ADMIN)")
    @GetMapping("/pendientes")
    public ResponseEntity<List<CargoDto>> pendientes() {
        return ResponseEntity.ok(service.pendientes());
    }

    @Operation(summary = "Registrar un consumo (ADMIN)",
            description = "Se carga a una reserva confirmada que aún no termina o a la cuenta de socio. El cliente recibe una notificación.")
    @PostMapping
    public ResponseEntity<CargoDto> registrar(@Valid @RequestBody CargoDto dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(dto, authentication.getName()));
    }

    @Operation(summary = "Marcar un consumo como pagado (ADMIN)")
    @PatchMapping("/{id}/pagar")
    public ResponseEntity<CargoDto> pagar(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(service.pagar(id, authentication.getName()));
    }

    @Operation(summary = "Pagar los pendientes de un cliente (ADMIN)",
            description = "Con idReserva: los de esa reserva (check-out). Con soloCuentaSocio: los de la cuenta de socio (fin de mes). Sin nada: todos.")
    @PatchMapping("/cuenta/{documento}/pagar")
    public ResponseEntity<CargoService.ResumenPago> pagarPendientes(@PathVariable String documento,
                                                                    @RequestParam(required = false) String idReserva,
                                                                    @RequestParam(defaultValue = "false") boolean soloCuentaSocio,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(service.pagarPendientes(documento, idReserva, soloCuentaSocio, authentication.getName()));
    }

    @Operation(summary = "Eliminar un consumo registrado por error (ADMIN)", description = "Solo si aún no está pagado.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
