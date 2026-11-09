# Stage 1: Build application JAR
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Debian runtime with real tools (ttyd, tshark, nmap, nginx)
FROM debian:bookworm-slim

ENV DEBIAN_FRONTEND=noninteractive 

# Install Java 17 headless runtime, Nginx, Nmap, TShark (Wireshark CLI), and tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    nginx \
    gettext-base \
    nmap \
    tshark \
    tcpdump \
    curl \
    ca-certificates \
    procps \
    bsdmainutils \
    && rm -rf /var/lib/apt/lists/*

# Install ttyd binary (requires glibc provided by Debian)
ARG TTYD_VERSION=1.7.7
RUN curl -sLo /usr/local/bin/ttyd https://github.com/tsl0922/ttyd/releases/download/${TTYD_VERSION}/ttyd.x86_64 && \
    chmod +x /usr/local/bin/ttyd

# Create non-root terminal user
RUN useradd -m -s /bin/bash questuser
WORKDIR /home/questuser

# Stage challenge evidence files
RUN mkdir -p /home/questuser/evidence
COPY src/main/resources/static/evidence/* /home/questuser/evidence/
RUN chown -R questuser:questuser /home/questuser && \
    chmod -R 755 /home/questuser/evidence


# Copy comiled Spring Boot JAR (Fixed path from /app/target/)
COPY --from=builder /app/target/*.jar /app/app.jar

# Add entrypoint and reverse proxy configurations
COPY nginx.conf.template /etc/nginx/nginx.conf.template
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 10000

ENTRYPOINT [ "/entrypoint.sh" ]
