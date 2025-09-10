# -------- Build stage --------
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

# Сначала зависимости (кэшируется отдельно)
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Затем исходники и сборка
COPY src ./src
RUN mvn -q -e -DskipTests package

# -------- Runtime stage --------
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_OPTS=""
# Если ты выставил порт 8082 — ок, EXPOSE носит информативный характер
EXPOSE 8082
COPY --from=build /app/target/tg-publisher-*.jar /app/app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
