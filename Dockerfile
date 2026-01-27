# ==================== STAGE 1: BUILD ====================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Копируем только pom.xml сначала (для кэширования зависимостей)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходники и собираем
COPY src ./src
RUN mvn clean package -DskipTests

# ==================== STAGE 2: RUNTIME ====================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache wget

# Создаём непривилегированного пользователя (для безопасности)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копируем JAR из stage build
COPY --from=build /app/target/*.jar app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]