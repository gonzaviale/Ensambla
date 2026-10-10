# Ensambla

> **Tu sistema, un bloque a la vez.**

Ensambla es una aplicación web **no-code** para que emprendedores, comercios y PyMEs armen el sistema interno de su
negocio —inventario, pedidos, clientes— sin programar: eligen una **plantilla** (o empiezan en blanco), agregan
**bloques** con **campos** tipados y cargan **registros** con formularios y tablas generados automáticamente.

Trabajo Práctico Integrador · UNLaM · Tecnicatura en Desarrollo de Aplicaciones Web/Móviles · 2026.

## Estado

Estamos en el **MVP 2** (el núcleo vendible). Esta base deja armadas la estructura, la pila tecnológica, el modelo de
datos y los pipelines para que el equipo arranque con las tareas `T-001` … `T-038` del backlog. Ver
[docs/backlog.md](docs/backlog.md).

## Estructura

| Carpeta | Contenido |
|---|---|
| [`backend/`](backend/README.md) | API REST: Spring Boot 4, Java 21, PostgreSQL 17, Flyway, sesión por cookie + CSRF |
| [`frontend/`](frontend/README.md) | SPA: Vite, React 19, TypeScript, TanStack Router/Query/Form, Tailwind 4, zod, MSW |
| [`docs/`](docs) | Modelo de datos, backlog del MVP 2 y guía de marca |
| `.claude/skills` | Skills para trabajar con Claude Code: `crear-use-case`, `escribir-tests`, `refactor-use-case` |
| `.github/` | CI (backend y frontend), deploy, plantilla de PR y guía de contribución |

## Cómo levantarlo

Requisitos: JDK 21, Node 22+, Docker.

```bash
# 1. Base de datos
cd backend
docker compose up -d postgres

# 2. Backend (http://localhost:8080, Swagger en /swagger-ui.html)
./mvnw spring-boot:run

# 3. Frontend (http://localhost:5173), en otra terminal
cd frontend
cp .env.example .env
npm install
npm run dev
```

Registro, login, logout y sesión funcionan contra el backend real. `.env.example` usa
`VITE_API_MOCKS=ninguno`; `completo` queda disponible para tests y demos simuladas.
El registro crea la cuenta y el negocio, y el formulario inicia sesión automáticamente.
Detalle y criterios verificados: [docs/acceso-sesion.md](docs/acceso-sesion.md).

## Calidad

| Qué | Comando |
|---|---|
| Backend: build + tests + 100 % de cobertura por clase | `cd backend && ./mvnw verify` (necesita Docker) |
| Frontend: lint, tipos, tests, build | `cd frontend && npm run lint && npm run typecheck && npm test && npm run build` |

## Contribuir

Ramas `tipo/descripcion-corta`, Conventional Commits, PR a `dev` con 1 aprobación y *Squash and merge*.
Detalle en [.github/CONTRIBUTING.md](.github/CONTRIBUTING.md). Flujo de ramas: `main` ← `stg` ← `dev` ← feature.
