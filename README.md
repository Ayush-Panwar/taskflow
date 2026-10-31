# TaskFlow

A small team task tracker: a Spring Boot and Kotlin REST API with a React frontend. People create projects, track tasks on a three-column board, and see at a glance what is overdue.

| | |
|---|---|
| **Backend** | Spring Boot 3.5, Kotlin, Gradle, Spring Security (JWT), Spring Data JPA, Flyway |
| **Frontend** | React 19, TypeScript, Vite, TanStack Query, dnd-kit |
| **Database** | H2 in memory by default, PostgreSQL through Docker Compose |
| **Tests** | JUnit 5, MockK, MockMvc, Jest, React Testing Library |

## Running it

### Option 1: without Docker

You need **JDK 17 or newer** and **Node 22.13 or newer**. Gradle downloads JDK 21 for the build by itself, so your installed JDK only has to be able to start Gradle.

```bash
# Terminal 1: the API, on http://localhost:8080
cd backend
./gradlew bootRun        # On Windows: .\gradlew.bat bootRun

# Terminal 2: the app, on http://localhost:5173
cd frontend
npm install
npm run dev
```

This uses an in-memory database that resets when the backend restarts, and creates sample data each time.

### Option 2: with Docker

```bash
docker compose up --build
```

The app is then at http://localhost:3000, backed by PostgreSQL. The defaults are for trying it locally; to change them, copy `.env.example` to `.env`.

### Sample login

| Email | Password |
|---|---|
| `demo@taskflow.dev` | `demo12345` |

The demo user owns three projects, one of them empty so that the empty states are visible. `priya@taskflow.dev` and `sam@taskflow.dev` use the same password and own nothing, which shows that projects are private. Seeding is on for the `dev` profile and Docker Compose, and off everywhere else by default.

### API documentation

Swagger UI is at http://localhost:8080/swagger-ui.html. Call `POST /api/auth/login`, then paste the `accessToken` into **Authorize**.

## Running the tests

```bash
cd backend && ./gradlew test    # 104 tests: services with MockK, full API with MockMvc and a real database
cd frontend && npm test         # 103 tests: components with Jest and React Testing Library
cd frontend && npm run lint
```

To run the backend tests against PostgreSQL, set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` first. CI runs them on both databases, and also builds and starts the Docker Compose stack.

## Architecture

```
Browser ── React ── /api ──► Controller ──► Service ──► Repository ──► Database
                                 │             │
                              validation    rules and
                              and DTOs      ownership checks
```

**Backend.** Each feature has its own package (`auth`, `user`, `project`, `task`), and inside it the layers are `Controller → Service → Repository`:

- **Controllers** handle HTTP only: they validate the request and call one service method.
- **Services** hold the business rules and own the transactions. They return DTOs, so JPA entities never leave this layer.
- **Repositories** are Spring Data interfaces.
- **Errors** all share one JSON shape. `GlobalExceptionHandler` covers everything raised during a request, and `SecurityErrorHandler` covers failures in the security filters, which run before any controller.

**Frontend.** `api/` is the only code that talks to the server. `features/` holds one folder per area of the app, `components/` holds reusable pieces such as `Button` and `Modal`, and `auth/` holds the session.

**Schema.** Flyway migrations own the database schema, and Hibernate only validates that the entities match it. The same migrations run on H2 and PostgreSQL.

### State management: TanStack Query

Almost all state in this app is server state: a local copy of data the API owns. TanStack Query is built for exactly that, and provides caching, loading and error flags, retries, cancellation and refreshing without any code here to maintain. Redux Toolkit would add a store and boilerplate for problems this app does not have. The little true client state is kept where it belongs:

- The session is in a small React Context, because the whole app needs it.
- Board filters are in the URL, so a filtered board can be reloaded, bookmarked and shared.
- Which modal is open is local component state.

## Design patterns

| Pattern | Where | Why it is used here |
|---|---|---|
| **Strategy** | `task/sort/`: `TaskSortStrategy`, `DueDateSortStrategy`, `PrioritySortStrategy` | Each sort has its own rule: priority sorts by business rank, and undated tasks always come last. `TaskSortResolver` picks one by name, so a new sort option is one new class, and clients can only sort by what is allowed. |
| **Specification** | `task/TaskSpecifications` | Each filter is a small condition that can be combined with others, which avoids a separate query for every combination of optional filters. |
| **Factory** | `task/TaskResponseFactory`, `common/error/ApiErrorFactory` | `TaskResponse` includes `overdue`, which needs the shared clock, so it is built in one place. Having one place to build errors is what keeps their shape consistent. |
| **Observer** | `TaskStatusChanged` event, `TaskActivityRecorder` | `TaskService` announces a status change without knowing about the activity log. A new reaction, such as a notification, would be a new listener with no change to the service. |
| **Repository** | `*Repository` interfaces | Keeps data access separate from business rules, and lets services be unit tested with mocks. |

## Assumptions and trade-offs

**Things the brief left open**

- **Three endpoints were added.** `GET /api/users` exists because the task form and the assignee filter need people to choose from. `GET /api/projects/{id}` exists because the board needs the project's name, even on a direct link. `GET /api/tasks/{id}/activity` is for the activity log.
- **All users are one team.** The brief has no team or membership model, so a task can be assigned to any registered user, and `GET /api/users` lists everyone. With open registration, that means any logged-in user can see every name and email. This fits a small internal team but not a public product, where a real team model would be the first thing to add.
- **An assignee gets no access.** The rule is that users only see projects they own, so being assigned a task does not let you open it. The assignee is a label until there is a sharing model.
- **Optional fields.** Only the title is required on a task. Status defaults to `TODO`, priority to `MEDIUM`, and due date and description are optional.
- **`PUT` semantics.** `PUT /api/tasks/{id}` replaces the editable fields, so leaving out the description, due date or assignee clears them. `status` and `priority` keep their current values if left out.
- **Public endpoints besides login.** `/actuator/health` (needed by the Docker health check, and it reveals only `UP` or `DOWN`) and the API documentation are also public. Setting `SWAGGER_ENABLED=false` hides the documentation. No task, project or user data is reachable without a token.

**Decisions worth explaining**

- **404, not 403, for other people's data.** A project someone else owns looks exactly like one that does not exist, so ids cannot be used to discover what exists. The ownership check is inside the database query, so it cannot be forgotten.
- **Each board column loads its own pages.** If the board fetched one mixed page of tasks, a column could look short or empty while its tasks sat on a later page. Each column instead uses the `status` filter and has its own "Load more".
- **The server decides what is overdue.** The API returns `overdue` on each task, so a highlighted card and the summary count can never disagree. A finished task is never overdue, and a task due today is not overdue yet.
- **The past-date rule applies on create only.** There are separate create and update request classes, because with a shared class an overdue task could not even have its title edited.
- **Moves save in the background.** A card moves immediately, and if the request fails it goes back and the user sees why.
- **Same origin instead of CORS.** The browser only talks to one origin: Vite's proxy in development and nginx in Docker. No CORS configuration is needed. Hosting the frontend on another domain would need it added.
- **JWT in `localStorage`.** This is simple and survives a reload, but scripts running on the page can read it, so an XSS flaw could expose the token. An `HttpOnly` cookie would prevent that, but would need CSRF protection. React's escaping and a one-hour expiry reduce the risk.
- **One "today" for everyone.** A single configured time zone (`TASKFLOW_TIME_ZONE`) decides what "today" means. Date input limits use the browser's local date, so someone far from that zone could see a date rejected near midnight. Per-user time zones would fix this.
- **A missing `JWT_SECRET` is allowed.** The app starts with a random key and logs a warning. That is safe, and tokens simply stop working after a restart. A secret that is set but too short stops the app from starting.
- **Keyboard users move cards with the dropdown.** Dragging is for mouse and touch. The status dropdown on every card does the same job for keyboards and screen readers.

## Configuration

Everything is set through environment variables, and no secrets are stored in the code. The one password in the repository is the public sample login above.

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` uses H2, `postgres` uses PostgreSQL |
| `JWT_SECRET` | random per start | Token signing key, at least 32 bytes |
| `JWT_EXPIRY` | `PT1H` | Token lifetime |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` | `localhost`, `5432`, `taskflow`, `taskflow` | PostgreSQL connection |
| `DB_PASSWORD` | none, required by `postgres` | PostgreSQL password |
| `TASKFLOW_TIME_ZONE` | the server's zone | What "today" means |
| `TASKFLOW_SEED_ENABLED`, `TASKFLOW_SEED_PASSWORD` | `true` and the sample password in `dev` | Sample data |
| `SWAGGER_ENABLED` | `true` | Set to `false` to hide the API docs |

## What I would do next

1. **Teams and sharing:** project members with roles, replacing the single-owner rule. Assignees would then actually be able to open their tasks.
2. **Refresh tokens,** so a session can last beyond an hour without a long-lived access token.
3. **Rate limiting on login,** to slow down password guessing.
4. **Live updates,** so teammates see each other's moves without reloading.
5. **Conflict detection for stale edits:** send the `version` with `PUT`, so that saving a form that has been open for a while warns about newer changes.
6. **Browser end-to-end tests** with Playwright, especially for drag and drop, which the component tests cannot exercise.
