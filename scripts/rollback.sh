#!/usr/bin/env bash
set -euo pipefail

# Roll Resibo back to the previously deployed image.
#
#   cd /opt/resibo/app && sudo bash scripts/rollback.sh
#
# deploy.sh already rolls back automatically when a new image fails its health
# check. This script is for the other case: the deploy succeeded, the health
# endpoint answered, and the build turned out to be bad anyway.
#
# It swaps resibo-api:current and resibo-api:previous, so running it twice
# returns you to where you started. That is deliberate — a rollback you cannot
# undo is a second outage waiting to happen.

APP_DIR="${APP_DIR:-/opt/resibo}"
HOST_PORT="${HOST_PORT:-8083}"
IMAGE="${RESIBO_IMAGE:-resibo-api}"
HEALTH_URL="http://127.0.0.1:${HOST_PORT}/actuator/health"
HEALTH_ATTEMPTS="${HEALTH_ATTEMPTS:-30}"

export RESIBO_ENV_FILE="${RESIBO_ENV_FILE:-$APP_DIR/resibo.env}"
export HOST_PORT
export APP_ENV="${APP_ENV:-prod}"
export RESIBO_IMAGE="$IMAGE"

if ! docker image inspect "${IMAGE}:previous" >/dev/null 2>&1; then
  echo "No ${IMAGE}:previous image exists — nothing to roll back to." >&2
  echo "Available tags:" >&2
  docker images "$IMAGE" --format '  {{.Repository}}:{{.Tag}}  {{.CreatedSince}}' >&2
  exit 1
fi

echo "==> Current:  $(docker image inspect "${IMAGE}:current"  --format '{{.Id}}' 2>/dev/null || echo 'none')"
echo "==> Previous: $(docker image inspect "${IMAGE}:previous" --format '{{.Id}}')"

# Swap the two tags via a scratch tag, so the rollback is itself reversible.
docker tag "${IMAGE}:current"  "${IMAGE}:rollback-staging" 2>/dev/null || true
docker tag "${IMAGE}:previous" "${IMAGE}:current"
if docker image inspect "${IMAGE}:rollback-staging" >/dev/null 2>&1; then
  docker tag "${IMAGE}:rollback-staging" "${IMAGE}:previous"
  docker rmi "${IMAGE}:rollback-staging" >/dev/null 2>&1 || true
fi

echo "==> Restarting on the previous image"
RESIBO_TAG=current docker compose up -d --no-build

echo "==> Waiting for health at ${HEALTH_URL}"
for _ in $(seq 1 "$HEALTH_ATTEMPTS"); do
  if curl -fsS --max-time 2 -o /dev/null "$HEALTH_URL"; then
    echo "Rollback healthy. Run this script again to go back the other way."
    exit 0
  fi
  sleep 2
done

echo "The rolled-back image is not answering its health check either." >&2
docker compose logs --tail 50 api >&2 || true
exit 1
