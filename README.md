# Ensambla

> **Tu sistema, un bloque a la vez.**

**Ensambla es el asistente del comerciante de barrio.** Mira lo que ya anota (el cuaderno, las fotos, WhatsApp) y le dice
qué hacer hoy: **reponer, avisar, promocionar**. Y le deja el trabajo listo: el pedido al proveedor, el mensaje, la
publicación. No es una plataforma de gestión: no lleva contabilidad, costos ni impuestos.

La definición completa, lo que no es y la regla para aceptar funciones nuevas están en
[docs/que-es-ensambla.md](docs/que-es-ensambla.md).

Trabajo Práctico Integrador · UNLaM · Tecnicatura en Desarrollo de Aplicaciones Web/Móviles · 2026.

## Qué hace

| Verbo | Funciones |
|---|---|
| **Cargar** | Foto del cuaderno leída con IA (con revisión antes de guardar) · Sami arma el sistema conversando · carga por WhatsApp · plantillas |
| **Avisar** | Avisos de Sami (stock, precio frente a la zona, productos parados, combos) · Mi zona · resumen de la mañana por WhatsApp |
| **Reponer** | Aviso de stock · pedido a proveedor en PDF |
| **Promocionar** | Publicaciones y lista de precios armadas por Sami para compartir |

## Estructura

| Carpeta | Contenido |
|---|---|
| [`backend/`](backend/README.md) | API REST: Spring Boot 4, Java 21, PostgreSQL 17, Flyway, sesión por cookie + CSRF |
| [`frontend/`](frontend/README.md) | SPA: Vite, React 19, TypeScript, TanStack Router/Query/Form, Tailwind 4, zod, MSW |
| [`docs/`](docs) | Definición del producto, modelo de datos, reglas de avisos, WhatsApp, Sami y guía de marca |
| `.claude/skills` y `.agents/skills` | Skills para trabajar con Claude Code y Codex (misma fuente): reglas de arquitectura, crear caso de uso, escribir tests y refactor |
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

`.env.example` usa `VITE_API_MOCKS=ninguno`; `completo` queda disponible para tests y demos simuladas. WhatsApp e IA
arrancan en modo `simulado` (no envían nada ni gastan): ver [docs/whatsapp-evolution.md](docs/whatsapp-evolution.md) y
[docs/sami-asistente.md](docs/sami-asistente.md) para conectarlos de verdad.

## Cómo se escribe el código

Las reglas del backend están en [backend/ARQUITECTURA.md](backend/ARQUITECTURA.md). Las dos que más importan al sumar
código: **las variantes por tipo se resuelven con polimorfismo** (una clase por variante, nunca un `switch`) y **los
métodos se explican solos** (una lista corta de pasos y todo el detalle en métodos privados). Los tests siguen la misma
forma. `ArchitectureTest` rechaza un `switch` nuevo.

## Calidad

| Qué | Comando |
|---|---|
| Backend: build + tests + 100 % de cobertura por clase | `cd backend && ./mvnw verify` (necesita Docker) |
| Frontend: lint, tipos, tests, build | `cd frontend && npm run lint && npm run typecheck && npm test && npm run build` |

## Contribuir

Ramas `tipo/descripcion-corta`, Conventional Commits, PR a `dev` con 1 aprobación y *Squash and merge*.
Detalle en [.github/CONTRIBUTING.md](.github/CONTRIBUTING.md). Flujo de ramas: `main` ← `stg` ← `dev` ← feature.
