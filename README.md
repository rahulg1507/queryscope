# QueryScope

> An experimental relational database engine with an interactive query-plan visualizer.

QueryScope is a database-engineering project, not a CRUD application. The long-term goal is to build a small relational database engine in Java and a React interface that makes query execution and planning visible.

## Project structure

```text
queryscope/
├── backend/       # Java/Spring Boot HTTP API
├── frontend/      # React/TypeScript web interface
├── .gitignore
└── README.md
```

## Architecture

The frontend runs as a separate development server and calls the backend through `/api`. The backend has distinct controller, service, model, repository, and engine packages so the database implementation has a clear home. Parsing remains pure application logic: it turns SQL text into an immutable AST. The execution service then builds an execution plan and evaluates it against the in-memory database.

## Tech stack

- Backend: Java 17+, Spring Boot 3.5, Maven, JUnit 5
- Frontend: React, TypeScript, Vite, Vitest, Testing Library
- Ports: backend `8080`, frontend `3000`

No Firebase, Firestore, authentication, or external application database is used.

## Run locally

### Backend

From `backend/`:

```bash
# Windows PowerShell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw test
./mvnw spring-boot:run
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
- Lexical analysis with case-insensitive `SELECT`, `FROM`, `JOIN`, `ON`, `WHERE`, and boolean keywords.
- Parsing of `SELECT`, optional qualified columns, `FROM`, one equality-based inner `JOIN`, optional `WHERE`, comparison expressions, integer/string/boolean literals, and optional trailing semicolons.
- Immutable AST generation for supported queries.
- `POST /api/query/parse` returns the parsed AST and uses HTTP 400 for invalid user SQL.
- The frontend displays successful parses as an expandable AST tree and shows parser/backend errors clearly.
- An in-memory, case-insensitive `users` and `expenses` demo schema is seeded at startup. Data resets when the backend restarts.
- `SELECT` execution supports wildcard or named projection, optional `WHERE` comparisons, and inner joins using a nested-loop operator.
- Qualified references such as `users.id` and `expenses.user_id` resolve across joined schemas. Ambiguous unqualified references return HTTP 400.
- `POST /api/query/execute` returns result columns, rows, row count, and `rowsScanned`/`rowsReturned` metrics. Unknown tables or columns and type mismatches return HTTP 400.
- Each execution response also contains an explicit plan tree with `PROJECTION`, `FILTER`, `NESTED_LOOP_JOIN`, and `TABLE_SCAN` nodes, measured input/output rows, and operator details.
- The frontend can parse SQL to inspect its AST or run it to display result rows, execution metrics, and the generated plan.

## Execution plan architecture

The AST describes what the user wrote. It is deliberately separate from the execution plan, which describes how QueryScope currently executes that statement:

```text
AST
 ↓
ExecutionPlanBuilder
 ↓
ExecutionPlan
 ↓
ExecutionPlanExecutor
 ↓
QueryResult + executionPlan
```

The current pipeline is a straightforward operator tree with no optimization:

```text
Projection (columns: name, age)
└── Filter (condition: age > 18)
    └── TableScan (table: users)
```

For a join, the plan branches at the join node:

```text
Projection (columns: users.name, expenses.amount)
└── NestedLoopJoin (condition: users.id = expenses.user_id)
    ├── TableScan (table: users)
    └── TableScan (table: expenses)
```

`SELECT * FROM users` uses only `TableScan`, while named projections add a `Projection` node. Joined wildcard results expose qualified column names such as `users.id` and `expenses.id`. Plan metadata reports only measured row counts; nested-loop joins additionally report left/right rows, comparisons, matches, and output rows. Timing, cost, estimates, and optimizer decisions are not exposed.

Example SQL:

```sql
SELECT name, age
FROM users
WHERE age > 18;
```

Example join:

```sql
SELECT users.name, expenses.amount
FROM users
JOIN expenses
ON users.id = expenses.user_id;
```

Example AST response:

```json
{
  "type": "SELECT",
  "columns": [
    { "type": "COLUMN", "name": "name" },
    { "type": "COLUMN", "name": "age" }
  ],
  "from": { "type": "TABLE", "name": "users" },
  "where": {
    "type": "COMPARISON",
    "operator": "GREATER_THAN",
    "left": { "type": "COLUMN", "name": "age" },
    "right": { "type": "NUMBER", "value": 18 }
  }
}
```

## API

- `POST /api/query/parse` accepts `{ "sql": "..." }` and returns an AST for supported SQL.
- `POST /api/query/execute` accepts `{ "sql": "..." }` and returns `{ "columns": [...], "rows": [...], "rowCount": number, "metrics": { "rowsScanned": number, "rowsReturned": number }, "executionPlan": {...} }`.
- The execute response also includes `executionPlan`, a recursive tree whose nodes expose `type`, operator-specific `details`, `inputRows`, `outputRows`, and `children`.
- Invalid SQL returns HTTP 400 with `{ "error": "..." }`.

The current demo database is intentionally read-only. `GET /api/schema`, table creation, row mutation, persistence, and full SQL semantics are future work.

## Roadmap

1. ~~SQL lexer/parser~~
2. ~~AST~~
3. ~~In-memory tables~~
4. ~~Table scan/filter/projection~~
5. ~~Execution plans~~
6. ~~Joins~~
7. Indexes
8. Query optimizer
9. Benchmarking
10. Visualization

The current milestone intentionally omits `LEFT`, `RIGHT`, `FULL`, `CROSS`, and `NATURAL JOIN`, multiple join chains, `USING`, non-equality joins, subqueries, grouping, ordering, limits, `NULL` semantics, mutations, indexes, hash joins, query optimization, persistence, and cost/timing estimates. The plan is generated directly from the parsed query; predicate pushdown, join ordering, and index selection are future work.
