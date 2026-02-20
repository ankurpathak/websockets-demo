#!/bin/bash

# 1. Path Discovery
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Find the Nginx binary from Homebrew
NGINX_BIN=$(brew --prefix nginx)/bin/nginx
CONF_FILE="$PROJECT_ROOT/nginx.conf"
WORKSPACE="$PROJECT_ROOT/instances"
PID_FILE="$WORKSPACE/nginx.pid"

# 2. Ensure Workspace exists for logs, PIDs, and temp buffers
mkdir -p "$WORKSPACE/client_body" "$WORKSPACE/proxy_temp"

start() {
    if [ -f "$PID_FILE" ] && ps -p $(cat "$PID_FILE") > /dev/null 2>&1; then
        echo "⚠️ Nginx (Layer 7) is already running (PID: $(cat "$PID_FILE"))."
        return
    fi

    echo "🚀 Starting Nginx Layer 7 Proxy (User Mode)..."
    echo "📍 Project Root: $PROJECT_ROOT"

    # EXECUTE WITHOUT SUDO
    # We override all temp paths to the local workspace to avoid permission errors
    "$NGINX_BIN" \
        -p "$PROJECT_ROOT/" \
        -c "$CONF_FILE" \
        -g "pid $PID_FILE;
            daemon on;"

    if [ $? -eq 0 ]; then
        echo "✅ Nginx started on https://demo.local:7443"
    else
        echo "❌ Failed to start. Check if port 7443 is taken (lsof -i :7443)."
    fi
}

stop() {
    if [ -f "$PID_FILE" ]; then
        echo "🛑 Stopping Nginx..."
        "$NGINX_BIN" -p "$PROJECT_ROOT/" -c "$CONF_FILE" -g "pid $PID_FILE;" -s stop
        rm -f "$PID_FILE"
        echo "✅ Nginx stopped."
    else
        echo "⚠️ No Nginx PID file found."
    fi
}

reload() {
    if [ -f "$PID_FILE" ]; then
        echo "♻️ Reloading configuration..."
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
