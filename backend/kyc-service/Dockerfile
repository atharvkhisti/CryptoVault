# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build: Maven + JDK 17
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper from the target service directory
COPY kyc-service/mvnw .
COPY kyc-service/.mvn .mvn

# Copy common-lib first to build and install it locally
COPY common-lib common-lib
RUN cd common-lib && chmod +x ../mvnw && ../mvnw clean install -DskipTests -q

# Copy kyc-service dependency configuration
COPY kyc-service/pom.xml kyc-service/
RUN chmod +x mvnw && ./mvnw -f kyc-service/pom.xml dependency:go-offline -q

# Copy kyc-service source files and build
COPY kyc-service/src kyc-service/src
RUN ./mvnw -f kyc-service/pom.xml package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime: Minimal Alpine JRE 17
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Pre-create local uploads directory and grant ownership to non-root user
RUN mkdir uploads && chown appuser:appgroup uploads

USER appuser

# Copy packaged JAR from build stage
COPY --from=builder /build/kyc-service/target/*.jar app.jar

# Expose microservice port
EXPOSE 8087

# JVM container optimization flags
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
