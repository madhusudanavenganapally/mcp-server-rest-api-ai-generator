# Stage 1: Build the application
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
# Skip tests during container build to speed it up, assuming pre-validation
RUN gradle build -x test --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Create a volume point for generated projects
VOLUME /workspace
WORKDIR /workspace

# Expose port (default 8080)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
