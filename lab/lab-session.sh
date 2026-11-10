#!/usr/bin/env bash
# Spawns a sandboxed workspace for each terminal connection
SESSION_DIR=$(mktemp -d /tmp/quest_XXXXXX)
cp -r /home/questuser/evidence "$SESSION_DIR/"
cd "$SESSION_DIR/evidence"
clear
echo "==================================================================="
echo "  CYBER QUEST FORENSIC WORKSPACE (Debian GNU/Linux)"
echo "  Tools: grep, awk, tshark (Wireshark CLI), nmap"
echo "  Workspace: $(pwd)"
echo "==================================================================="
ls -lah
exec bash