# Docker Deployment Guide

## Overview

This guide explains how to build and run the Billing Service using Docker and Docker Compose with PostgreSQL 17.

## Prerequisites

- Docker 20.10 or higher
- Docker Compose 2.0 or higher
- At least 2GB of available RAM
- Ports 8080, 5433, and 5050 available

## Quick Start

### 1. Environment Setup

Copy the `.env.example` file to `.env` and configure your credentials:

```bash
cp .env.example .env
```

Edit `.env` with your actual values:

```properties
# AWS Credentials
AWS_REGION=us-east-2
AWS_ACCESS_KEY=your-aws-access-key
AWS_SECRET_KEY=your-aws-secret-key

# Mercado Pago
MERCADOPAGO_ACCESS_TOKEN=your-mercadopago-token
MERCADOPAGO_PUBLIC_KEY=your-mercadopago-public-key

# Optional: Email
GMAIL_USERNAME=your-email@gmail.com
GMAIL_APP_PASSWORD=your-app-password
```

### 2. Build and Run - Full Stack (App + Database)

```bash
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f

# Check service health
docker-compose ps
```

### 3. Run Database Only (For Local Development)

If you want to run the application locally but use Docker for the database:

```bash
# Start only PostgreSQL and PgAdmin
docker-compose -f docker-compose.dev.yml up -d

# The database will be available at localhost:5433
# PgAdmin will be available at http://localhost:5050
```

Then run the Spring Boot application from your IDE or command line:

```bash
# Set profile and run
export SPRING_PROFILES_ACTIVE=development
mvn spring-boot:run
```

## Docker Commands

### Build the Application

```bash
# Build the Docker image
docker build -t billing-service:latest .

# Build with custom tag
docker build -t billing-service:1.0.0 .
```

### Run with Docker Compose

```bash
# Start all services in detached mode
docker-compose up -d

# Start with build
docker-compose up -d --build

# Start specific service
docker-compose up -d postgres

# Start with PgAdmin (dev profile)
docker-compose --profile dev up -d
```

### Stop and Clean Up

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes database data)
docker-compose down -v

# Remove all containers, networks, and images
docker-compose down --rmi all -v
```

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

### Execute Commands in Containers

```bash
# Access billing-service container
docker exec -it billing-service-app sh

# Access PostgreSQL
docker exec -it billing-service-postgres psql -U postgres -d billing_db

# Run SQL commands
docker exec -it billing-service-postgres psql -U postgres -d billing_db -c "SELECT * FROM payments;"
```

## Service Endpoints

### Application

- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info

### Database

- **PostgreSQL**: localhost:5433
  - Username: `postgres`
  - Password: `postgres123`
  - Database: `billing_db`

### PgAdmin (with --profile dev)

- **URL**: http://localhost:5050
- **Email**: admin@billing-service.com
- **Password**: admin123

#### PgAdmin Connection Setup

1. Access http://localhost:5050
2. Right-click "Servers" → "Register" → "Server"
3. General tab:
   - Name: `Billing Service DB`
4. Connection tab:
   - Host: `postgres`
   - Port: `5432`
   - Database: `billing_db`
   - Username: `postgres`
   - Password: `postgres123`

## Environment Variables

### Application Configuration

| Variable                     | Description                                          | Default                                    | Required |
| ---------------------------- | ---------------------------------------------------- | ------------------------------------------ | -------- |
| `SPRING_PROFILES_ACTIVE`     | Spring profile (development/homologation/production) | development                                | No       |
| `SPRING_DATASOURCE_URL`      | Database JDBC URL                                    | jdbc:postgresql://postgres:5432/billing_db | No       |
| `SPRING_DATASOURCE_USERNAME` | Database username                                    | postgres                                   | No       |
| `SPRING_DATASOURCE_PASSWORD` | Database password                                    | postgres123                                | No       |
| `AWS_REGION`                 | AWS region                                           | us-east-2                                  | Yes      |
| `AWS_ACCESS_KEY`             | AWS access key                                       | -                                          | Yes      |
| `AWS_SECRET_KEY`             | AWS secret key                                       | -                                          | Yes      |
| `MERCADOPAGO_ACCESS_TOKEN`   | Mercado Pago access token                            | -                                          | Yes      |
| `MERCADOPAGO_PUBLIC_KEY`     | Mercado Pago public key                              | test-public-key                            | No       |

## Troubleshooting

### Container Won't Start

```bash
# Check container logs
docker-compose logs billing-service

# Check if port is already in use
lsof -i :8080
lsof -i :5433

# Restart services
docker-compose restart
```

### Database Connection Issues

```bash
# Check if PostgreSQL is healthy
docker-compose ps postgres

# Test database connection
docker exec billing-service-postgres pg_isready -U postgres

# View PostgreSQL logs
docker-compose logs postgres
```

### Application Health Check Failing

```bash
# Check if application is running
docker exec billing-service-app ps aux

# Test health endpoint manually
curl http://localhost:8080/actuator/health

# Check application logs
docker-compose logs -f billing-service
```

### Out of Memory

```bash
# Check container resource usage
docker stats

# Adjust memory limits in docker-compose.yml
services:
  billing-service:
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G
```

### Clean Restart

```bash
# Stop everything
docker-compose down -v

# Remove old images
docker rmi billing-service:latest

# Rebuild and start
docker-compose up -d --build
```

## Production Deployment

### Performance Tuning

Edit the Dockerfile to enable production JVM options:

```dockerfile
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx1024m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

### Resource Limits

Add resource limits to `docker-compose.yml`:

```yaml
services:
  billing-service:
    deploy:
      resources:
        limits:
          cpus: "1.0"
          memory: 2G
        reservations:
          cpus: "0.5"
          memory: 1G
```

### Monitoring

```bash
# Real-time resource monitoring
docker stats billing-service-app billing-service-postgres

# Export metrics (if Prometheus is configured)
curl http://localhost:8080/actuator/prometheus
```

## Database Backup and Restore

### Backup

```bash
# Backup database to file
docker exec billing-service-postgres pg_dump -U postgres billing_db > backup_$(date +%Y%m%d_%H%M%S).sql

# Or using docker-compose
docker-compose exec postgres pg_dump -U postgres billing_db > backup.sql
```

### Restore

```bash
# Restore from backup file
docker exec -i billing-service-postgres psql -U postgres billing_db < backup.sql

# Or using docker-compose
docker-compose exec -T postgres psql -U postgres billing_db < backup.sql
```

## Security Notes

⚠️ **Important Security Considerations:**

1. **Change default passwords** in production
2. **Use secrets management** for sensitive data (AWS Secrets Manager, Vault)
3. **Enable SSL/TLS** for PostgreSQL connections in production
4. **Use read-only file systems** where possible
5. **Scan images** for vulnerabilities: `docker scan billing-service:latest`
6. **Keep base images updated** regularly

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Build Docker Image
  run: docker build -t billing-service:${{ github.sha }} .

- name: Push to Registry
  run: |
    docker tag billing-service:${{ github.sha }} registry/billing-service:latest
    docker push registry/billing-service:latest
```

### AWS ECR Push

```bash
# Login to ECR
aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-2.amazonaws.com

# Tag and push
docker tag billing-service:latest <account-id>.dkr.ecr.us-east-2.amazonaws.com/billing-service:latest
docker push <account-id>.dkr.ecr.us-east-2.amazonaws.com/billing-service:latest
```

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [PostgreSQL 17 Documentation](https://www.postgresql.org/docs/17/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
