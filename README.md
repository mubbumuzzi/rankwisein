# RankWise

**Your Rank. Your Right College.**

A personalized TS/TG EAPCET (EAMCET) counselling platform. Students enter their rank, category, gender, and preferred branches, and RankWise analyzes historical cutoff data to return **Dream**, **Target**, and **Safe** colleges.

## Stack

- **Frontend:** Angular 20, TypeScript, Tailwind CSS, Angular Material
- **Backend:** Spring Boot 3, Java 21, Maven
- **Database:** PostgreSQL + Flyway
- **Auth:** Spring Security + JWT
- **Docs:** Swagger / OpenAPI
- **PDF import:** Apache PDFBox (deterministic parser)
- **Deploy:** Docker + Docker Compose

## Run

```bash
docker-compose up --build
```

| Service   | URL                                         |
|-----------|---------------------------------------------|
| Frontend  | http://localhost                            |
| Backend   | http://localhost:8080                       |
| Swagger   | http://localhost:8080/swagger-ui.html       |
| Health    | http://localhost:8080/actuator/health       |

The database schema and seed data (branches, sample colleges, an admin user, and sample cutoffs) are created automatically by Flyway on first start.

### Default admin credentials

Defined in `.env` (change before any non-local deploy):

```
username: admin
password: admin123
```

## Configuration

All runtime config has working defaults in `.env`. No extra configuration is required for local use.

| Variable            | Purpose                          |
|---------------------|----------------------------------|
| `POSTGRES_*`        | Database name / user / password  |
| `JWT_SECRET`        | JWT signing secret               |
| `JWT_EXPIRATION_MS` | Token lifetime (ms)              |
| `ADMIN_*`           | Seeded admin account             |

## Local development (without Docker)

> Angular 20 requires Node >= 20.19. The Docker frontend build uses Node 22, so the host Node version only matters for local dev.

```bash
# Backend (needs a local Postgres on :5432 matching .env)
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm install && npm start   # http://localhost:4200
```

## Admin features

- JWT-protected admin panel
- Dashboard metrics (colleges, branches, cutoff records, searches, latest imports)
- College / Branch / Cutoff CRUD with search, filter, pagination, CSV export
- **PDF import:** upload a TG EAPCET cutoff PDF, preview parsed rows (search / filter / delete), then approve. Deterministic parser, batch inserts (1000/batch).

## Project layout

```
rankWise/
├── docker-compose.yml
├── .env
├── backend/    # Spring Boot
└── frontend/   # Angular
```
