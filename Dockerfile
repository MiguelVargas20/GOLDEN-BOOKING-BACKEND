# ─────────────────────────────────────────────────────────────────────
# Imagen del backend de Golden Booking (para Render u otro hosting con Docker).
#
# Etapa 1 compila el .jar con Java 25; etapa 2 solo lleva el JRE y el .jar,
# así la imagen final es liviana y no incluye el código fuente ni Maven.
# Probar en local:
#   docker build -t goldenbooking-api .
#   docker run --rm -p 8080:8080 --env-file .env goldenbooking-api
# ─────────────────────────────────────────────────────────────────────

FROM eclipse-temurin:25-jdk AS compilacion
WORKDIR /app

# Primero solo lo necesario para descargar dependencias: Docker reutiliza esta
# capa mientras pom.xml no cambie y los despliegues siguientes son más rápidos.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && (./mvnw -B -q dependency:go-offline || true)

COPY src src
RUN ./mvnw -B -q -DskipTests package && cp target/goldenbooking-*.jar app.jar


FROM eclipse-temurin:25-jre
WORKDIR /app

# Usuario sin privilegios (no root) y carpeta para los logs de logback
RUN useradd --system --uid 1001 --no-create-home app \
    && mkdir -p /app/logs && chown app /app/logs
COPY --from=compilacion /app/app.jar app.jar

# Pensado para 512 MB de RAM (plan gratis de Render): el heap usa hasta el 70 %
# de la memoria del contenedor, GC serial (menos memoria) y arranque más rápido.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError"

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
