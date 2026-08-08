package com.sena.goldenbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GoldenbookingApplication {

    public static void main(String[] args) {
        cargarVariablesDeEnv();
        SpringApplication.run(GoldenbookingApplication.class, args);
    }

    /**
     * Lee el archivo .env de la raíz del proyecto y carga cada línea
     * como una propiedad del sistema (System.setProperty). Spring Boot
     * resuelve los placeholders ${...} también contra System Properties,
     * así que esto es suficiente para que application.properties funcione
     * sin depender de ninguna librería externa de terceros.
     */
    private static void cargarVariablesDeEnv() {
        File archivoEnv = new File(".env");

        if (!archivoEnv.exists()) {
            System.out.println("=== ADVERTENCIA: no se encontró el archivo .env en la raíz del proyecto ===");
            return;
        }

        try (BufferedReader lector = new BufferedReader(new FileReader(archivoEnv))) {
            String linea;
            int cargadas = 0;

            while ((linea = lector.readLine()) != null) {
                linea = linea.trim();

                // Ignora líneas vacías y comentarios
                if (linea.isEmpty() || linea.startsWith("#")) continue;

                int separador = linea.indexOf('=');
                if (separador > 0) {
                    String clave = linea.substring(0, separador).trim();
                    String valor = linea.substring(separador + 1).trim();
                    System.setProperty(clave, valor);
                    cargadas++;
                }
            }

            System.out.println("=== .env cargado correctamente: " + cargadas + " variables ===");
        } catch (Exception e) {
            System.out.println("=== ERROR leyendo .env: " + e.getMessage() + " ===");
        }
    }
}