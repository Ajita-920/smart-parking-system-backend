# ---------- Base ----------
FROM maven:3.9-eclipse-temurin-21 AS base

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml .

COPY src ./src


# ---------- Build ----------
FROM base AS build

RUN mvn clean package -DskipTests


# ---------- Development ----------
FROM base AS development

ENV SPRING_DEVTOOLS_RESTART_POLL_INTERVAL=2000 \
    SPRING_DEVTOOLS_RESTART_QUIET_PERIOD=1000

EXPOSE 8080

CMD ["mvn", "spring-boot:run"]


# ---------- Production ----------
FROM eclipse-temurin:21-jre-alpine AS production

WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S spring \
    && adduser -S spring -G spring

COPY --from=build /app/target/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
