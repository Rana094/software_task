FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,id=university-maven,target=/root/.m2/repository mvn -B clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system campus && useradd --system --gid campus campus
COPY --from=build --chown=campus:campus /app/target/university-management-1.0.0.jar app.jar
USER campus
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

