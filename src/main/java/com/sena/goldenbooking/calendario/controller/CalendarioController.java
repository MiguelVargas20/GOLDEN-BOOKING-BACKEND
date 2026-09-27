package com.sena.goldenbooking.calendario.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sena.goldenbooking.calendario.dto.CalendarioSemanaDto;
import com.sena.goldenbooking.calendario.service.CalendarioService;
import com.sena.goldenbooking.compartido.config.ZonaHoraria;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Calendario", description = "Ocupación semanal de espacios y habitaciones (ADMIN).")
@RestController
@RequestMapping("/api/calendario")
public class CalendarioController {

    private final CalendarioService service;

    public CalendarioController(CalendarioService service) {
        this.service = service;
    }

    @Operation(summary = "Ocupación de una semana",
            description = "Reservas no canceladas de cada espacio deportivo y habitación en los 7 días desde 'desde'.")
    @GetMapping("/semana")
    public ResponseEntity<CalendarioSemanaDto> semana(
            @Parameter(description = "Primer día (yyyy-MM-dd). Por defecto, hoy.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde) {
        return ResponseEntity.ok(service.semana(desde != null ? desde : ZonaHoraria.ahora().toLocalDate()));
    }
}
