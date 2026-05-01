# ---------- Base (shared setup) ----------
FROM maven:3.9-eclipse-temurin-21 AS base

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src


# ---------- Build Stage ----------
FROM base AS build

RUN mvn clean package -DskipTests


# ---------- Development Stage ----------
FROM base AS development

# Spring DevTools uses its own polling-based restart — no OS-level inotify needed.
# This works identically on macOS, Linux, and Windows Docker Desktop.
ENV SPRING_DEVTOOLS_RESTART_POLL_INTERVAL=2000 \
    SPRING_DEVTOOLS_RESTART_QUIET_PERIOD=1000

EXPOSE 8080

CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.jvmArguments=-Dspring.devtools.remote.secret=secret"]


# ---------- Production Stage ----------
FROM eclipse-temurin:21-jdk-alpine AS production

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]