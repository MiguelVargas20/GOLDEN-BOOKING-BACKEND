package com.sena.goldenbooking.calendario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sena.goldenbooking.calendario.dto.CalendarioSemanaDto;
import com.sena.goldenbooking.calendario.dto.FilaCalendarioDto;
import com.sena.goldenbooking.habitaciones.model.EstadoHabitacion;
import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.habitaciones.repository.HabitacionRepository;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.model.EspacioDeportivo;
import com.sena.goldenbooking.reservasdeportivas.model.EstadoEspacio;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.EspacioDeportivoRepository;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

class CalendarioServiceTest {

    private final LocalDate lunes = LocalDate.of(2026, 10, 5);

    @Test
    void agrupaLasReservasDeLaSemanaPorEspacioYHabitacion() {
        EspacioDeportivoRepository espacioRepo = mock(EspacioDeportivoRepository.class);
        HabitacionRepository habitacionRepo = mock(HabitacionRepository.class);
        ReservaDeporteRepository reservaDeporteRepo = mock(ReservaDeporteRepository.class);
        ReservaHotelRepository reservaHotelRepo = mock(ReservaHotelRepository.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        CalendarioService service = new CalendarioService(espacioRepo, habitacionRepo, reservaDeporteRepo, reservaHotelRepo, usuarioService);

        when(espacioRepo.findByEstadoIn(any(), any())).thenReturn(List.of(
                EspacioDeportivo.builder().id("e1").nombre("Cancha 1").deporte("Tenis").estado(EstadoEspacio.ACTIVO)
                        .horaApertura(LocalTime.of(6, 0)).horaCierre(LocalTime.of(22, 0)).build(),
                EspacioDeportivo.builder().id("e2").nombre("Cancha 2").deporte("Pádel").estado(EstadoEspacio.MANTENIMIENTO).build()));
        when(habitacionRepo.findAll()).thenReturn(List.of(
                Habitacion.builder().id("h2").numHab("102").estado(EstadoHabitacion.DISPONIBLE).build(),
                Habitacion.builder().id("h1").numHab("101").estado(EstadoHabitacion.DISPONIBLE).build()));
        when(reservaDeporteRepo.findByFechaReservaGreaterThanEqualAndFechaReservaLessThanAndEstadoNot(
                lunes.atStartOfDay(), lunes.plusDays(7).atStartOfDay(), EstadoReserva.CANCELADA)).thenReturn(List.of(
                ReservaDeporte.builder().idReservaDeporte("rd2").espacioId("e1").docUsuario("123").estado(EstadoReserva.PENDIENTE)
                        .fechaReserva(lunes.atTime(16, 0)).fechaFinReserva(lunes.atTime(17, 0)).build(),
                ReservaDeporte.builder().idReservaDeporte("rd1").espacioId("e1").docUsuario("123").estado(EstadoReserva.CONFIRMADA)
                        .fechaReserva(lunes.atTime(8, 0)).fechaFinReserva(lunes.atTime(9, 0)).build()));
        when(reservaHotelRepo.findByFechaCheckInLessThanAndFechaCheckOutGreaterThanEqualAndEstadoIn(any(), any(), any())).thenReturn(List.of(
                ReservaHotel.builder().idHotelReserva("rh1").idHabitacion("h1").docUsuario("999").estado(EstadoReserva.CONFIRMADA)
                        .fechaCheckIn(lunes.minusDays(1).atTime(15, 0)).fechaCheckOut(lunes.plusDays(2).atTime(12, 0)).build()));
        when(usuarioService.obtenerMapaPorDocNums(anyList())).thenReturn(
                Map.of("123", UsuarioDto.builder().nombre("Laura").apellido("Pérez").build()));

        CalendarioSemanaDto semana = service.semana(lunes);

        assertEquals(lunes.plusDays(6), semana.hasta());
        FilaCalendarioDto cancha = semana.espacios().get(0);
        assertEquals(2, cancha.reservas().size());
        assertEquals("rd1", cancha.reservas().get(0).idReserva()); // en orden de hora
        assertEquals("Laura Pérez", cancha.reservas().get(0).cliente());
        assertEquals(0, semana.espacios().get(1).reservas().size());
        // habitaciones en orden de número; cliente desconocido muestra el documento
        assertEquals("Habitación 101", semana.habitaciones().get(0).nombre());
        assertEquals("Doc. 999", semana.habitaciones().get(0).reservas().get(0).cliente());
        assertEquals(0, semana.habitaciones().get(1).reservas().size());
    }
}
