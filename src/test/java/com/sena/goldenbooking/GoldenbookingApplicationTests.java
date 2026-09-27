package com.sena.goldenbooking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que la aplicación arranca completa (todos los beans y la conexión a
 * MongoDB). Necesita una base de datos: si no hay ninguna disponible, la prueba
 * se omite (queda como "skipped") en vez de fallar, así las pruebas unitarias
 * se pueden correr en cualquier equipo.
 */
@SpringBootTest
@EnabledIf("mongoDisponible")
class GoldenbookingApplicationTests {

	@Test
	void contextLoads() {
	}

	/** ¿Responde el MongoDB configurado (MONGODB_URI, del entorno o del .env)? */
	static boolean mongoDisponible() {
		String uri = System.getenv("MONGODB_URI");
		if (uri == null) uri = leerDelEnv("MONGODB_URI");
		if (uri == null) uri = "mongodb://localhost:27017/goldenbooking";
		// Atlas (mongodb+srv) resuelve los servidores por DNS: se intenta arrancar
		if (uri.startsWith("mongodb+srv://")) return true;

		String servidor = uri.replaceFirst("^mongodb://", "").replaceFirst("^[^@/]*@", "")
				.split("[/?]")[0].split(",")[0];
		String[] partes = servidor.split(":");
		int puerto = partes.length > 1 ? Integer.parseInt(partes[1]) : 27017;
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(partes[0], puerto), 1000);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private static String leerDelEnv(String clave) {
		try {
			return Files.readAllLines(Path.of(".env")).stream()
					.map(String::trim)
					.filter(l -> l.startsWith(clave + "="))
					.map(l -> l.substring(clave.length() + 1).replaceAll("^[\"']|[\"']$", ""))
					.findFirst().orElse(null);
		} catch (IOException e) {
			return null;
		}
	}
}
