FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY category/settings.gradle.kts category/build.gradle.kts ./
COPY category/gradle ./gradle
COPY category/gradlew ./
COPY category/src ./src

RUN --mount=type=cache,target=/root/.gradle chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
