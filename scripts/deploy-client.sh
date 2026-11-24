#!/usr/bin/env bash
set -euo pipefail

# Deploy the Resibo React client on the server.
#
# Same shape as gainsoclock/scripts/deploy.sh for its frontend: the build
# happens in CI, this script only places files. It takes a directory of built
# files and an environment name.
#
#   bash scripts/deploy-client.sh /path/to/dist live
#
# This script deliberately does NOT touch the API. The backend is a Docker
# container on 127.0.0.1:8083; the frontend is static files under /var/www.
# They share no process, no port and no directory, which is what makes a
# frontend release incapable of restarting the API — see resibo-plan.md §7.4.
#
# Nothing here reads a secret. The client holds no secrets: every VITE_ value
# is compiled into the bundle the browser downloads.

build_dir="${1:?Provide the directory holding the built files (client/dist)}"
environment="${2:-live}"

case "$environment" in
  live) target=/var/www/resibo/live ;;
  prelive) target=/var/www/resibo/prelive ;;
  *)
    echo "Unknown environment: $environment (expected live or prelive)" >&2
    exit 1
    ;;
esac

# A build with no index.html is not a build. Catching it here means a broken
# artifact never reaches the served directory.
if [[ ! -f "$build_dir/index.html" ]]; then
  echo "No index.html in $build_dir — refusing to deploy an incomplete build" >&2
  exit 1
fi

if [[ ! -d "$target" ]]; then
  echo "Target directory does not exist: $target" >&2
  echo "Run scripts/setup-client-server.sh once to create it." >&2
  exit 1
fi

# --- keep the previous release, so a rollback needs no rebuild -------------
# Stored under /var/www/resibo/.previous rather than beside the live
# directory: the parent is deliberately root-owned so a compromised deploy
# cannot rewrite a sibling, and .previous is the one place in it the deploy
# account may write. It sits outside the vhost's root, so nginx never serves
# it.
previous="/var/www/resibo/.previous/${environment}"
if [[ -d "$(dirname "$previous")" ]] && [[ -n "$(ls -A "$target" 2>/dev/null)" ]]; then
  rm -rf "$previous"
  cp -a "$target" "$previous"
  echo "==> Kept the current release as $previous"
elif [[ ! -d "$(dirname "$previous")" ]]; then
  echo "==> No $(dirname "$previous") directory; skipping the rollback copy."
  echo "    Re-run scripts/setup-client-server.sh to create it."
fi

# --- place the new files ----------------------------------------------------
# index.html is excluded here and installed last. Keep old assets: a browser
# may still hold the previous index.html while this sync runs, and deleting its
# referenced hashes would make that page fail to load. Unused assets can be
# cleaned up separately after their old HTML is no longer in circulation.
echo "==> Syncing assets into $target"
rsync -r --exclude='/index.html' "$build_dir"/ "$target"/

find "$target" -type d -exec chmod 755 {} +
find "$target" -type f -exec chmod 644 {} +

echo "==> Swapping index.html"
install -m 0644 "$build_dir/index.html" "$target/index.html.next"
mv -f "$target/index.html.next" "$target/index.html"

# --- prove it is actually being served --------------------------------------
# nginx needs no reload for static files, so this is a check rather than a
# step: it confirms the vhost is up and serving the file just written.
if command -v curl >/dev/null 2>&1; then
  host="resibo.polbinsenthabyer.uk"
  [[ "$environment" == "prelive" ]] && host="prelive-resibo.polbinsenthabyer.uk"
  if curl -fsS --max-time 5 -o /dev/null -H "Host: $host" http://127.0.0.1/; then
    echo "==> Served OK through nginx as $host"
  else
    echo "WARNING: nginx did not serve $host — files are in place, the vhost may not be." >&2
  fi
fi

echo "$environment Resibo client deployed."
