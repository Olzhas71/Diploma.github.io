# Parking System — Frontend

React 18 + TypeScript + Vite + Tailwind CSS SPA for the parking-system backend.

## Stack

- **React 18** + TypeScript
- **Vite** — dev server & bundler
- **React Router 6** — routing with protected routes
- **TanStack Query** — server state, caching, retries
- **Zustand** (with `persist`) — auth store, JWT in `localStorage`
- **Axios** — HTTP client with JWT interceptor + auto-refresh on 401
- **Tailwind CSS** — utility-first styling
- **Recharts** — ML forecast chart
- **React Leaflet** — map of nearby parkings
- **Native WebSocket** — real-time spot status

## Run (dev)

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173, proxies /api and /ws → http://localhost:8080
```

The Vite dev server proxies REST and WebSocket calls to the Spring Boot backend, so just start the backend separately (`mvn spring-boot:run` or `docker compose up app postgres redis`).

## Run (full stack via Docker)

```bash
docker compose up --build
```

- Frontend → http://localhost:3000
- Backend → http://localhost:8080/api
- Swagger → http://localhost:8080/api/swagger-ui.html

The frontend container is a static Nginx image that reverse-proxies `/api` and `/ws` to the `app` service.

## Default credentials

- `admin@parking.local` / `admin123` (ADMIN role)
- Or register a new DRIVER account from the UI.

## Pages

| Route                   | Role             | Description                                                  |
|-------------------------|------------------|--------------------------------------------------------------|
| `/`                     | public           | Landing                                                      |
| `/login`, `/register`   | public           | Auth                                                         |
| `/parkings`             | any auth         | Parkings list with live free-spot bars                       |
| `/parkings/:id`         | any auth         | Spot grid + tariffs + booking dialog (real-time via WS)      |
| `/map`                  | any auth         | Leaflet map of nearby parkings (uses geolocation)            |
| `/bookings`             | any auth         | My bookings, cancel, pay (CARD / WALLET / SUBSCRIPTION)      |
| `/vehicles`             | any auth         | CRUD vehicles                                                |
| `/forecast/:id`         | any auth         | 24-hour ML occupancy forecast (Recharts)                     |
| `/admin/parkings`       | ADMIN, OPERATOR  | Parkings management table + create dialog                    |

## Build

```bash
npm run build   # → dist/
npm run lint    # tsc --noEmit
```
