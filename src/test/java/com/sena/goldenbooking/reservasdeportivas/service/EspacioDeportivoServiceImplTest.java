package com.sena.goldenbooking.reservasdeportivas.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;

import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.reservasdeportivas.dto.EspacioDeportivoDto;
import com.sena.goldenbooking.reservasdeportivas.mapper.EspacioDeportivoMapperImpl;
import com.sena.goldenbooking.reservasdeportivas.model.EspacioDeportivo;
import com.sena.goldenbooking.reservasdeportivas.model.EstadoEspacio;
import com.sena.goldenbooking.reservasdeportivas.repository.EspacioDeportivoRepository;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;

class EspacioDeportivoServiceImplTest {

    private EspacioDeportivoRepository repo;
    private ReservaDeporteRepository reservaRepo;
    private GridFsTemplate gridFs;
    private EspacioDeportivoServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = mock(EspacioDeportivoRepository.class);
        reservaRepo = mock(ReservaDeporteRepository.class);
        gridFs = mock(GridFsTemplate.class);
        service = new EspacioDeportivoServiceImpl(repo, reservaRepo, new EspacioDeportivoMapperImpl(), gridFs);
        when(repo.save(any(EspacioDeportivo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private EspacioDeportivoDto dto(String nombre) {
        return EspacioDeportivoDto.builder()
                .nombre(nombre).deporte("Fútbol").capacidad(22).tarifaHora(50000.0)
                .horaApertura(LocalTime.of(6, 0)).horaCierre(LocalTime.of(22, 0))
                .build();
    }

    private void existe(EstadoEspacio estado) {
        when(repo.findById("e1")).thenReturn(Optional.of(
                EspacioDeportivo.builder().id("e1").nombre("Cancha 1").estado(estado).build()));
    }

    @Test
    void creaActivoPorDefectoYLimpiaEspaciosDelNombre() {
        EspacioDeportivoDto creado = service.crear(dto("  Cancha   de Fútbol 1 "));

        assertEquals(EstadoEspacio.ACTIVO, creado.getEstado());
        assertEquals("Cancha de Fútbol 1", creado.getNombre());
        assertNull(creado.getImagenUrl());
    }

    @Test
    void rechazaNombreRepetido() {
        when(repo.existsByNombreIgnoreCase("Cancha 1")).thenReturn(true);

        assertThrows(ConflictoDeNegocioException.class, () -> service.crear(dto("Cancha 1")));
        verify(repo, never()).save(any());
    }

    @Test
    void rechazaHorarioConCierreAntesDeApertura() {
        EspacioDeportivoDto d = dto("Cancha 1");
        d.setHoraCierre(LocalTime.of(5, 0));

        assertThrows(SolicitudInvalidaException.class, () -> service.crear(d));
    }

    @Test
    void enMantenimientoNoSePuedeReservar() {
        existe(EstadoEspacio.MANTENIMIENTO);
        assertThrows(ConflictoDeNegocioException.class, () -> service.obtenerReservable("e1"));
    }

    @Test
    void inactivoNoSePuedeReservar() {
        existe(EstadoEspacio.INACTIVO);
        assertThrows(ConflictoDeNegocioException.class, () -> service.obtenerReservable("e1"));
    }

    @Test
    void noSeEliminaSiTieneHistorialDeReservas() {
        existe(EstadoEspacio.ACTIVO);
        when(reservaRepo.existsByEspacioId("e1")).thenReturn(true);

        assertThrows(ConflictoDeNegocioException.class, () -> service.eliminar("e1"));
        verify(repo, never()).delete(any());
    }

    @Test
    void rechazaArchivoQueNoEsImagenAunqueDigaSerPng() {
        existe(EstadoEspacio.ACTIVO);
        MockMultipartFile falso = new MockMultipartFile("archivo", "foto.png", "image/png",
                "<html><script>alert(1)</script></html>".getBytes());

        assertThrows(SolicitudInvalidaException.class, () -> service.subirImagen("e1", falso));
        verify(gridFs, never()).store(any(java.io.InputStream.class), anyString(), anyString());
    }

    @Test
    void detectaTiposDeImagenPorSuFirma() {
        assertEquals("image/jpeg", EspacioDeportivoServiceImpl.detectarTipoImagen(
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 }));
        assertEquals("image/png", EspacioDeportivoServiceImpl.detectarTipoImagen(
                new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0 }));
        assertEquals("image/webp", EspacioDeportivoServiceImpl.detectarTipoImagen(
                "RIFF0000WEBPVP8 ".getBytes()));
        assertNull(EspacioDeportivoServiceImpl.detectarTipoImagen("GIF89a".getBytes()));
    }

    // ── Dimensiones de imagen ──────────────────────────────────────────────

    /** PNG o JPG real generado en memoria. */
    private static byte[] imagen(String formato, int ancho, int alto) throws Exception {
        java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(ancho, alto,
                java.awt.image.BufferedImage.TYPE_INT_RGB), formato, salida);
        return salida.toByteArray();
    }

    @Test
    void leeLasDimensionesDePngYJpgDesdeLaCabecera() throws Exception {
        assertArrayEquals(new int[] { 800, 600 },
                EspacioDeportivoServiceImpl.leerDimensiones(imagen("png", 800, 600), "image/png"));
        assertArrayEquals(new int[] { 1024, 768 },
                EspacioDeportivoServiceImpl.leerDimensiones(imagen("jpg", 1024, 768), "image/jpeg"));
    }

    @Test
    void leeLasDimensionesDeLosTresTiposDeWebp() {
        // VP8X: ancho-1 y alto-1 en 24 bits (1199 → 1200, 799 → 800)
        byte[] vp8x = new byte[30];
        System.arraycopy("RIFF\0\0\0\0WEBPVP8X".getBytes(), 0, vp8x, 0, 16);
        vp8x[24] = (byte) 0xAF; vp8x[25] = 0x04; // 1199
        vp8x[27] = 0x1F; vp8x[28] = 0x03;        // 799
        assertArrayEquals(new int[] { 1200, 800 }, EspacioDeportivoServiceImpl.leerDimensiones(vp8x, "image/webp"));

        // VP8 (con pérdida): firma 9D 01 2A y ancho/alto de 14 bits
        byte[] vp8 = new byte[30];
        System.arraycopy("RIFF\0\0\0\0WEBPVP8 ".getBytes(), 0, vp8, 0, 16);
        vp8[23] = (byte) 0x9D; vp8[24] = 0x01; vp8[25] = 0x2A;
        vp8[26] = (byte) 0x80; vp8[27] = 0x02; // 640
        vp8[28] = (byte) 0xE0; vp8[29] = 0x01; // 480
        assertArrayEquals(new int[] { 640, 480 }, EspacioDeportivoServiceImpl.leerDimensiones(vp8, "image/webp"));

        // VP8L (sin pérdida): firma 0x2F y 14 bits por lado (ancho-1, alto-1)
        byte[] vp8l = new byte[30];
        System.arraycopy("RIFF\0\0\0\0WEBPVP8L".getBytes(), 0, vp8l, 0, 16);
        vp8l[20] = 0x2F;
        int bits = (500 - 1) | ((400 - 1) << 14);
        vp8l[21] = (byte) bits; vp8l[22] = (byte) (bits >> 8); vp8l[23] = (byte) (bits >> 16); vp8l[24] = (byte) (bits >> 24);
        assertArrayEquals(new int[] { 500, 400 }, EspacioDeportivoServiceImpl.leerDimensiones(vp8l, "image/webp"));
    }

    @Test
    void rechazaImagenDemasiadoPequena() throws Exception {
        existe(EstadoEspacio.ACTIVO);
        MockMultipartFile pequena = new MockMultipartFile("archivo", "logo.png", "image/png", imagen("png", 120, 80));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.subirImagen("e1", pequena));
        assertTrue(ex.getMessage().contains("muy pequeña"));
        verify(gridFs, never()).store(any(java.io.InputStream.class), anyString(), anyString());
    }

    @Test
    void rechazaArchivoDanadoConFirmaDeImagen() {
        existe(EstadoEspacio.ACTIVO);
        // Empieza como PNG pero el resto está corrupto
        byte[] danado = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3 };
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.png", "image/png", danado);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.subirImagen("e1", archivo));
        assertTrue(ex.getMessage().contains("dañada"));
    }

    @Test
    void cambiarEstadoSinEstadoEsSolicitudInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> service.cambiarEstado("e1", null));
        verify(repo, never()).findById(eq("e1"));
    }
}
