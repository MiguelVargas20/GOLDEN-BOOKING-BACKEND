package com.sena.goldenbooking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.sena.goldenbooking.dtos.EspacioDeportivoDto;
import com.sena.goldenbooking.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.mapper.EspacioDeportivoMapperImpl;
import com.sena.goldenbooking.models.EspacioDeportivo;
import com.sena.goldenbooking.models.EstadoEspacio;
import com.sena.goldenbooking.repositories.EspacioDeportivoRepository;
import com.sena.goldenbooking.repositories.ReservaDeporteRepository;

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

    @Test
    void cambiarEstadoSinEstadoEsSolicitudInvalida() {
        assertThrows(SolicitudInvalidaException.class, () -> service.cambiarEstado("e1", null));
        verify(repo, never()).findById(eq("e1"));
    }
}
