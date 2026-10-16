# CLAUDE.md — Ensambla

Guía para trabajar en este repo con Claude Code (y para que cualquier persona del equipo lo levante rápido).

**Ensambla** · «Contá tu negocio. Ensambla lo arma.» Sistema de gestión simple hecho de **bloques**: la persona cuenta qué hace,
Ensambla ensambla su sistema con piezas de un catálogo y cada pieza **desbloquea una respuesta** («te quedó $ X», «quién te
debe»). No arma tablas, columnas ni cálculos. TP integrador UNLaM 2026.

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
La sesión y los datos simulados viven en memoria: **si recargás la página hay que volver a ingresar** (y rearmar el sistema).
Un camino corto para ver todo: `/probar` (sin cuenta) → «Guardar y empezar» → registro → `/bienvenida`.

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
| Frontend: tests (≈ 3 min, ~600 tests) | `npm test` |
| Frontend: build | `npm run build` |
| Backend: build + tests + cobertura | `cd backend && ./mvnw verify` (necesita Docker) |

Un test puntual: `npx vitest run src/test/routes/inicio.test.tsx`.

## Dónde está cada cosa

| Qué | Dónde |
|---|---|
| App web | `frontend/` — Vite, React 19, TS, TanStack Router/Query/Form, Tailwind 4, zod, Zustand, MSW, Vitest |
| API | `backend/` — Spring Boot 4, Java 21, PostgreSQL, Flyway (ver `backend/README.md`, `backend/AGENTS.md`) |
| Documentación | `docs/` (marca, backlog por MVP, propuesta, modelo de datos, **`ensambla-gestion-simple.md`**) |
| Skills del proyecto | `.claude/skills`: `crear-use-case`, `escribir-tests`, `refactor-use-case` (backend) |

### Frontend por capas (ESLint lo hace cumplir)

`routes > pages > layouts > components > hooks > api / stores > types, lib`. Cada capa sólo importa de las de abajo; la UI
consume datos por `hooks/`, nunca por `api/` ni `mocks/`. Alias `@/` → `frontend/src`.

### «Se ensambla solo»

La persona cuenta su negocio → el **ensamblador** elige piezas del **catálogo** → se crean como tablas con la API de siempre →
las **respuestas** se calculan de esas tablas por **rol** de columna. El motor real (bloques, campos, registros, reportes,
cuaderno con IA, recetas) es el de siempre; las pantallas nuevas son una capa simple encima.

| Qué | Archivo |
|---|---|
| Tipos | `frontend/src/types/negocio.ts` |
| Catálogo de piezas (Productos, Ventas, Gastos, Fiados, Pedidos, Insumos) y rubros | `frontend/src/lib/catalogo/piezas.ts` |
| Ensamblador: relato → piezas con su motivo (reglas; reemplazable por IA) | `frontend/src/lib/ensamblador/index.ts` |
| Respuestas: definiciones y cálculos puros | `frontend/src/lib/respuestas/*` |
| «Contá qué pasó» (parser simulado) | `frontend/src/lib/relato/parser.ts` |
| Planes y suscripción simulada | `frontend/src/lib/planes/*`, `frontend/src/stores/suscripcion.ts` |
| Hooks para la UI | `frontend/src/hooks/negocio/*` (`useMiNegocio`, `useRespuestas`, `useArmarSistema`, `usePlanActual`…) |
| Brief del diseño, planes y reglas de UI | `docs/ensambla-gestion-simple.md` |

Pantallas: `/` landing · `/probar` demo pública · `/bienvenida` onboarding · `/inicio` · `/subir` («Contá qué pasó») ·
`/bloques` · `/respuestas` · `/plan` · `/cuenta`. «Armar», Stock y Reportes siguen vivos pero fuera del menú principal
(menú «Más» → «Avanzado: Mis sistemas»).

Cosas a saber:
- **Mi negocio** es el sistema (proyecto) más reciente. Una tabla cuenta como pieza si se llama como la pieza (o un alias, ver
  `piezaDeNombre`); las respuestas buscan las columnas por **rol** (nombre definido en el catálogo).
- **Agregar una pieza o un rubro** es tocar el catálogo/ensamblador; las pantallas no cambian.
- **Planes**: Gratis / Negocio / Negocio +, prueba de 7 días, pago simulado (backend de planes: T-066 a T-070, no existe). En
  `/plan` hay una sección «Sólo para la demo» para adelantar el fin de la prueba y ver los candados. Precios ilustrativos.
- **Recorrido guiado** (`components/Recorrido`, `lib/recorrido/pasos.ts`): tarjetas ancladas a elementos con `data-recorrido="…"`;
  se reabre desde «Recorrido guiado» (barra, «Más» y `/cuenta`). Los tooltips usan `components/ui/InfoTip.tsx`.
- Si agregás una dependencia con el dev server en marcha, **reinicialo** (Vite re-optimiza y puede duplicar React).

## Convenciones

- Texto de UI en español rioplatense (voseo), sin jerga: glosario en `docs/marca.md`.
- Máx. 3 datos grandes por pantalla, 1 acción principal (botón `solid`), tablas sólo en «Ver todo».
- Accesibilidad: texto de lectura 18 px, controles de 48 px, flujo completo a 360 px, foco visible, `axe` en los tests de ruta.
- Lo simulado (planes, IA del ensamblador, Mercado Pago, WhatsApp) se rotula como tal en pantalla.
- Commits: Conventional Commits (`feat(negocio): …`). Flujo de ramas: `main` ← `stg` ← `dev` ← feature; PR a `dev`.
- No formatees todo `src/` con Prettier: parte del repo tiene otros finales de línea y genera ruido. Formateá sólo lo que tocás.
- `frontend/src/routeTree.gen.ts` lo genera el plugin de TanStack Router (al correr `dev` o `build`); se commitea.
