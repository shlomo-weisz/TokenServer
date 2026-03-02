# TokenLearn Server

Spring Boot backend for TokenLearn with:
- Java 17
- REST API
- JDBC + SQL Server
- JWT auth
- JMS settlement using transactional outbox

## Run

1. Create SQL Server database:
```sql
CREATE DATABASE tokenlearn;
```

2. Configure DB/JMS values in `src/main/resources/application.properties`.

3. Run:
```bash
mvn spring-boot:run
```

## Run With Docker (Server + DB + JMS)

### Prerequisites
- Docker Engine + Docker Compose plugin installed.
- Port `8080`, `1433`, `61616`, and `8161` must be free.

### 1) Start all services
```bash
docker compose up -d --build
```

### 2) Wait for SQL Server to be ready
```bash
docker logs -f tokenlearn-sqlserver
```
When you see `SQL Server is now ready for client connections`, stop following logs (`Ctrl+C`).

### 3) Create the `tokenlearn` database (idempotent)
```bash
docker exec tokenlearn-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "YourStrong!Passw0rd" -Q "IF DB_ID('tokenlearn') IS NULL CREATE DATABASE tokenlearn;"
```

### 4) Restart backend so Flyway runs against the new DB
```bash
docker compose restart tokenlearn-server
```

### 5) Validate health
```bash
curl http://localhost:8080/health
```
Expected response (wrapped by API format):
```json
{"success":true,"data":{"status":"UP"}}
```

### 6) Optional checks
```bash
docker compose ps
curl http://localhost:8161
```
- Artemis Web Console: `http://localhost:8161` (`admin` / `admin`).

### Stop / cleanup
```bash
docker compose down
docker compose down -v   # also deletes SQL Server volume
```

## Google Sign-In configuration

To enable real Google login verification on the backend, configure your OAuth Web Client ID:

```properties
google.client-id=YOUR_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com
```

The `/api/auth/google` endpoint expects a Google **ID token** in this payload:

```json
{ "googleToken": "<google_id_token>" }
```

The server verifies signature/issuer/expiry via Google JWKS, validates audience against `google.client-id`, and requires `email_verified=true`.

## Core financial model

- `available_balance`: spendable
- `locked_balance`: escrow for open/approved requests
- request creation: `available -> locked`
- reject/cancel: `locked -> available`
- complete lesson: enqueue outbox event; consumer settles `student.locked -> tutor.available`

## Implemented API groups

- `/api/auth/*`
- `/api/users/*`
- `/api/tokens/*`
- `/api/courses/*`
- `/api/lesson-requests/*`
- `/api/tutors/*`
- `/api/lessons/*`
- `/api/admin/*`

All endpoints return:
```json
{
  "success": true,
  "data": {}
}
```
or
```json
{
  "success": false,
  "error": { "code": "ERROR_CODE", "message": "..." }
}
```
