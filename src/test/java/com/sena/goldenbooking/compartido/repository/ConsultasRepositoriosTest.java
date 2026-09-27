package com.sena.goldenbooking.compartido.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.sena.goldenbooking.reservas.model.EstadoReserva;
import com.sena.goldenbooking.reservas.model.Reserva;
import com.sena.goldenbooking.reservas.repository.ReservaRepository;
import com.sena.goldenbooking.reservasdeportivas.model.ReservaDeporte;
import com.sena.goldenbooking.reservasdeportivas.repository.ReservaDeporteRepository;
import com.sena.goldenbooking.reservashoteleras.model.ReservaHotel;
import com.sena.goldenbooking.reservashoteleras.repository.ReservaHotelRepository;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

/**
 * Ejecuta las consultas de los repositorios contra un MongoDB en memoria.
 *
 * Las pruebas de los services usan repositorios simulados y no pueden detectar
 * una consulta que Spring Data rechaza: los rangos sobre un mismo campo
 * ("findByFechaGreaterThanEqualAndFechaLessThan") lanzaban
 * InvalidMongoDbApiUsageException y el calendario y los reportes respondían 503.
 */
class ConsultasRepositoriosTest {

    private static MongoServer servidor;
    private static MongoClient cliente;
    private static MongoTemplate mongo;
    private static ReservaDeporteRepository deporteRepo;
    private static ReservaHotelRepository hotelRepo;
    private static ReservaRepository reservaRepo;

    private final LocalDate lunes = LocalDate.of(2026, 10, 5);

    @BeforeAll
    static void iniciar() {
        servidor = new MongoServer(new MemoryBackend());
        String direccion = "mongodb://" + servidor.bindAndGetConnectionString().replace("mongodb://", "");
        cliente = MongoClients.create(direccion);
        mongo = new MongoTemplate(cliente, "pruebas");
        MongoRepositoryFactory fabrica = new MongoRepositoryFactory(mongo);
        deporteRepo = fabrica.getRepository(ReservaDeporteRepository.class);
        hotelRepo = fabrica.getRepository(ReservaHotelRepository.class);
        reservaRepo = fabrica.getRepository(ReservaRepository.class);
    }

    @AfterAll
    static void detener() {
        cliente.close();
        servidor.shutdown();
    }

    @BeforeEach
    void datos() {
        mongo.getDb().drop();
        deporteRepo.saveAll(List.of(
                deporte("antes", lunes.minusDays(1).atTime(10, 0), EstadoReserva.CONFIRMADA),
                deporte("lunes", lunes.atTime(8, 0), EstadoReserva.PENDIENTE),
                deporte("cancelada", lunes.plusDays(2).atTime(9, 0), EstadoReserva.CANCELADA),
                deporte("domingo", lunes.plusDays(6).atTime(20, 0), EstadoReserva.FINALIZADA),
                deporte("despues", lunes.plusDays(7).atTime(8, 0), EstadoReserva.CONFIRMADA)));
        hotelRepo.saveAll(List.of(
                ReservaHotel.builder().idHotelReserva("h-dentro").estado(EstadoReserva.CONFIRMADA)
                        .fechaCheckIn(lunes.plusDays(1).atTime(15, 0)).fechaCheckOut(lunes.plusDays(3).atTime(12, 0)).build(),
                ReservaHotel.builder().idHotelReserva("h-fuera").estado(EstadoReserva.CONFIRMADA)
                        .fechaCheckIn(lunes.plusDays(9).atTime(15, 0)).fechaCheckOut(lunes.plusDays(10).atTime(12, 0)).build()));
        reservaRepo.saveAll(List.of(
                Reserva.builder().id("r1").estado(EstadoReserva.CONFIRMADA).fechaInicio(lunes.atTime(10, 0)).precioTotal(100.0).build(),
                Reserva.builder().id("r2").estado(EstadoReserva.PENDIENTE).fechaInicio(lunes.atTime(11, 0)).precioTotal(50.0).build(),
                Reserva.builder().id("r3").estado(EstadoReserva.FINALIZADA).fechaInicio(lunes.plusMonths(1).atTime(9, 0)).precioTotal(70.0).build()));
    }

    private static ReservaDeporte deporte(String id, LocalDateTime inicio, EstadoReserva estado) {
        return ReservaDeporte.builder().idReservaDeporte(id).espacioId("e1").estado(estado)
                .fechaReserva(inicio).fechaFinReserva(inicio.plusHours(1)).build();
    }

    private static List<String> ids(List<ReservaDeporte> lista) {
        return lista.stream().map(ReservaDeporte::getIdReservaDeporte).sorted().toList();
    }

    @Test
    void semanaDeportivaSinCanceladas() {
        List<ReservaDeporte> r = deporteRepo.findByFechaReservaGreaterThanEqualAndFechaReservaLessThanAndEstadoNot(
                lunes.atStartOfDay(), lunes.plusDays(7).atStartOfDay(), EstadoReserva.CANCELADA);

        assertEquals(List.of("domingo", "lunes"), ids(r));
    }

    @Test
    void reporteDeportivoConTodosLosEstados() {
        List<ReservaDeporte> r = deporteRepo.findByFechaReservaGreaterThanEqualAndFechaReservaLessThan(
                lunes.atStartOfDay(), lunes.plusDays(7).atStartOfDay());

        assertEquals(List.of("cancelada", "domingo", "lunes"), ids(r));
    }

    @Test
    void reporteHoteleroPorFechaDeCheckIn() {
        List<ReservaHotel> r = hotelRepo.findByFechaCheckInGreaterThanEqualAndFechaCheckInLessThan(
                lunes.atStartOfDay(), lunes.plusDays(7).atStartOfDay());

        assertEquals(1, r.size());
        assertEquals("h-dentro", r.get(0).getIdHotelReserva());
    }

    @Test
    void ingresosDelMesSoloConEstadosCobrados() {
        List<Reserva> r = reservaRepo.findByFechaInicioGreaterThanEqualAndFechaInicioLessThanAndEstadoIn(
                lunes.withDayOfMonth(1).atStartOfDay(), lunes.withDayOfMonth(1).plusMonths(1).atStartOfDay(),
                List.of(EstadoReserva.CONFIRMADA, EstadoReserva.FINALIZADA));

        assertEquals(1, r.size());
        assertEquals("r1", r.get(0).getId());
    }

    @Test
    void estadiasQueTocanLaSemana() {
        List<ReservaHotel> r = hotelRepo.findByFechaCheckInLessThanAndFechaCheckOutGreaterThanEqualAndEstadoIn(
                lunes.plusDays(7).atStartOfDay(), lunes.atStartOfDay(), List.of(EstadoReserva.CONFIRMADA));

        assertEquals(1, r.size());
    }

    // ── Consultas nuevas (socios, cargos y eventos) ────────────────────────

    @Test
    void reservasPorEstadoYPorCliente() {
        deporteRepo.save(ReservaDeporte.builder().idReservaDeporte("mia").docUsuario("123").estado(EstadoReserva.CONFIRMADA)
                .fechaReserva(lunes.atTime(9, 0)).fechaFinReserva(lunes.atTime(10, 0)).build());
        List<EstadoReserva> cuentan = List.of(EstadoReserva.CONFIRMADA, EstadoReserva.FINALIZADA);

        assertEquals(4, deporteRepo.findByEstadoIn(cuentan).size());
        assertEquals(1, deporteRepo.countByDocUsuarioAndEstadoIn("123", cuentan));
        assertEquals(1, deporteRepo.findByDocUsuarioAndEstado("123", EstadoReserva.CONFIRMADA).size());
        assertEquals(2, hotelRepo.findByEstadoIn(cuentan).size());
    }

    @Test
    void eventosPublicadosQueNoHanTerminado() {
        var eventos = new MongoRepositoryFactory(mongo).getRepository(com.sena.goldenbooking.eventos.repository.EventoRepository.class);
        eventos.saveAll(List.of(
                com.sena.goldenbooking.eventos.model.Evento.builder().id("ok").publicado(true)
                        .fechaInicio(lunes.atTime(19, 0)).fechaFin(lunes.atTime(23, 0)).build(),
                com.sena.goldenbooking.eventos.model.Evento.builder().id("borrador").publicado(false)
                        .fechaInicio(lunes.atTime(19, 0)).fechaFin(lunes.atTime(23, 0)).build(),
                com.sena.goldenbooking.eventos.model.Evento.builder().id("pasado").publicado(true)
                        .fechaInicio(lunes.minusDays(3).atTime(19, 0)).fechaFin(lunes.minusDays(3).atTime(23, 0)).build()));

        var r = eventos.findByPublicadoTrueAndFechaFinAfter(lunes.atStartOfDay(), org.springframework.data.domain.Sort.by("fechaInicio"));

        assertEquals(1, r.size());
        assertEquals("ok", r.get(0).getId());
    }

    @Test
    void cargosPendientesDeUnCliente() {
        var cargos = new MongoRepositoryFactory(mongo).getRepository(com.sena.goldenbooking.cargos.repository.CargoRepository.class);
        cargos.saveAll(List.of(
                com.sena.goldenbooking.cargos.model.Cargo.builder().docUsuario("123").estado(com.sena.goldenbooking.cargos.model.EstadoCargo.PENDIENTE).fecha(lunes.atStartOfDay()).build(),
                com.sena.goldenbooking.cargos.model.Cargo.builder().docUsuario("123").estado(com.sena.goldenbooking.cargos.model.EstadoCargo.PAGADO).fecha(lunes.atStartOfDay()).build()));

        assertEquals(1, cargos.findByDocUsuarioAndEstado("123", com.sena.goldenbooking.cargos.model.EstadoCargo.PENDIENTE).size());
        assertEquals(2, cargos.findByDocUsuarioOrderByFechaDesc("123").size());
    }
}
