package com.sena.goldenbooking.eventos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sena.goldenbooking.compartido.config.ZonaHoraria;
import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.compartido.imagenes.AlmacenImagenes;
import com.sena.goldenbooking.eventos.dto.EventoDto;
import com.sena.goldenbooking.eventos.model.CategoriaEvento;
import com.sena.goldenbooking.eventos.model.Evento;
import com.sena.goldenbooking.eventos.repository.EventoRepository;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.service.NotificacionService;
import com.sena.goldenbooking.usuarios.model.Documento;
import com.sena.goldenbooking.usuarios.model.EstadoUsuario;
import com.sena.goldenbooking.usuarios.model.Rol;
import com.sena.goldenbooking.usuarios.model.Usuario;
import com.sena.goldenbooking.usuarios.model.UsuarioAuth;
import com.sena.goldenbooking.usuarios.repository.UsuarioAuthRepository;
import com.sena.goldenbooking.usuarios.repository.UsuarioRepository;

class EventoServiceTest {

    private EventoRepository repo;
    private NotificacionService notificaciones;
    private EventoService service;
    private final LocalDateTime manana = ZonaHoraria.ahora().plusDays(1).withHour(19).withMinute(0);

    private static Usuario usuario(String id, String doc, EstadoUsuario estado) {
        return Usuario.builder().id(id).docId(new Documento("CC", doc)).estado(estado).build();
    }

    @BeforeEach
    void setUp() {
        repo = mock(EventoRepository.class);
        notificaciones = mock(NotificacionService.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        UsuarioAuthRepository authRepo = mock(UsuarioAuthRepository.class);
        service = new EventoService(repo, mock(AlmacenImagenes.class), notificaciones, usuarioRepo, authRepo);
        when(repo.save(any(Evento.class))).thenAnswer(inv -> { Evento e = inv.getArgument(0); if (e.getId() == null) e.setId("ev1"); return e; });
        UsuarioAuth admin = new UsuarioAuth();
        admin.setId("a1");
        admin.setRls(List.of(Rol.ROL_ADMIN));
        when(authRepo.findAll()).thenReturn(List.of(admin));
        when(usuarioRepo.findAll()).thenReturn(List.of(usuario("a1", "0", EstadoUsuario.ACTIVO),
                usuario("c1", "1", EstadoUsuario.ACTIVO), usuario("c2", "2", EstadoUsuario.ACTIVO), usuario("c3", "3", EstadoUsuario.INACTIVO)));
    }

    private EventoDto dto(boolean publicado) {
        return EventoDto.builder().titulo(" Noche de salsa ").categoria(CategoriaEvento.BAILE).lugar("Salón principal")
                .fechaInicio(manana).fechaFin(manana.plusHours(4)).precio(0.0).publicado(publicado).build();
    }

    @Test
    void alPublicarloSeAvisaATodosLosClientesActivos() {
        EventoDto e = service.crear(dto(true));

        assertEquals("Noche de salsa", e.getTitulo());
        assertTrue(e.isNuevo());
        assertNull(e.getPrecio()); // 0 = gratis
        verify(notificaciones, times(2)).notificar(anyString(), eq(TipoNotificacion.EVENTO), any(), eq("ev1"), eq("Nuevo evento"), anyString());
    }

    @Test
    void unBorradorNoAvisaHastaQueSePublica() {
        service.crear(dto(false));
        verify(notificaciones, never()).notificar(any(), any(), any(), any(), any(), any());

        Evento guardado = Evento.builder().id("ev1").publicado(false).fechaCreacion(ZonaHoraria.ahora()).build();
        when(repo.findById("ev1")).thenReturn(Optional.of(guardado));
        service.actualizar("ev1", dto(true));
        verify(notificaciones, times(2)).notificar(anyString(), eq(TipoNotificacion.EVENTO), any(), any(), any(), any());
    }

    @Test
    void validaLasFechas() {
        EventoDto alReves = dto(true);
        alReves.setFechaFin(manana.minusHours(1));
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(alReves));

        EventoDto pasado = dto(true);
        pasado.setFechaInicio(manana.minusDays(5));
        pasado.setFechaFin(manana.minusDays(4));
        assertThrows(SolicitudInvalidaException.class, () -> service.crear(pasado));
    }

    @Test
    void unEventoViejoYaNoSeMarcaComoNuevo() {
        when(repo.findByPublicadoTrueAndFechaFinAfter(any(), any())).thenReturn(List.of(
                Evento.builder().id("v").titulo("Festival").publicado(true).fechaInicio(manana).fechaFin(manana.plusHours(2))
                        .fechaCreacion(ZonaHoraria.ahora().minusDays(30)).build()));
        assertEquals(false, service.proximos().get(0).isNuevo());
    }
}
