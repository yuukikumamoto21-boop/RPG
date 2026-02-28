#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID} -ne 0 ]]; then
  echo "Run as root (sudo)." >&2
  exit 1
fi

APP_USER=${APP_USER:-rpg}
APP_DIR=${APP_DIR:-/opt/rpg}

mapfile -t releases < <(find "${APP_DIR}/releases" -mindepth 1 -maxdepth 1 -type d | sort)
if [[ ${#releases[@]} -lt 2 ]]; then
  echo "Need at least 2 releases to rollback." >&2
  exit 1
fi

target="${releases[$(( ${#releases[@]} - 2 ))]}"
ln -sfn "${target}" "${APP_DIR}/current"
chown -h "${APP_USER}:${APP_USER}" "${APP_DIR}/current"

systemctl restart rpg-backend.service
systemctl reload nginx

echo "Rolled back to: ${target}"
