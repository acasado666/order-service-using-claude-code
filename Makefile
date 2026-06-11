COMPOSE  := docker compose
JAVA_HOME_21 := C:/Program Files/Java/jdk-21
SERVICE  ?= order-service

.PHONY: build up down logs test

## Build the order-service image (no cache)
build:
	$(COMPOSE) build --no-cache order-service

## Start the full stack in the background
up:
	$(COMPOSE) up -d
	@echo ""
	@echo "Stack is up:"
	@echo "  order-service → http://localhost:8080/swagger-ui.html"
	@echo "  Prometheus     → http://localhost:9090"
	@echo "  Grafana        → http://localhost:3000  (admin / admin)"
	@echo ""

## Stop and remove containers (volumes are preserved)
down:
	$(COMPOSE) down

## Tail logs — override service with: make logs SERVICE=prometheus
logs:
	$(COMPOSE) logs -f $(SERVICE)

## Run unit + integration tests locally (requires Java 21)
test:
	JAVA_HOME="$(JAVA_HOME_21)" mvn test
