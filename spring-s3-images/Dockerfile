# -------- Build stage --------
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package  # создаст target/app.jar (boot-jar)

# -------- Runtime stage --------
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS=""
EXPOSE 8086
COPY --from=build /app/target/app.jar /app/app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
