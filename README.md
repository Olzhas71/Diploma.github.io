# Parking System

Intelligent network parking management system. Spring Boot 3 + PostgreSQL + Redis + JWT + WebSocket.

## Features

- **Auth**: JWT (access + refresh), BCrypt, role-based access (`ADMIN`, `OPERATOR`, `DRIVER`).
- **Parking**: CRUD parkings / spots / tariffs, geo radius search, free-spot counts.
- **Booking**: Time-window conflict detection, optimistic + pessimistic locking, scheduled auto-completion.
- **Pricing**: Tariff selection by day-of-week, time-of-day, vehicle type; dynamic multiplier.
- **Payments**: Simulated PSP integration; refund flow.
- **ML**: Per-hour occupancy forecast from sensor history (24h horizon, 30d window), Redis-cached.
- **Real-time**: WebSocket `/ws/occupancy` broadcasts spot status changes.
- **Ops**: Flyway migrations, Actuator, Swagger UI at `/api/swagger-ui.html`, Docker compose.

## Quick start (Docker)

```bash
docker compose up --build
```

App: http://localhost:8080/api  ·  Swagger: http://localhost:8080/api/swagger-ui.html

Default seeded admin: `admin@parking.local` / `admin123`.

## Quick start (local)

Requires JDK 17, Maven 3.9+, Postgres 15, Redis 7.

```bash
psql -U postgres -c "CREATE USER parking WITH PASSWORD 'parking'; CREATE DATABASE parking OWNER parking;"
mvn spring-boot:run
```

## Tests

```bash
mvn test
```

## API surface (selected)

| Method | Path                                  | Role                |
|--------|---------------------------------------|---------------------|
| POST   | `/auth/register`                      | public              |
| POST   | `/auth/login`                         | public              |
| POST   | `/auth/refresh`                       | public              |
| GET    | `/users/me`                           | any auth            |
| GET    | `/vehicles`                           | DRIVER+             |
| POST   | `/vehicles`                           | DRIVER+             |
| GET    | `/parkings`                           | any auth            |
| GET    | `/parkings/nearby?lat&lon&radiusKm`   | any auth            |
| POST   | `/parkings`                           | ADMIN, OPERATOR     |
| GET    | `/parkings/{id}/spots`                | any auth            |
| POST   | `/parkings/{id}/spots`                | ADMIN, OPERATOR     |
| POST   | `/bookings`                           | DRIVER+             |
| POST   | `/bookings/{id}/cancel`               | owner               |
| POST   | `/bookings/{id}/activate`             | ADMIN, OPERATOR     |
| POST   | `/payments`                           | DRIVER+             |
| GET    | `/ml/parkings/{parkingId}/forecast`   | any auth            |
| WS     | `/ws/occupancy`                       | any                 |

## Project layout

```
src/main/java/com/parking/
  config/        Security, Redis, OpenAPI, WebSocket
  controller/    REST endpoints
  service/       Business logic
  ml/            Occupancy forecasting
  websocket/     Real-time broadcast
  repository/    Spring Data JPA
  entity/        JPA entities + enums
  dto/           Request / response records
  mapper/        MapStruct
  security/      JWT + UserDetails
  exception/     Custom + GlobalExceptionHandler
  util/          SecurityUtils
src/main/resources/
  application*.yml
  db/migration/  Flyway V1, V2
```

## Environment variables (prod profile)

| Var          | Default     |
|--------------|-------------|
| `DB_HOST`    | postgres    |
| `DB_PORT`    | 5432        |
| `DB_NAME`    | parking     |
| `DB_USER`    | parking     |
| `DB_PASSWORD`| parking     |
| `REDIS_HOST` | redis       |
| `REDIS_PORT` | 6379        |
| `JWT_SECRET` | (set me)    |
