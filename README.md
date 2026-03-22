# TokenLearn Server

Spring Boot backend for TokenLearn.

The server provides:

- JWT-based authentication
- user profile and tutor discovery APIs
- lesson requests and lesson management
- token reservation, refund, transfer, and settlement flows
- inbox notifications
- admin APIs
- SQL Server persistence with Flyway migrations
- ActiveMQ Artemis messaging for asynchronous settlement

Detailed code walkthrough: [docs/server-architecture.md](docs/server-architecture.md)

## Stack

- Java 17
- Spring Boot 3
- Spring Security
- Spring JDBC
- Flyway
- SQL Server
- ActiveMQ Artemis
- Maven
- Docker Compose

## Project Layout

```text
server/
  src/main/java/com/tokenlearn/server/
    config/        Spring configuration
    controller/    REST endpoints
    service/       business workflows and scheduled jobs
    dao/           SQL access with NamedParameterJdbcTemplate
    domain/        persistence entities
    dto/           API request/response types
    security/      JWT issuance and request authentication
    messaging/     outbox publisher and settlement consumer
  src/main/resources/
    application.properties
    db/migration/
  .env.example
  Dockerfile
  docker-compose.yml
  README.md
```

## Configuration

Default configuration lives in `src/main/resources/application.properties`, but
the runtime values are now sourced from environment variables. For local runs,
the server imports `server/.env` automatically when present, so you should edit
`.env` instead of the committed properties file.

Important settings:

| Property | Purpose | Environment variable |
| --- | --- | --- |
| `server.port` | HTTP port | `SERVER_PORT` |
| `spring.datasource.url` | SQL Server JDBC URL | `SPRING_DATASOURCE_URL` |
| `spring.datasource.username` | SQL Server user | `SPRING_DATASOURCE_USERNAME` |
| `spring.datasource.password` | SQL Server password | `SPRING_DATASOURCE_PASSWORD` |
| `spring.artemis.broker-url` | Artemis broker URL | `SPRING_ARTEMIS_BROKER_URL` |
| `spring.artemis.user` | Artemis username | `SPRING_ARTEMIS_USER` |
| `spring.artemis.password` | Artemis password | `SPRING_ARTEMIS_PASSWORD` |
| `jwt.secret` | JWT signing secret | `JWT_SECRET` |
| `jwt.expiration` | JWT lifetime in milliseconds | `JWT_EXPIRATION` |
| `google.client-id` | Allowed Google OAuth Web Client IDs, comma-separated | `GOOGLE_CLIENT_ID` |
| `app.course-catalog.enabled` | Enable startup catalog sync | `APP_COURSE_CATALOG_ENABLED` |
| `app.course-catalog.path` | Path to `openu_all_courses.json` | `APP_COURSE_CATALOG_PATH` |
| `app.lesson-request-approval-buffer-hours` | Minimum lead time before a requested lesson | `APP_LESSON_REQUEST_APPROVAL_BUFFER_HOURS` |

Notes:

- The local `.env` template uses `TOKENLEARN_*` keys. `application.properties`
  maps those keys into the Spring properties above automatically.
- Flyway is enabled by default and runs automatically on startup.
- When running from source, the default catalog path is `../openu_all_courses.json`.
- In Docker, the compose file mounts the catalog file to `/app/openu_all_courses.json`.
- If you do not want catalog sync in an environment, set `APP_COURSE_CATALOG_ENABLED=false`.
- If `google.client-id` is empty, `POST /api/sessions` with `googleToken` will return a configuration error.

### Docker Compose secret handling

The Docker Compose setup no longer stores secrets directly inside
`docker-compose.yml`.

Use this flow:

1. Copy `.env.example` to `.env`
2. Put real secrets only in `.env`
3. Keep `.env` out of Git

Example:

```bash
cp .env.example .env
```

Variables loaded from `.env` include:

- `TOKENLEARN_DB_PASSWORD`
- `TOKENLEARN_ARTEMIS_PASSWORD`
- `TOKENLEARN_JWT_SECRET`
- `TOKENLEARN_GOOGLE_CLIENT_ID`

Both Spring Boot local runs and `docker compose` load `.env` from the `server/`
directory.

## Local Development

### Prerequisites

- Java 17
- Maven 3.9+
- Docker Engine with Docker Compose plugin

### Recommended workflow: run infrastructure in Docker, run the app locally

1. Start SQL Server and Artemis:

```bash
docker compose up -d sqlserver artemis
```

2. Wait for SQL Server to finish booting:

```bash
docker logs -f tokenlearn-sqlserver
```

Stop the log tail once you see `SQL Server is now ready for client connections`.

3. Create the application database:

```bash
docker exec tokenlearn-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "YourStrong!Passw0rd" -Q "IF DB_ID('tokenlearn') IS NULL CREATE DATABASE tokenlearn;"
```

4. Copy `.env.example` to `.env` if you have not already, then edit the values
   you need for your local run.

The local app reads `.env` automatically. The standard template controls:

- `TOKENLEARN_SQLSERVER_PORT` for SQL Server, usually `1433`
- `TOKENLEARN_ARTEMIS_BROKER_PORT` for Artemis, usually `61616`
- `TOKENLEARN_SERVER_PORT` for the backend HTTP port, usually `8080`

5. Start the app:

```bash
mvn spring-boot:run
```

6. Verify the server:

```bash
curl http://localhost:8080/api/system/status
```

Expected response:

```json
{"status":"UP"}
```

### Run everything with Docker Compose

This starts SQL Server, Artemis, and the backend together.

1. Create your local `.env` from the template:

```bash
cp .env.example .env
```

2. Edit `.env` and replace at least these values:

- `TOKENLEARN_DB_PASSWORD`
- `TOKENLEARN_ARTEMIS_PASSWORD`
- `TOKENLEARN_JWT_SECRET`
- `TOKENLEARN_GOOGLE_CLIENT_ID` if Google login is enabled

3. Start the stack:

```bash
docker compose up -d --build
```

4. Wait for SQL Server to be ready:

```bash
docker logs -f tokenlearn-sqlserver
```

5. Create the database using the password from `.env`:

```bash
docker exec tokenlearn-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "YOUR_DB_PASSWORD_FROM_ENV" -Q "IF DB_ID('tokenlearn') IS NULL CREATE DATABASE tokenlearn;"
```

6. Restart the backend so it reconnects after the database exists:

```bash
docker compose restart tokenlearn-server
```

7. Verify health:

```bash
curl http://localhost:8080/api/system/status
```

Useful checks:

```bash
docker compose ps
docker compose logs --tail=100 tokenlearn-server
curl http://localhost:8161
```

Artemis web console defaults:

- URL: `http://localhost:${TOKENLEARN_ARTEMIS_WEB_PORT}`
- username: value of `TOKENLEARN_ARTEMIS_USER`
- password: value of `TOKENLEARN_ARTEMIS_PASSWORD`

Stop the stack:

```bash
docker compose down
docker compose down -v
```

`-v` also deletes the SQL Server volume.

### Build and run the jar directly

```bash
mvn -q -DskipTests package
java -jar target/tokenlearn-server-0.0.1-SNAPSHOT.jar
```

Use this mode when SQL Server and Artemis already exist outside Docker.

## Tests

Run the automated tests with:

```bash
mvn test -q
```

Test profile notes:

- tests use H2 in MSSQL compatibility mode
- Flyway is disabled in tests
- Artemis listeners are not started in tests
- course catalog sync is disabled in tests

## Deployment To A Remote Linux Host

This repository already contains a deployable Docker Compose setup for the
server, database, and broker.

### Remote host prerequisites

- Docker Engine
- Docker Compose plugin
- enough disk space for SQL Server data volume
- firewall rules for the ports you intend to expose

Typical ports:

- `8080` for the backend API
- `1433` for SQL Server
- `61616` for Artemis broker traffic
- `8161` for Artemis web console

### File layout on the remote host

The compose file expects the catalog JSON one directory above `server/`.

```text
/opt/tokenlearn/
  server/
  openu_all_courses.json
```

### Copy files

Example with `scp`:

```bash
scp -r server openu_all_courses.json user@REMOTE_HOST:/opt/tokenlearn/
```

### Review secrets before first start

Copy `.env.example` to `.env` and put real values there.
Do not keep production secrets inside `docker-compose.yml` or commit `.env`.

At minimum replace:

- `TOKENLEARN_DB_PASSWORD`
- `TOKENLEARN_ARTEMIS_PASSWORD`
- `TOKENLEARN_JWT_SECRET`
- `TOKENLEARN_GOOGLE_CLIENT_ID` if Google login is enabled

### Start the deployment

```bash
cd /opt/tokenlearn/server
cp .env.example .env
# edit .env with production values
docker compose up -d --build
```

### Create the database

```bash
docker exec tokenlearn-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "YOUR_DB_PASSWORD_FROM_ENV" -Q "IF DB_ID('tokenlearn') IS NULL CREATE DATABASE tokenlearn;"
```

### Restart the backend once

```bash
docker compose restart tokenlearn-server
```

### Validate the deployment

```bash
curl http://localhost:8080/api/system/status
docker compose ps
docker compose logs --tail=100 tokenlearn-server
```

### Recommended production hardening

- put the backend behind Nginx, Traefik, or another reverse proxy
- terminate TLS at the proxy or load balancer
- avoid exposing SQL Server and Artemis publicly unless required
- restrict access to port `8161`
- replace default credentials
- store `.env` securely on the host and restrict its filesystem permissions
- back up the SQL Server volume

## Operational Notes

### Health and startup

- `GET /api/system/status` returns the direct API status payload
- Flyway runs on server startup
- course catalog sync also runs on startup when enabled

### Scheduled jobs

- pending lesson requests are expired automatically
- finished scheduled lessons are auto-completed
- lesson reminders are generated roughly one hour before start time
- outbox rows are published to Artemis every few seconds
- failed outbox rows are retried automatically

### Financial model

- `available_balance`: spendable tokens
- `locked_balance`: tokens reserved for pending or approved lesson flow
- request creation: `available -> locked`
- request rejection/cancel/expiry: `locked -> available`
- completed lesson: asynchronous settlement moves `student.locked -> tutor.available`

All balance-changing operations are also recorded in `token_transactions`.

## API Groups

- `/api/sessions`
- `/api/password-reset-requests*`
- `/api/users`
- `/api/users/me`
- `/api/users/me/photo`
- `/api/users/me/wallet`
- `/api/users/me/token-transactions`
- `/api/users/{userId}`
- `/api/users/{userId}/ratings`
- `/api/users/{userId}/token-transactions`
- `/api/courses/*`
- `/api/lesson-requests/*`
- `/api/lessons/*`
- `/api/ratings/*`
- `/api/tutors/*`
- `/api/notifications/*`
- `/api/support-threads/*`
- `/api/admin/reports/*`
- `/api/system/status`

Successful API responses return direct JSON resources. Errors are returned as RFC 9457 problem documents (`application/problem+json`), for example:

```json
{
  "type": "urn:tokenlearn:problem:invalid_request",
  "title": "Bad Request",
  "status": 400,
  "detail": "The request is invalid",
  "code": "INVALID_REQUEST"
}
```

## Troubleshooting

### Backend starts before the database exists

Create the `tokenlearn` database, then run:

```bash
docker compose restart tokenlearn-server
```

### Course catalog warnings on startup

Either place `openu_all_courses.json` at the configured path or disable the sync:

```bash
APP_COURSE_CATALOG_ENABLED=false
```

### Google login fails with configuration error

Set a valid Google OAuth Web Client ID:

```properties
google.client-id=YOUR_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com
```

### Broker or settlement issues

Check:

```bash
docker compose logs --tail=100 tokenlearn-artemis
docker compose logs --tail=100 tokenlearn-server
```

The server uses a transactional outbox, so settlement events that fail to publish
are retried automatically.
