# Build stage — has the JDK + Maven, not shipped in the final image
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime stage — just a JRE + the built jar, everything else discarded
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/bank-demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
