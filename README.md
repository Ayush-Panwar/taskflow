# TaskFlow

A small team task tracker: a Spring Boot and Kotlin REST API with a React frontend. People create projects, track tasks on a three-column board, and see at a glance what is overdue.

**Demo video (4:28):** https://youtu.be/Qm2PW9LK86c

| | |
|---|---|
| **Backend** | Spring Boot 3.5, Kotlin, Gradle, Spring Security (JWT), Spring Data JPA, Flyway |
| **Frontend** | React 19, TypeScript, Vite, TanStack Query, dnd-kit |
| **Database** | H2 in memory by default, PostgreSQL through Docker Compose |
| **Tests** | JUnit 5, MockK, MockMvc, Jest, React Testing Library |

**Everything the brief asks for is built, including all five bonus items.** This page explains what each part does and where to find it, so you can follow the project without running it. To see it working, watch the demo video above, or run it and take the [five-minute tour](#a-five-minute-tour).

## What is built

### The bonus items

| Bonus item | What it does here | Where to look |
|---|---|---|
| **Database migrations** | Flyway builds the whole schema from four versioned SQL files, on both H2 and PostgreSQL. Hibernate only validates against them, so the app will not start if the code and the schema disagree. [More](#database-migrations-flyway) | [`db/migration/`](backend/src/main/resources/db/migration) |
| **Activity log** | Records who changed a task's status, between which columns, and when. It appears as **History** in the task form, newest first. [More](#activity-log) | [`task/activity/`](backend/src/main/kotlin/com/taskflow/task/activity), [`TaskHistory.tsx`](frontend/src/features/board/TaskHistory.tsx) |
| **Docker Compose** | One command builds and starts PostgreSQL, the API and the frontend, each waiting until the one before it is healthy. | [`docker-compose.yml`](docker-compose.yml) |
| **OpenAPI and Swagger UI** | Every endpoint is documented and can be tried from the browser. | http://localhost:8080/swagger-ui.html |
| **GitHub Actions** | Every push runs the backend tests on both H2 and PostgreSQL, and the frontend lint, tests and build. It then starts the whole Docker stack and signs in through it. | [`ci.yml`](.github/workflows/ci.yml) |

### The app

**Accounts and security**

- Register and sign in, with validation on every field. Passwords are hashed with BCrypt.
- All project, task and user endpoints need a valid JWT, which lasts one hour.
- Logged-out users are sent to sign in, and an expired token returns them there with a clear message.
- Users can only reach their own projects. Anything else returns a 404, so ids can't reveal what exists.

**The board**

- Three columns (To Do, In Progress, Done), and a summary bar with the count per status and the number overdue.
- Move a card by dragging it, with a mouse or by touch, or with the status dropdown on every card, which also works from the keyboard.
- Moves appear at once and save in the background. If a save fails, the card goes back and a message says why.
- Overdue tasks are highlighted in red. The server decides what is overdue, so the cards and the summary always agree.
- Filter by priority and assignee, and sort by due date or priority in either direction. These are kept in the URL, so a view survives a reload and can be shared.
- Each column loads its own pages, with a "Load more" button.

**Tasks**

- Create and edit in a modal: title, description, priority, due date and assignee.
- Delete with a confirmation step.
- Validation messages appear under each field, from both the form and the API. A title is required and limited to 120 characters, and a new task can't be due in the past.

**When things go wrong**

- Every screen that loads data has loading, empty and error states, with a retry button.
- If the server can't be reached, the message says so, rather than waiting forever.
- All API errors share one JSON shape, with a message per field on a 400.
- If two requests change the same task at once, the second gets a 409 instead of silently overwriting the first.
- The layout works from phone width to desktop.

**Engineering**

- A layered backend (`Controller → Service → Repository`). JPA entities never leave the service layer; the API only returns DTOs.
- [Five design patterns](#design-patterns), each named and explained.
- More than 100 backend tests and more than 100 frontend tests, [run on every push](#running-the-tests).
- Sample data with a sample login, and no real secrets in the code.

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

## A five-minute tour

With the app running:

1. **See the board.** Sign in with the sample login and open **Website Redesign**. The summary bar shows the counts, and overdue cards are red.
2. **Move a card.** Drag it to another column. The summary updates at once, and a reload shows it was saved. The dropdown on each card does the same from the keyboard.
3. **Read the activity log.** Click the title of the card you moved. **History**, under the form, shows who moved it, between which columns, and when.
4. **Filter and sort.** Choose an assignee and a priority, and watch the URL change. Reload, and the view is still there. Then try the sort options.
5. **Break the rules.** Click **New task** and submit it empty, then with a past due date. Each field shows its own message.
6. **Delete a task.** It asks for confirmation first.
7. **Try someone else's project.** Sign out, register a new user and create a project. Sign back in with the sample login and open that project's URL. You get "Project not found".
8. **Go offline.** Turn on Offline in the browser's developer tools and move a card. It goes back, and a message says why. Turn Offline off, and retry buttons recover the page.
9. **Check the rest.** Open [Swagger UI](http://localhost:8080/swagger-ui.html) for the API, and this repository's **Actions** tab for the test runs.

## Running the tests

```bash
cd backend && ./gradlew test    # Services with MockK, and the full API with MockMvc and a real database
cd frontend && npm test         # Components with Jest and React Testing Library
cd frontend && npm run lint
```

| Tests | Tools | What they cover |
|---|---|---|
| **Service tests** | JUnit 5, MockK | Business rules in the services, with the database mocked |
| **API integration tests** | `@SpringBootTest`, MockMvc, a real database | The full request path, including Flyway's migrations: validation, security, ownership, filters, sorting, pagination, the summary and the activity log |
| **Frontend tests** | Jest, React Testing Library | Forms, the board, task cards, protected routes, the API client and the session |

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

### Database migrations (Flyway)

Flyway owns the schema. It applies any new migrations when the app starts, so every database gets the same changes in the same order. Hibernate only validates that the entities match (`ddl-auto: validate`), and never changes a table itself. The same files run on H2 and PostgreSQL, and CI proves it on both.

| Migration | What it creates |
|---|---|
| `V1__create_users.sql` | `users`, with a unique email |
| `V2__create_projects.sql` | `projects`, each owned by a user |
| `V3__create_tasks.sql` | `tasks`, with database checks on status and priority, and indexes for the board's queries |
| `V4__create_task_activity.sql` | `task_activity`, the status history of each task |

A schema change is a new numbered file. Existing migrations are never edited.

### Activity log

```
TaskService ── TaskStatusChanged event ──► TaskActivityRecorder ──► task_activity table
                                                                          │
History in the task form ◄── GET /api/tasks/{id}/activity ◄───────────────┘
```

- **What is recorded:** who changed the status, the old and new status, and when.
- **Every route is covered:** a drag, the dropdown, `PATCH /api/tasks/{id}/status` and a status sent with `PUT` all record an entry. A save that doesn't change the status records nothing.
- **It can't get out of step:** the entry is saved in the same transaction as the status change, so both are saved or neither is.
- **It follows the same access rule:** only the project's owner can read a task's history, and deleting a task removes its history.
- **It uses the Observer pattern:** `TaskService` announces the change without knowing about the log, so a new reaction, such as a notification, would be a new listener.

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
- **All users are one team.** The brief has no team or membership model, so a task can be assigned to any registered user, and `GET /api/users` lists everyone's name. Email addresses are left out, so accounts can't be harvested through it. A real team model would be the first thing to add for a public product.
- **An assignee gets no access.** The rule is that users only see projects they own, so being assigned a task does not let you open it. The assignee is a label until there is a sharing model.
- **The activity log covers status changes.** That is what the brief describes, so editing a task's title, priority or other fields saves normally but adds no history.
- **Optional fields.** Only the title is required on a task. Status defaults to `TODO`, priority to `MEDIUM`, and due date and description are optional.
- **`PUT` semantics.** `PUT /api/tasks/{id}` replaces the editable fields, so leaving out the description, due date or assignee clears them. `status` and `priority` keep their current values if left out.
- **Public endpoints besides login.** `/actuator/health` (needed by the Docker health check, and it reveals only `UP` or `DOWN`) and the API documentation are also public. Setting `SWAGGER_ENABLED=false` hides the documentation. No task, project or user data is reachable without a token.

**Decisions worth explaining**

- **404, not 403, for other people's data.** A project someone else owns looks exactly like one that does not exist, so ids cannot be used to discover what exists. Every task and project operation goes through one of two checks, `ProjectService.requireOwned` or `TaskService.requireOwned`, which have the owner in their database query.
- **Each board column loads its own pages.** If the board fetched one mixed page of tasks, a column could look short or empty while its tasks sat on a later page. Each column instead uses the `status` filter and has its own "Load more".
- **The server decides what is overdue.** The API returns `overdue` on each task, so the highlighted cards and the summary count follow the same rule, whatever the browser's clock says. A finished task is never overdue, and a task due today is not overdue yet.
- **The past-date rule applies on create only.** There are separate create and update request classes, because with a shared class an overdue task could not even have its title edited.
- **Moves save in the background.** A card moves immediately, and if the request fails it goes back and the user sees why. Moves are sent one at a time, so moving a card twice quickly can't make the requests overlap.
- **Same origin instead of CORS.** The browser only talks to one origin: Vite's proxy in development and nginx in Docker. No CORS configuration is needed. Hosting the frontend on another domain would need it added.
- **JWT in `localStorage`.** This is simple and survives a reload, but scripts running on the page can read it, so an XSS flaw could expose the token. An `HttpOnly` cookie would prevent that, but would need CSRF protection. React's escaping and a one-hour expiry reduce the risk.
- **One "today" for everyone.** A single configured time zone (`TASKFLOW_TIME_ZONE`) decides what "today" means. Date input limits use the browser's local date, so someone far from that zone could see a date rejected near midnight. Per-user time zones would fix this.
- **A missing `JWT_SECRET` is allowed.** The app starts with a random key and logs a warning. That is safe, and tokens simply stop working after a restart. A secret that is set but too short stops the app from starting.
- **Keyboard users move cards with the dropdown.** Dragging is for mouse and touch. The status dropdown on every card does the same job for keyboards and screen readers.

## Configuration

Everything is set through environment variables, and no real secrets are stored in the code. The only passwords in the repository are the public sample login above and Docker Compose's default database password, which is for local use and can be overridden in `.env`.

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
6. **A fuller activity log,** recording changes to the priority, assignee and other fields as well as the status.
7. **Browser end-to-end tests** with Playwright, especially for drag and drop, which the component tests cannot exercise.
