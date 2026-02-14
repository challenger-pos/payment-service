# Docker Setup - Quick Reference

## 📦 Files Created

### Core Files

- **Dockerfile** - Multi-stage build for the Spring Boot application
  - Stage 1: Maven build (compile and package)
  - Stage 2: Runtime with JRE (slim image)
  - Features: Non-root user, health checks, optimized layers

- **docker-compose.yml** - Full stack orchestration
  - PostgreSQL 17 database
  - Billing Service application
  - Optional PgAdmin (with `--profile dev`)
  - Networking and volume management

- **docker-compose.dev.yml** - Database only for local development
  - PostgreSQL 17
  - PgAdmin included
  - Perfect for running app in IDE

### Helper Files

- **.dockerignore** - Optimizes Docker build context
- **start.sh** - Interactive startup script
- **Makefile** - Convenient command shortcuts
- **init-scripts/01-init.sql** - Database initialization
- **DOCKER_GUIDE.md** - Comprehensive Docker documentation

## 🚀 Quick Start Commands

### Using start.sh (Easiest)

```bash
./start.sh
```

Interactive menu with options:

1. Start full stack
2. Start database only
3. Start with PgAdmin
4. Stop all services
5. View logs
6. Clean up

### Using Make

```bash
make help        # Show all commands
make up          # Start everything
make db-only     # Database only
make logs        # View logs
make down        # Stop all
make clean       # Clean up everything
```

### Using Docker Compose

```bash
# Full stack
docker-compose up -d --build

# Database only
docker-compose -f docker-compose.dev.yml up -d

# With PgAdmin
docker-compose --profile dev up -d

# Stop
docker-compose down

# Clean up (removes volumes!)
docker-compose down -v
```

## 🔧 Configuration

### Environment Variables

Edit `.env` file with your credentials:

```bash
# Required for application
AWS_ACCESS_KEY=your-key
AWS_SECRET_KEY=your-secret
MERCADOPAGO_ACCESS_TOKEN=your-token

# Optional
SPRING_PROFILES_ACTIVE=development
```

### Database Connection

**From Docker container (app to db):**

- Host: `postgres`
- Port: `5432`

**From host machine (local tools):**

- Host: `localhost`
- Port: `5433`

**Credentials:**

- Username: `postgres`
- Password: `postgres123`
- Database: `billing_db`

## 📋 Service Endpoints

| Service      | URL                                   | Credentials                          |
| ------------ | ------------------------------------- | ------------------------------------ |
| Application  | http://localhost:8080                 | -                                    |
| Health Check | http://localhost:8080/actuator/health | -                                    |
| PostgreSQL   | localhost:5433                        | postgres / postgres123               |
| PgAdmin      | http://localhost:5050                 | admin@billing-service.com / admin123 |

## 🏗️ Architecture

```
┌─────────────────────────────────────────────┐
│          Docker Network (billing-network)   │
│                                             │
│  ┌──────────────────┐  ┌─────────────────┐ │
│  │ billing-service  │  │   PostgreSQL 17 │ │
│  │   (Port 8080)    │──│   (Port 5432)   │ │
│  └──────────────────┘  └─────────────────┘ │
│                                             │
└─────────────────────────────────────────────┘
           │                    │
      Port 8080            Port 5433
       (exposed)           (exposed)
```

## ⚙️ Dockerfile Features

### Multi-Stage Build

1. **Build Stage** (maven:3.9.9-eclipse-temurin-21-alpine)
   - Downloads dependencies (cached layer)
   - Compiles source code
   - Creates JAR file

2. **Runtime Stage** (eclipse-temurin:21-jre-alpine)
   - Minimal JRE image
   - Non-root user (appuser)
   - Health check configured
   - Optimized for production

### Security Features

- ✅ Non-root user execution
- ✅ Minimal base image (Alpine)
- ✅ No unnecessary tools
- ✅ Health checks enabled
- ✅ Read-only file system compatible

### Performance Optimizations

- ✅ Layer caching for dependencies
- ✅ Multi-stage build (smaller final image)
- ✅ .dockerignore for faster builds
- ✅ Health checks for zero-downtime deployments

## 🗄️ PostgreSQL 17 Features

### Configuration

- Alpine Linux base (smaller image)
- Persistent data with Docker volumes
- Health checks enabled
- Auto-creation scripts support
- UTF-8 encoding by default

### Volumes

```yaml
postgres_data:
  - Database files persisted
  - Survives container restarts
  - Can be backed up separately
```

### Initialization

SQL scripts in `init-scripts/` run automatically on first start:

- `01-init.sql` - Creates extensions, schemas, and initial setup

## 🧪 Testing Docker Setup

### Validate Configuration

```bash
# Check docker-compose syntax
docker-compose config --quiet

# Verify Dockerfile
docker build -t test-build --target build .
```

### Health Checks

```bash
# Check container health
docker-compose ps

# Manual health check
curl http://localhost:8080/actuator/health

# Database health
docker exec billing-service-postgres pg_isready -U postgres
```

## 📊 Monitoring

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f billing-service
docker-compose logs -f postgres

# Last 100 lines
docker-compose logs --tail=100 billing-service
```

### Resource Usage

```bash
# Real-time stats
docker stats

# Using make
make status
```

## 🔒 Security Recommendations

### ⚠️ Development (Current Setup)

- Default passwords (OK for local dev)
- Exposed ports (OK for local dev)
- No SSL/TLS (OK for local dev)

### ✅ Production (To Do)

- [ ] Change all default passwords
- [ ] Use secrets management (AWS Secrets Manager)
- [ ] Enable SSL/TLS for PostgreSQL
- [ ] Restrict exposed ports
- [ ] Use private Docker registry
- [ ] Scan images for vulnerabilities
- [ ] Set resource limits
- [ ] Enable logging to external system

## 🛠️ Troubleshooting

### Port Already in Use

```bash
# Find process using port
lsof -i :8080
lsof -i :5433

# Kill process or change ports in docker-compose.yml
```

### Database Connection Failed

```bash
# Check if database is healthy
docker-compose ps postgres

# View database logs
docker-compose logs postgres

# Test connection
docker exec billing-service-postgres pg_isready -U postgres
```

### Application Won't Start

```bash
# Check logs
docker-compose logs billing-service

# Verify .env file exists and has credentials
cat .env

# Rebuild
docker-compose down
docker-compose up -d --build
```

### Clean Slate

```bash
# Nuclear option - removes everything
docker-compose down -v --rmi all
docker system prune -af --volumes
```

## 📚 Additional Resources

- **DOCKER_GUIDE.md** - Comprehensive Docker documentation
- **README.md** - Application documentation
- **REFACTORING_SUMMARY.md** - Recent changes
- [Docker Documentation](https://docs.docker.com/)
- [PostgreSQL 17 Docs](https://www.postgresql.org/docs/17/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)

## 🎯 Common Use Cases

### Scenario 1: Full Development

Run everything in Docker:

```bash
docker-compose up -d --build
docker-compose logs -f
```

### Scenario 2: Backend Development

Database in Docker, app in IDE:

```bash
docker-compose -f docker-compose.dev.yml up -d
mvn spring-boot:run
```

### Scenario 3: Testing Changes

Quick iteration:

```bash
make restart
make logs-app
```

### Scenario 4: Database Management

Use PgAdmin:

```bash
docker-compose --profile dev up -d
# Open http://localhost:5050
```

### Scenario 5: Production Simulation

Full stack with production profile:

```bash
SPRING_PROFILES_ACTIVE=production docker-compose up -d
```

## 🎉 Success Indicators

When everything is working correctly:

```bash
$ docker-compose ps

NAME                        STATUS              PORTS
billing-service-app         Up (healthy)        0.0.0.0:8080->8080/tcp
billing-service-postgres    Up (healthy)        0.0.0.0:5433->5432/tcp
```

```bash
$ curl http://localhost:8080/actuator/health

{"status":"UP"}
```

---

**Need help?** Check [DOCKER_GUIDE.md](DOCKER_GUIDE.md) for detailed documentation.
