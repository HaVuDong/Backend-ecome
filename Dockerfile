# ====================================
# DOCKERFILE - ECOME BACKEND
# Spring Boot + Java 21
# ====================================

# ============ BUILD STAGE ============
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy pom.xml trước để cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code và build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============ RUN STAGE ============
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health check
RUN apk add --no-cache curl

# Tạo user không phải root (bảo mật)
RUN addgroup -S spring && adduser -S spring -G spring

# Tạo thư mục uploads
RUN mkdir -p /app/uploads/images && chown -R spring:spring /app

# Copy jar từ build stage
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar

# Chuyển sang user spring
USER spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/categories || exit 1

# Environment variables mặc định
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SERVER_PORT=8080

# Run với JAVA_OPTS
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

