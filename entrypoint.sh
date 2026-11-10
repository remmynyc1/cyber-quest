#!/usr/bin/env bash
set -e

export PORT="${PORT:-10000}"

# 1. nginx configuration (only ${PORT} is substituted)
envsubst '${PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

# 2. Spring Boot as the unprivileged appuser, on loopback only.
#    setpriv keeps the environment (database settings) and drops root.
echo "Starting Spring Boot on 127.0.0.1:8085..."
setpriv --reuid=appuser --regid=appuser --clear-groups \
  java -XX:MaxRAMPercentage=50 -XX:+UseSerialGC -Xss512k \
       -Dserver.port=8085 -Dserver.address=127.0.0.1 -jar /app/app.jar &
JAVA_PID=$!

# 3. ttyd as questuser, loopback only.
#    -O  refuse WebSocket connections from other sites
#    -m  at most 5 simultaneous terminals
#    -P  ping every 30 s so dead connections are dropped
echo "Starting ttyd on 127.0.0.1:7681..."
su - questuser -c "ttyd -i 127.0.0.1 -p 7681 -b /terminal -W -O -m 5 -P 30 /usr/local/bin/lab-session.sh" &

# 4. Wait for Spring Boot (no curl needed). Stop with a clear message if it dies.
wait_for_spring() {
  for _ in $(seq 1 150); do
    if (exec 3<>/dev/tcp/127.0.0.1/8085) 2>/dev/null; then return 0; fi
    if ! kill -0 "$JAVA_PID" 2>/dev/null; then return 1; fi
    sleep 2
  done
  return 1
}
echo "Waiting for Spring Boot on 127.0.0.1:8085..."
if wait_for_spring; then
  echo "Spring Boot is online. Starting nginx..."
else
  echo "Spring Boot did not start. Check the database settings in the Render environment."
  exit 1
fi

# 5. nginx in the foreground
exec nginx -g 'daemon off;'
