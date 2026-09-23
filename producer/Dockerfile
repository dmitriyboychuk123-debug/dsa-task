FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    sh ./gradlew :producer:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S -g 10001 app && adduser -S -D -H -u 10001 -G app app

WORKDIR /app

COPY --from=build /workspace/producer/build/libs/producer-*-SNAPSHOT.jar app.jar

USER 10001:10001

ENTRYPOINT ["java", "-jar", "app.jar"]
