#!/usr/bin/env bash
# Backup diario de Postgres y de las fotos de cuadernos (volumen backend-datos).
# Uso manual:  ./backup.sh
# Cron diario (3:15):  15 3 * * * /home/gonza/Ensambla/deploy/backup.sh >> /home/gonza/backups/backup.log 2>&1
# Los backups quedan en ~/backups; se conservan DIAS_A_CONSERVAR días. Copiarlos además fuera del VPS.
set -euo pipefail

DESTINO="${DESTINO:-$HOME/backups}"
DIAS_A_CONSERVAR="${DIAS_A_CONSERVAR:-14}"
cd "$(dirname "$0")"

# Credenciales de la base: las mismas del .env
set -a
. ./.env
set +a

mkdir -p "$DESTINO"
sello="$(date +%Y%m%d-%H%M%S)"

docker compose exec -T postgres \
  pg_dump -U "${POSTGRES_USER:-ensambla_user}" -d "${POSTGRES_DB:-ensambla}" --no-owner \
  | gzip > "$DESTINO/db-$sello.sql.gz"

docker run --rm -v ensambla-prod_backend-datos:/datos:ro -v "$DESTINO":/destino alpine \
  tar czf "/destino/datos-$sello.tar.gz" -C /datos .

find "$DESTINO" -type f \( -name 'db-*.sql.gz' -o -name 'datos-*.tar.gz' \) -mtime +"$DIAS_A_CONSERVAR" -delete

echo "$(date -Is) backup ok: db-$sello.sql.gz, datos-$sello.tar.gz"
