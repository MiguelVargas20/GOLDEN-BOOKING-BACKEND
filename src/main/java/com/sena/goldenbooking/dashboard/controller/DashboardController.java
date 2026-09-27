package com.sena.goldenbooking.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sena.goldenbooking.dashboard.dto.DashboardDto;
import com.sena.goldenbooking.dashboard.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Panel de control del administrador (solo ADMIN, ver SecurityConfig). */
@Tag(name = "Dashboard", description = "Resumen operativo para el administrador.")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @Operation(summary = "Resumen del día (ADMIN)",
            description = "Indicadores, agenda de hoy, reservas pendientes, estado de las habitaciones, "
                    + "reservas recibidas por día y espacios deportivos más reservados.")
    @GetMapping
    public ResponseEntity<DashboardDto> resumen(
            @Parameter(description = "Días de la tendencia y del ranking de espacios (7 a 90).", example = "14")
            @RequestParam(defaultValue = "14") int dias) {
        return ResponseEntity.ok(service.generar(dias));
    }
}
