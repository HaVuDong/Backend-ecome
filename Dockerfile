# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy jar từ build stage
COPY --from=build /app/target/*.jar app.jar

# Tạo thư mục uploads
RUN mkdir -p /app/uploads/images

# Expose port
EXPOSE 8080

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
