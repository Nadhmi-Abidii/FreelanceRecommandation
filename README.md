# ToWork Monorepo

## Project Overview
This repository contains the ToWork platform split into:
- `frontend/`: Angular application
- `towork-backend/`: Spring Boot API
- PostgreSQL database (run with Docker Compose)

The stack below is production-like for local usage:
- Frontend on `http://localhost:4200`
- Backend on `http://localhost:9020`
- PostgreSQL on `localhost:${POSTGRES_PORT}` (default `5432`)

## Architecture
- Frontend: Angular (served by Nginx container)
- Backend: Spring Boot (Java 17)
- Database: PostgreSQL 16
- Orchestration: Docker Compose

## Prerequisites
- Docker Desktop (Windows/Mac) or Docker Engine (Linux)
- Docker Compose v2 (`docker compose`)

## Quick Start (Docker)
1. Create your local env file:
   - PowerShell:
     ```powershell
     Copy-Item .env.example .env
     ```
   - Bash:
     ```bash
     cp .env.example .env
     ```
2. Update `.env` values if needed (DB password, JWT secret, AI settings).
3. Start the full stack:
   ```bash
   docker compose up --build -d
   ```

## Access URLs
- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:9020`
- Swagger UI: `http://localhost:9020/swagger-ui.html`
- Health endpoint: `http://localhost:9020/actuator/health`

## Common Commands
- Start services:
  ```bash
  docker compose up -d
  ```
- Rebuild and restart:
  ```bash
  docker compose up --build -d
  ```
- Show logs:
  ```bash
  docker compose logs -f
  ```
- Show logs for one service:
  ```bash
  docker compose logs -f backend
  ```
- Stop services:
  ```bash
  docker compose down
  ```
- Stop and remove volumes (resets DB and uploads):
  ```bash
  docker compose down -v
  ```

## Configuration (Env Vars)
Copy `.env.example` to `.env` and adjust values.

| Variable | Default | Description |
| --- | --- | --- |
| `POSTGRES_DB` | `towork` | PostgreSQL database name |
| `POSTGRES_USER` | `postgres` | PostgreSQL username |
| `POSTGRES_PASSWORD` | `postgres` | PostgreSQL password |
| `POSTGRES_PORT` | `5432` | Host port mapped to PostgreSQL container |
| `JWT_SECRET` | `MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=` | Base64-encoded JWT signing secret used by backend |
| `AI_ENABLED` | `false` | Enables/disables AI features |
| `AI_OPENAI_API_KEY` | *(empty)* | OpenAI API key used when AI is enabled |

## Data Persistence
Docker Compose creates persistent named volumes:
- `postgres_data`: stores PostgreSQL data
- `backend_uploads`: stores uploaded backend files (`/app/uploads`)

Data remains after container restarts.  
Use `docker compose down -v` to remove persisted data.

## Troubleshooting
- Ports already in use:
  - Change host ports in `.env` (`POSTGRES_PORT`) or in `docker-compose.yml` for frontend/backend mappings.
- Database not ready yet:
  - Backend waits for DB health check, but first startup can take longer.
  - Check with:
    ```bash
    docker compose logs -f db
    docker compose logs -f backend
    ```
- Frontend can load but API calls fail:
  - Ensure backend is running on `http://localhost:9020`.
  - Current backend CORS setup expects frontend origin `http://localhost:4200`.

## Security Notes
- Do not commit real secrets in git-tracked files.
- Keep secrets only in your local `.env` or a secret manager.
- The previously exposed OpenAI key in backend config should be considered compromised and must be rotated immediately in your provider dashboard.
