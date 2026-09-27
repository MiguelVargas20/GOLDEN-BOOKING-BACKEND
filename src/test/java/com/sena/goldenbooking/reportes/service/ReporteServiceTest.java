package com.sena.goldenbooking.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.reportes.dto.ReporteDto;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

/** Reporte de reservas e ingresos y su exportación a Excel y PDF. */
class ReporteServiceTest {

    private final LocalDate desde = LocalDate.of(2026, 9, 1);
    private final LocalDate hasta = LocalDate.of(2026, 9, 30);
    private ReservaDeporteRepository reservaDeporteRepo;
    private ReservaHotelRepository reservaHotelRepo;
    private ReporteService service;

    @BeforeEach
    void setUp() {
        reservaDeporteRepo = mock(ReservaDeporteRepository.class);
        reservaHotelRepo = mock(ReservaHotelRepository.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        service = new ReporteService(reservaDeporteRepo, reservaHotelRepo, usuarioService);

        // "hasta" queda incluido: la consulta llega hasta el día siguiente a medianoche
        when(reservaDeporteRepo.findByFechaReservaGreaterThanEqualAndFechaReservaLessThan(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())).thenReturn(List.of(
                deporte("rd1", EstadoReserva.CONFIRMADA, 40000, 10),
                deporte("rd2", EstadoReserva.CANCELADA, 60000, 5),
                deporte("rd3", EstadoReserva.PENDIENTE, 50000, 20)));
        when(reservaHotelRepo.findByFechaCheckInGreaterThanEqualAndFechaCheckInLessThan(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())).thenReturn(List.of(
                ReservaHotel.builder().idHotelReserva("rh1").docUsuario("123").estado(EstadoReserva.FINALIZADA)
                        .datosH(Habitacion.builder().numHab("101").build()).precioTotal(360000.0)
                        .fechaCheckIn(desde.plusDays(2).atTime(15, 0)).fechaCheckOut(desde.plusDays(4).atTime(12, 0)).build()));
        when(usuarioService.obtenerMapaPorDocNums(anyList())).thenReturn(
                Map.of("123", UsuarioDto.builder().nombre("Laura").apellido("Pérez").build()));
    }

    private ReservaDeporte deporte(String id, EstadoReserva estado, double precio, int dia) {
        return ReservaDeporte.builder().idReservaDeporte(id).docUsuario("123").tipoCancha("Cancha 1").estado(estado)
                .precio(precio).fechaReserva(desde.plusDays(dia).atTime(10, 0)).fechaFinReserva(desde.plusDays(dia).atTime(11, 0))
                .build();
    }

    @Test
    void sumaSoloIngresosDeReservasConfirmadasYFinalizadas() {
        ReporteDto r = service.generar(desde, hasta);

        assertEquals(4, r.resumen().totalReservas());
        assertEquals(3, r.resumen().reservasDeporte());
        assertEquals(40000.0, r.resumen().ingresosDeporte());
        assertEquals(360000.0, r.resumen().ingresosHotel());
        assertEquals(400000.0, r.resumen().ingresos());
        assertEquals(1L, r.resumen().porEstado().get("CANCELADA"));
        assertEquals(1L, r.resumen().porEstado().get("PENDIENTE"));
    }

    @Test
    void ordenaLasFilasPorFechaDeInicio() {
        ReporteDto r = service.generar(desde, hasta);

        assertEquals("rh1", r.filas().get(0).idReserva());
        assertEquals("Habitación 101", r.filas().get(0).lugar());
        assertEquals("Laura Pérez", r.filas().get(0).cliente());
        assertEquals("rd3", r.filas().get(3).idReserva());
    }

    @Test
    void validaElRango() {
        assertThrows(SolicitudInvalidaException.class, () -> service.generar(hasta, desde));
        assertThrows(SolicitudInvalidaException.class, () -> service.generar(desde, desde.plusYears(2)));
        assertThrows(SolicitudInvalidaException.class, () -> service.generar(null, hasta));
    }

    @Test
    void exportaUnExcelConResumenYDetalle() throws Exception {
        byte[] archivo = new ExportadorReporte().excel(service.generar(desde, hasta));

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            assertEquals("Resumen", libro.getSheetAt(0).getSheetName());
            Sheet detalle = libro.getSheet("Reservas");
            assertEquals("Cliente", detalle.getRow(0).getCell(1).getStringCellValue());
            assertEquals(4, detalle.getLastRowNum()); // encabezado + 4 reservas
            assertEquals("Hotel", detalle.getRow(1).getCell(0).getStringCellValue());
            assertEquals(360000.0, detalle.getRow(1).getCell(7).getNumericCellValue());
            assertEquals("Finalizada", detalle.getRow(1).getCell(6).getStringCellValue());
        }
    }

    @Test
    void exportaUnPdfValido() {
        byte[] archivo = new ExportadorReporte().pdf(service.generar(desde, hasta));

        assertTrue(archivo.length > 1000);
        assertEquals("%PDF", new String(archivo, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void elPdfSeGeneraAunqueNoHayaReservas() {
        when(reservaDeporteRepo.findByFechaReservaGreaterThanEqualAndFechaReservaLessThan(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())).thenReturn(List.of());
        when(reservaHotelRepo.findByFechaCheckInGreaterThanEqualAndFechaCheckInLessThan(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())).thenReturn(List.of());

        ReporteDto vacio = service.generar(desde, hasta);

        assertEquals(0, vacio.resumen().totalReservas());
        assertEquals("%PDF", new String(new ExportadorReporte().pdf(vacio), 0, 4, StandardCharsets.US_ASCII));
    }
}
