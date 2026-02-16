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
- View logs:
  ```bash
  docker compose logs -f backend
  ```
- Stop services:
  ```bash
  docker compose down
  ```

## API Examples
Base URL: `http://localhost:9020`

### 1) Create Administrator
Endpoint: `POST /auth/register/admin`  
Authentication: none

Request body:
```json
{
  "firstName": "Super",
  "lastName": "Admin",
  "email": "admin@example.com",
  "password": "Password123!",
  "phone": "123456789",
  "address": "Tunis"
}
```

cURL:
```bash
curl -X POST "http://localhost:9020/auth/register/admin" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName":"Super",
    "lastName":"Admin",
    "email":"admin@example.com",
    "password":"Password123!",
    "phone":"123456789",
    "address":"Tunis"
  }'
```

PowerShell:
```powershell
$body = @{
  firstName = "Super"
  lastName  = "Admin"
  email     = "admin@example.com"
  password  = "Password123!"
  phone     = "123456789"
  address   = "Tunis"
} | ConvertTo-Json

Invoke-WebRequest -UseBasicParsing `
  -Uri "http://localhost:9020/auth/register/admin" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body
```

Note: success response includes a JWT token in `data.token`.

### 2) Add Domaine
Endpoint: `POST /domaines`  
Authentication: Bearer token required (`Authorization: Bearer <token>`)

Request body:
```json
{
  "name": "Web Development",
  "description": "Frontend and backend development",
  "icon": "code",
  "color": "#2563EB"
}
```

cURL:
```bash
curl -X POST "http://localhost:9020/domaines" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -d '{
    "name":"Web Development",
    "description":"Frontend and backend development",
    "icon":"code",
    "color":"#2563EB"
  }'
```

### Common API Errors
- `400` on register admin: email already used or invalid/missing fields.
- `401/403` on create domaine: missing or invalid token.
