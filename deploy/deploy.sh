#!/usr/bin/env bash
# Despliegue en el VPS: trae el commit indicado, reconstruye y espera a que el backend responda.
# Lo ejecuta el pipeline (.github/workflows/deploy-main.yml) por SSH con una clave restringida a este script;
# el SHA llega en SSH_ORIGINAL_COMMAND. A mano:  bash deploy.sh [sha]   (sin sha: último commit de origin/main)
set -euo pipefail

principal() {
  local rama="${RAMA:-main}"
  local objetivo="${1:-${SSH_ORIGINAL_COMMAND:-}}"
  cd "$(dirname "$0")"
  local repo
  repo="$(git rev-parse --show-toplevel)"

  # Un deploy a la vez
  exec 9>/tmp/ensambla-deploy.lock
  flock -n 9 || { echo "Ya hay un deploy en curso"; exit 1; }

  git -C "$repo" fetch --quiet origin "$rama"
  if [ -z "$objetivo" ]; then
    objetivo="$(git -C "$repo" rev-parse "origin/$rama")"
  elif ! [[ "$objetivo" =~ ^[0-9a-f]{40}$ ]]; then
    echo "SHA inválido" >&2
    exit 1
  fi
  git -C "$repo" checkout --quiet -f -B "$rama" "$objetivo"

  GIT_SHA="$(git -C "$repo" rev-parse HEAD)"
  export GIT_SHA
  echo "Desplegando $GIT_SHA"

  docker compose up -d --build --remove-orphans

  # El backend no publica puertos: se consulta desde el contenedor web, por la red interna.
  echo "Esperando al backend..."
  for _ in $(seq 1 40); do
    if docker compose exec -T web wget -q --spider http://backend:8080/api/auth/csrf; then
      docker image prune -f > /dev/null
      echo "$(date -Is) $GIT_SHA" >> "$HOME/deploy.log"
      echo "DEPLOY_OK commit=$GIT_SHA"
      return 0
    fi
    sleep 5
  done

  echo "El backend no respondió a tiempo" >&2
  docker compose logs --tail 40 backend >&2
  exit 1
}

# El script se reescribe con el git checkout: se ejecuta todo como una sola función y se sale acá.
principal "$@"
exit
