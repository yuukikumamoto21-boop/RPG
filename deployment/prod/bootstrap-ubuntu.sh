#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID} -ne 0 ]]; then
  echo "Run as root (sudo)." >&2
  exit 1
fi

DOMAIN=${DOMAIN:-}
EMAIL=${EMAIL:-}
APP_USER=${APP_USER:-rpg}
APP_DIR=${APP_DIR:-/opt/rpg}

if [[ -z "${DOMAIN}" || -z "${EMAIL}" ]]; then
  echo "Set DOMAIN and EMAIL environment variables." >&2
  echo "Example: DOMAIN=rpg.example.com EMAIL=admin@example.com sudo bash deployment/prod/bootstrap-ubuntu.sh" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

apt-get update
apt-get install -y nginx certbot python3-certbot-nginx openjdk-17-jdk git ufw rsync

if ! id -u "${APP_USER}" >/dev/null 2>&1; then
  useradd --system --create-home --shell /usr/sbin/nologin "${APP_USER}"
fi

mkdir -p "${APP_DIR}/releases" "${APP_DIR}/shared"
chown -R "${APP_USER}:${APP_USER}" "${APP_DIR}"

if command -v ufw >/dev/null 2>&1; then
  ufw allow OpenSSH || true
  ufw allow 'Nginx Full' || true
  ufw --force enable || true
fi

NGINX_TEMPLATE="${PROJECT_ROOT}/deployment/nginx/rpg.prod.conf.template"
NGINX_TARGET="/etc/nginx/sites-available/rpg.conf"

sed \
  -e "s|__DOMAIN__|${DOMAIN}|g" \
  -e "s|__APP_DIR__|${APP_DIR}|g" \
  "${NGINX_TEMPLATE}" > "${NGINX_TARGET}"

ln -sf "${NGINX_TARGET}" /etc/nginx/sites-enabled/rpg.conf
rm -f /etc/nginx/sites-enabled/default

nginx -t
systemctl reload nginx

SYSTEMD_TEMPLATE="${PROJECT_ROOT}/deployment/systemd/rpg-backend.service.template"
SYSTEMD_TARGET="/etc/systemd/system/rpg-backend.service"

sed \
  -e "s|__APP_USER__|${APP_USER}|g" \
  -e "s|__APP_DIR__|${APP_DIR}|g" \
  "${SYSTEMD_TEMPLATE}" > "${SYSTEMD_TARGET}"

systemctl daemon-reload
systemctl enable rpg-backend.service

if [[ ! -f "${APP_DIR}/shared/backend.env" ]]; then
  install -m 0644 -o "${APP_USER}" -g "${APP_USER}" \
    "${PROJECT_ROOT}/deployment/prod/env/backend.env.example" \
    "${APP_DIR}/shared/backend.env"
fi

certbot --nginx \
  -d "${DOMAIN}" \
  --non-interactive \
  --agree-tos \
  -m "${EMAIL}" \
  --redirect

systemctl reload nginx

echo "Bootstrap completed. Next: deploy app code with deployment/prod/deploy-app.sh"
