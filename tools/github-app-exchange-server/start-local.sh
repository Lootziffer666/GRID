#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <APP_ID> <PATH_TO_PEM> [PORT]"
  exit 1
fi

APP_ID_VALUE="$1"
APP_PRIVATE_KEY_PATH_VALUE="$2"
PORT_VALUE="${3:-3000}"

if [[ ! -f "$APP_PRIVATE_KEY_PATH_VALUE" ]]; then
  echo "PEM file not found: $APP_PRIVATE_KEY_PATH_VALUE"
  exit 1
fi

export APP_ID="$APP_ID_VALUE"
export APP_PRIVATE_KEY_PATH="$APP_PRIVATE_KEY_PATH_VALUE"
export PORT="$PORT_VALUE"

echo "Starting GitHub App exchange server on port $PORT..."
npm install
npm start
