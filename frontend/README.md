# Ensambla · Frontend

SPA de Ensambla. Vite · React 19 · TypeScript · TanStack Router / Query / Form · Tailwind 4 · zod · MSW · Vitest.

## Cómo correrlo

```bash
cp .env.example .env
npm install
npm run dev        # http://localhost:5173 (proxy /api → http://localhost:8080)
```

| Script | Qué hace |
|---|---|
| `npm run dev` | Servidor de desarrollo |
| `npm run build` | Chequeo de tipos + build de producción |
| `npm run typecheck` | `tsc -b` |
| `npm run lint` | ESLint (incluye las reglas de capas) |
| `npm run format` | Prettier |
| `npm test` | Vitest con MSW (no necesita backend) |

### Modo de la API (`VITE_API_MOCKS`)

| Valor | Comportamiento |
|---|---|
| `parcial` (default) | MSW simula sólo lo que el backend todavía no tiene: registro, login y sesión (T-005, T-006) |
| `completo` | Todo simulado con MSW; no hace falta backend |
| `ninguno` | Todo contra el backend real. Usar cuando T-005 y T-006 estén hechos |

Cuando el backend implemente un endpoint, se saca su handler de `src/mocks/handlers/index.ts` del modo `parcial`.

### Probar proyectos (ENS-4)

El CRUD de proyectos y las plantillas ya tienen backend. Mientras falten T-005/T-006, usá el modo `completo`
para recorrer la interfaz: la sesión simulada del modo `parcial` no autentica contra el servidor real.

```powershell
$env:VITE_API_MOCKS = 'completo'
npm.cmd run dev -- --host 127.0.0.1
```

Ingresá en `/login` con `demo@ensambla.local` / `EnsamblaDemo2026!`. Esta cuenta sólo existe en la simulación.
Podés crear en blanco o desde Inventario, Pedidos y Clientes; abrir, renombrar y eliminar proyectos.
Los datos simulados viven en memoria y se reinician al recargar la página.

La carga efectiva de fotos y el editor de bloques corresponden a otras historias. El constructor muestra
los campos copiados de la plantilla o una orientación para el primer bloque si el proyecto está vacío.

### Probar la tabla de registros (H-06)

En el modo `completo`, abrí un proyecto y entrá a `/proyectos/<id>/usar`. La tabla lista los registros de cada
bloque con búsqueda, orden por columna (un segundo click invierte el sentido), paginación y una ficha lateral
con todos los datos y la eliminación con confirmación.

- En `npm run dev`, los proyectos creados desde una plantilla traen 30 registros de ejemplo por bloque
  (`src/mocks/db/registros.ts`); en los tests arrancan vacíos.
- El contrato es el de `GET /api/bloques/{id}/registros` y `DELETE /api/registros/{id}` del backend
  (`src/api/registros.ts`). Los handlers simulan el mismo orden, búsqueda y paginación.
- Falta la edición desde la ficha: reutiliza el `FormRenderer` de T-024. La carga de registros llega con T-025
  y T-031.

## Estructura

```
src/
├── routes/        rutas file-based de TanStack Router: finas (guard + loader + <Page/>)
├── pages/         una carpeta por pantalla
├── layouts/       PublicLayout, AppLayout
├── components/    ui/ (átomos), AuthForm/, Navbar/ … componentes por feature
├── hooks/<dominio>/  queries.ts (queryOptions + keys + useSuspenseQuery), mutations.ts
├── api/           cliente fetch (CSRF, errores) + un archivo por recurso con schemas zod
├── stores/        estado de cliente con zustand (vacío por ahora)
├── types/         schemas zod y tipos inferidos
├── lib/           utilidades puras (cn, formErrors, destino)
├── config/        env, queryClient, router, providers, guards
├── mocks/         MSW: handlers y base en memoria
├── styles/        index.css con los tokens de marca (@theme)
└── test/          setup, renderApp, helpers y tests
```

### Capas (las verifica ESLint)

`routes > pages > layouts > components > hooks > api / stores > types, lib`. Cada capa sólo importa de las de abajo.
La UI **no** llama a `api/` ni a `mocks/`: los datos llegan por hooks.

## Cómo agregar una pantalla

1. Contrato en `types/<dominio>.ts` (schema zod) y función en `api/<recurso>.ts`.
2. `hooks/<dominio>/queries.ts` con `queryOptions` + hook `useSuspenseQuery`.
3. Página en `pages/<Pantalla>/<Pantalla>Page.tsx`.
4. Ruta en `routes/` (el árbol `routeTree.gen.ts` se regenera solo con `npm run dev` o `npm run build`).
5. Handler MSW en `mocks/handlers/` si el backend todavía no existe, y un test en `test/`.

Pantallas pendientes del MVP 2 están marcadas con `<PendingTask tarea="T-0XX" />`.

## Marca

Tokens de color y tipografía en `src/styles/index.css`; guía en [../docs/marca.md](../docs/marca.md).
