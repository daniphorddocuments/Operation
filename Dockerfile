FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
RUN mvn -q -DskipTests dependency:go-offline

COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
COPY python_ai_service python_ai_service

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
