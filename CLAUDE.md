# CLAUDE.md — Ensambla

Guía para trabajar en este repo con Claude Code (y para que cualquier persona del equipo lo levante rápido).

**Ensambla** · «Tu sistema, un bloque a la vez.» Aplicación web de gestión para comercios de barrio y emprendedores: parte del
cuaderno en papel (foto + IA) y de la lista de precios del proveedor, y **avisa por su cuenta** qué precio corregir, qué falta y
dónde conviene comprar. TP integrador UNLaM 2026.

## Levantarlo (lo más rápido: sólo el frontend con datos simulados)

Requisitos: Node 22+. No hace falta backend ni Docker para ver la app.

```bash
cd frontend
npm install
```

Modo demo (todo simulado con MSW, sin backend):

```powershell
$env:VITE_API_MOCKS = 'completo'
npm run dev            # http://localhost:5173
```

```bash
# bash / Git Bash
VITE_API_MOCKS=completo npm run dev
```

Ingresar en `/login` con `demo@ensambla.local` / `EnsamblaDemo2026!` (cuenta que sólo existe en la simulación).
La sesión simulada vive en memoria: **si recargás la página hay que volver a ingresar**.

## Levantar todo (frontend + backend real)

Requisitos: JDK 21, Node 22+, Docker.

```bash
# 1. Base de datos (Postgres 17 en el puerto 5435)
cd backend
docker compose up -d postgres

# 2. Backend: http://localhost:8080, Swagger en /swagger-ui.html
./mvnw spring-boot:run

# 3. Frontend con el backend real (VITE_API_MOCKS=ninguno)
cd ../frontend
cp .env.example .env
npm run dev
```

- El backend lee `src/main/resources/application-ensambla-secrets.properties` (copiar del `.example`, está en `.gitignore`).
  Con `ENSAMBLA_IA_PROVEEDOR=simulado` la lectura de cuadernos devuelve un ejemplo sin costo.
- `VITE_API_MOCKS`: `ninguno` (backend real) · `parcial` (backend salvo lo pendiente) · `completo` (todo MSW).
- El proxy de Vite manda `/api` a `http://localhost:8080`.

## Comandos de calidad

| Qué | Comando |
|---|---|
| Frontend: tipos | `cd frontend && npx tsc -b` |
| Frontend: lint | `npm run lint` |
| Frontend: tests (≈ 3 min, ~480 tests) | `npm test` |
| Frontend: build | `npm run build` |
| Backend: build + tests + cobertura | `cd backend && ./mvnw verify` (necesita Docker) |

Un test puntual: `npx vitest run src/test/routes/hoy.test.tsx`.

## Dónde está cada cosa

| Qué | Dónde |
|---|---|
| App web | `frontend/` — Vite, React 19, TS, TanStack Router/Query/Form, Tailwind 4, zod, Zustand, MSW, Vitest |
| API | `backend/` — Spring Boot 4, Java 21, PostgreSQL, Flyway (ver `backend/README.md`, `backend/AGENTS.md`) |
| Documentación | `docs/` (marca, backlog por MVP, propuesta, modelo de datos, guía de pantallas) |
| Skills del proyecto | `.claude/skills`: `crear-use-case`, `escribir-tests`, `refactor-use-case` (backend) |

### Frontend por capas (ESLint lo hace cumplir)

`routes > pages > layouts > components > hooks > api / stores > types, lib`. Cada capa sólo importa de las de abajo; la UI
consume datos por `hooks/`, nunca por `api/` ni `mocks/`. Alias `@/` → `frontend/src`.

### La red proactiva (MVP 3) — todo en el navegador

Pantallas `/hoy`, `/listas`, `/mostrador`, `/precios`, `/red`, `/red/mayorista`, `/plan`, `/cuenta`, `/bienvenida`.
**No hay API**: los datos de demostración viven en `localStorage` (`ensambla-red-demo-v1`) y funcionan en los tres modos.

| Capa | Archivo |
|---|---|
| Tipos | `frontend/src/types/red.ts` |
| Cálculos, avisos (derivados), planes, valor, semilla, acciones puras | `frontend/src/lib/red/*` |
| Estado (Zustand + persist) | `frontend/src/stores/red.ts` |
| Hooks para la UI | `frontend/src/hooks/red/useRed.ts` |
| Mapa de pantallas, planes y reglas de UI | `docs/pantallas-red-proactiva.md` |
| El producto (la idea y el diferencial) | `docs/diferencial-red-proactiva.md` |

Cosas a saber:
- Los **avisos se derivan** de los datos (`lib/red/alertas.ts`); no se guardan. Al aplicar un precio el aviso desaparece solo.
- **Planes**: Gratis / Comercio / Comercio + Red, prueba de 7 días, pago simulado. En `/plan` hay una sección «Sólo para la demo»
  para adelantar el fin de la prueba y ver los candados. Precios ilustrativos.
- «Mi cuenta → Modo demostración» vuelve a los datos de ejemplo. En tests, `reiniciarRed()` corre solo entre tests.
- **Recorrido guiado** (`frontend/src/components/Recorrido`, `lib/recorrido/pasos.ts`): recorre las pantallas con tarjetas ancladas
  a elementos marcados con `data-recorrido="…"`; se reabre desde «Recorrido guiado» (barra superior, «Más» y `/cuenta`).
  Los tooltips de ayuda usan `components/ui/InfoTip.tsx`.
- Si agregás una dependencia nueva con el dev server en marcha, **reinicialo**: Vite re-optimiza y puede duplicar React
  («Invalid hook call»).

## Convenciones

- Texto de UI en español rioplatense (voseo), sin jerga: glosario en `docs/marca.md` (sistema, tabla, columna, fila, lista).
- Accesibilidad: texto de lectura 18 px, controles de 48 px, flujo completo a 360 px, foco visible, `axe` en los tests de ruta.
- Una acción principal por pantalla (botón `solid`); nada de color como única señal.
- Commits: Conventional Commits (`feat(red): …`). Flujo de ramas: `main` ← `stg` ← `dev` ← feature; PR a `dev`.
- No formatees todo `src/` con Prettier: parte del repo está con otros finales de línea y genera ruido en el diff. Formateá sólo lo que tocás.
- `frontend/src/routeTree.gen.ts` es generado por el plugin de TanStack Router (al correr `dev` o `build`); se commitea.
