# syntax=docker/dockerfile:1
# ---------------------------------------------------------------------------
# FASHION E-COMMERCE - BACKEND
# Multi-stage build: Maven (build) -> Temurin JRE 17 (runtime)
# ---------------------------------------------------------------------------

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies first (pom rarely changes vs source)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as non-root user
RUN groupadd -r app && useradd -r -g app app

COPY --from=build /build/target/fashion-ecommerce-1.0.0-SNAPSHOT.jar /app/app.jar

USER app
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
