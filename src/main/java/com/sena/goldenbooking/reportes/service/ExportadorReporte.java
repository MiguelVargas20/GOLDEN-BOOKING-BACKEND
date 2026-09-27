package com.sena.goldenbooking.reportes.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sena.goldenbooking.reportes.dto.FilaReporteDto;
import com.sena.goldenbooking.reportes.dto.ReporteDto;
import com.sena.goldenbooking.reportes.dto.ResumenReporteDto;

/** Convierte el reporte en un archivo Excel (.xlsx) o PDF. */
@Component
public class ExportadorReporte {

    private static final Locale ES_CO = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", ES_CO);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", ES_CO);
    private static final String[] COLUMNAS =
            {"Tipo", "Cliente", "Documento", "Lugar", "Inicio", "Fin", "Estado", "Total", "Solicitada", "Recepción"};
    private static final Map<String, String> ESTADOS = Map.of(
            "PENDIENTE", "Pendiente", "CONFIRMADA", "Confirmada", "CANCELADA", "Cancelada", "FINALIZADA", "Finalizada");
    private static final Color NARANJA = new Color(0xF3, 0x8D, 0x1E);

    // ═══════════════════════════════ Excel ═══════════════════════════════

    public byte[] excel(ReporteDto reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle titulo = estiloTitulo(libro);
            CellStyle encabezado = estiloEncabezado(libro);
            CellStyle moneda = libro.createCellStyle();
            moneda.setDataFormat(libro.createDataFormat().getFormat("\"$\"#,##0"));

            // Hoja 1: resumen
            Sheet resumen = libro.createSheet("Resumen");
            fila(resumen, 0).createCell(0).setCellValue("Reporte de reservas e ingresos — Golden Booking");
            resumen.getRow(0).getCell(0).setCellStyle(titulo);
            fila(resumen, 1).createCell(0).setCellValue(rango(reporte));
            ResumenReporteDto r = reporte.resumen();
            Object[][] datos = {
                {"Reservas en el periodo", r.totalReservas()},
                {"Reservas deportivas", r.reservasDeporte()},
                {"Reservas hoteleras", r.reservasHotel()},
                {"Pendientes", r.porEstado().getOrDefault("PENDIENTE", 0L)},
                {"Confirmadas", r.porEstado().getOrDefault("CONFIRMADA", 0L)},
                {"Finalizadas", r.porEstado().getOrDefault("FINALIZADA", 0L)},
                {"Canceladas", r.porEstado().getOrDefault("CANCELADA", 0L)},
                {"Ingresos deportes", r.ingresosDeporte()},
                {"Ingresos hotel", r.ingresosHotel()},
                {"Ingresos totales", r.ingresos()},
            };
            for (int i = 0; i < datos.length; i++) {
                Row fila = fila(resumen, i + 3);
                fila.createCell(0).setCellValue((String) datos[i][0]);
                Cell valor = fila.createCell(1);
                if (datos[i][1] instanceof Double d) {
                    valor.setCellValue(d);
                    valor.setCellStyle(moneda);
                } else {
                    valor.setCellValue(((Number) datos[i][1]).doubleValue());
                }
            }
            fila(resumen, datos.length + 4).createCell(0)
                    .setCellValue("Los ingresos cuentan las reservas confirmadas y finalizadas.");
            resumen.setColumnWidth(0, 32 * 256);
            resumen.setColumnWidth(1, 18 * 256);

            // Hoja 2: detalle
            Sheet detalle = libro.createSheet("Reservas");
            Row cabecera = fila(detalle, 0);
            for (int i = 0; i < COLUMNAS.length; i++) {
                Cell c = cabecera.createCell(i);
                c.setCellValue(COLUMNAS[i]);
                c.setCellStyle(encabezado);
            }
            List<FilaReporteDto> filas = reporte.filas();
            for (int i = 0; i < filas.size(); i++) {
                FilaReporteDto f = filas.get(i);
                Row fila = fila(detalle, i + 1);
                fila.createCell(0).setCellValue(tipo(f.categoria()));
                fila.createCell(1).setCellValue(texto(f.cliente()));
                fila.createCell(2).setCellValue(texto(f.documento()));
                fila.createCell(3).setCellValue(texto(f.lugar()));
                fila.createCell(4).setCellValue(fecha(f.inicio(), f.categoria()));
                fila.createCell(5).setCellValue(fecha(f.fin(), f.categoria()));
                fila.createCell(6).setCellValue(ESTADOS.getOrDefault(f.estado(), f.estado()));
                Cell total = fila.createCell(7);
                total.setCellValue(f.total() != null ? f.total() : 0);
                total.setCellStyle(moneda);
                fila.createCell(8).setCellValue(f.solicitada() != null ? f.solicitada().format(FECHA_HORA) : "");
                fila.createCell(9).setCellValue(f.registradaEnRecepcion() ? "Sí" : "No");
            }
            int[] anchos = {10, 26, 14, 24, 18, 18, 12, 14, 18, 11};
            for (int i = 0; i < anchos.length; i++) detalle.setColumnWidth(i, anchos[i] * 256);
            detalle.createFreezePane(0, 1);
            if (!filas.isEmpty()) {
                detalle.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, filas.size(), 0, COLUMNAS.length - 1));
            }

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel del reporte.", e);
        }
    }

    private static Row fila(Sheet hoja, int indice) {
        Row fila = hoja.getRow(indice);
        return fila != null ? fila : hoja.createRow(indice);
    }

    private static CellStyle estiloTitulo(XSSFWorkbook libro) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 14);
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        return estilo;
    }

    private static CellStyle estiloEncabezado(XSSFWorkbook libro) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setBorderBottom(BorderStyle.THIN);
        return estilo;
    }

    // ════════════════════════════════ PDF ════════════════════════════════

    public byte[] pdf(ReporteDto reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter.getInstance(documento, salida);
        documento.open();

        com.lowagie.text.Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        com.lowagie.text.Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
        com.lowagie.text.Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        com.lowagie.text.Font blanca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

        documento.add(new Paragraph("Reporte de reservas e ingresos — Golden Booking", titulo));
        documento.add(new Paragraph(rango(reporte) + " · generado el " + reporte.generadoEn().format(FECHA_HORA), normal));
        documento.add(new Paragraph(" "));

        ResumenReporteDto r = reporte.resumen();
        PdfPTable resumen = new PdfPTable(4);
        resumen.setWidthPercentage(60);
        resumen.setHorizontalAlignment(Element.ALIGN_LEFT);
        celdaResumen(resumen, "Reservas", String.valueOf(r.totalReservas()), negrita, normal);
        celdaResumen(resumen, "Ingresos totales", pesos(r.ingresos()), negrita, normal);
        celdaResumen(resumen, "Deportivas", r.reservasDeporte() + " · " + pesos(r.ingresosDeporte()), negrita, normal);
        celdaResumen(resumen, "Hoteleras", r.reservasHotel() + " · " + pesos(r.ingresosHotel()), negrita, normal);
        celdaResumen(resumen, "Confirmadas / finalizadas",
                r.porEstado().getOrDefault("CONFIRMADA", 0L) + " / " + r.porEstado().getOrDefault("FINALIZADA", 0L), negrita, normal);
        celdaResumen(resumen, "Pendientes / canceladas",
                r.porEstado().getOrDefault("PENDIENTE", 0L) + " / " + r.porEstado().getOrDefault("CANCELADA", 0L), negrita, normal);
        documento.add(resumen);
        documento.add(new Paragraph("Los ingresos cuentan las reservas confirmadas y finalizadas.", normal));
        documento.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(new float[] {7, 16, 10, 15, 12, 12, 9, 10, 12, 7});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        for (String columna : COLUMNAS) {
            PdfPCell c = new PdfPCell(new Phrase(columna, blanca));
            c.setBackgroundColor(NARANJA);
            c.setPadding(4);
            tabla.addCell(c);
        }
        if (reporte.filas().isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase("No hay reservas en este periodo.", normal));
            vacia.setColspan(COLUMNAS.length);
            vacia.setPadding(6);
            tabla.addCell(vacia);
        }
        for (FilaReporteDto f : reporte.filas()) {
            for (String valor : new String[] {
                    tipo(f.categoria()), texto(f.cliente()), texto(f.documento()), texto(f.lugar()),
                    fecha(f.inicio(), f.categoria()), fecha(f.fin(), f.categoria()),
                    ESTADOS.getOrDefault(f.estado(), f.estado()), pesos(f.total()),
                    f.solicitada() != null ? f.solicitada().format(FECHA_HORA) : "", f.registradaEnRecepcion() ? "Sí" : "No"}) {
                PdfPCell c = new PdfPCell(new Phrase(valor, normal));
                c.setPadding(3);
                tabla.addCell(c);
            }
        }
        documento.add(tabla);
        documento.close();
        return salida.toByteArray();
    }

    private static void celdaResumen(PdfPTable tabla, String etiqueta, String valor,
                                     com.lowagie.text.Font negrita, com.lowagie.text.Font normal) {
        PdfPCell e = new PdfPCell(new Phrase(etiqueta, negrita));
        e.setPadding(4);
        tabla.addCell(e);
        PdfPCell v = new PdfPCell(new Phrase(valor, normal));
        v.setPadding(4);
        tabla.addCell(v);
    }

    // ═════════════════════════════ Utilidades ════════════════════════════

    private static String rango(ReporteDto reporte) {
        return "Del " + reporte.desde().format(FECHA) + " al " + reporte.hasta().format(FECHA);
    }

    private static String tipo(String categoria) {
        return "HOTEL".equals(categoria) ? "Hotel" : "Deporte";
    }

    /** Hotel: solo el día (la hora de check-in/out es fija); deporte: día y hora. */
    private static String fecha(LocalDateTime valor, String categoria) {
        if (valor == null) return "";
        return "HOTEL".equals(categoria) ? valor.format(FECHA) : valor.format(FECHA_HORA);
    }

    private static String pesos(Double valor) {
        return valor == null ? "—" : String.format(ES_CO, "$%,.0f", valor);
    }

    private static String texto(String valor) {
        return valor != null ? valor : "";
    }
}
