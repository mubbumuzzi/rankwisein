#!/usr/bin/env bash
# Redeploy RankWise on the VPS (run from repo root on the server).
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> Pulling latest..."
git pull --ff-only

echo "==> Rebuilding and restarting..."
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build

echo "==> Status:"
docker compose ps

echo "==> Health:"
curl -sf http://localhost/actuator/health || curl -sf http://localhost/api/meta | head -c 80
echo ""
echo "Deploy complete."
