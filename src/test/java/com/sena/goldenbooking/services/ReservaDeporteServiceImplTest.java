package com.sena.goldenbooking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.sena.goldenbooking.config.ZonaHoraria;
import com.sena.goldenbooking.dtos.ReservaDeporteDto;
import com.sena.goldenbooking.mapper.ReservaDeporteMapperImpl;
import com.sena.goldenbooking.models.Reserva;
import com.sena.goldenbooking.models.ReservaDeporte;
import com.sena.goldenbooking.repositories.ReservaDeporteRepository;
import com.sena.goldenbooking.repositories.ReservaRepository;

/**
 * Pruebas unitarias (sin Spring ni Mongo) de las validaciones de fechas y
 * del cálculo de precio al crear una reserva deportiva.
 */
class ReservaDeporteServiceImplTest {

    private ReservaDeporteRepository reservaDeporteRepo;
    private ReservaRepository reservaRepo;
    private ReservaDeporteServiceImpl service;

    @BeforeEach
    void setUp() {
        reservaDeporteRepo = mock(ReservaDeporteRepository.class);
        reservaRepo = mock(ReservaRepository.class);
        service = new ReservaDeporteServiceImpl(
                reservaDeporteRepo,
                reservaRepo,
                new ReservaDeporteMapperImpl(),
                mock(SimpMessagingTemplate.class),
                mock(EmailService.class),
                mock(UsuarioService.class));
        ReflectionTestUtils.setField(service, "tarifaHora", 50000.0);

        when(reservaDeporteRepo.findSolapadas(anyString(), any(), any())).thenReturn(List.of());
        when(reservaRepo.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservaDeporteRepo.save(any(ReservaDeporte.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ReservaDeporteDto dto(LocalDateTime inicio, LocalDateTime fin) {
        return ReservaDeporteDto.builder()
                .docUsuario("123")
                .tCancha("Fútbol")
                .fInicioReserva(inicio)
                .fFinReserva(fin)
                .build();
    }

    @Test
    void rechazaReservaConInicioEnElPasado() {
        LocalDateTime ayer = ZonaHoraria.ahora().minusDays(1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crear(dto(ayer, ayer.plusHours(2))));

        assertEquals("La fecha de inicio no puede estar en el pasado.", ex.getMessage());
        verify(reservaRepo, never()).save(any());
    }

    @Test
    void rechazaReservaDeMenosDeUnaHora() {
        LocalDateTime manana = ZonaHoraria.ahora().plusDays(1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crear(dto(manana, manana.plusMinutes(30))));

        assertEquals("La reserva debe durar al menos una hora.", ex.getMessage());
    }

    @Test
    void cobraProporcionalAlosMinutosReservados() {
        LocalDateTime manana = ZonaHoraria.ahora().plusDays(1);

        service.crear(dto(manana, manana.plusMinutes(90)));

        // 1h30 a 50.000/h = 75.000 (antes se truncaba a 1h = 50.000)
        ArgumentCaptor<ReservaDeporte> captor = ArgumentCaptor.forClass(ReservaDeporte.class);
        verify(reservaDeporteRepo).save(captor.capture());
        assertEquals(75000.0, captor.getValue().getPrecio());
    }
}
