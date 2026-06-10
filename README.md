# order-service

Order management microservice built with Spring Boot 4.0.5, Java 25, and PostgreSQL.

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 25 |
| Framework | Spring Boot 4.0.5 |
| Persistence | Spring Data JPA + Liquibase |
| Database (dev) | H2 in-memory (PostgreSQL mode) |
| Database (staging/prod) | PostgreSQL 18 |
| Mapping | MapStruct 1.6.3 |
| Documentation | SpringDoc OpenAPI 3.0.2 |
| Metrics | Micrometer + Prometheus |
| Logging | Logback + logstash-logback-encoder |

## Prerequisites

- Java 25
- Maven 3.9+
- Docker (for containerised runs)
- PostgreSQL 18 (staging/prod only)

## Running Locally (dev profile — H2)

```bash
./mvnw spring-boot:run
```

Or with Maven installed:

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080` with an H2 in-memory database.  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:orderdb`)

## Running with a Profile

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=staging
```

Required environment variables for `staging` and `prod`:

| Variable | Description |
|---|---|
| `DB_HOST` | PostgreSQL host |
| `DB_PORT` | PostgreSQL port (default: 5432) |
| `DB_NAME` | Database name |
| `DB_USER` | Database username |
| `DB_PASSWORD` | Database password |

## Running Tests

```bash
mvn test
```

Integration tests use Testcontainers and require a Docker daemon.

## Building the JAR

```bash
mvn -DskipTests package
```

The fat JAR is produced at `target/order-service-1.0.0-SNAPSHOT.jar`.

## Docker

### Build

```bash
docker build -t order-service:latest .
```

> **Note:** The build stage uses `maven:3.9-eclipse-temurin-25`. If this tag is not yet
> available, substitute with the closest available JDK 25 Maven image or install Maven
> inside `eclipse-temurin:25-jdk`.

### Run (dev — H2)

```bash
docker run -p 8080:8080 order-service:latest
```

### Run (staging — PostgreSQL)

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=staging \
  -e DB_HOST=your-pg-host \
  -e DB_PORT=5432 \
  -e DB_NAME=orderdb \
  -e DB_USER=order_user \
  -e DB_PASSWORD=secret \
  order-service:latest
```

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/orders` | Create a new order |
| `GET` | `/api/v1/orders` | List all orders (paginated) |
| `GET` | `/api/v1/orders/{id}` | Get order by ID |
| `GET` | `/api/v1/orders/customer/{customerId}` | List orders by customer |
| `PATCH` | `/api/v1/orders/{id}/status` | Update order status |
| `DELETE` | `/api/v1/orders/{id}` | Cancel an order |

## Order State Machine

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED → REFUNDED
       ↘           ↘           ↘         ↘         ↘
        CANCELLED   CANCELLED   CANCELLED  CANCELLED  (terminal)
```

## Observability

- **Health:** `GET /actuator/health`
- **Prometheus metrics:** `GET /actuator/prometheus`
- **OpenAPI spec:** `GET /v3/api-docs`

## Project Structure

```
src/main/java/com/skmcore/orderservice/
├── controller/      REST controllers
├── service/         Business logic interfaces + impl/
├── repository/      Spring Data JPA repositories
├── model/           JPA entities and enums
├── dto/             Request/response records
├── mapper/          MapStruct interfaces
├── config/          Security, OpenAPI configuration
├── exception/       Custom exceptions + global handler
└── event/           Domain event records
```
