
# Etapa 1: Compilar el proyecto con Maven
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests

# Etapa 2: Ejecutar la aplicacion
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/ms-campuslab-bookings-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
