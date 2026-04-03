# AWS Academy Deployment Guide

## Target architecture
- One EC2 instance in `us-east-1`
- Frontend exposed on `http://<EC2_PUBLIC_IP>/`
- Backend exposed on `http://<EC2_PUBLIC_IP>:9020`
- PostgreSQL kept private on the EC2 host through `127.0.0.1:5432`
- Persistent Docker volumes for database data and uploaded files

## Prerequisites
- AWS Academy Learner Lab account
- Region set to `us-east-1`
- EC2 key pair available

## EC2 configuration
- AMI: Amazon Linux 2023
- Instance type: `t3.medium`
- Storage: `20 GiB` `gp3`
- Security Group inbound rules:
  - `22/TCP` from your IP
  - `80/TCP` from `0.0.0.0/0`
  - `9020/TCP` from `0.0.0.0/0`
- Do not open `5432/TCP`

## Instance setup
```bash
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
newgrp docker
docker --version
docker compose version
```

## Project setup
```bash
git clone <YOUR_REPOSITORY_URL>
cd FreelanceRecommandation
cp .env.example .env
```

## AWS `.env` values
Update `.env` before deployment:

```dotenv
POSTGRES_DB=towork
POSTGRES_USER=towork
POSTGRES_PASSWORD=<strong-password>
POSTGRES_PORT=5432
POSTGRES_BIND_ADDRESS=127.0.0.1

SPRING_PROFILES_ACTIVE=prod
BACKEND_PORT=9020
BACKEND_INTERNAL_PORT=9020
FRONTEND_PORT=80
FILE_UPLOAD_DIR=/app/uploads

JWT_SECRET=<strong-base64-secret>
CORS_ALLOWED_ORIGINS=http://<EC2_PUBLIC_IP>

AI_ENABLED=false
AI_OPENAI_API_KEY=
```

## Deploy
```bash
docker compose -f docker-compose.yml -f docker-compose.aws.yml up --build -d
docker compose ps
docker compose logs -f backend
```

## Validation
- Frontend: `http://<EC2_PUBLIC_IP>/`
- Backend health: `http://<EC2_PUBLIC_IP>:9020/actuator/health`
- Swagger UI: `http://<EC2_PUBLIC_IP>:9020/swagger-ui.html`

## Functional checks
1. Create an admin with `POST /auth/register/admin`
2. Log in and confirm JWT authentication works
3. Create and fetch a mission
4. Upload a file and confirm it still exists after `docker compose restart backend`

## Operations
- Stop the stack:
  ```bash
  docker compose -f docker-compose.yml -f docker-compose.aws.yml down
  ```
- Rebuild after changes:
  ```bash
  docker compose -f docker-compose.yml -f docker-compose.aws.yml up --build -d
  ```
- Stop the EC2 instance when not in use to preserve AWS Academy credits
