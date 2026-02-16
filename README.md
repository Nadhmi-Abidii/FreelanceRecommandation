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
