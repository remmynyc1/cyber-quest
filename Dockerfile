# Stage 1: Build application JAR
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Distroless JRE Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S cqgroup && adduser -S cqoper -G cqgroup
USER cqoper

COPY --from=builder /build/target/demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]