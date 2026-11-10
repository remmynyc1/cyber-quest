# Stage 1: Build application JAR
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Download ttyd binary
FROM debian:bookworm-slim AS ttyd
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*
ARG TTYD_VERSION=1.7.7
RUN curl -sLo /ttyd https://github.com/tsl0922/ttyd/releases/download/${TTYD_VERSION}/ttyd.x86_64 \
    && chmod +x /ttyd

# Stage 3: Runtime environment
FROM debian:bookworm-slim
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    nginx \
    gettext-base \
    tshark \
    gawk \
    procps \
    bsdmainutils \
    ca-certificates \
    util-linux \
    && rm -rf /var/lib/apt/lists/*

COPY --from=ttyd /ttyd /usr/local/bin/ttyd

# Create separate users for app execution and terminal access
RUN useradd -r -m -s /usr/sbin/nologin appuser && useradd -m -s /bin/bash questuser

# Stage challenge evidence files
RUN mkdir -p /home/questuser/evidence 
COPY src/main/resources/static/evidence/* /home/questuser/evidence/
# Fixed: changed -r to uppercase -R
RUN chown -R questuser:questuser /home/questuser && chmod -R 755 /home/questuser/evidence

# Copy lab wrappers if present (ensure lab/ exists in your project)
COPY lab/nmap /usr/local/bin/nmap
COPY lab/lab-session.sh /usr/local/bin/lab-session.sh
RUN chmod 755 /usr/local/bin/nmap /usr/local/bin/lab-session.sh

# Copy application JAR and restrict read access to appuser only
COPY --from=builder /app/target/*.jar /app/app.jar
# Fixed: changed /app/app.kar to /app/app.jar
RUN chown appuser:appuser /app/app.jar && chmod 400 /app/app.jar

COPY nginx.conf.template /etc/nginx/nginx.conf.template
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 10000

ENTRYPOINT ["/entrypoint.sh"]