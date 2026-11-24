#!/usr/bin/env bash
set -euo pipefail

# One-time (and safely repeatable) server bootstrap for Resibo.
#
# Creates the deployment directory, the env file, the nginx vhost, and the
# self-hosted GitHub Actions runner. Everything here is idempotent: running it
# twice changes nothing the second time.
#
#   sudo bash scripts/setup-server.sh
#
# The runner step is skipped unless RUNNER_TOKEN is supplied, because the
# registration token GitHub issues is short-lived (about an hour) and must be
# fetched fresh from:
#   Repo → Settings → Actions → Runners → New self-hosted runner
#
#   sudo RUNNER_TOKEN=XXXX bash scripts/setup-server.sh
#
# No credential is written into this script or into the repository.

APP_DIR="${APP_DIR:-/opt/resibo}"
APP_USER="${APP_USER:-${SUDO_USER:-$USER}}"
HOST_PORT="${HOST_PORT:-8083}"
SERVER_NAME="${SERVER_NAME:-resibo-api.polbinsenthabyer.uk}"
REPO_URL="${REPO_URL:-https://github.com/polbinsenthabyer/resibo.git}"
BRANCH="${BRANCH:-dev/api/resibo}"
RUNNER_DIR="${RUNNER_DIR:-$APP_DIR/actions-runner}"
RUNNER_LABELS="${RUNNER_LABELS:-self-hosted,resibo-deploy}"
RUNNER_VERSION="${RUNNER_VERSION:-2.338.2}"

if [[ $EUID -ne 0 ]]; then
  echo "Run with sudo: sudo bash scripts/setup-server.sh" >&2
  exit 1
fi

say() { printf '\n==> %s\n' "$*"; }

# --- deployment directory ---------------------------------------------------
say "Deployment directory $APP_DIR"
install -d -o "$APP_USER" -g "$APP_USER" -m 0755 "$APP_DIR"

# --- env file ---------------------------------------------------------------
# Created empty-but-valid if absent; never overwritten, so a real env file is
# safe from a re-run.
if [[ -f "$APP_DIR/resibo.env" ]]; then
  echo "    resibo.env already exists — left untouched"
else
  say "Creating $APP_DIR/resibo.env"
  cat > "$APP_DIR/resibo.env" <<'ENVEOF'
# Resibo server environment. NOT in git. Values only.
# SERVER_PORT is forced to 8080 inside the container by docker-compose.yml;
# the published host port is set by HOST_PORT at deploy time.
APP_ENV=prod
ENVEOF
  chown "$APP_USER":"$APP_USER" "$APP_DIR/resibo.env"
  chmod 600 "$APP_DIR/resibo.env"
fi

# --- source checkout --------------------------------------------------------
if [[ -d "$APP_DIR/app/.git" ]]; then
  say "Updating checkout at $APP_DIR/app"
  sudo -u "$APP_USER" git -C "$APP_DIR/app" fetch --quiet origin "$BRANCH"
  sudo -u "$APP_USER" git -C "$APP_DIR/app" checkout --quiet "$BRANCH"
  sudo -u "$APP_USER" git -C "$APP_DIR/app" reset --hard --quiet "origin/$BRANCH"
else
  say "Cloning $REPO_URL ($BRANCH)"
  sudo -u "$APP_USER" git clone --quiet --branch "$BRANCH" "$REPO_URL" "$APP_DIR/app"
fi
sudo -u "$APP_USER" git -C "$APP_DIR/app" log --oneline -1

# --- docker access ----------------------------------------------------------
say "Docker group membership for $APP_USER"
if id -nG "$APP_USER" | tr ' ' '\n' | grep -qx docker; then
  echo "    already in the docker group"
else
  usermod -aG docker "$APP_USER"
  echo "    added — takes effect on the next login or service restart"
fi

# --- nginx vhost ------------------------------------------------------------
# Written to a temp file and installed, never piped through sudo: a pipe would
# put whatever is on stdin into the file.
say "nginx vhost for $SERVER_NAME"
tmp_vhost="$(mktemp)"
cat > "$tmp_vhost" <<NGINXEOF
server {
	listen 127.0.0.1:80;
	server_name ${SERVER_NAME};

	location / {
		proxy_pass http://127.0.0.1:${HOST_PORT};
		proxy_set_header Host \$host;
	}
}
NGINXEOF
install -m 0644 -o root -g root "$tmp_vhost" /etc/nginx/sites-available/resibo
rm -f "$tmp_vhost"
ln -sfn /etc/nginx/sites-available/resibo /etc/nginx/sites-enabled/resibo

if nginx -t; then
  systemctl reload nginx
  echo "    nginx reloaded"
else
  echo "nginx config test failed — not reloading" >&2
  exit 1
fi

# --- GitHub Actions self-hosted runner --------------------------------------
# The server is on a private network, so GitHub-hosted runners cannot reach
# it. A self-hosted runner is the only way CI can deploy here.
if [[ -z "${RUNNER_TOKEN:-}" ]]; then
  say "Skipping runner setup (no RUNNER_TOKEN given)"
  echo "    To install it, get a fresh token from:"
  echo "      Repo → Settings → Actions → Runners → New self-hosted runner"
  echo "    then re-run: sudo RUNNER_TOKEN=XXXX bash scripts/setup-server.sh"
elif [[ -f "$RUNNER_DIR/.runner" ]]; then
  say "Runner already configured at $RUNNER_DIR — left untouched"
else
  say "Installing GitHub Actions runner $RUNNER_VERSION"
  install -d -o "$APP_USER" -g "$APP_USER" -m 0755 "$RUNNER_DIR"
  tarball="actions-runner-linux-x64-${RUNNER_VERSION}.tar.gz"
  sudo -u "$APP_USER" curl -fsSL -o "$RUNNER_DIR/$tarball" \
    "https://github.com/actions/runner/releases/download/v${RUNNER_VERSION}/${tarball}"
  sudo -u "$APP_USER" tar xzf "$RUNNER_DIR/$tarball" -C "$RUNNER_DIR"
  rm -f "$RUNNER_DIR/$tarball"

  sudo -u "$APP_USER" "$RUNNER_DIR/config.sh" \
    --unattended --replace \
    --url "${REPO_URL%.git}" \
    --token "$RUNNER_TOKEN" \
    --name "resibo-deploy" \
    --labels "$RUNNER_LABELS" \
    --work "_work"

  "$RUNNER_DIR/svc.sh" install "$APP_USER"
  "$RUNNER_DIR/svc.sh" start
  "$RUNNER_DIR/svc.sh" status | head -5
fi

say "Done"
echo "Next: push to $BRANCH, or deploy by hand with"
echo "  cd $APP_DIR/app && sudo bash scripts/deploy.sh"
