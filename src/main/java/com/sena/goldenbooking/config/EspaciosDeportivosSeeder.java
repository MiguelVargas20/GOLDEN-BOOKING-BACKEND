package com.sena.goldenbooking.config;

import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.sena.goldenbooking.models.EspacioDeportivo;
import com.sena.goldenbooking.models.EstadoEspacio;
import com.sena.goldenbooking.models.ReservaDeporte;
import com.sena.goldenbooking.repositories.EspacioDeportivoRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Datos iniciales de espacios deportivos (se ejecuta al arrancar).
 *
 * 1. Si la colección está vacía, crea los 10 espacios que antes estaban fijos
 *    en el catálogo del frontend, para que el cliente no vea el catálogo vacío.
 *    Después el admin los edita, desactiva o crea nuevos desde la app.
 * 2. Asocia las reservas deportivas antiguas (que solo guardaban el nombre de
 *    la cancha como texto) con su espacio, por nombre. Así la validación de
 *    horarios ocupados, que ahora trabaja por espacioId, también las tiene en
 *    cuenta. Es idempotente: solo toca reservas que aún no tienen espacioId.
 */
@Slf4j
@Component
@Order(2) // después de MongoValidatorConfig
public class EspaciosDeportivosSeeder implements CommandLineRunner {

    private final EspacioDeportivoRepository repo;
    private final MongoTemplate mongoTemplate;

    /** Tarifa que se usaba para todas las canchas antes de que cada espacio tuviera la suya. */
    @Value("${app.reservas.deporte.tarifa-hora}")
    private double tarifaPorDefecto;

    public EspaciosDeportivosSeeder(EspacioDeportivoRepository repo, MongoTemplate mongoTemplate) {
        this.repo = repo;
        this.mongoTemplate = mongoTemplate;
    }

    private record EspacioInicial(String nombre, String descripcion, int capacidad) {}

    private static final List<EspacioInicial> ESPACIOS_INICIALES = List.of(
            new EspacioInicial("Fútbol", "Canchas profesionales.", 22),
            new EspacioInicial("Basketball", "Múltiples canchas.", 10),
            new EspacioInicial("Tennis", "Categorías por nivel.", 4),
            new EspacioInicial("Natación", "Piscina olímpica y recreativa.", 30),
            new EspacioInicial("Golf", "Campo abierto.", 8),
            new EspacioInicial("Voleybol", "Campo abierto.", 12),
            new EspacioInicial("Ping Pong", "Mesas profesionales.", 4),
            new EspacioInicial("Patinaje", "Pista de patinaje.", 20),
            new EspacioInicial("Hockey", "Campo abierto.", 20),
            new EspacioInicial("Ciclismo", "Pista de ciclismo.", 15));

    @Override
    public void run(String... args) {
        crearEspaciosIniciales();
        asociarReservasAntiguas();
    }

    private void crearEspaciosIniciales() {
        if (repo.count() > 0) return;

        var ahora = ZonaHoraria.ahora();
        List<EspacioDeportivo> espacios = ESPACIOS_INICIALES.stream()
                .map(e -> EspacioDeportivo.builder()
                        .nombre(e.nombre())
                        .deporte(e.nombre())
                        .descripcion(e.descripcion())
                        .capacidad(e.capacidad())
                        .tarifaHora(tarifaPorDefecto)
                        .horaApertura(LocalTime.of(6, 0))
                        .horaCierre(LocalTime.of(22, 0))
                        .estado(EstadoEspacio.ACTIVO)
                        .fechaCreacion(ahora)
                        .fechaActualizacion(ahora)
                        .build())
                .toList();
        repo.saveAll(espacios);
        log.info("Se crearon {} espacios deportivos iniciales.", espacios.size());
    }

    private void asociarReservasAntiguas() {
        long total = 0;
        for (EspacioDeportivo espacio : repo.findAll()) {
            Query sinEspacio = Query.query(Criteria.where("espacioId").exists(false)
                    .and("tipoCancha").regex("^" + Pattern.quote(espacio.getNombre()) + "$", "i"));
            total += mongoTemplate.updateMulti(sinEspacio, Update.update("espacioId", espacio.getId()),
                    ReservaDeporte.class).getModifiedCount();
        }
        if (total > 0) {
            log.info("Se asociaron {} reservas deportivas antiguas con su espacio.", total);
        }
    }
}
