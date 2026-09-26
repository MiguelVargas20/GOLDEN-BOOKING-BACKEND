package com.sena.goldenbooking.habitaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import com.sena.goldenbooking.compartido.exception.RecursoNoEncontradoException;
import com.sena.goldenbooking.compartido.imagenes.AlmacenImagenes;
import com.sena.goldenbooking.habitaciones.dto.HabitacionDto;
import com.sena.goldenbooking.habitaciones.mapper.HabitacionMapperImpl;
import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.habitaciones.repository.HabitacionRepository;

/** Imagen de una habitación: subir, reemplazar, quitar y borrar al eliminar. */
class ImagenHabitacionTest {

    private HabitacionRepository repo;
    private AlmacenImagenes imagenes;
    private HabitacionServiceImpl service;
    private Habitacion hab;

    @BeforeEach
    void setUp() {
        repo = mock(HabitacionRepository.class);
        imagenes = mock(AlmacenImagenes.class);
        service = new HabitacionServiceImpl(repo, new HabitacionMapperImpl(), imagenes,
                mock(com.sena.goldenbooking.habitaciones.repository.TipoHabitacionRepository.class));
        hab = Habitacion.builder().id("h1").numHab("101").imagenId("vieja").build();
        when(repo.findById("h1")).thenReturn(Optional.of(hab));
        when(repo.save(any(Habitacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void subirReemplazaYBorraLaImagenAnterior() {
        MultipartFile archivo = mock(MultipartFile.class);
        when(imagenes.guardar(archivo, "habitacion-h1")).thenReturn("nueva");

        HabitacionDto dto = service.subirImagen("h1", archivo);

        assertEquals("/api/habitaciones/h1/imagen?v=nueva", dto.getImagenUrl());
        verify(imagenes).borrar("vieja");
    }

    @Test
    void quitarLaImagenDejaLaUrlVacia() {
        HabitacionDto dto = service.eliminarImagen("h1");
        assertNull(dto.getImagenUrl());
        verify(imagenes).borrar("vieja");
    }

    @Test
    void alEliminarLaHabitacionSeBorraSuImagen() {
        service.eliminar("h1");
        verify(repo).deleteById("h1");
        verify(imagenes).borrar("vieja");
    }

    @Test
    void sinImagenPropiaResponde404() {
        hab.setImagenId(null);
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerImagen("h1"));
    }

    // ── Galería (hasta 5) ──────────────────────────────────────────────────

    @Test
    void laGaleriaIncluyeLaImagenAntiguaComoPortada() {
        MultipartFile archivo = mock(MultipartFile.class);
        when(imagenes.guardar(archivo, "habitacion-h1")).thenReturn("g2");

        HabitacionDto dto = service.agregarImagen("h1", archivo);

        assertEquals(2, dto.getImagenes().size());
        assertEquals("vieja", dto.getImagenes().get(0).id());
        assertEquals("/api/habitaciones/h1/imagenes/g2", dto.getImagenes().get(1).url());
    }

    @Test
    void noAdmiteMasDeCincoImagenes() {
        hab.guardarGaleria(java.util.List.of("a", "b", "c", "d", "e"));
        assertThrows(com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException.class,
                () -> service.agregarImagen("h1", mock(MultipartFile.class)));
    }

    @Test
    void elegirPortadaYQuitarUnaImagen() {
        hab.guardarGaleria(java.util.List.of("a", "b", "c"));

        HabitacionDto dto = service.elegirPortada("h1", "c");
        assertEquals("c", dto.getImagenes().get(0).id());
        assertEquals("/api/habitaciones/h1/imagen?v=c", dto.getImagenUrl());

        dto = service.quitarImagen("h1", "a");
        assertEquals(2, dto.getImagenes().size());
        verify(imagenes).borrar("a");
        assertThrows(RecursoNoEncontradoException.class, () -> service.quitarImagen("h1", "otra"));
    }

    @Test
    void soloSeSirvenImagenesDeLaPropiaHabitacion() {
        hab.guardarGaleria(java.util.List.of("a"));
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerImagenGaleria("h1", "ajena"));
    }

    @Test
    void alEliminarLaHabitacionSeBorranTodasSusImagenes() {
        hab.guardarGaleria(java.util.List.of("a", "b"));
        service.eliminar("h1");
        verify(imagenes).borrar("a");
        verify(imagenes).borrar("b");
    }
}
