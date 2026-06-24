# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build: Maven + JDK 17
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper from the target service directory
COPY wallet-service/mvnw .
COPY wallet-service/.mvn .mvn

# Copy common-lib first to build and install it locally
COPY common-lib common-lib
RUN cd common-lib && chmod +x ../mvnw && ../mvnw clean install -DskipTests -q

# Copy wallet-service dependency configuration
COPY wallet-service/pom.xml wallet-service/
RUN chmod +x mvnw && ./mvnw -f wallet-service/pom.xml dependency:go-offline -q

# Copy wallet-service source files and build
COPY wallet-service/src wallet-service/src
RUN ./mvnw -f wallet-service/pom.xml package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime: Minimal Alpine JRE 17
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Copy packaged JAR from build stage
COPY --from=builder /build/wallet-service/target/*.jar app.jar

# Expose microservice port
EXPOSE 8081

# JVM container optimization flags
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
