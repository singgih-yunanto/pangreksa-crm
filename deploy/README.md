# Pangreksa CRM — dev deployment

Single-branch (`main`) deployment with **auto-routing** between two possible machines:

- **Dedicated runner** — a self-hosted GitHub Actions runner on a *separate* host. Builds
  both apps, then ships artifacts to the dev server over **SSH/rsync** and restarts the
  services there.
- **Dev server** — the host that actually runs the apps (backend jar + Next.js server behind
  nginx). It can *also* run a self-hosted runner; when the job lands here it deploys
  **locally** (no SSH).

Both machines carry the shared label **`deploy`**. The single job in
`.github/workflows/deploy-dev.yml` is scheduled on whichever machine is **online**, giving
automatic fallback: prefer the dedicated runner, fall back to the server itself when the
runner is offline.

> Note: when *both* machines are online, GitHub picks an available one arbitrarily — there's
> no native "runner-first" preference. Fallback-to-online is guaranteed; strict preference is
> not.

## How a machine knows which mode to use

A marker file exists **only on the dev server**: `/opt/pangreksa-crm/.local-deploy`.
The `Detect deploy mode` step checks it — present → **local** deploy, absent → **remote**
(SSH) deploy.

```
push to main ─▶ job scheduled on an ONLINE [self-hosted, Linux, deploy] runner
                 ├─ build backend  → crm-be-*.jar
                 ├─ build frontend → .next   (NEXT_PUBLIC_* baked from frontend.env)
                 └─ detect marker:
                     ├─ absent  → REMOTE: rsync-over-SSH to dev server + restart scripts
                     └─ present → LOCAL : cp/rsync in place + restart scripts (no SSH)

                                       nginx (:80) ──/api/*──▶ backend  127.0.0.1:8080
                                                   ──/*──────▶ frontend 127.0.0.1:3000
```

Why the frontend runs as a process (not static files): the Next.js App Router app has
runtime-dynamic routes (`/deals/<id>`, etc.), so it can't be a static export. nginx
reverse-proxies to `next start`; everything is same-origin, so there's no CORS.

Why `npm ci` runs on the dev server (both paths): only the build output (`.next`) is
deployed, not `node_modules`. Next's native binaries are platform-specific, so runtime deps
are installed on the host that runs the app to match its OS/arch.

## Files
| Path | Purpose |
|---|---|
| `.github/workflows/deploy-dev.yml` | Auto-routing CI/CD pipeline (push to `main`) |
| `deploy/pangreksa-be.sh` | start/stop/restart/status the backend jar |
| `deploy/pangreksa-fe.sh` | start/stop/restart/status the Next.js server |
| `deploy/nginx/pangreksa.conf` | nginx reverse-proxy vhost |
| `deploy/application.yaml.example` | template for the backend's external config |
| `deploy/frontend.env.example` | template for the frontend's build-time env |

The control scripts (`pangreksa-be.sh` / `pangreksa-fe.sh`) are installed **manually** into
`$BIN_DIR` (`/opt/pangreksa-crm/bin`) on each machine — the workflow does not ship them.

## GitHub secrets / variables
Set under **Settings → Secrets and variables → Actions** (used by the remote path):

| Name | Kind | Value |
|---|---|---|
| `DEV_SSH_HOST` | secret | Dev server hostname / IP |
| `DEV_SSH_USER` | secret | Deploy user on the dev server (owns `/opt/pangreksa-crm`) |
| `DEV_SSH_PORT` | variable | SSH port (optional, defaults to `22`) |

The dedicated runner must already have **passwordless SSH** to the dev server (key in the
deploy user's `~/.ssh/authorized_keys`, and the host in the runner's `~/.ssh/known_hosts`).

## Runner labels
Both runners need the shared `deploy` label so the job can land on either:

| Machine | Labels |
|---|---|
| Dedicated runner | `self-hosted, Linux, runner, deploy` |
| Dev server | `self-hosted, Linux, local, deploy` |

(JDK 25 / Node 20 are provisioned per-run by `setup-java` / `setup-node`. Ensure `git`,
`rsync`, `ssh`, `curl` are present.)

## One-time setup — every machine that runs the job
Place the **frontend build env** (not in the repo), since the build can run anywhere:
```bash
sudo mkdir -p /opt/pangreksa-crm/config
sudo cp deploy/frontend.env.example /opt/pangreksa-crm/config/frontend.env   # then edit
```
Leave `NEXT_PUBLIC_API_BASE_URL` empty for the same-origin nginx setup.

## One-time setup — DEV SERVER (also when it runs the local path)
1. Install **JDK 25, Node 20, nginx, rsync, PostgreSQL** (DB `pangreksa_crm`).
2. Create the deploy directories (owned by the deploy user) and the **local-deploy marker**:
   ```bash
   sudo mkdir -p /opt/pangreksa-crm/{backend/logs,frontend,bin,config}
   sudo chown -R "$DEPLOY_USER":"$DEPLOY_USER" /opt/pangreksa-crm
   touch /opt/pangreksa-crm/.local-deploy        # <-- marks THIS machine as the server
   ```
3. Install the control scripts:
   ```bash
   cp deploy/pangreksa-be.sh deploy/pangreksa-fe.sh /opt/pangreksa-crm/bin/
   chmod +x /opt/pangreksa-crm/bin/*.sh
   ```
4. **Backend external config** — fill in real secrets (server only; do NOT place the marker
   or this file on the dedicated runner):
   ```bash
   cp deploy/application.yaml.example /opt/pangreksa-crm/config/application.yaml
   # edit: DB password, JWT secret (openssl rand -base64 48), dev host
   ```
5. **nginx** (once, as root):
   ```bash
   sudo cp deploy/nginx/pangreksa.conf /etc/nginx/sites-available/pangreksa.conf
   sudo ln -sf /etc/nginx/sites-available/pangreksa.conf /etc/nginx/sites-enabled/pangreksa.conf
   sudo nginx -t && sudo systemctl reload nginx
   ```

### Server memory / swap (required on low-RAM servers)
A Next.js production build needs well over 2 GB of memory. On a **≤ 2 GB** server the build is
killed by the OOM killer (the step exits **137** with no message) unless swap is present. Add
~4 GB swap once, as root:
```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab   # persist across reboots
```
The workflow auto-sizes the V8 heap to ~75% of RAM, but swap is still required on a 2 GB box.
Builds on the server are slow — prefer the dedicated runner when it's online.

After that, every push to `main` builds on an online `deploy` runner and (re)deploys to the
dev server. The app processes are restarted by each deploy; for boot persistence on the dev
server, wrap `pangreksa-be.sh` / `pangreksa-fe.sh` in systemd units or add `@reboot` cron
entries.

## Manual control (run on the dev server)
```bash
/opt/pangreksa-crm/bin/pangreksa-be.sh {start|stop|restart|status}
/opt/pangreksa-crm/bin/pangreksa-fe.sh {start|stop|restart|status}
```
