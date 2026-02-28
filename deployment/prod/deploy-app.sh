#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID} -ne 0 ]]; then
  echo "Run as root (sudo)." >&2
  exit 1
fi

REPO_URL=${REPO_URL:-}
BRANCH=${BRANCH:-main}
APP_USER=${APP_USER:-rpg}
APP_DIR=${APP_DIR:-/opt/rpg}

if [[ -z "${REPO_URL}" ]]; then
  echo "Set REPO_URL environment variable." >&2
  echo "Example: REPO_URL=https://github.com/you/project.git sudo bash deployment/prod/deploy-app.sh" >&2
  exit 1
fi

timestamp="$(date +%Y%m%d%H%M%S)"
release_dir="${APP_DIR}/releases/${timestamp}"
current_link="${APP_DIR}/current"

mkdir -p "${release_dir}"
git clone --depth 1 --branch "${BRANCH}" "${REPO_URL}" "${release_dir}"

cd "${release_dir}"

mkdir -p backend/out
mapfile -t sources < <(find backend/src/main/java -name '*.java' -type f)
if [[ ${#sources[@]} -eq 0 ]]; then
  echo "No Java source files found." >&2
  exit 1
fi

javac -encoding UTF-8 -d backend/out "${sources[@]}"

ln -sfn "${release_dir}" "${current_link}"
chown -h "${APP_USER}:${APP_USER}" "${current_link}"
chown -R "${APP_USER}:${APP_USER}" "${release_dir}"

if [[ ! -f "${APP_DIR}/shared/backend.env" ]]; then
  install -m 0644 -o "${APP_USER}" -g "${APP_USER}" \
    "${release_dir}/deployment/prod/env/backend.env.example" \
    "${APP_DIR}/shared/backend.env"
fi

systemctl restart rpg-backend.service
systemctl reload nginx

echo "Deploy completed: ${release_dir}"
echo "Service status:"
systemctl --no-pager --full status rpg-backend.service || true
