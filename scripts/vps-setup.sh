#!/usr/bin/env bash
# One-time Ubuntu VPS bootstrap for RankWise.
# Run as root: bash scripts/vps-setup.sh
set -euo pipefail

echo "==> Installing Docker..."
apt-get update
apt-get install -y ca-certificates curl git ufw
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${VERSION_CODENAME}") stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

echo "==> Firewall (SSH + HTTP)..."
ufw allow OpenSSH
ufw allow 80/tcp
ufw --force enable

echo "==> Done."
echo "Next steps:"
echo "  1. git clone <your-repo> /opt/rankwise && cd /opt/rankwise"
echo "  2. cp .env.example .env && nano .env"
echo "  3. docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build"
echo "  4. Point rankwise.co.in A record to this server's IP in Cloudflare"
