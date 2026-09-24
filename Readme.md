# Golden Booking — Backend

## Requisitos
- Java 17+
- Maven
- MongoDB corriendo en localhost:27017

## Configuración
El archivo `application.properties` ya tiene la configuración por defecto.
Para producción configura estas variables de entorno:
- `JWT_SECRET` — clave secreta para JWT (mínimo 32 caracteres)
- `CORS_ALLOWED_ORIGINS` — orígenes del frontend separados por coma (ej: `https://goldenbooking.vercel.app`). Se usa para CORS y para el WebSocket.
- `COOKIE_SAME_SITE` — SameSite de la cookie del refresh token (por defecto `None`, necesario con front y back en dominios distintos). **Requiere que el backend se sirva por HTTPS.**
- `APP_ZONA_HORARIA` — zona horaria del negocio (por defecto `America/Bogota`). El servidor puede correr en UTC; esta zona se usa para las reglas de cancelación, recordatorios y el `.ics`.

## Correr el proyecto
```bash
./mvnw spring-boot:run
```

## Endpoints principales
- `POST /auth/login` — Login
- `POST /api/usuarios/registro` — Registro público
- `GET /api/reservas` — Listar reservas (requiere JWT)
- `POST /api/reservas/hotel` — Crear reserva hotel (requiere JWT)
- `POST /api/reservas/deporte` — Crear reserva deporte (requiere JWT)

## Usuario admin por defecto
Crear manualmente vía POST /api/usuarios/registro:
```json
{
  "nombre": "Admin",
  "apellido": "Golden",
  "documento": { "tipoD": "CC", "numeroD": "0000000001" },
  "email": "admin@goldenbooking.com",
  "username": "admin",
  "password": "admin123",
  "estado": "ACTIVO",
  "roles": ["ROL_ADMIN"]
}

```