# Golden Booking — Backend

## Requisitos
- Java 25 (lo exige el pom.xml)
- Maven
- MongoDB (por defecto localhost:27017; configurable con `MONGODB_URI`). La base `goldenbooking` se crea sola.

## Configuración
El archivo `application.properties` ya tiene la configuración por defecto.
Para producción configura estas variables de entorno:
- `JWT_SECRET` — clave secreta para JWT (mínimo 32 caracteres)
- `CORS_ALLOWED_ORIGINS` — orígenes del frontend separados por coma (ej: `https://goldenbooking.vercel.app`). Se usa para CORS y para el WebSocket.
- `COOKIE_SAME_SITE` — SameSite de la cookie del refresh token (por defecto `None`, necesario con front y back en dominios distintos). **Requiere que el backend se sirva por HTTPS.**
- `APP_ZONA_HORARIA` — zona horaria del negocio (por defecto `America/Bogota`). El servidor puede correr en UTC; esta zona se usa para las reglas de cancelación, recordatorios y el `.ics`.
- `APP_RESERVAS_CIERRE_MS` — cada cuánto corre el cierre automático de reservas (por defecto `1800000` = 30 min): finaliza las confirmadas que ya terminaron y vence las pendientes que nadie aprobó a tiempo (el cliente recibe un correo).

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

## Estructura del proyecto
El código está organizado **por módulo** (igual que el frontend). Cada módulo
tiene sus capas: `controller`, `dto`, `model`, `repository`, `service` y `mapper`.

```
com.sena.goldenbooking
├── GoldenbookingApplication
├── auth/                 Login, refresh token, logout, verificación y recuperación de contraseña, rate limit
├── usuarios/             Registro, perfil y administración de usuarios
├── habitaciones/         Habitaciones y tipos de habitación
├── reservas/             Lo común a todas las reservas: Reserva "padre", estados, reglas de
│                         aprobación/cancelación, plantillas de correo y recordatorios
├── reservasdeportivas/   Espacios deportivos (con imágenes en GridFS) y sus reservas
├── reservashoteleras/    Reservas de habitaciones
├── mensajes/             Formulario de contacto y respuestas del admin
├── security/             JWT, filtro de autenticación y reglas de acceso (SecurityConfig)
└── compartido/           Lo que usan varios módulos:
    ├── config/           MongoDB, Swagger, WebSocket, zona horaria
    ├── email/            Envío de correos (asíncrono)
    ├── exception/        Excepciones de negocio y GlobalExceptionHandler (formato único de errores)
    └── web/              Paginación segura
```

Las pruebas unitarias están en `src/test` con la misma estructura. Para correrlas
sin necesitar MongoDB:
```bash
./mvnw test -Dtest='*Test,!GoldenbookingApplicationTests'
```

## Respaldos de la base de datos

Ver [`scripts/README-respaldos.md`](scripts/README-respaldos.md): script de respaldo diario (`scripts/backup-mongo.sh`), cómo programarlo con cron y cómo restaurar.
