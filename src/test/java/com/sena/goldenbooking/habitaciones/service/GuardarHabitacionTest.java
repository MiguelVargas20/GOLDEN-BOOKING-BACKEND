package com.sena.goldenbooking.habitaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.compartido.imagenes.AlmacenImagenes;
import com.sena.goldenbooking.habitaciones.dto.HabitacionDto;
import com.sena.goldenbooking.habitaciones.mapper.HabitacionMapperImpl;
import com.sena.goldenbooking.habitaciones.model.EstadoHabitacion;
import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.habitaciones.model.TipoHabitacion;
import com.sena.goldenbooking.habitaciones.repository.HabitacionRepository;
import com.sena.goldenbooking.habitaciones.repository.TipoHabitacionRepository;

/** Crear / editar habitaciones: validaciones y tipo tomado de la base de datos. */
class GuardarHabitacionTest {

    private HabitacionRepository repo;
    private HabitacionServiceImpl service;
    private final TipoHabitacion suite = TipoHabitacion.builder().id("t1").nomTipo("Suite").desc("Vista al mar").cap(4).build();

    @BeforeEach
    void setUp() {
        repo = mock(HabitacionRepository.class);
        TipoHabitacionRepository tipoRepo = mock(TipoHabitacionRepository.class);
        service = new HabitacionServiceImpl(repo, new HabitacionMapperImpl(), mock(AlmacenImagenes.class), tipoRepo);
        when(tipoRepo.findById("t1")).thenReturn(Optional.of(suite));
        when(repo.findAllByNumHab(any())).thenReturn(List.of());
        when(repo.save(any(Habitacion.class))).thenAnswer(inv -> {
            Habitacion h = inv.getArgument(0);
            if (h.getId() == null) h.setId("nueva");
            return h;
        });
    }

    private static HabitacionDto dto(String numero, String idTipo) {
        return HabitacionDto.builder().numeroHabitacion(numero).precioNoche(180000.0)
                .datosTipoHabitacion(TipoHabitacion.builder().id(idTipo).build()).build();
    }

    @Test
    void elTipoSeCompletaDesdeLaBaseAunqueElNavegadorSoloMandeElId() {
        HabitacionDto creada = service.crear(dto(" 101 ", "t1"));
        assertEquals("101", creada.getNumeroHabitacion());
        assertEquals("Suite", creada.getDatosTipoHabitacion().getNomTipo());
        assertEquals(4, creada.getDatosTipoHabitacion().getCap());
        assertEquals(EstadoHabitacion.DISPONIBLE, creada.getEstadoHabitacion());
    }

    @Test
    void rechazaTipoInexistenteNumeroRepetidoYPrecioInvalido() {
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(dto("101", "no-existe")));
        when(repo.findAllByNumHab("101")).thenReturn(List.of(Habitacion.builder().id("otra").build()));
        assertThrows(ConflictoDeNegocioException.class, () -> service.crear(dto("101", "t1")));
        HabitacionDto sinPrecio = dto("102", "t1");
        sinPrecio.setPrecioNoche(0.0);
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(sinPrecio));
    }

    @Test
    void editarNoChocaConSuPropioNumeroNiBorraLaImagen() {
        Habitacion existente = Habitacion.builder().id("h1").numHab("101").imagenId("img").build();
        when(repo.findById("h1")).thenReturn(Optional.of(existente));
        when(repo.findAllByNumHab("101")).thenReturn(List.of(existente));

        HabitacionDto editada = service.actualizar("h1", dto("101", "t1"));

        assertEquals("/api/habitaciones/h1/imagen?v=img", editada.getImagenUrl());
    }
}
