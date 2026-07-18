# Installation & Setup Guide

Setup instructions for the **Auto Accessories Store** — a full-stack application with a
Spring Boot backend and a React (Vite) frontend.

---

## 1. Prerequisites

Install the following before you begin.

| Tool | Version | Purpose | Check |
|------|---------|---------|-------|
| **JDK** | 21 | Build & run the backend | `java -version` |
| **Node.js** | 18+ (LTS) | Build & run the frontend | `node -v` |
| **npm** | 9+ (bundled with Node) | Frontend package manager | `npm -v` |
| **Docker + Docker Compose** | latest | Run PostgreSQL, Redis & Kafka | `docker -v` |
| **Git** | latest | Clone the repo | `git --version` |

> Maven is **not** required — the backend ships with the Maven Wrapper (`mvnw` / `mvnw.cmd`).

### External services / accounts (optional but needed for full functionality)
- **Cloudinary** account — image uploads (`cloud-name`, `api-key`, `api-secret`)
- **Gmail App Password** — outgoing email (16-char app password)
- **Google OAuth 2.0 Client** — "Sign in with Google" (`client-id`, `client-secret`)
- **SePay** API key — payment gateway

---

## 2. Clone the repository

```bash
git clone <repository-url>
cd auto_accessories_store
```

---

## 3. Start infrastructure (PostgreSQL, Redis, Kafka)

The backend depends on PostgreSQL, Redis, and Kafka. The easiest way to run them is via the
provided Docker Compose file.

```bash
cd backend

# Create the infra env file from the template and edit the passwords
cp .env.example .env      # Windows PowerShell: copy .env.example .env

# Start only the infrastructure services
docker compose --profile infra up -d
```

This starts:
- **PostgreSQL 16**   → `localhost:5432`  (database `store`)
- **Redis 7.4**   → `localhost:6379`
- **Kafka 3.9**   → `localhost:9094`

Optional dev tools (RedisInsight on `:5540`, Kafka UI on `:8090`):

```bash
docker compose --profile tools up -d
```

> **No Docker?** Install PostgreSQL 16, Redis 7, and Kafka 3.9 manually and make sure they
> listen on the ports above.

---

## 4. Configure & run the backend

### 4a. Configure `application.yaml`

Edit [backend/src/main/resources/application.yaml](backend/src/main/resources/application.yaml)
and replace the placeholder values so they match what you set in `.env`:

| Setting | Placeholder to replace |
|---------|------------------------|
| `spring.datasource.username / password` | your PostgreSQL user & password |
| `spring.data.redis.password` | your Redis password |
| `spring.mail.username / password` | Gmail address + App Password |
| `jwt.signerKey` | a base64 secret — generate with `openssl rand -base64 48` |
| `cloudinary.*` | Cloudinary credentials |
| `sepay.*` | SePay payment credentials |
| `google.client-id / client-secret` | Google OAuth credentials |

> Tip: for local dev you only strictly need a valid **PostgreSQL**, **Redis**, and **JWT signer key**.
> The mail/Cloudinary/SePay/Google values can stay as placeholders until you exercise those features.

### 4b. Run

```bash
cd backend

# Windows
./mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

The API starts at **http://localhost:8080/api/v1**

- Swagger UI: **http://localhost:8080/api/v1/swagger-ui.html**
- Health check: **http://localhost:8080/api/v1/actuator/health**

> On first run, Hibernate (`ddl-auto: update`) creates the schema automatically.

To build a runnable JAR instead:

```bash
./mvnw clean package        # output in backend/target/
```

---

## 5. Configure & run the frontend

```bash
cd frontend

# Create the env file from the template (edit values as needed)
cp .env.example .env        # Windows PowerShell: copy .env.example .env

# Install dependencies
npm install

# Start the dev server
npm run dev
```

The app runs at **http://localhost:3000** and proxies `/api` requests to the backend on
`localhost:8080` (configured in [frontend/vite.config.ts](frontend/vite.config.ts)).

Other scripts:

```bash
npm run build     # production build
npm run preview   # preview the production build
npm run lint      # run ESLint
```

---

## 6. Quick start (TL;DR)

```bash
# 1. Infra
cd backend
cp .env.example .env                       # edit passwords
docker compose --profile infra up -d

# 2. Backend  (edit application.yaml first)
./mvnw spring-boot:run                      # ./mvnw.cmd on Windows

# 3. Frontend (new terminal)
cd ../frontend
cp .env.example .env
npm install
npm run dev
```

Then open **http://localhost:3000**.

---

## 7. Run everything with Docker (alternative)

To build and run the backend + frontend as containers alongside infra:

```bash
cd backend
docker compose --profile infra --profile app up -d --build
```

> Note: the `frontend` service builds from `../store-fe` in
> [docker-compose.yml](backend/docker-compose.yml). Adjust that build context to
> `../frontend` if you run the frontend container from this repo layout.

---

## 8. Troubleshooting

| Problem | Fix |
|---------|-----|
| `java: invalid target release: 21` | Install JDK 21 and set `JAVA_HOME` to it |
| Backend can't connect to PostgreSQL | Ensure `docker compose --profile infra up -d` is running; verify credentials match between `.env` and `application.yaml` |
| `password authentication failed for user ...` | PostgreSQL username/password mismatch in `application.yaml` |
| Redis auth error | `spring.data.redis.password` must match `REDIS_PASSWORD` in `.env` |
| Kafka connection refused | Confirm the `kafka` container is healthy: `docker compose ps` |
| Port already in use (5432/6379/9094/8080/3000) | Stop the conflicting process or change the port |
| Frontend `/api` calls 404/timeout | Make sure the backend is running on `:8080` |
