#!/usr/bin/env bash
set -euo pipefail

# Deploy Resibo on the server.
#
# Same shape as gainsoclock/scripts/deploy.sh — stage the new build, restart,
# health-check, roll back if it fails — but the artifact is a container image
# rather than a JAR, so "keep the previous one" means keeping an image tag
# instead of app.jar.previous.
#
# Run it on the server, from the repository checkout:
#
#   cd /opt/resibo/app && git pull && sudo -E bash scripts/deploy.sh
#
# Nothing here reads a secret. Values come from the env file named by
# RESIBO_ENV_FILE, which lives outside the checkout and is never committed.

APP_DIR="${APP_DIR:-/opt/resibo}"
HOST_PORT="${HOST_PORT:-8083}"
IMAGE="${RESIBO_IMAGE:-resibo-api}"
HEALTH_URL="http://127.0.0.1:${HOST_PORT}/actuator/health"
HEALTH_ATTEMPTS="${HEALTH_ATTEMPTS:-30}"

export RESIBO_ENV_FILE="${RESIBO_ENV_FILE:-$APP_DIR/resibo.env}"
export HOST_PORT
export APP_ENV="${APP_ENV:-prod}"
export RESIBO_IMAGE="$IMAGE"

compose() { docker compose "$@"; }

if [[ ! -f "$RESIBO_ENV_FILE" ]]; then
  echo "Env file not found: $RESIBO_ENV_FILE" >&2
  echo "Create it from .env.example — it is deliberately not in the repo." >&2
  exit 1
fi

# --- build the new image, without disturbing what is currently running ------
echo "==> Building ${IMAGE}:next"
RESIBO_TAG=next compose build

# --- remember what is running now, so a failure is recoverable --------------
had_previous=false
if docker image inspect "${IMAGE}:current" >/dev/null 2>&1; then
  docker tag "${IMAGE}:current" "${IMAGE}:previous"
  had_previous=true
  echo "==> Tagged the running image as ${IMAGE}:previous"
else
  echo "==> No current image; this is the first deployment"
fi

docker tag "${IMAGE}:next" "${IMAGE}:current"

start_app() {
  RESIBO_TAG=current compose up -d --no-build
}

restore_previous() {
  if [[ "$had_previous" == true ]]; then
    echo "Restoring previous image" >&2
    docker tag "${IMAGE}:previous" "${IMAGE}:current"
    start_app || echo "Rollback restart failed; inspect: docker compose logs api" >&2
  else
    echo "No previous image to restore; this was the first deployment" >&2
    RESIBO_TAG=current compose down || true
  fi
}

echo "==> Starting ${IMAGE}:current"
if ! start_app; then
  restore_previous
  exit 1
fi

# --- health gate ------------------------------------------------------------
echo "==> Waiting for health at ${HEALTH_URL}"
for _ in $(seq 1 "$HEALTH_ATTEMPTS"); do
  if curl -fsS --max-time 2 -o /dev/null "$HEALTH_URL"; then
    echo "Deployment healthy"
    docker image prune -f --filter "label=stage=build" >/dev/null 2>&1 || true
    exit 0
  fi
  sleep 2
done

echo "New image failed its health check" >&2
compose logs --tail 50 api >&2 || true
restore_previous
exit 1
