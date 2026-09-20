# QueryScope

> An experimental relational database engine with an interactive query-plan visualizer.

QueryScope is a database-engineering project, not a CRUD application. The long-term goal is to build a small relational database engine in Java and a React interface that makes query execution and planning visible. The database engine is intentionally not implemented yet.

## Project structure

```text
queryscope/
├── backend/       # Java/Spring Boot HTTP API
├── frontend/      # React/TypeScript web interface
├── .gitignore
└── README.md
```

## Architecture

The frontend runs as a separate development server and calls the backend through `/api`. The backend has distinct controller, service, model, repository, and engine packages so the future database implementation has a clear home. Only the health endpoint is active in this milestone.

## Tech stack

- Backend: Java 17+, Spring Boot 3.5, Maven, JUnit 5
- Frontend: React, TypeScript, Vite, Vitest, Testing Library
- Ports: backend `8080`, frontend `3000`

No Firebase, Firestore, authentication, or external application database is used.

## Run locally

### Backend

From `backend/`:

```bash
mvn test
mvn spring-boot:run
```

The API is available at `http://localhost:8080`.

### Frontend

From `frontend/`:

```bash
npm install
npm start
```

The web interface is available at `http://localhost:3000`. The Vite development proxy forwards `/api` requests to the backend at port 8080.

Frontend checks:

```bash
npm test
npm run build
```

## Current capabilities

- `GET /api/health` returns `{ "status": "ok" }`.
- The frontend reports whether the backend health check is reachable.
- The SQL editor, results panel, and query-plan panel are intentionally placeholders.
- No SQL execution or fake query results are implemented.

## API roadmap

Future API areas include `POST /api/query`, `GET /api/schema`, `POST /api/tables`, and `POST /api/tables/{table}/rows`. They are not implemented yet.

## Roadmap

1. SQL lexer/parser
2. AST
3. In-memory tables
4. Table scan/filter/projection
5. Execution plans
6. Joins
7. Indexes
8. Query optimizer
9. Benchmarking
10. Visualization

None of the roadmap items are claimed as implemented in this foundation milestone.
