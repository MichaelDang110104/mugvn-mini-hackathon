# ============================================
# Stage 1: Cache Maven dependencies
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS deps

WORKDIR /app

# Copy Maven wrapper and config first (changes less frequently)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make mvnw executable and download dependencies (cached unless pom.xml changes)
RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B -q

# ============================================
# Stage 2: Build the application
# ============================================
FROM deps AS builder

WORKDIR /app

# Copy source code (changes most frequently — this layer busts only on code changes)
COPY src/ src/

# Build the JAR, skip tests for faster Docker builds
RUN ./mvnw package -DskipTests -B -q && \
    # Extract the layered JAR for better caching in the final image
    java -Djarmode=tools -jar target/*.jar extract --layers --destination extracted

# ============================================
# Stage 3: Minimal runtime image
# ============================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy extracted layers in order of change frequency (least → most)
# This maximises Docker layer cache reuse across deployments
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Switch to non-root user
USER appuser

EXPOSE 8686

# Use exec form for proper signal handling
ENTRYPOINT ["java", "-jar", "backend-0.0.1-SNAPSHOT.jar"]
