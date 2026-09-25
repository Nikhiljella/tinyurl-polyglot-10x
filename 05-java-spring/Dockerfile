FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true
COPY src/ ./src/
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/tinyurl-spring-1.0.0.jar app.jar
ENV PORT=8005
EXPOSE 8005
ENTRYPOINT ["java", "-jar", "app.jar"]
