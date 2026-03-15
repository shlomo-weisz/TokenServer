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
  Dockerfile
  docker-compose.yml
  README.md
```

## Configuration

Default configuration lives in `src/main/resources/application.properties`.
For local non-Docker runs, prefer overriding values with environment variables
instead of editing the committed file.

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

- Flyway is enabled by default and runs automatically on startup.
- When running from source, the default catalog path is `../openu_all_courses.json`.
- In Docker, the compose file mounts the catalog file to `/app/openu_all_courses.json`.
- If you do not want catalog sync in an environment, set `APP_COURSE_CATALOG_ENABLED=false`.
- If `google.client-id` is empty, `/api/auth/google` will return a configuration error.

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

4. Review local config in `src/main/resources/application.properties`.

The default local file already points to:

- SQL Server on `localhost:1433`
- Artemis on `localhost:61616`
- port `8080`

5. Start the app:

```bash
mvn spring-boot:run
```

6. Verify the server:

```bash
curl http://localhost:8080/health
```

Expected response:

```json
{"success":true,"data":{"status":"UP"}}
```

### Run everything with Docker Compose

This starts SQL Server, Artemis, and the backend together.

1. Start the stack:

```bash
docker compose up -d --build
```

2. Wait for SQL Server to be ready:

```bash
docker logs -f tokenlearn-sqlserver
```

3. Create the database:

```bash
docker exec tokenlearn-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "YourStrong!Passw0rd" -Q "IF DB_ID('tokenlearn') IS NULL CREATE DATABASE tokenlearn;"
```

4. Restart the backend so it reconnects after the database exists:

```bash
docker compose restart tokenlearn-server
```

5. Verify health:

```bash
curl http://localhost:8080/health
```

Useful checks:

```bash
docker compose ps
docker compose logs --tail=100 tokenlearn-server
curl http://localhost:8161
```

Artemis web console defaults:

- URL: `http://localhost:8161`
- username: `admin`
- password: `admin`

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

Update the values in `docker-compose.yml` or provide a compose override file.
Do not keep the example secrets in production.

At minimum replace:

- SQL Server `sa` password
- Artemis credentials
- `JWT_SECRET`
- `GOOGLE_CLIENT_ID` if Google login is enabled

### Start the deployment

```bash
cd /opt/tokenlearn/server
docker compose up -d --build
```

### Create the database

```bash
docker exec tokenlearn-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "YourStrong!Passw0rd" -Q "IF DB_ID('tokenlearn') IS NULL CREATE DATABASE tokenlearn;"
```

### Restart the backend once

```bash
docker compose restart tokenlearn-server
```

### Validate the deployment

```bash
curl http://localhost:8080/health
docker compose ps
docker compose logs --tail=100 tokenlearn-server
```

### Recommended production hardening

- put the backend behind Nginx, Traefik, or another reverse proxy
- terminate TLS at the proxy or load balancer
- avoid exposing SQL Server and Artemis publicly unless required
- restrict access to port `8161`
- replace default credentials
- back up the SQL Server volume

## Operational Notes

### Health and startup

- `GET /health` returns the API health envelope
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

- `/api/auth/*`
- `/api/users/*`
- `/api/tokens/*`
- `/api/courses/*`
- `/api/lesson-requests/*`
- `/api/lessons/*`
- `/api/tutors/*`
- `/api/notifications/*`
- `/api/admin/*`
- `/health`

Every API response is wrapped as:

```json
{
  "success": true,
  "data": {}
}
```

Or on error:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message"
  }
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
