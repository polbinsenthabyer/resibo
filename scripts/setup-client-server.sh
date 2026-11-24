#!/usr/bin/env bash
set -euo pipefail

# One-time server preparation for the Resibo React client.
#
# Creates the directory nginx serves from and installs the vhost. Run it once,
# on the server, from the repository checkout:
#
#   cd /opt/resibo/app && sudo -E bash scripts/setup-client-server.sh
#
# Safe to re-run: every step is idempotent.
#
# This script does NOT touch the API — not its container, not its vhost, not
# its port. A frontend release and a backend release share nothing.
#
# ⚠️ Files destined for root-owned locations are staged as the invoking user
# and moved into place with `install`. Never write them through a `sudo` pipe:
# if a password is ever on stdin, any command in the pipeline can swallow it
# into the output, which is exactly how a credential once ended up inside
# /etc/nginx/sites-available/resibo. See resibo-plan.md §8.1.

ENVIRONMENT="${1:-live}"
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WWW_ROOT="/var/www/resibo"
TARGET="${WWW_ROOT}/${ENVIRONMENT}"
VHOST_NAME="resibo-web"
VHOST_SRC="${REPO_DIR}/deploy/nginx/${VHOST_NAME}.conf"
VHOST_DEST="/etc/nginx/sites-available/${VHOST_NAME}"

# Who should own the served files. The deploy runs as this account, and it is
# the same arrangement gainsoclock uses (/var/www/gainsoclock/live is owned by
# `deploy`, while its parent stays root-owned).
OWNER="${DEPLOY_USER:-${SUDO_USER:-$(id -un)}}"

if [[ $EUID -ne 0 ]]; then
  echo "Run with sudo: sudo -E bash scripts/setup-client-server.sh" >&2
  exit 1
fi

if [[ ! -f "$VHOST_SRC" ]]; then
  echo "Vhost template not found: $VHOST_SRC" >&2
  exit 1
fi

# --- the directory nginx serves ---------------------------------------------
# Parent stays root-owned; only the environment directory is writable by the
# deploying account, so a compromised deploy cannot rewrite a sibling.
install -d -o root -g root -m 755 "$WWW_ROOT"
install -d -o "$OWNER" -g "$OWNER" -m 755 "$TARGET"
echo "==> $TARGET ready, owned by $OWNER"

# A placeholder so the vhost has something to serve before the first deploy.
if [[ ! -f "$TARGET/index.html" ]]; then
  printf '%s\n' '<!doctype html><title>Resibo</title><p>Not deployed yet.</p>' \
    > "$TARGET/index.html"
  chown "$OWNER:$OWNER" "$TARGET/index.html"
  chmod 644 "$TARGET/index.html"
  echo "==> Wrote a placeholder index.html"
fi

# --- the vhost ---------------------------------------------------------------
install -o root -g root -m 644 "$VHOST_SRC" "$VHOST_DEST"
ln -sfn "$VHOST_DEST" "/etc/nginx/sites-enabled/${VHOST_NAME}"
echo "==> Installed $VHOST_DEST and enabled it"

# Validate before reloading. A bad config that is never tested takes every
# site on this host down, not just this one — the server also fronts
# gainsoclock and eloms.
if ! nginx -t; then
  echo "nginx config test FAILED — removing the symlink and leaving nginx as it was" >&2
  rm -f "/etc/nginx/sites-enabled/${VHOST_NAME}"
  exit 1
fi

systemctl reload nginx
echo "==> nginx reloaded"

# --- prove it serves ---------------------------------------------------------
if curl -fsS --max-time 5 -o /dev/null -H 'Host: resibo.polbinsenthabyer.uk' http://127.0.0.1/; then
  echo "==> Serving resibo.polbinsenthabyer.uk from $TARGET"
else
  echo "WARNING: nginx did not answer for that Host header" >&2
  exit 1
fi

# --- do not disturb the neighbours -------------------------------------------
# This host fronts several unrelated projects. A reload that broke one of them
# would otherwise be discovered by its owner, not by us.
for neighbour in resibo-api.polbinsenthabyer.uk gainsoclock-api.polbinsenthabyer.uk; do
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 -H "Host: $neighbour" http://127.0.0.1/ || echo 000)
  echo "    $neighbour -> HTTP $code"
done

cat <<'DONE'

Done. Still to do, outside this script:
  - Cloudflare: add resibo.polbinsenthabyer.uk to the personal-projects tunnel
    ingress (APPEND before the http_status:404 catch-all — a whole-config PUT
    carrying only this rule silently drops every neighbouring service) and
    create the proxied CNAME.
  - Deploy the client: scripts/deploy-client.sh <dist> live
DONE
