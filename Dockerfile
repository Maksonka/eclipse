# Build stage
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

# Run stage: JRE
FROM eclipse-temurin:25-jre
WORKDIR /app

# Application
COPY --from=build /app/target/*.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 8080
ENV PORT=8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]