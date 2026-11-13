# Stage 1: Build JAR using Maven
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S borrowhub && adduser -S borrowhub -G borrowhub
USER borrowhub

COPY --from=builder /build/target/borrowhub-backend-1.0.0.jar /app/borrowhub-backend.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "/app/borrowhub-backend.jar"]
