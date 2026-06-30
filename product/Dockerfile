FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY product/settings.gradle.kts product/build.gradle.kts ./
COPY product/gradle ./gradle
COPY product/gradlew ./
COPY product/src ./src

RUN --mount=type=cache,target=/root/.gradle chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
