package com.sena.goldenbooking.compartido.config;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Verifica al arrancar que MongoDB responde, antes de que los inicializadores
 * de datos (EspaciosDeportivosSeeder, AdminInicialSeeder) intenten usarlo.
 *
 * Antes el arranque FALLABA si la base 'goldenbooking' aún no existía. En un
 * Mongo nuevo (local o un servidor recién instalado) eso pasa siempre, aunque
 * Mongo crea la base automáticamente al guardar el primer dato: obligaba a
 * crearla a mano en Compass. Ahora solo se exige que el servidor responda; si
 * la base no existe se avisa y se crea sola.
 */
@Slf4j
@Component
@Order(1) // antes que los inicializadores de datos
public class MongoValidatorConfig implements CommandLineRunner {

    private final MongoClient mongoClient;

    @Value("${spring.mongodb.database}")
    private String databaseName;

    public MongoValidatorConfig(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public void run(String... args) {
        log.info("--- Verificando conexión con MongoDB ---");
        try {
            mongoClient.getDatabase(databaseName).runCommand(new Document("ping", 1));
        } catch (Exception e) {
            log.error("No se pudo conectar a MongoDB. Revisa que el servidor esté encendido y la variable MONGODB_URI. Detalle: {}",
                    e.getMessage());
            throw new IllegalStateException("Fallo en el arranque: MongoDB no responde.", e);
        }

        boolean existe = mongoClient.listDatabaseNames().into(new java.util.ArrayList<>()).contains(databaseName);
        if (existe) {
            log.info("Conexión exitosa: base de datos '{}' lista.", databaseName);
        } else {
            log.warn("La base de datos '{}' aún no existe: MongoDB la creará al guardar el primer dato.", databaseName);
        }
    }
}
