#!/usr/bin/env bash
set -e

export PORT="${PORT:-10000}"

# 1. Substitute Render external port into Nginx configuration and validate
envsubst '${PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf
nginx -t

# 2. Start Nginx early so Render detects the bound port on 0.0.0.0
echo "Starting Nginx reverse proxy on port $PORT..."
nginx -g 'daemon off;' &
NGINX_PID=$!

# 3. Launch Spring Boot on 127.0.0.1:8085
echo "Starting Spring Boot on 127.0.0.1:8085..."
java -XX:MaxRAMPercentage=50 -XX:+UseSerialGC -Xss512k \
     -Dserver.port=8085 -Dserver.address=127.0.0.1 -jar /app/app.jar &
JAVA_PID=$!

# 4. Launch ttyd terminal daemon on 127.0.0.1:7681
echo "Starting ttyd on 127.0.0.1:7681..."
su -s /bin/bash questuser -c "ttyd -i 127.0.0.1 -p 7681 -b /terminal -W /usr/local/bin/lab-session.sh" > /tmp/ttyd.log 2>&1 &

# 5. Wait for port 8085 to open
echo "Waiting for Spring Boot on 127.0.0.1:8085..."
READY=0
for i in {1..75}; do
    if ! kill -0 $JAVA_PID 2>/dev/null; then
        echo "Spring Boot exited unexpectedly before opening port 8085."
        kill $NGINX_PID 2>/dev/null || true
        exit 1
    fi
    if (echo > /dev/tcp/127.0.0.1/8085) 2>/dev/null; then
        READY=1
        echo "Spring Boot is active on port 8085!"
        break
    fi
    echo "Waiting for Spring Boot to start (attempt $i of 75)..."
    sleep 2
done

if [ "$READY" -ne 1 ]; then
    echo "Spring Boot did not start in time."
    kill -9 $JAVA_PID 2>/dev/null || true
    kill $NGINX_PID 2>/dev/null || true
    exit 1
fi

wait $NGINX_PID