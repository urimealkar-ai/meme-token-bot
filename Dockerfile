# Этап 1: Сборка (build)
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests   # ← ЗДЕСЬ КОМПИЛИРУЕТСЯ КОД!

# Этап 2: Запуск (runtime)
FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /app/target/*.jar app.jar  # ← БЕРЁТ JAR ИЗ ЭТАПА СБОРКИ
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
