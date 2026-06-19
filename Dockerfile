# ══════════════════════════════════════════════════════════
# Stage 1: Build — Full JDK for Maven compilation
# ══════════════════════════════════════════════════════════
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy POM first to cache dependencies
COPY pom.xml .
RUN mvn dependency:resolve -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ══════════════════════════════════════════════════════════
# Stage 2: Run — Lightweight JRE only
# ══════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
