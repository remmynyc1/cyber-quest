#!/usr/bin/env bash 
set -e 

# Default to 8080 if running outside Render
export PORT="${PORT:-8080}"

# 1. Inject the dynamic $PORT into Nginx configuration
envsubst '${PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

# 2. Start Spring Boot on local port 8080
echo "Starting Spring Boot on port 8080..."
SERVER_PORT=8080 java -jar /app/app.jar &

# 3. Start ttyd on local port 7681
# mktemp creates a private workspace per tab so users don't overwrite each other's work
echo "Starting ttyd on port 7681..."
su - questuser -c "ttyd -p 7681 -b /terminal -W bash -c'
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
# 4. Start Nginx in the foreground to keep the container alive 
echo "Starting Nginx reverse proxy on port $PORT..."
exec nginx -g 'daemon off;'