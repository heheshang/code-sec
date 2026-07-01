# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml backend/pom.xml
COPY backend/common/pom.xml backend/common/pom.xml
COPY backend/engine/pom.xml backend/engine/pom.xml
COPY backend/api/pom.xml backend/api/pom.xml
COPY backend/engine-adapter/pom.xml backend/engine-adapter/pom.xml
COPY backend/worker/pom.xml backend/worker/pom.xml

# Download dependencies (cache layer)
RUN mvn dependency:go-offline -pl backend/worker -am -B

# Copy sources
COPY backend/common/ backend/common/
COPY backend/engine/ backend/engine/
COPY backend/api/ backend/api/
COPY backend/engine-adapter/ backend/engine-adapter/
COPY backend/worker/ backend/worker/

# Build Worker JAR
RUN mvn package -pl backend/worker -am -B -DskipTests

# Stage 2 — Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/backend/worker/target/code-sec-backend-worker-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
