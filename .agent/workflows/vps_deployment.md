---
description: VPS Initialization and Deployment Runbook
---

# VPS Deployment and Initialization Runbook

This workflow describes the exact prerequisites and commands required to set up a raw Linux VPS (Ubuntu/Debian) to be production-ready for the Beet application deployment. **This must be executed manually once before the CI/CD pipeline runs.**

## 1. Initial System Setup & Hardening

Run these commands as `root` on the fresh VPS:

```bash
# Update system packages
apt-get update && apt-get upgrade -y

# Install prerequisite tools
apt-get install -y curl ufw fail2ban
```

### UFW (Firewall) Configuration
We block everything by default and only open HTTP/HTTPS for Caddy and SSH for administration.

```bash
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp  # SSH
ufw allow 80/tcp  # HTTP
ufw allow 443/tcp # HTTPS
# Optional: ufw allow 443/udp # HTTPS HTTP/3
ufw --force enable
```

### Fail2ban (Anti-brute force)
```bash
systemctl enable fail2ban
systemctl start fail2ban
```

## 2. Install Docker & Docker Compose

```bash
# Install Docker Engine
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Ensure Docker starts on boot
systemctl enable docker
systemctl start docker
```

## 3. Environment Directories & Secure .env Setup

The CI/CD pipeline expects specific directories to exist for each environment (`dev`, `qa`, `prod`).

```bash
mkdir -p /opt/beet/dev
mkdir -p /opt/beet/qa
mkdir -p /opt/beet/prod

# Create secure .env file for DEV
cat << 'EOF' > /opt/beet/dev/.env
# --- DEV ENVIRONMENT VARIABLES ---
DB_USER=beet_admin
DB_PASSWORD=YOUR_SECURE_PASSWORD_DEV
JWT_SECRET=GENERATE_A_LONG_SECRET_KEY_DEV
EOF
chmod 600 /opt/beet/dev/.env

# Create secure .env file for QA
cat << 'EOF' > /opt/beet/qa/.env
# --- QA ENVIRONMENT VARIABLES ---
DB_USER=beet_admin
DB_PASSWORD=YOUR_SECURE_PASSWORD_QA
JWT_SECRET=GENERATE_A_LONG_SECRET_KEY_QA
EOF
chmod 600 /opt/beet/qa/.env

# Create secure .env file for PROD
cat << 'EOF' > /opt/beet/prod/.env
# --- PROD ENVIRONMENT VARIABLES ---
DB_USER=beet_admin
DB_PASSWORD=YOUR_SECURE_PASSWORD_PROD
JWT_SECRET=GENERATE_A_LONG_SECRET_KEY_PROD
EOF
chmod 600 /opt/beet/prod/.env
```

## 4. SSH Key Setup for GitHub Actions

GitHub Actions needs an SSH key to log into this VPS and deploy the code.

```bash
# Generate a new SSH key pair ONLY for deployment (no passphrase)
ssh-keygen -t ed25519 -C "github-actions-deploy" -f /root/.ssh/github_actions_key -N ""

# Add the public key to authorized_keys so GitHub can log in
cat /root/.ssh/github_actions_key.pub >> /root/.ssh/authorized_keys

# Get the Private Key to paste into GitHub Secrets (VPS_SSH_KEY)
# -> Copy the output of this command and save it in your GitHub Repo Settings
cat /root/.ssh/github_actions_key
```

## 5. Agent Diagnosis Commands (For the AI to use later)

If the user asks you (the Agent) to debug a production issue on the VPS, use the following commands via `run_command` over SSH or if the user pastes the terminal output:

- **Check Container Status:** `docker compose -f /opt/beet/prod/docker-compose.yml -f /opt/beet/prod/docker-compose.prod.yml ps -a`
- **Check Backend Logs (Last 100 lines):** `docker logs --tail 100 beet-backend`
- **Check Caddy Proxy Logs:** `docker logs --tail 100 caddy`
- **Inspect DB Container Health:** `docker inspect --format='{{json .State.Health}}' beet-db`
