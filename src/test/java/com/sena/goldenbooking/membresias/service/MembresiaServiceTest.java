package com.sena.goldenbooking.membresias.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sena.goldenbooking.compartido.exception.SolicitudInvalidaException;
import com.sena.goldenbooking.membresias.dto.BeneficioVigente;
import com.sena.goldenbooking.membresias.dto.SocioDto;
import com.sena.goldenbooking.membresias.model.ConfigMembresia;
import com.sena.goldenbooking.membresias.repository.ConfigMembresiaRepository;
import com.sena.goldenbooking.notificaciones.model.TipoNotificacion;
import com.sena.goldenbooking.notificaciones.service.NotificacionService;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;
import com.sena.goldenbooking.usuarios.model.Documento;
import com.sena.goldenbooking.usuarios.model.Rol;
import com.sena.goldenbooking.usuarios.model.TipoMembresia;
import com.sena.goldenbooking.usuarios.model.Usuario;
import com.sena.goldenbooking.usuarios.model.UsuarioAuth;
import com.sena.goldenbooking.usuarios.repository.UsuarioAuthRepository;
import com.sena.goldenbooking.usuarios.repository.UsuarioRepository;

class MembresiaServiceTest {

    private ConfigMembresiaRepository configRepo;
    private UsuarioRepository usuarioRepo;
    private UsuarioAuthRepository authRepo;
    private ReservaDeporteRepository deporteRepo;
    private ReservaHotelRepository hotelRepo;
    private NotificacionService notificaciones;
    private MembresiaService service;

    private static Usuario usuario(String id, String doc, TipoMembresia tipo) {
        Documento d = new Documento();
        d.setNumeroD(doc);
        return Usuario.builder().id(id).nomUsr("Laura").apellUsr("Pérez").docId(d).correo(id + "@correo.com").membresia(tipo).build();
    }

    @BeforeEach
    void setUp() {
        configRepo = mock(ConfigMembresiaRepository.class);
        usuarioRepo = mock(UsuarioRepository.class);
        authRepo = mock(UsuarioAuthRepository.class);
        deporteRepo = mock(ReservaDeporteRepository.class);
        hotelRepo = mock(ReservaHotelRepository.class);
        notificaciones = mock(NotificacionService.class);
        service = new MembresiaService(configRepo, usuarioRepo, authRepo, deporteRepo, hotelRepo, notificaciones);
        when(configRepo.findById(ConfigMembresia.ID)).thenReturn(Optional.empty()); // valores por defecto
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void cadaCategoriaTieneSusBeneficios() {
        when(usuarioRepo.findByDocNum("1")).thenReturn(Optional.of(usuario("u1", "1", null)));
        when(usuarioRepo.findByDocNum("2")).thenReturn(Optional.of(usuario("u2", "2", TipoMembresia.MIEMBRO)));

        BeneficioVigente sin = service.beneficiosDe("1");
        BeneficioVigente miembro = service.beneficiosDe("2");

        assertEquals(TipoMembresia.NINGUNA, sin.membresia());
        assertEquals(0, sin.descuento());
        assertEquals(60, sin.diasAnticipacion());
        assertEquals(10, miembro.descuento());
        assertEquals(365, miembro.diasAnticipacion());
        assertEquals(90000.0, miembro.aplicar(100000));
    }

    @Test
    void sugiereHacerSocioAQuienAlcanzaElMinimoSinContarAdmins() {
        UsuarioAuth admin = new UsuarioAuth();
        admin.setId("admin");
        admin.setRls(List.of(Rol.ROL_ADMIN));
        when(authRepo.findAll()).thenReturn(List.of(admin));
        when(usuarioRepo.findAll()).thenReturn(List.of(usuario("admin", "0", null), usuario("u1", "1", null),
                usuario("u2", "2", null), usuario("u3", "3", TipoMembresia.OCASIONAL)));
        ReservaDeporte d1 = ReservaDeporte.builder().docUsuario("1").build();
        ReservaHotel h1 = ReservaHotel.builder().docUsuario("1").build();
        when(deporteRepo.findByEstadoIn(any())).thenReturn(List.of(d1, d1, d1, ReservaDeporte.builder().docUsuario("3").build(),
                ReservaDeporte.builder().docUsuario("3").build(), ReservaDeporte.builder().docUsuario("3").build()));
        when(hotelRepo.findByEstadoIn(any())).thenReturn(List.of(h1, h1, ReservaHotel.builder().docUsuario("3").build(),
                ReservaHotel.builder().docUsuario("3").build()));

        List<SocioDto> socios = service.listarClientes();

        assertEquals(3, socios.size()); // sin el admin
        assertEquals("1", socios.get(0).documento()); // el sugerido va primero
        assertTrue(socios.get(0).sugerido());
        assertEquals(5, socios.get(0).reservas());
        SocioDto yaSocio = socios.stream().filter(s -> s.documento().equals("3")).findFirst().orElseThrow();
        assertFalse(yaSocio.sugerido()); // ya es socio aunque tenga 5 reservas
    }

    @Test
    void asignarGuardaLaCategoriaYAvisaAlCliente() {
        Usuario u = usuario("u1", "1", null);
        when(usuarioRepo.findById("u1")).thenReturn(Optional.of(u));

        SocioDto s = service.asignar("u1", TipoMembresia.OCASIONAL);

        assertEquals(TipoMembresia.OCASIONAL, s.membresia());
        assertEquals(TipoMembresia.OCASIONAL, u.getMembresia());
        verify(notificaciones).notificar(eq("1"), eq(TipoNotificacion.MEMBRESIA), any(), any(), eq("¡Ahora eres socio Ocasional!"), anyString());

        service.asignar("u1", TipoMembresia.NINGUNA);
        assertNull(u.getFechaMembresia());
    }

    @Test
    void losSociosNoPuedenTenerMenosAnticipacionQueUnCliente() {
        ConfigMembresia c = ConfigMembresia.porDefecto();
        c.getOcasional().setDiasAnticipacion(30); // menos que los 60 generales
        assertThrows(SolicitudInvalidaException.class, () -> service.guardarConfig(c));
    }

    @Test
    void miMembresiaCuentaSoloReservasConfirmadasYFinalizadas() {
        when(usuarioRepo.findByDocNum("1")).thenReturn(Optional.of(usuario("u1", "1", null)));
        when(deporteRepo.countByDocUsuarioAndEstadoIn(eq("1"), any())).thenReturn(2L);
        when(hotelRepo.countByDocUsuarioAndEstadoIn(eq("1"), any())).thenReturn(1L);

        var mia = service.miMembresia("1");

        assertEquals(3, mia.reservas());
        assertEquals(5, mia.reservasParaSugerir());
        assertTrue(MembresiaService.CUENTAN.contains(EstadoReserva.FINALIZADA));
        assertFalse(MembresiaService.CUENTAN.contains(EstadoReserva.PENDIENTE));
    }
}
