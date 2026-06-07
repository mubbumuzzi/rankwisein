# Deploy RankWise on a VPS (24/7, no laptop)

Your current setup uses **Docker on your laptop** + **Cloudflare Tunnel**. When the laptop sleeps, the site goes down.

This guide moves the same stack to a **small cloud server** that stays on. You keep **rankwise.co.in** on Cloudflare — only the origin changes from tunnel → VPS IP.

## Recommended providers

| Provider | Plan | RAM | Approx. cost | Notes |
|----------|------|-----|--------------|-------|
| [Hetzner](https://www.hetzner.com/cloud) | CX22 | 4 GB | ~€4/mo | Good value, EU/US regions |
| [DigitalOcean](https://www.digitalocean.com/) | Basic 4 GB | 4 GB | ~$24/mo | Simple UI |
| [AWS Lightsail](https://aws.amazon.com/lightsail/) | 4 GB | 4 GB | ~$24/mo | Mumbai region for India |

**Minimum:** 4 GB RAM (SQL Server + backend + frontend). 2 GB is too tight.

## One-time VPS setup

1. Create an Ubuntu 24.04 VPS and note its **public IP**.
2. SSH in as root (or sudo user):

```bash
ssh root@YOUR_VPS_IP
```

3. Run the bootstrap script from your repo (after cloning), or paste commands from `scripts/vps-setup.sh`.

4. Clone the repo:

```bash
git clone https://github.com/YOUR_USER/rankWise.git /opt/rankwise
cd /opt/rankwise
cp .env.example .env
nano .env   # set strong passwords, JWT_SECRET, CURSOR_API_KEY, admin password
```

5. First deploy:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

6. Open `http://YOUR_VPS_IP` — you should see RankWise.

## Cloudflare DNS (replace tunnel)

In [Cloudflare Dashboard](https://dash.cloudflare.com) → **rankwise.co.in** → **DNS**:

1. **Remove** or disable the tunnel CNAME (if any points to `*.cfargotunnel.com`).
2. Add **A** record: `@` → `YOUR_VPS_IP` (Proxied / orange cloud ON).
3. Add **A** or **CNAME**: `www` → `YOUR_VPS_IP` or `rankwise.co.in` (Proxied ON).
4. **SSL/TLS** → set mode to **Flexible** (quick start: Cloudflare HTTPS → VPS HTTP on port 80).

Stop `cloudflared tunnel run` on your laptop — you no longer need it.

### Optional: Full (strict) SSL

Generate a **Cloudflare Origin Certificate**, install on nginx, set SSL mode to **Full (strict)**. See Cloudflare docs → SSL → Origin Server.

## Updates (after code changes)

On the VPS:

```bash
cd /opt/rankwise
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Or use GitHub Actions (see below).

## GitHub Actions auto-deploy (optional)

1. Add repository secrets:
   - `VPS_HOST` — public IP or hostname
   - `VPS_USER` — e.g. `root` or `deploy`
   - `VPS_SSH_KEY` — private key for SSH

2. Push to `main` — workflow in `.github/workflows/deploy-vps.yml` pulls and rebuilds on the VPS.

## Firewall

```bash
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp   # if you add HTTPS on the VPS later
ufw enable
```

Do **not** expose SQL Server (1433) or backend (8080) publicly — prod compose keeps them internal.

## Backups

Important data:

- Docker volume `sqldata` — database
- Docker volume `imports` — uploaded PDFs

Example backup:

```bash
docker run --rm -v rankwise_sqldata:/data -v $(pwd):/backup alpine tar czf /backup/sqldata-backup.tar.gz /data
```

Schedule weekly backups (cron + object storage) before relying on production traffic.

## Troubleshooting

| Issue | Check |
|-------|--------|
| Site unreachable | `docker ps`, `ufw status`, Cloudflare A record IP |
| 502 / timeout | `docker logs rankwise-backend`, SQL Server health |
| Admin import 524 | Already fixed with async approve; ensure latest backend is deployed |
| CORS errors | `CORS_ALLOWED_ORIGINS` in `.env` includes your domain |

## Other options (not implemented in-repo)

- **Managed Postgres + Render/Railway** — requires migrating off SQL Server (Flyway scripts are MSSQL-specific today).
- **Azure VM + Azure SQL** — good if you want managed DB; more setup than single VPS + Docker.

For RankWise today, **VPS + Docker Compose** is the smallest change from what you already run locally.
