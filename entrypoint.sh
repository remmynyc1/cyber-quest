#!/usr/bin/env bash
set -e

# Default to Render standard port 10000 if $PORT is unset
export PORT="${PORT:-10000}"

# 1. Generate Nginx configuration with the assigned external port
envsubst '${PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

# 2. Start Spring Boot on dedicated internal port 8085
echo "Starting Spring Boot on 127.0.0.1:8085..."
java -Dserver.port=8085 -Dserver.address=127.0.0.1 -jar /app/app.jar &

# 3. Start ttyd bound strictly to loopback (127.0.0.1:7681)
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

# 4. Start Nginx in the foreground to listen on $PORT
echo "Starting Nginx on public port $PORT..."
exec nginx -g 'daemon off;'