# ---------- build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -DskipTests
COPY src src
RUN ./mvnw package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]