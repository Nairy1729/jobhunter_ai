# ==============================================================================
# JobHunter AI — Backend Production Dockerfile (Context: backend/)
# ==============================================================================

FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd -r jobhunter && useradd -r -g jobhunter jobhunter

COPY --from=builder /workspace/target/jobhunter-ai-backend-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/storage/resumes && chown -R jobhunter:jobhunter /app

USER jobhunter

ENV PORT=8085
EXPOSE 8085

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Xmx350m", "-XX:+UseG1GC", "-jar", "app.jar"]
