# -------- Build stage --------
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Кеш зависимостей
COPY pom.xml .
COPY .m2/settings.xml .m2/settings.xml
RUN mvn -q -s .m2/settings.xml -DskipTests dependency:go-offline

# Сборка приложения
COPY src ./src
RUN mvn -q -s .m2/settings.xml -DskipTests package

# -------- Frontend build stage --------
FROM node:22-alpine AS frontend
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --progress=false
COPY frontend .
RUN npm run build

# -------- Runtime stage --------
FROM ubuntu:24.04

# Обновления и базовые пакеты:
RUN apt-get update \
 && apt-get upgrade -y \
 && apt-get install -y --no-install-recommends \
      build-essential \
      openssl \
      zlib1g \
      wget \
      gnupg \
      ca-certificates \
      curl \
 && rm -rf /var/lib/apt/lists/*

# Установка Eclipse Temurin 21 (JRE)
RUN set -eux; \
    mkdir -p /etc/apt/keyrings; \
    wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public \
      | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg; \
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb/ $(. /etc/os-release && echo $VERSION_CODENAME) main" \
      > /etc/apt/sources.list.d/adoptium.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends temurin-21-jre; \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
ENV JAVA_OPTS=""
ENV LD_LIBRARY_PATH=/tdlibs
EXPOSE 8080

# Приложение
COPY --from=build /app/target/spring-telegram-explorer-0.0.1-SNAPSHOT.jar /app/app.jar
# Статические файлы Next.js
COPY --from=frontend /web/out /app/public

# Нативная библиотека TDLib
COPY tdlibs/libtdjni.so /tdlibs/libtdjni.so

# Регистрируем каталог с .so
RUN bash -lc 'echo "/tdlibs" > /etc/ld.so.conf.d/tdlibs.conf && ldconfig'

# Запуск
ENTRYPOINT ["/bin/bash", "-lc", "exec java $JAVA_OPTS -Djava.library.path=/tdlibs -jar /app/app.jar"]
