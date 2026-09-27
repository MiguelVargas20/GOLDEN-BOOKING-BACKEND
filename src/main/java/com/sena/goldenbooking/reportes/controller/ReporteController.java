package com.sena.goldenbooking.reportes.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sena.goldenbooking.reportes.dto.ReporteDto;
import com.sena.goldenbooking.reportes.service.ExportadorReporte;
import com.sena.goldenbooking.reportes.service.ReporteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Reportes", description = "Reservas e ingresos por rango de fechas, en pantalla, Excel o PDF (ADMIN).")
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final MediaType EXCEL =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService service;
    private final ExportadorReporte exportador;

    public ReporteController(ReporteService service, ExportadorReporte exportador) {
        this.service = service;
        this.exportador = exportador;
    }

    @Operation(summary = "Reporte en JSON (vista previa)",
            description = "Reservas cuya fecha de inicio o check-in está entre 'desde' y 'hasta' (ambos incluidos).")
    @GetMapping
    public ResponseEntity<ReporteDto> ver(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.generar(desde, hasta));
    }

    @Operation(summary = "Descargar el reporte en Excel (.xlsx)")
    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return archivo(exportador.excel(service.generar(desde, hasta)), EXCEL, nombre(desde, hasta, "xlsx"));
    }

    @Operation(summary = "Descargar el reporte en PDF")
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return archivo(exportador.pdf(service.generar(desde, hasta)), MediaType.APPLICATION_PDF, nombre(desde, hasta, "pdf"));
    }

    private static String nombre(LocalDate desde, LocalDate hasta, String extension) {
        return "reporte-reservas-" + desde + "-a-" + hasta + "." + extension;
    }

    private static ResponseEntity<byte[]> archivo(byte[] contenido, MediaType tipo, String nombre) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(contenido);
    }
}
