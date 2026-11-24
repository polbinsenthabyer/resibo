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
# The vhost lives in this script rather than in a committed .conf file, for
# the same reason the plan is not duplicated across branches: every other
# vhost on this host exists only on the server, and a second copy in the
# repository is a second source of truth that drifts. This script is the
# reproducible artifact — if the server is ever rebuilt, run it.
#
# This script does NOT touch the API — not its container, not its vhost, not
# its port. A frontend release and a backend release share nothing.
#
# ⚠️ The vhost is written as the invoking user and moved into place with
# `install`. Never write a root-owned file through a `sudo` pipe: if a
# password is ever on stdin, any command in the pipeline can swallow it into
# the output, which is exactly how a credential once ended up inside
# /etc/nginx/sites-available/resibo. See resibo-plan.md §8.1.

ENVIRONMENT="${1:-live}"
WWW_ROOT="/var/www/resibo"
TARGET="${WWW_ROOT}/${ENVIRONMENT}"
VHOST_NAME="resibo-web"
VHOST_DEST="/etc/nginx/sites-available/${VHOST_NAME}"
SERVER_NAME="resibo.polbinsenthabyer.uk"
[[ "$ENVIRONMENT" == "prelive" ]] && SERVER_NAME="prelive-resibo.polbinsenthabyer.uk"

# Who should own the served files. The deploy runs as this account, and it is
# the same arrangement gainsoclock uses (/var/www/gainsoclock/live is owned by
# `deploy`, while its parent stays root-owned).
OWNER="${DEPLOY_USER:-${SUDO_USER:-$(id -un)}}"

if [[ $EUID -ne 0 ]]; then
  echo "Run with sudo: sudo -E bash scripts/setup-client-server.sh" >&2
  exit 1
fi

# --- the directory nginx serves ---------------------------------------------
# Parent stays root-owned; only the environment directory is writable by the
# deploying account, so a compromised deploy cannot rewrite a sibling.
install -d -o root -g root -m 755 "$WWW_ROOT"
install -d -o "$OWNER" -g "$OWNER" -m 755 "$TARGET"
# Where deploy-client.sh keeps the previous release for rollback. It has to be
# writable by the deploying account, and it sits outside the vhost's root so
# nginx never serves it.
install -d -o "$OWNER" -g "$OWNER" -m 755 "${WWW_ROOT}/.previous"
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
# Staged in a temp file first, then installed. Not piped into tee under sudo.
STAGED="$(mktemp)"
trap 'rm -f "$STAGED"' EXIT

cat > "$STAGED" <<VHOST
# Resibo React client. Managed by scripts/setup-client-server.sh — edit it
# there, not here, or the next run will overwrite your change.
#
# listen 127.0.0.1:80 — loopback only, exactly like the API vhost. The only
# thing that reaches nginx is the local cloudflared process, so the Cloudflare
# tunnel is the single ingress by construction rather than by policy. Nothing
# Resibo runs is reachable from the LAN.
#
# Which app answers is decided here by server_name, not by the tunnel: every
# ingress rule on the personal-projects tunnel points at http://localhost:80.

server {
    listen 127.0.0.1:80;
    server_name ${SERVER_NAME};

    root ${TARGET};
    index index.html;

    # Single-page app: React Router owns the path, so any URL that is not a
    # real file has to return index.html rather than a 404. Without this,
    # refreshing on any route but "/" gives nginx's 404 page.
    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # Vite fingerprints every file under /assets/ with a content hash, so a
    # given URL's content never changes — safe to cache hard. index.html is
    # deliberately NOT cached, so a deploy is visible on the next request.
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        access_log off;
    }

    location = /index.html {
        add_header Cache-Control "no-cache";
    }

    # The client calls the API at its own public hostname
    # (resibo-api.polbinsenthabyer.uk), so there is no /api proxy here and the
    # two vhosts stay independent.
}
VHOST

install -o root -g root -m 644 "$STAGED" "$VHOST_DEST"
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
# `systemctl reload` returns before the new workers are necessarily serving,
# so a single immediate request can still be answered by an old worker that
# has never heard of this vhost — which shows up as the default server's
# response (a 401 from one of the APIs) rather than a 404. Retry briefly
# instead of reporting a failure that is really a race.
served=false
for _ in 1 2 3 4 5; do
  if curl -fsS --max-time 5 -o /dev/null -H "Host: ${SERVER_NAME}" http://127.0.0.1/; then
    served=true
    break
  fi
  sleep 1
done

if [[ "$served" == true ]]; then
  echo "==> Serving ${SERVER_NAME} from $TARGET"
else
  echo "WARNING: nginx did not answer for that Host header after 5 attempts" >&2
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
  - Cloudflare: add the hostname to the personal-projects tunnel ingress
    (APPEND before the http_status:404 catch-all — a whole-config PUT
    carrying only this rule silently drops every neighbouring service) and
    create the proxied CNAME.
  - Deploy the client: scripts/deploy-client.sh <dist> live
DONE
