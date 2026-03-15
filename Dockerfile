# Build stage
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/DoAn2-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT java -Xmx512m -Dserver.port=${PORT:-8080} -jar app.jar
