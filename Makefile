# Makefile for Billing Service
# Provides convenient commands for Docker operations

.PHONY: help build up down logs restart clean test db-only

# Default target
help:
	@echo "Billing Service - Docker Commands"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  build       - Build Docker images"
	@echo "  up          - Start all services"
	@echo "  down        - Stop all services"
	@echo "  restart     - Restart all services"
	@echo "  logs        - View logs (Ctrl+C to exit)"
	@echo "  logs-app    - View application logs only"
	@echo "  logs-db     - View database logs only"
	@echo "  clean       - Remove containers, volumes, and images"
	@echo "  db-only     - Start database only (for local dev)"
	@echo "  db-down     - Stop database only"
	@echo "  with-pgadmin - Start with PgAdmin"
	@echo "  test        - Run tests"
	@echo "  shell-app   - Access application container shell"
	@echo "  shell-db    - Access database container"
	@echo "  backup-db   - Backup database"
	@echo "  status      - Show container status"

# Build Docker images
build:
	@echo "Building Docker images..."
	docker-compose build

# Start all services
up:
	@echo "Starting all services..."
	docker-compose up -d
	@echo "✓ Services started!"
	@echo "Application: http://localhost:8080"
	@echo "Health: http://localhost:8080/actuator/health"

# Stop all services
down:
	@echo "Stopping services..."
	docker-compose down
	@echo "✓ Services stopped"

# Restart services
restart: down up

# View logs
logs:
	docker-compose logs -f

# View application logs only
logs-app:
	docker-compose logs -f billing-service

# View database logs only
logs-db:
	docker-compose logs -f postgres

# Clean up everything
clean:
	@echo "Removing containers, volumes, and images..."
	docker-compose down -v --rmi all
	docker-compose -f docker-compose.dev.yml down -v
	@echo "✓ Cleanup complete"

# Start database only
db-only:
	@echo "Starting database only..."
	docker-compose -f docker-compose.dev.yml up -d
	@echo "✓ Database started at localhost:5433"
	@echo "Run app locally with: mvn spring-boot:run"

# Stop database only
db-down:
	docker-compose -f docker-compose.dev.yml down

# Start with PgAdmin
with-pgadmin:
	@echo "Starting with PgAdmin..."
	docker-compose --profile dev up -d
	@echo "✓ Services started with PgAdmin!"
	@echo "PgAdmin: http://localhost:5050"

# Run tests
test:
	@echo "Running tests..."
	mvn test

# Access application container
shell-app:
	docker exec -it billing-service-app sh

# Access database container
shell-db:
	docker exec -it billing-service-postgres psql -U postgres -d billing_db

# Backup database
backup-db:
	@echo "Backing up database..."
	docker exec billing-service-postgres pg_dump -U postgres billing_db > backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "✓ Backup created"

# Show container status
status:
	@echo "Container Status:"
	@docker-compose ps
	@echo ""
	@echo "Resource Usage:"
	@docker stats --no-stream billing-service-app billing-service-postgres 2>/dev/null || true
