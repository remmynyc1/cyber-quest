#!/usr/bin/env bash
set -e

export PORT="${PORT:-10000}"

# 1. Generate Nginx configuration
envsubst '${PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

# 2. Start Spring Boot on dedicated internal port 8085
echo "Starting Spring Boot on 127.0.0.1:8085..."
java -Dserver.port=8085 -Dserver.address=127.0.0.1 -jar /app/app.jar &

# 3. Start ttyd bound strictly to loopback
echo "Starting ttyd on 127.0.0.1:7681..."
su - questuser -c "ttyd -i 127.0.0.1 -p 7681 -b /terminal -W bash -c '
    SESSION_DIR=\$(mktemp -d /tmp/quest_XXXXXX)
    cp -r /home/questuser/evidence \"\$SESSION_DIR/\"
    cd \"\$SESSION_DIR/evidence\"
    clear
    echo \"===================================================================\"
    echo \"  CYBER QUEST FORENSIC WORKSPACE (Debian GNU/Linux)\"
    echo \"  Tools: grep, awk, tshark (Wireshark CLI), nmap, coreutils\"
    echo \"  Directory: \$(pwd)\"
    echo \"===================================================================\"
    ls -lah
    exec bash
'" &

# 4. Wait until Spring Boot has fully loaded Hibernate and bound port 8085
echo "Waiting for Spring Boot to be healthy on 127.0.0.1:8085..."
until curl -s http://127.0.0.1:8085 > /dev/null 2>&1; do
    sleep 2
done
echo "Spring Boot is online! Starting Nginx reverse proxy..."

# 5. Launch Nginx once backends are ready
exec nginx -g 'daemon off;'