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
La documentación completa (con ejemplos y "Probar") está en Swagger: `http://localhost:8080/swagger-ui.html`.

| Módulo | Endpoints |
|---|---|
| Autenticación | `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/verificar-cuenta`, `POST /auth/solicitar-recuperacion`, `POST /auth/restablecer-password` |
| Usuarios | `POST /api/usuarios/registro` (público, siempre cliente), `POST /api/usuarios?rol=` (admin), `GET/PUT/DELETE /api/usuarios/**` (admin), `GET/PATCH /api/usuarios/perfil/{id}` (perfil propio) |
| Reservas deportivas | `/api/reservas/deporte` — crear (con acompañantes), mis reservas, gestión del admin (aprobar / cancelar con motivo), `PATCH /{id}/reprogramar`, `PATCH /{id}/miembros`, resumen |
| Reservas hoteleras | `/api/reservas/hotel` — crear (con acompañantes), mis reservas, gestión del admin, `PATCH /{id}/reprogramar`, `PATCH /{id}/miembros`, fechas ocupadas por habitación |
| Espacios deportivos | `/api/espacios-deportivos` — CRUD (admin), estado, imagen (GridFS) e implementos (sugeridos según el deporte si no se definen) |
| Habitaciones | `/api/habitaciones` y `/api/tipohabitaciones` — CRUD (admin), galería de hasta 5 imágenes: `POST /{id}/imagenes`, `DELETE /{id}/imagenes/{imagenId}`, `PATCH /{id}/imagenes/{imagenId}/portada`, `GET /{id}/imagenes/{imagenId}` (público) |
| Consumos | `/api/cargos` — `GET /mios` (cliente); admin: `GET /cuenta/{documento}`, `GET /pendientes`, `POST`, `PATCH /{id}/pagar`, `PATCH /cuenta/{documento}/pagar?idReserva=&soloCuentaSocio=`, `DELETE /{id}` |
| Eventos | `GET /api/eventos/proximos` (cualquier usuario); admin: `GET`, `POST`, `PUT /{id}`, `DELETE /{id}`, imagen `POST/DELETE /{id}/imagen` (`GET` público) |
| Socios | `GET /api/membresias/mia` (usuario en sesión); admin: `GET/PUT /config`, `GET /socios`, `PATCH /socios/{idUsuario}?tipo=` (NINGUNA, OCASIONAL o MIEMBRO) |
| Dashboard | `GET /api/dashboard?dias=14` (admin) |
| Calendario | `GET /api/calendario/semana?desde=2026-10-05` (admin) — ocupación semanal por espacio y habitación |
| Reportes | `GET /api/reportes`, `/api/reportes/excel`, `/api/reportes/pdf` con `?desde=&hasta=` (admin) |
| Notificaciones | `/api/notificaciones` — campana del cliente: listar, contar no leídas, marcar leídas |
| Calificaciones | `POST /api/calificaciones` (reserva finalizada, una vez), `GET /resumen?categoria=`, `GET ?categoria=&idRecurso=`, `GET /mias` |
| Mensajes | `/api/contacto` — contacto, bandeja del admin, respuestas |
| Tiempo real | WebSocket STOMP en `/ws` (`/topic/reservas-deporte` público, `/topic/admin/reservas` solo admin) |

## Primer usuario administrador
El registro público **siempre** crea clientes (por seguridad no acepta roles). Para el primer admin:

1. Regístrate normalmente desde el frontend.
2. En MongoDB Compass, colección `UsuarioPerfil`: pon `verificado: true` (si no configuraste el correo).
3. Colección `UsuarioAuth`: deja `rls: ["ROL_ADMIN"]` y vuelve a iniciar sesión.

Desde ahí, los demás administradores se crean desde el panel (**Usuarios → Agregar usuario**), con el rol elegido y la cuenta ya verificada.

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
│                         aprobación/cancelación/reprogramación, historial, plantillas de correo y recordatorios
├── reservasdeportivas/   Espacios deportivos (con imágenes en GridFS) y sus reservas
├── reservashoteleras/    Reservas de habitaciones
├── calendario/           Ocupación semanal de espacios y habitaciones (admin)
├── reportes/             Reservas e ingresos por rango de fechas, en Excel (Apache POI) y PDF (OpenPDF)
├── cargos/               Consumos cargados a una reserva activa o a la cuenta de socio y su cobro
├── eventos/              Eventos del club (baile, recreación, festivales...) con imagen
├── membresias/           Programa de socios: reglas, beneficios (descuento y anticipación) y asignación
├── notificaciones/       Campana del cliente (reservas, calificar, eventos, consumos, membresía)
├── calificaciones/       Estrellas y opiniones de espacios y habitaciones
├── mensajes/             Formulario de contacto y respuestas del admin
├── security/             JWT, filtro de autenticación y reglas de acceso (SecurityConfig)
└── compartido/           Lo que usan varios módulos:
    ├── config/           MongoDB, Swagger, WebSocket, zona horaria
    ├── email/            Envío de correos (asíncrono)
    ├── exception/        Excepciones de negocio y GlobalExceptionHandler (formato único de errores)
    └── web/              Paginación segura
```

Las pruebas unitarias están en `src/test` con la misma estructura y no necesitan
MongoDB (usan Mockito):
```bash
./mvnw test
```
La prueba de arranque completo (`GoldenbookingApplicationTests`) solo corre si hay
un MongoDB disponible; si no, aparece como omitida (*skipped*) en vez de fallar.

## Respaldos de la base de datos

Ver [`scripts/README-respaldos.md`](scripts/README-respaldos.md): script de respaldo diario (`scripts/backup-mongo.sh`), cómo programarlo con cron y cómo restaurar.
