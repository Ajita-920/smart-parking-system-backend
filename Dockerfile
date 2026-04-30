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

RUN apt-get update && apt-get install -y inotify-tools

EXPOSE 8080

CMD ["mvn", "spring-boot:run"]


# ---------- Production Stage ----------
FROM eclipse-temurin:21-jdk-alpine AS production

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]