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
| `parcial` | Backend real salvo lo pendiente: hoy no queda nada simulado |
| `completo` | Todo simulado con MSW; no hace falta backend |
| `ninguno` (default) | Todo contra el backend real, incluidos registro, login y logout |

La sesión se guarda en una cookie HttpOnly del servidor. TanStack Query mantiene la identidad del usuario
en memoria; los guards revalidan la sesión al navegar a rutas privadas. Logout vuelve a `/login`.
El formulario de registro incluye nombre del negocio y muestra errores del cliente y del servidor.
Los tests usan MSW completo y un entorno jsdom que conserva los AbortSignal nativos de Node para fetch.

### Probar proyectos (ENS-4)

El CRUD de proyectos y las plantillas ya tienen backend. Mientras falten T-005/T-006, usá el modo `completo`
para recorrer la interfaz: la sesión simulada del modo `parcial` no autentica contra el servidor real.

```powershell
$env:VITE_API_MOCKS = 'completo'
npm.cmd run dev -- --host 127.0.0.1
```

Ingresá en `/login` con `demo@ensambla.local` / `EnsamblaDemo2026!`. Esta cuenta sólo existe en la simulación.
Podés crear en blanco o desde Inventario, Pedidos y Clientes; abrir, renombrar y eliminar proyectos.
Cada tarjeta muestra sus bloques y cuántos datos tiene cada uno (se leen de `GET /api/proyectos/{id}`, porque
`GET /api/proyectos` todavía no trae la estructura); «Usar» es la acción principal y renombrar/eliminar están en «Más opciones».
Los datos simulados viven en memoria y se reinician al recargar la página.

La carga efectiva de fotos corresponde a otra historia.

### Probar el constructor (ENS-5 · H-04)

En `/proyectos/<id>/construir` (botón «Cambiar estructura» de la tarjeta, o «Armar bloques» si está vacío): lista de bloques a la izquierda y el bloque elegido
a la derecha; en el celular, primero la lista y al tocar un bloque su detalle («Ver todos los bloques» o el
botón atrás vuelven). El bloque abierto queda en la URL (`?bloque=<id>`). Se agregan, renombran y eliminan
bloques (con confirmación) y sus datos (campos) de los cinco tipos, con obligatorio, valor inicial y opciones.
En la interfaz los campos se llaman «datos» (cada uno es una columna de la tabla).

- Contrato: `src/api/bloques.ts` (T-017) y `src/api/campos.ts` (T-018), los dos ya en el backend.
- `completo`: bloques y campos simulados sobre el proyecto en memoria, con las validaciones del backend
  (409 por nombre repetido, 400 con `errors`).
- `ninguno` o `parcial`: todo contra el backend real.

### Cargar registros (ENS-6 · ENS-52/T-024 y ENS-53/T-025)

En modo `completo`, creá un proyecto desde una plantilla: se abre directo en Usar (con un aviso para cambiar la estructura en Construir); elegí la pestaña **Cargar** o «Cargar el primer registro».
El modo usar permite elegir un bloque y genera sus controles de texto, número, fecha, opción y sí/no,
respetando el orden y los valores iniciales. Los borradores se conservan al cambiar de bloque dentro
de la pantalla; se pierden al salir o recargar. Después de confirmar el guardado se vuelven a aplicar
los valores iniciales para cargar el próximo registro.

- Probá guardar Inventario sin Nombre: aparece el error junto al campo y recibe el foco.
- Completá Nombre y guardá: Stock `0` es válido; aparece el aviso de éxito.
- En Pedidos, Entrega usa un control de fecha y Entregado permite Sí/No, inicialmente No.
- Durante el guardado quedan deshabilitados el formulario y el selector de bloque. Los errores
  del servidor aparecen por campo y en un aviso; conservan lo escrito. No hay reintento automático
  ante fallas de red.
- Un proyecto sin bloques o un bloque sin campos muestra una orientación y no permite enviar.

El cliente usa `POST /api/bloques/{bloqueId}/registros` con `{ valores: { [idCampo]: valor } }`,
sesión y CSRF. Envía números y booleanos JSON, fechas `YYYY-MM-DD` y omite opcionales vacíos.
El contrato sigue la rama de Facundo `feat/ens-7-registros-backend`. El backend de ENS-50/T-022 y
ENS-51/T-023 y la autenticación real deben integrarse en `dev` antes de probar con `ninguno`.
La simulación y los tests no verifican persistencia real en PostgreSQL. El listado/edición de registros
corresponde a ENS-7 y no se incluye aquí. Ver [plan y contrato](../docs/plan-ENS-6.md).

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

### Subir foto del cuaderno (T-091)

`/cuaderno` (botón «Subir foto de mi cuaderno» en Mis proyectos): sacar foto con la cámara o elegir de la galería,
miniaturas con girar y quitar, consejos para una buena foto y «Leer mi cuaderno», que manda hasta 10 páginas a
`POST /api/lotes` (multipart, campo `fotos`). Antes de subir, `lib/imagen.ts` prepara cada foto en el navegador
(T-092): la endereza según la orientación EXIF, aplica el giro elegido, achica el lado mayor a 2000px y la comprime
en JPEG bajando la calidad hasta quedar por debajo de 1,5 MB. Si no puede decodificarla (HEIC fuera de Safari) manda
el archivo original. En modo `completo` el lote se
simula sin guardar los archivos.

## Estructura

```
src/
├── routes/        rutas file-based de TanStack Router: finas (guard + loader + <Page/>)
├── pages/         una carpeta por pantalla
├── layouts/       PublicLayout, AppLayout
├── components/    ui/ (átomos), AuthForm/, Navbar/, Landing/ … componentes por feature
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
