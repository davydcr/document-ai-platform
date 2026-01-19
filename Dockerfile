# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy pom files
COPY backend/pom.xml .
COPY backend/domain domain/
COPY backend/application application/
COPY backend/infrastructure infrastructure/

# Build the project
RUN mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre

# Install Tesseract and dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-por \
    libsm6 \
    libxext6 \
    libxrender-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Create directories for storage and temp
RUN mkdir -p /var/document-ai/{uploads,temp}

# Copy built jar from builder
COPY --from=builder /workspace/infrastructure/target/infrastructure-1.0.0-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.JarLauncher -c "curl -f http://localhost:8080/api/documents || exit 1" || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
