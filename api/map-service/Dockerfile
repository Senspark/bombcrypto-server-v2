# Build stage: compiles the app and produces a fat jar via the Shadow plugin.
FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /app

COPY settings.gradle.kts build.gradle.kts ./
RUN gradle --no-daemon dependencies >/dev/null 2>&1 || true

COPY src ./src
RUN gradle --no-daemon shadowJar

# Runtime stage: just a JRE + the fat jar. No DB driver, no OkHttp, no dependency on the main
# server's stack at all -- MapService is a pure compute service, callee only.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/mapservice.jar ./mapservice.jar

ENV MAP_SERVICE_PORT=80
EXPOSE 80

CMD ["java", "-jar", "mapservice.jar"]
