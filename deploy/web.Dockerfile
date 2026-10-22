# Contexto: ../frontend (ver docker-compose.yml)

# ---- Etapa de build ----
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Backend real; la API se sirve en el mismo origen bajo /api.
ENV VITE_API_URL=/api \
    VITE_API_MOCKS=ninguno
RUN npm run build

# ---- Etapa de ejecución: Caddy sirve el build y hace proxy a /api ----
FROM caddy:2-alpine
COPY --from=build /app/dist /srv
# El Caddyfile se monta desde docker-compose.yml (queda fuera de este contexto).
