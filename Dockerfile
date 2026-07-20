FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
COPY config ./config
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 10001 bookranker
COPY --from=build /workspace/target/book-ranker-1.0.0.jar app.jar

USER bookranker
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
