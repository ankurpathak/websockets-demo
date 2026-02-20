#!/bin/bash

# 1. Path Configuration
# Points to target/ from the scripts/ directory
JAR_FILE="../target/app.war"
PORTS=(8443 8444 8445 8446)

# 2. Workspace Configuration
# Create a folder in the project root for runtime files
WORKSPACE="../instances"
PID_FILE="$WORKSPACE/server_pids.txt"

# Ensure workspace exists
mkdir -p "$WORKSPACE"

start() {
    if [ ! -f "$JAR_FILE" ]; then
        echo "❌ Error: Jar file not found at $JAR_FILE"
        echo "Run 'mvn package' in the project root first."
        exit 1
    fi

    echo "🚀 Starting 4 Instances (Dynamic SSL Bundle)..."
    > "$PID_FILE" # Clear old PIDs

    for PORT in "${PORTS[@]}"
    do
        LOG_FILE="$WORKSPACE/server_$PORT.log"

        # Run Java in background
        # -DSERVER_PORT overrides the property in application.properties
        java -DSERVER_PORT=$PORT -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &

        PID=$!
        echo $PID >> "$PID_FILE"
        echo "[+] Port $PORT: Started with PID $PID (Log: instances/server_$PORT.log)"
    done

    echo "⏳ Waiting for Ports to open..."
    for PORT in "${PORTS[@]}"
    do
        printf "Checking Port $PORT..."
        # Loop until Netcat (nc) can connect to the port
        # -z: scan mode, -w1: 1 second timeout
        until nc -z localhost "$PORT" > /dev/null 2>&1; do
            printf "."
            sleep 1
        done
        echo " OPEN ✅"
    done

    echo "✨ All instances are responding on TCP ports!"
    echo "Test with: curl -k https://localhost:${PORTS[0]}"
}

stop() {
    if [ ! -f "$PID_FILE" ]; then
        echo "⚠️ No $PID_FILE found. Using pkill as backup..."
        pkill -f "websockets-demo"
        return
    fi

    echo "Stopping instances..."
    while read -r PID; do
        if ps -p "$PID" > /dev/null; then
            kill "$PID"
            echo "[-] Stopped PID $PID"
        else
            echo "[!] PID $PID not found (already stopped)."
        fi
    done < "$PID_FILE"

    rm "$PID_FILE"
    echo "🛑 All instances stopped."
}

status() {
    echo "--- Current Instances ---"
    if [ -f "$PID_FILE" ]; then
        while read -r PID; do
            if ps -p "$PID" > /dev/null; then
                # Get the port associated with this PID from the command line
                PORT=$(ps -fp "$PID" | grep -o 'SERVER_PORT=[0-9]*' | cut -d'=' -f2)
                echo "PID $PID is running on Port $PORT"
            fi
        done < "$PID_FILE"
    else
        echo "No instances running according to $PID_FILE"
    fi
}

case "$1" in
    start)   start ;;
    stop)    stop ;;
    restart) stop; sleep 2; start ;;
    status)  status ;;
    *)       echo "Usage: $0 {start|stop|restart|status}" ;;
esac
