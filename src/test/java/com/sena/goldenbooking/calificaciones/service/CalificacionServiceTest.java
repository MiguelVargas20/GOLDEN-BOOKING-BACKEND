package com.sena.goldenbooking.calificaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sena.goldenbooking.calificaciones.dto.CalificacionDto;
import com.sena.goldenbooking.calificaciones.dto.ResumenCalificacionDto;
import com.sena.goldenbooking.calificaciones.model.Calificacion;
import com.sena.goldenbooking.calificaciones.repository.CalificacionRepository;
import com.sena.goldenbooking.compartido.exception.AccesoDenegadoException;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.reservas.model.AccionReserva;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.TipoReserva;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

class CalificacionServiceTest {

    private CalificacionRepository repo;
    private ReservaDeporteRepository reservaDeporteRepo;
    private ReservaHotelRepository reservaHotelRepo;
    private CalificacionService service;

    @BeforeEach
    void setUp() {
        repo = mock(CalificacionRepository.class);
        reservaDeporteRepo = mock(ReservaDeporteRepository.class);
        reservaHotelRepo = mock(ReservaHotelRepository.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        service = new CalificacionService(repo, reservaDeporteRepo, reservaHotelRepo, usuarioService);

        when(usuarioService.obtenerPorDocNum("123")).thenReturn(UsuarioDto.builder().nombre("Laura").apellido("Pérez").build());
        when(repo.save(any(Calificacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void reservaDeporte(EstadoReserva estado) {
        when(reservaDeporteRepo.findById("r1")).thenReturn(Optional.of(ReservaDeporte.builder()
                .idReservaDeporte("r1").docUsuario("123").espacioId("e1").tipoCancha("Cancha 1").estado(estado).build()));
    }

    private static CalificacionDto dto(TipoReserva categoria, String idReserva, int puntuacion) {
        return CalificacionDto.builder().categoria(categoria).idReserva(idReserva).puntuacion(puntuacion)
                .comentario("  Muy buena  ").build();
    }

    @Test
    void calificaUnaReservaFinalizadaConNombrePublicoYHistorial() {
        reservaDeporte(EstadoReserva.FINALIZADA);

        CalificacionDto c = service.calificar(dto(TipoReserva.DEPORTE, "r1", 5), "123");

        assertEquals("e1", c.getIdRecurso());
        assertEquals("Cancha 1", c.getNombreRecurso());
        assertEquals("Laura P.", c.getNombreCliente());
        assertEquals("Muy buena", c.getComentario());
        verify(reservaDeporteRepo).save(any(ReservaDeporte.class));
    }

    @Test
    void guardaLaCalificacionEnElHistorialDeLaReserva() {
        ReservaHotel rh = ReservaHotel.builder().idHotelReserva("rh1").docUsuario("123").idHabitacion("h1")
                .datosH(Habitacion.builder().numHab("101").build()).estado(EstadoReserva.FINALIZADA).build();
        when(reservaHotelRepo.findById("rh1")).thenReturn(Optional.of(rh));

        CalificacionDto c = service.calificar(dto(TipoReserva.HOTEL, "rh1", 4), "123");

        assertEquals("Habitación 101", c.getNombreRecurso());
        assertEquals(AccionReserva.CALIFICADA, rh.getHistorial().get(0).getAccion());
        assertEquals("4 estrellas", rh.getHistorial().get(0).getDetalle());
    }

    @Test
    void soloSeCalificaCuandoFinalizo() {
        reservaDeporte(EstadoReserva.CONFIRMADA);

        assertThrows(ConflictoDeNegocioException.class, () -> service.calificar(dto(TipoReserva.DEPORTE, "r1", 5), "123"));
        verify(repo, never()).save(any());
    }

    @Test
    void soloElDuenoCalificaYUnaSolaVez() {
        reservaDeporte(EstadoReserva.FINALIZADA);
        assertThrows(AccesoDenegadoException.class, () -> service.calificar(dto(TipoReserva.DEPORTE, "r1", 5), "otro"));

        when(repo.existsByIdReserva("r1")).thenReturn(true);
        assertThrows(ConflictoDeNegocioException.class, () -> service.calificar(dto(TipoReserva.DEPORTE, "r1", 5), "123"));
    }

    @Test
    void laPuntuacionVaDe1A5() {
        assertThrows(SolicitudInvalidaException.class, () -> service.calificar(dto(TipoReserva.DEPORTE, "r1", 0), "123"));
        assertThrows(SolicitudInvalidaException.class, () -> service.calificar(dto(TipoReserva.DEPORTE, "r1", 6), "123"));
    }

    @Test
    void comentarioVacioSeGuardaComoNulo() {
        reservaDeporte(EstadoReserva.FINALIZADA);
        CalificacionDto sinComentario = dto(TipoReserva.DEPORTE, "r1", 3);
        sinComentario.setComentario("   ");

        assertNull(service.calificar(sinComentario, "123").getComentario());
    }

    @Test
    void resumenPromediaPorEspacioConUnDecimal() {
        when(repo.findByCategoria(TipoReserva.DEPORTE)).thenReturn(List.of(
                Calificacion.builder().idRecurso("e1").puntuacion(5).build(),
                Calificacion.builder().idRecurso("e1").puntuacion(4).build(),
                Calificacion.builder().idRecurso("e1").puntuacion(4).build(),
                Calificacion.builder().idRecurso("e2").puntuacion(2).build()));

        List<ResumenCalificacionDto> resumen = service.resumen(TipoReserva.DEPORTE);

        ResumenCalificacionDto e1 = resumen.stream().filter(r -> r.idRecurso().equals("e1")).findFirst().orElseThrow();
        assertEquals(4.3, e1.promedio());
        assertEquals(3, e1.total());
        assertEquals(2, resumen.size());
    }
}
