# Despliegue (VPS Contabo)

Todo corre con Docker Compose en el VPS: Caddy (HTTPS, frontend y proxy `/api`), backend y Postgres. Evolution API
queda apagada hasta activar el perfil (`COMPOSE_PROFILES=whatsapp` en el `.env`; ver `docs/whatsapp-evolution.md`).

## CI/CD

Un push a `main` (que toque `backend/`, `frontend/` o `deploy/`) dispara `.github/workflows/deploy-main.yml`:
build y tests del backend y del frontend, y después `deploy`, que entra por SSH al VPS y ejecuta `deploy.sh` con el SHA
del commit. El script trae ese commit, corre `docker compose up -d --build` y espera a que el backend responda.

Secretos del entorno `production` en GitHub: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY` (clave privada de despliegue) y
`VPS_KNOWN_HOSTS` (salida de `ssh-keyscan -t ed25519 <ip>`).

La clave pública de despliegue va en `~/.ssh/authorized_keys` del VPS con restricciones, para que sólo pueda correr el script:

```text
command="bash /home/gonza/Ensambla/deploy/deploy.sh",no-port-forwarding,no-X11-forwarding,no-agent-forwarding,no-pty ssh-ed25519 AAAA... github-actions-deploy
```

## Qué versión está corriendo

Cada contenedor de la app lleva la etiqueta `ensambla.commit` con el SHA desplegado:

```text
cd ~/Ensambla && git fetch -q && echo "origin/main: $(git rev-parse origin/main)" && docker inspect -f '{{.Name}}: {{index .Config.Labels "ensambla.commit"}} (creado {{.Created}})' ensambla-backend ensambla-web && tail -3 ~/deploy.log
```

Si el SHA de los contenedores coincide con el de `origin/main`, el último push a `main` ya está desplegado.

Un deploy a mano: `bash ~/Ensambla/deploy/deploy.sh` (último commit de `origin/main`). No usar `docker compose up` suelto:
sin `GIT_SHA` la etiqueta queda en `desconocido`.

## Backups y base de datos

`backup.sh` hace el backup diario (cron). Postgres escucha sólo en `127.0.0.1:5432` del VPS: se entra con túnel SSH.
