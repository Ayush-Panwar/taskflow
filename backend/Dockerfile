FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle gradle
# Downloads dependencies in their own layer, so changing source code does not repeat the download.
RUN ./gradlew dependencies --no-daemon --quiet
COPY src src
RUN ./gradlew bootJar --no-daemon --quiet

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow
USER taskflow
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
