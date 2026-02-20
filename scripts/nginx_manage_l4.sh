#!/bin/bash

# 1. Path Discovery
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
# Get the actual binary path (e.g., /opt/homebrew/bin/nginx)
NGINX_BIN=$(brew --prefix nginx)/bin/nginx
CONF_FILE="$PROJECT_ROOT/nginx.l4.conf"
WORKSPACE="$PROJECT_ROOT/instances"
PID_FILE="$WORKSPACE/nginx.pid"

# 2. Ensure Workspace Exists and is Writable
# Create temp directories to avoid permission errors
mkdir -p "$WORKSPACE/client_body" "$WORKSPACE/proxy_temp"

start() {
    if [ -f "$PID_FILE" ] && ps -p $(cat "$PID_FILE") > /dev/null 2>&1; then
        echo "⚠️ Nginx (Layer 4) is already running (PID: $(cat "$PID_FILE"))."
        return
    fi

    echo "🚀 Starting Nginx Layer 4 TCP Proxy (No Sudo)..."

    # We remove 'sudo'
    # Nginx will run as your current user.
    # Ensure port 7443 is used (ports > 1024 don't need root).
    "$NGINX_BIN" \
        -p "$PROJECT_ROOT/" \
        -c "$CONF_FILE" \
        -g "pid $PID_FILE;
            daemon on;"

    if [ $? -eq 0 ]; then
        echo "✅ Nginx started. Listening on demo.local:7443"
    else
        echo "❌ Failed to start. Check if port 7443 is taken or if log paths are writable."
    fi
}

stop() {
    if [ -f "$PID_FILE" ]; then
        echo "🛑 Stopping Nginx..."
        "$NGINX_BIN" -p "$PROJECT_ROOT/" -c "$CONF_FILE" -g "pid $PID_FILE;" -s stop
        rm -f "$PID_FILE"
        echo "✅ Nginx stopped."
    else
        echo "⚠️ No PID file found."
    fi
}

reload() {
    if [ -f "$PID_FILE" ]; then
        "$NGINX_BIN" -p "$PROJECT_ROOT/" -c "$CONF_FILE" -g "pid $PID_FILE;" -s reload
        echo "✅ Configuration reloaded."
    else
        echo "❌ Nginx is not running."
    fi
}

status() {
    if [ -f "$PID_FILE" ] && ps -p $(cat "$PID_FILE") > /dev/null 2>&1; then
        echo "🟢 Nginx is RUNNING (PID: $(cat "$PID_FILE"))"
        lsof -i :7443 -P | grep LISTEN
    else
        echo "🔴 Nginx is STOPPED"
    fi
}

case "$1" in
    start)  start ;;
    stop)   stop ;;
    reload) reload ;;
    status) status ;;
    *)      echo "Usage: $0 {start|stop|reload|status}" ;;
esac
