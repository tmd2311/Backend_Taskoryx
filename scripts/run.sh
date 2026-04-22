#!/bin/bash
set -e

APP_DIR="/home/ubuntu/taskoryx-backend"
LOG_FILE="$APP_DIR/logs/app.log"
JAR="$APP_DIR/app.jar"
ENV_FILE="$APP_DIR/.env"

cd "$APP_DIR"

if [ -f "$ENV_FILE" ]; then
  source "$ENV_FILE"
else
  echo "ERROR: .env not found at $ENV_FILE"
  exit 1
fi

PID=$(pgrep -f 'app.jar' || true)
if [ -n "$PID" ]; then
  echo "Stopping existing app (PID: $PID)..."
  kill -15 "$PID"
  sleep 5
  if kill -0 "$PID" 2>/dev/null; then
    kill -9 "$PID"
  fi
fi

mkdir -p "$APP_DIR/logs"

echo "Starting app with profile: prod"
nohup java \
  -Xms256m -Xmx512m \
  -Dspring.profiles.active=prod \
  -jar "$JAR" \
  > "$LOG_FILE" 2>&1 &

echo "App started with PID: $!"
