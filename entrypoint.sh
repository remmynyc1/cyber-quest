#!/usr/bin/env bash
set -e

export PORT="${PORT:-10000}"

# 1. Substitute Render external port into Nginx configuration
envsubst '${PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

# 2. Launch Spring Boot on 127.0.0.1:8085
echo "Starting Spring Boot on 127.0.0.1:8085..."
java -XX:MaxRAMPercentage=50 -XX:+UseSerialGC -Xss512k \
     -Dserver.port=8085 -Dserver.address=127.0.0.1 -jar /app/app.jar &
JAVA_PID=$!

# 3. Launch ttyd terminal daemon on 127.0.0.1:7681
echo "Starting ttyd on 127.0.0.1:7681..."
su - questuser -c "ttyd -i 127.0.0.1 -p 7681 -b /terminal -W /usr/local/bin/lab-session.sh" &

# 4. Wait up to 60 seconds for port 8085 to open
echo "Waiting for Spring Boot on 127.0.0.1:8085..."
READY=0
for i in {1..30}; do
    if (echo > /dev/tcp/127.0.0.1/8085) 2>/dev/null; then
        READY=1
        echo "Spring Boot is active on port 8085!"
        break
    fi
    echo "Waiting for Spring Boot to start (attempt $i of 30)..."
    sleep 2
done

if [ "$READY" -ne 1 ]; then
    echo "Spring Boot did not start within 60 seconds."
    kill -9 $JAVA_PID 2>/dev/null || true
    exit 1
fi

# 5. Start Nginx
echo "Starting Nginx reverse proxy on port $PORT..."
exec nginx -g 'daemon off;'