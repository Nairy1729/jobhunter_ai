# ==============================================================================
# JobHunter AI — Multi-Stage Production Dockerfile (Java 17 / Spring Boot 3.3.4)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

# Copy Maven Wrapper and POM first to leverage Docker layer caching for dependencies
COPY backend/pom.xml .
COPY backend/mvnw .
COPY backend/.mvn .mvn

RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy backend source code and build production fat-jar
COPY backend/src src
RUN ./mvnw clean package -DskipTests

# ------------------------------------------------------------------------------
# Stage 2: Minimal Hardened JRE Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as dedicated unprivileged user for container security
RUN groupadd -r jobhunter && useradd -r -g jobhunter jobhunter

# Copy executable jar from builder stage
COPY --from=builder /workspace/target/jobhunter-ai-backend-0.0.1-SNAPSHOT.jar app.jar

# Setup local storage directory for temporary runtime files
RUN mkdir -p /app/storage/resumes && chown -R jobhunter:jobhunter /app

USER jobhunter

# Dynamic port binding (Render/Koyeb/Fly.io injects $PORT)
ENV PORT=8085
EXPOSE 8085

# Optimized JVM memory settings for zero-cost / 512MB RAM cloud tiers
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Xmx350m", "-XX:+UseG1GC", "-jar", "app.jar"]
