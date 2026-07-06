# Build stage: Maven + JDK 25 on Alpine
FROM eclipse-temurin:25-jdk-alpine AS builder
RUN apk add --no-cache maven
WORKDIR /app
# Download dependencies as a separate cached layer
COPY pom.xml .
RUN mvn dependency:go-offline -q
# Build
COPY src ./src
RUN mvn -DskipTests -Dkotlin.compiler.execution.strategy=in-process package -q

# Runtime stage: JRE 25 on Alpine (~120 MB)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
