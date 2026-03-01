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

1. Build and start:
```bash
docker compose up -d --build
```

2. Create the DB once:
```bash
docker exec -it tokenlearn-sqlserver /opt/mssql-tools18/bin/sqlcmd -C -S localhost -U sa -P "YourStrong!Passw0rd" -Q "IF DB_ID('tokenlearn') IS NULL CREATE DATABASE tokenlearn;"
```

3. Restart the app service so Flyway migrates the new DB:
```bash
docker compose restart tokenlearn-server
```

4. Health check:
```bash
curl http://localhost:8080/health
```

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
