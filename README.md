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
- Lexical analysis with case-insensitive `SELECT`, `CREATE INDEX`, `FROM`, `JOIN`, `ON`, `WHERE`, `GROUP BY`, `COUNT`, `SUM`, `AVG`, and boolean keywords.
- Parsing of `SELECT`, one-column `CREATE INDEX`, optional qualified columns and aggregate expressions, `FROM`, one equality-based inner `JOIN`, optional `WHERE`, optional `GROUP BY`, comparison expressions, integer/string/boolean literals, and optional trailing semicolons.
- Immutable AST generation for supported queries.
- `POST /api/query/parse` returns the parsed AST and uses HTTP 400 for invalid user SQL.
- The frontend displays successful parses as an expandable AST tree and shows parser/backend errors clearly.
- An in-memory, case-insensitive `users` and `expenses` demo schema is seeded at startup. Data resets when the backend restarts.
- `SELECT` execution supports wildcard or named projection, optional `WHERE` comparisons, and equality-based inner joins using either nested-loop or hash execution.
- `GROUP BY` supports one or more grouping columns with `COUNT(*)`, `COUNT(column)`, `SUM(integer_column)`, and `AVG(integer_column)`.
- Aggregation runs after `WHERE` and joins. Group keys preserve integer, string, and boolean value types; output groups retain deterministic first-seen input order.
- Qualified references such as `users.id` and `expenses.user_id` resolve across joined schemas. Ambiguous unqualified references return HTTP 400.
- `POST /api/query/execute` returns result columns, rows, row count, and `rowsScanned`/`rowsReturned` metrics. Unknown tables or columns and type mismatches return HTTP 400.
- Each execution response also contains an explicit plan tree with `PROJECTION`, `FILTER`, `AGGREGATE`, `NESTED_LOOP_JOIN`, `HASH_JOIN`, `TABLE_SCAN`, and `INDEX_SCAN` nodes, measured input/output rows, and operator details.
- Join strategy is selected explicitly per execution with `NESTED_LOOP` (the default) or `HASH`; QueryScope does not choose automatically. Hash joins build on the smaller input, breaking equal-size ties in favor of the left input.
- A hand-built order-4 B+ tree indexes integer and string columns. Indexes are populated from existing rows and maintained for later inserts. Exact and inclusive/exclusive range lookups return row positions, including duplicate keys.
- Scan strategy is selected explicitly per execution with `TABLE` (the default) or `INDEX`; QueryScope does not optimize or automatically choose an index. An index scan requires a matching single-column index and a `WHERE` equality/range predicate.
- `GET /api/schema` exposes tables, columns, and indexes. `POST /api/schema/indexes` accepts `{ "name": "idx_amount", "table": "expenses", "column": "amount" }` and creates an index.
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

The same join can be executed as a hash join by sending `joinStrategy: "HASH"`. Its plan reports the build/probe side, build and probe row counts, rows inserted, hash lookups, bucket count, matches, and output rows. Nested-loop joins report left/right rows, comparisons, matches, and output rows. Both operators share the same schema resolution, equality validation, and row assembly logic.

For a left input of `N` rows and a right input of `M` rows, nested-loop execution performs `N × M` comparisons. Hash execution builds a hash table on the smaller input and probes the larger input, with one lookup per probe row plus hash-bucket matching work. This is an execution-strategy comparison fixture, not an optimizer: users explicitly choose the strategy and results are compared as unordered row sets in the frontend.

`SELECT * FROM users` uses only `TableScan`, while named projections add a `Projection` node. A selected index scan replaces the table/filter pair and reports the index name, lookup count, leaf entries visited, rows examined, and rows returned. Joined wildcard results expose qualified column names such as `users.id` and `expenses.id`. Plan metadata reports only measured row counts; nested-loop joins additionally report left/right rows, comparisons, matches, and output rows. Aggregate nodes additionally report grouping expressions, aggregate functions, input rows, groups produced, and output rows. `COUNT` and `SUM` use the existing integer-compatible long-valued representation; `AVG` returns `DOUBLE` without truncation. With no input rows, `COUNT` returns one row containing zero, grouped aggregates return no groups, and ungrouped `SUM`/`AVG` return a clear execution error because the current row model has no NULL value.

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

Example aggregation:

```sql
SELECT user_id, SUM(amount)
FROM expenses
WHERE amount > 50
GROUP BY user_id;
```

The plan for this query is `Projection → Aggregate → Filter → TableScan`. For a joined aggregate, the aggregate sits above the selected nested-loop or hash join. Every selected non-aggregate column must appear in `GROUP BY`; QueryScope does not choose arbitrary values.

Example index workflow:

```http
POST /api/schema/indexes
Content-Type: application/json

{ "name": "idx_expenses_amount", "table": "expenses", "column": "amount" }
```

```json
{ "sql": "SELECT description FROM expenses WHERE amount > 50", "scanStrategy": "INDEX" }
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
- `POST /api/query/execute` accepts `{ "sql": "...", "joinStrategy": "NESTED_LOOP" | "HASH", "scanStrategy": "TABLE" | "INDEX" }` and returns `{ "columns": [...], "rows": [...], "rowCount": number, "metrics": { "rowsScanned": number, "rowsReturned": number }, "executionPlan": {...} }`. Both strategies are optional and default to `NESTED_LOOP` and `TABLE`; values are case-insensitive.
- `GET /api/schema` returns `{ "tables": [{ "name": "expenses", "columns": [{ "name": "amount", "type": "INTEGER" }], "indexes": [{ "name": "idx_amount", "column": "amount" }] }] }`.
- `POST /api/schema/indexes` accepts `{ "name": "idx_amount", "table": "expenses", "column": "amount" }` and returns the updated schema. Indexes currently support one INTEGER or STRING column per index.
- Aggregate queries use the same execute endpoint and return deterministic result column names such as `SUM(amount)` and `AVG(amount)`. Aggregate plan details include `groupBy`, `functions`, `groups`, `inputRows`, and `outputRows`.
- On non-join queries, a supplied `joinStrategy` is accepted but has no effect; table scans, filters, and projections keep their existing plan.
- The execute response also includes `executionPlan`, a recursive tree whose nodes expose `type`, operator-specific `details`, `inputRows`, `outputRows`, and `children`.
- Invalid SQL returns HTTP 400 with `{ "error": "..." }`.

The current demo database supports schema inspection and index creation, but remains intentionally limited: table creation, row mutation, persistence, composite/unique/partial/expression indexes, and `DROP INDEX` are future work.

## Roadmap

1. ~~SQL lexer/parser~~
2. ~~AST~~
3. ~~In-memory tables~~
4. ~~Table scan/filter/projection~~
5. ~~Execution plans~~
6. ~~Joins~~
7. ~~GROUP BY and aggregates~~
8. ~~B+ tree indexes and index scans~~
9. Query optimizer
10. Benchmarking
11. Visualization

The current milestone intentionally omits `LEFT`, `RIGHT`, `FULL`, `CROSS`, and `NATURAL JOIN`, multiple join chains, `USING`, non-equality joins, subqueries, `HAVING`, `ORDER BY`, `LIMIT`, `MIN`, `MAX`, `DISTINCT`, window functions, `NULL` semantics, mutations, composite/unique/partial/expression indexes, `DROP INDEX`, query optimization, persistence, and cost/timing estimates. The plan is generated directly from the parsed query; predicate pushdown, join ordering, and automatic index selection are future work.
