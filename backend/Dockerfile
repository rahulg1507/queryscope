FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw
COPY src src
RUN ./mvnw --batch-mode package -DskipTests

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install --no-install-recommends --yes curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 queryscope

WORKDIR /app
COPY --from=build /workspace/target/queryscope-backend-*.jar app.jar

USER queryscope
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=20s --timeout=5s --start-period=25s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
