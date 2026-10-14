# HireSense

Interview preparation and skill assessment platform. Upload your resume, discover
skill gaps, take role-specific mock interviews with structured feedback, and learn
with personalized study plans - all in one place.

## Features

- **ATS Resume Analysis** - upload PDF/DOCX/TXT resumes, extract skills, and get an
  ATS score, keyword match, strengths and improvement suggestions.
- **Skill Gap Analysis** - compare your skills against curated job roles
  (Java Backend, Full Stack, Frontend, DevOps, Data Scientist, Data Engineer,
  QA Automation) and see exactly what to learn next.
- **Mock Interviews** - generate role-specific, difficulty-tuned question sets and
  receive an evaluation (score, feedback, strengths, missing keywords, suggested
  answer) on every response.
- **Personalized Learning Plans** - turn weak topics and skill gaps into a prioritized,
  trackable study plan with resources and daily targets.
- **Progress Tracking** - interviews completed, average scores, questions answered and
  plan completion on one dashboard.
- **Admin Dashboard** - platform stats and user role management.
- **Swagger API documentation** at `/swagger-ui.html`.

## Tech Stack

| Layer      | Technology                                                       |
|------------|------------------------------------------------------------------|
| Backend    | Java 21, Spring Boot 3.3, Spring Data JPA, Spring Security + JWT |
| Frontend   | React 18, TypeScript, Vite 5, React Router, Axios                |
| Database   | PostgreSQL 16 (H2 for tests)                                     |
| AI         | Pluggable provider - OpenAI or built-in offline mock provider    |
| Infra      | Docker / docker-compose                                          |

## Project Structure

```
Hiresense Ai/
├── backend/            # Spring Boot REST API
│   ├── src/main/java/com/hiresense/
│   │   ├── controller/    # REST controllers
│   │   ├── service/       # business logic (+ service/ai for AI providers)
│   │   ├── repository/    # Spring Data repositories
│   │   ├── entity/        # JPA entities
│   │   ├── dto/           # request/response DTOs
│   │   ├── security/      # JWT auth + security config
│   │   ├── enums/         # shared enums
│   │   ├── config/        # beans, swagger, data initializer
│   │   └── exception/     # global error handling
│   └── src/test/          # integration + unit tests (H2)
├── frontend/           # React + Vite SPA
│   └── src/
│       ├── api/           # axios client + interceptors
│       ├── auth/          # auth context
│       ├── components/    # shared UI
│       ├── pages/         # page components
│       └── types/         # TypeScript API types
├── database/           # PostgreSQL bootstrap scripts
└── docker-compose.yml  # full stack orchestration
```

## Quick Start with Docker

1. Create a `.env` file from the template and set a strong `JWT_SECRET`:

   ```bash
   copy .env.example .env
   # Linux/macOS: cp .env.example .env
   ```

   Generating a secret:

   ```bash
   openssl rand -base64 48
   ```

2. Build and start all services:

   ```bash
   docker compose up --build
   ```

3. Open the app:

   - Web app: http://localhost:8090
   - Swagger docs: http://localhost:8080/swagger-ui.html
   - PostgreSQL: `localhost:5432` (user/password/db all `hiresense`)

The default administrator account is created on first boot:

- Email: `admin@hiresense.com`
- Password: `Admin@12345`

> Change `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` in your `.env` before first boot.

## Local Development

### Prerequisites

- Java 21+
- Node.js 18+ and npm
- Maven 3.9+ (or use the bundled `mvnw` wrapper)
- PostgreSQL 16 running locally

### 1. Database

Create a database:

```sql
CREATE DATABASE hiresense;
CREATE USER hiresense WITH PASSWORD 'hiresense';
GRANT ALL PRIVILEGES ON DATABASE hiresense TO hiresense;
```

The schema is created automatically by Hibernate (`ddl-auto: update`). The backend
also seeds an admin account and 7 job roles with 58 skills on first boot.

### 2. Backend

Set environment variables (PowerShell example):

```powershell
$env:JWT_SECRET = "<at-least-32-characters>"
$env:DB_URL = "jdbc:postgresql://localhost:5432/hiresense"
$env:DB_USERNAME = "hiresense"
$env:DB_PASSWORD = "hiresense"
```

Run:

```bash
cd backend
mvnw.cmd spring-boot:run        # PowerShell
./mvnw spring-boot:run          # Linux/macOS
```

The API is served at `http://localhost:8080` with Swagger UI at
`/swagger-ui.html`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs at `http://localhost:5173` and proxies `/api` requests to
`http://localhost:8080`.

### Running tests

```bash
cd backend
mvnw.cmd test        # PowerShell
./mvnw test          # Linux/macOS
```

Tests run against an in-memory H2 database and cover registration/login,
resume upload + skill gap analysis, the full interview flow, and the mock AI
provider.

## AI Configuration

HireSense uses a pluggable AI provider. You can run the entire platform without
any external key or network access:

- **Mock provider (`AI_PROVIDER=mock`, default)** - deterministic, rule-based
  analysis used for local development, offline environments, and the test suite.
  Intelligence features are fully functional but do not call an external model.
- **OpenAI provider (`AI_PROVIDER=openai`)** - requires `OPENAI_API_KEY`; uses a
  validating JSON parser so responses are parsed and validated before being stored.

To keep product output honest, the application does not claim live-AI results when
the mock provider is active:

- `GET /api/config` is a public endpoint that reports `aiProvider` and
  `aiConfigured`, so clients can adapt.
- When the mock provider is active, the web UI shows a development notice
  ("Development mode: AI features use a built-in mock provider") until a real
  provider is configured.

> Use `AI_PROVIDER=mock` only for development/test environments. Configure a real
> provider (or remove the dev notice) for production deployments.

| Variable                 | Default                       | Notes                                      |
|--------------------------|-------------------------------|--------------------------------------------|
| `AI_PROVIDER`            | `mock`                        | `mock` or `openai`                          |
| `OPENAI_API_KEY`         | empty                         | required for `openai`                      |
| `OPENAI_BASE_URL`        | `https://api.openai.com/v1`   | override for compatible providers          |
| `OPENAI_MODEL`           | `gpt-4o-mini`                 |                                            |
| `OPENAI_TEMPERATURE`     | `0.3`                         | 0-1, lower is more deterministic           |
| `OPENAI_MAX_TOKENS`      | `2000`                        |                                            |
| `OPENAI_TIMEOUT_SECONDS` | `60`                          |                                            |

## Configuration Reference

| Variable                  | Default                  | Description                                  |
|---------------------------|--------------------------|----------------------------------------------|
| `SPRING_PROFILES_ACTIVE`  | `dev`                    | `dev` / `prod` / `test`                      |
| `SERVER_PORT`             | `8080`                   | Backend port                                 |
| `JWT_SECRET`              | **required**             | JWT signing key, min 32 characters           |
| `JWT_EXPIRATION_MS`       | `86400000`               | Token lifetime (1 day default)               |
| `DB_URL`                  | -                        | JDBC URL (used by dev/prod profiles)         |
| `DB_USERNAME` / `DB_PASSWORD` | -                    | Database credentials                         |
| `APP_UPLOAD_DIR`          | `./uploads`              | Resume storage directory                     |
| `APP_ADMIN_EMAIL`         | `admin@hiresense.com`    | Seeded admin email                           |
| `APP_ADMIN_PASSWORD`      | `Admin@12345`            | Seeded admin password                        |
| `APP_ADMIN_NAME`          | `HireSense Admin`        | Seeded admin name                            |
| `CORS_ALLOWED_ORIGINS`    | localhost 5173 origins   | Comma-separated allowed browser origins      |
| `MOCK_QUESTION_BANK_SIZE` | `6`                      | Candidate questions per topic (mock AI)      |
| `LOG_LEVEL`               | `INFO`                   | Log level for `com.hiresense`                |

## Security

- Passwords are hashed with BCrypt.
- All protected endpoints require a `Bearer` JWT (unless public).
- `/api/admin/**` is restricted to the `ADMIN` role.
- Secrets must be supplied via environment variables - never hard-coded.
- `JWT_SECRET` has no default and the application will refuse to start without it.
- CORS is restricted to the allowed origins configured for the frontend.
- File uploads are limited to 10MB and validated for allowed resume formats.
- Schema bootstrap uses `ddl-auto: update` (dev and prod profiles) for a
  self-contained setup; switch to `validate` in hardened environments.

## API Overview

| Area            | Endpoints                                                       |
|-----------------|-----------------------------------------------------------------|
| App config      | `GET /api/config` (public)                                       |
| Auth            | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Profile         | `GET`/`PUT /api/users/me/profile`                               |
| Resumes         | `POST /api/resumes/upload`, `GET`/`GET {id}`/`DELETE /api/resumes/{id}`, `POST /api/resumes/{id}/analyze` |
| Job roles       | `GET /api/job-roles`, `GET /api/job-roles/{id}`, `GET /api/skills`, `POST /api/job-roles/{id}/skill-gap` |
| Interviews      | `POST /api/interviews/setup`, `GET /api/interviews`, `GET /api/interviews/{id}`, `POST .../start`, `GET .../questions`, `POST .../questions/{qid}/answer`, `GET .../result` |
| Learning plans  | `POST /api/learning-plans`, `GET /api/learning-plans`, `GET .../{id}`, `POST .../{id}/items/{itemId}/complete`, `DELETE .../{id}` |
| Progress        | `GET /api/progress`, `GET /api/progress/summary`                  |
| Admin           | `GET /api/admin/stats`, `GET /api/admin/users`, `PATCH /api/admin/users/{userId}/role` |

Full interactive documentation is available in Swagger UI at
`/swagger-ui.html`.

## License

Private project. All rights reserved.