package com.sena.goldenbooking.config;

import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Esta clase se ejecutará al iniciar la aplicación para verificar que la base de datos MongoDB esté accesible y que la base de datos 'goldenbooking' exista. Si no se encuentra, se lanzará una excepción para evitar que la aplicación continúe con errores posteriores.

@Component
public class MongoValidatorConfig implements CommandLineRunner {

    private final MongoClient mongoClient;

    // Esto lee el nombre de la DB directamente de tu application.properties o .yml
    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    // Inyectamos el MongoClient para poder interactuar con el servidor de MongoDB
    public MongoValidatorConfig(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }


    // Este método se ejecutará al iniciar la aplicación
    @Override
    public void run(String... args) {
        System.out.println("--- Iniciando verificación de base de datos ---");
        
        // 1. Obtenemos la lista de nombres de BD en el servidor
        List<String> databases = mongoClient
                .listDatabaseNames()
                .into(new ArrayList<>());

        // 2. Verificamos si existe nuestra DB 'goldenbooking'
        if (!databases.contains(databaseName)) {
            System.err.println("CRITICAL ERROR: La base de datos '" + databaseName + "' no fue encontrada.");
            System.err.println("Bases de datos detectadas: " + databases);
            
            // Forzamos el cierre de la aplicación para evitar errores en ejecución
            throw new IllegalStateException("Fallo en el arranque: La base de datos '" + databaseName + "' es obligatoria.");
        }


        // Si llegamos aquí, la conexión es exitosa y la base de datos existe
        System.out.println("CONEXIÓN EXITOSA: Base de datos '" + databaseName + "' verificada y lista.");
        System.out.println("-----------------------------------------------");
    }
}