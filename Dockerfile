# Build stage
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Add a non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy the built JAR
COPY --from=build /app/target/ofood-0.0.1-SNAPSHOT.jar app.jar

# Enforce listening on 0.0.0.0 for Render (and Docker in general)
ENV SERVER_ADDRESS=0.0.0.0

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
