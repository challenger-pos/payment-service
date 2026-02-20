# Multi-stage Dockerfile for Billing Service

# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
# RUN ./mvnw dependency:go-offline -B
# Note: Skipping dependency:go-offline due to sonar-maven-plugin version unavailability
# Dependencies will be downloaded during the clean package phase

# Copy source code and build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -g 1001 -S appgroup && \
  adduser -u 1001 -S appuser -G appgroup

# Copy the jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Optional JVM arguments for production
# ENTRYPOINT ["java", \
#     "-Xms512m", \
#     "-Xmx1024m", \
#     "-XX:+UseG1GC", \
#     "-XX:MaxGCPauseMillis=200", \
#     "-XX:+UseStringDeduplication", \
#     "-Djava.security.egd=file:/dev/./urandom", \
#     "-jar", "app.jar"]
