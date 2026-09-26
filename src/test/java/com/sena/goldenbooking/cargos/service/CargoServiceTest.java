package com.sena.goldenbooking.cargos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sena.goldenbooking.cargos.dto.CargoDto;
import com.sena.goldenbooking.cargos.dto.DestinoDisponibleDto;
import com.sena.goldenbooking.cargos.model.Cargo;
import com.sena.goldenbooking.cargos.model.CategoriaCargo;
import com.sena.goldenbooking.cargos.model.DestinoCargo;
import com.sena.goldenbooking.cargos.model.EstadoCargo;
import com.sena.goldenbooking.cargos.repository.CargoRepository;
import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.exception.ConflictoDeNegocioException;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.habitaciones.model.Habitacion;
import com.sena.goldenbooking.membresias.service.MembresiaService;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.service.NotificacionService;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.dto.UsuarioDto;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import com.sena.goldenbooking.usuarios.model.TipoMembresia;
import com.sena.goldenbooking.usuarios.service.UsuarioService;

/** Consumos cargados a la reserva activa o a la cuenta de socio. */
class CargoServiceTest {

    private CargoRepository repo;
    private MembresiaService membresias;
    private NotificacionService notificaciones;
    private CargoService service;
    private final LocalDateTime ahora = ZonaHoraria.ahora();

    @BeforeEach
    void setUp() {
        repo = mock(CargoRepository.class);
        ReservaHotelRepository hotelRepo = mock(ReservaHotelRepository.class);
        ReservaDeporteRepository deporteRepo = mock(ReservaDeporteRepository.class);
        UsuarioService usuarioService = mock(UsuarioService.class);
        membresias = mock(MembresiaService.class);
        notificaciones = mock(NotificacionService.class);
        service = new CargoService(repo, hotelRepo, deporteRepo, usuarioService, membresias, notificaciones);

        when(usuarioService.obtenerPorDocNum("123")).thenReturn(UsuarioDto.builder().nombre("Laura").apellido("Pérez").estado(EstadoUsuario.ACTIVO).build());
        when(membresias.membresiaDe("123")).thenReturn(TipoMembresia.NINGUNA);
        when(hotelRepo.findByDocUsuarioAndEstado("123", EstadoReserva.CONFIRMADA)).thenReturn(List.of(
                ReservaHotel.builder().idHotelReserva("activa").datosH(Habitacion.builder().numHab("101").build())
                        .fechaCheckIn(ahora.minusDays(1)).fechaCheckOut(ahora.plusDays(1)).build(),
                ReservaHotel.builder().idHotelReserva("terminada").fechaCheckIn(ahora.minusDays(5)).fechaCheckOut(ahora.minusDays(3)).build()));
        when(deporteRepo.findByDocUsuarioAndEstado("123", EstadoReserva.CONFIRMADA)).thenReturn(List.of(
                ReservaDeporte.builder().idReservaDeporte("rd1").tipoCancha("Cancha 1")
                        .fechaReserva(ahora.plusHours(2)).fechaFinReserva(ahora.plusHours(3)).build()));
        when(repo.save(any(Cargo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static CargoDto cargo(DestinoCargo destino, String idReserva) {
        return CargoDto.builder().docUsuario("123").concepto(" Agua sin gas ").categoria(CategoriaCargo.RESTAURANTE)
                .cantidad(2).valorUnitario(4500).destino(destino).idReserva(idReserva).build();
    }

    @Test
    void soloOfreceReservasActivasYLaCuentaDeSocioSiLoEs() {
        List<DestinoDisponibleDto> sinSocio = service.destinos("123");
        assertEquals(2, sinSocio.size());
        assertEquals("activa", sinSocio.get(0).idReserva());
        assertEquals(true, sinSocio.get(0).descripcion().startsWith("Habitación 101"));

        when(membresias.membresiaDe("123")).thenReturn(TipoMembresia.MIEMBRO);
        assertEquals(DestinoCargo.CUENTA_SOCIO, service.destinos("123").get(2).destino());
    }

    @Test
    void registraElConsumoConSuTotalYAvisaAlCliente() {
        CargoDto c = service.registrar(cargo(DestinoCargo.RESERVA_HOTEL, "activa"), "admin");

        assertEquals(9000.0, c.getTotal());
        assertEquals("Agua sin gas", c.getConcepto());
        assertEquals(EstadoCargo.PENDIENTE, c.getEstado());
        assertEquals("admin", c.getRegistradoPor());
        verify(notificaciones).notificar(eq("123"), eq(TipoNotificacion.CARGO), any(), any(), anyString(), anyString());
    }

    @Test
    void noCargaAReservasTerminadasNiALaCuentaDeUnNoSocio() {
        assertThrows(SolicitudInvalidaException.class, () -> service.registrar(cargo(DestinoCargo.RESERVA_HOTEL, "terminada"), "admin"));
        assertThrows(SolicitudInvalidaException.class, () -> service.registrar(cargo(DestinoCargo.CUENTA_SOCIO, null), "admin"));
        verify(repo, never()).save(any());
    }

    @Test
    void unSocioPuedeCargarASuCuenta() {
        when(membresias.membresiaDe("123")).thenReturn(TipoMembresia.OCASIONAL);
        CargoDto c = service.registrar(cargo(DestinoCargo.CUENTA_SOCIO, "ignorado"), "admin");
        assertEquals(null, c.getIdReserva());
    }

    @Test
    void pagarLosPendientesDeUnaReservaAlHacerCheckOut() {
        Cargo a = Cargo.builder().id("a").docUsuario("123").idReserva("activa").destino(DestinoCargo.RESERVA_HOTEL).total(9000).estado(EstadoCargo.PENDIENTE).build();
        Cargo b = Cargo.builder().id("b").docUsuario("123").destino(DestinoCargo.CUENTA_SOCIO).total(5000).estado(EstadoCargo.PENDIENTE).build();
        when(repo.findByDocUsuarioAndEstado("123", EstadoCargo.PENDIENTE)).thenReturn(List.of(a, b));

        CargoService.ResumenPago r = service.pagarPendientes("123", "activa", false, "admin");

        assertEquals(1, r.cantidad());
        assertEquals(9000.0, r.total());
        assertEquals(EstadoCargo.PAGADO, a.getEstado());
        assertEquals(EstadoCargo.PENDIENTE, b.getEstado());
    }

    @Test
    void noSeEliminaUnConsumoPagado() {
        when(repo.findById("x")).thenReturn(Optional.of(Cargo.builder().id("x").estado(EstadoCargo.PAGADO).build()));
        assertThrows(ConflictoDeNegocioException.class, () -> service.eliminar("x"));
        assertThrows(ConflictoDeNegocioException.class, () -> service.pagar("x", "admin"));
    }
}
