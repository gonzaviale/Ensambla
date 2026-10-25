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
| `parcial` | Todas las rutas actuales usan el backend real; se mantiene el valor por compatibilidad |
| `completo` | Todo simulado con MSW; no hace falta backend |
| `ninguno` (default) | Todo contra el backend real, incluidos registro, login y logout |

La sesión se guarda en la cookie HttpOnly `ENSAMBLA_SESSION`, con dos horas de inactividad por defecto.
TanStack Query mantiene la identidad del usuario
en memoria; los guards revalidan la sesión al navegar a rutas privadas. Logout vuelve a `/login`.
El formulario de registro incluye nombre del negocio y muestra errores del cliente y del servidor.
Los tests usan MSW completo y un entorno jsdom que conserva los AbortSignal nativos de Node para fetch.

### Recorrer la aplicación

Con `VITE_API_MOCKS=completo` ingresá en `/login` con `demo@ensambla.local` / `EnsamblaDemo2026!` (la cuenta sólo
existe en la simulación). La primera vez aparece la bienvenida con la guía interactiva. Después: inicio,
plantillas, taller con Sami, editor (Construir y Usar), avisos de Sami con la campana, y «Mi zona». La base
simulada persiste en `localStorage`. Detalle en [../docs/ui-mockup.md](../docs/ui-mockup.md).

## Planes y prueba premium (ENS-97)

La opción «Planes» abre `/planes` sin exigir sesión. Visitantes consultan solo
`GET /api/planes/catalogo`, con precios y capacidades globales; no se consultan sistemas, zona,
consumo ni pagos. Con sesión se conserva el plan efectivo, consumo y prueba desde `GET /api/planes`. Mientras no se defina precio/moneda,
Premium muestra «Precio por definir». ENS-98 configura ARS 14.900 cada treinta días y agrega
contratación y cancelación; los botones de cobro se habilitan sólo si el backend tiene configurado el proveedor.

La confirmación explica siete días, diez páginas totales, una única prueba por negocio y conservación
de datos; `POST /api/planes/prueba` usa sesión y CSRF. El botón evita doble envío. Se vuelve a consultar
tras éxito o error, al recuperar el foco y periódicamente; cerca del vencimiento se usa la hora del
servidor para actualizar el estado. Un 403 con código comercial se conserva sin repetir la escritura.

`completo` simula planes y conserva el historial de prueba en la base local de demostración;
`parcial` y `ninguno` usan el backend real, incluidos sistemas, preferencias, Sami y zona. Para validar
los límites comerciales usá el backend y PostgreSQL reales; `completo` sigue siendo una simulación.
Gratuito incluye tres avisos mensuales y tres recomendaciones mensuales compartidas entre Mi zona y
Otra zona. Los cupos se guardan por negocio en el servidor y el contenido adicional invita a Planes.
Detalle de pruebas y entorno local aislado en [plan-ENS-97.md](../docs/plan-ENS-97.md).

### Renovación automática (ENS-98)

La tarjeta Premium consulta `/api/planes/renovacion`. La confirmación presenta precio, frecuencia,
fecha del primer cobro y condiciones; luego permite continuar en el checkout alojado de Mercado Pago.
Una autorización se muestra separada del acceso pago. Sólo `GET /api/planes`, con la sesión actual,
decide el plan mostrado; parámetros de retorno como `status=approved` no habilitan Premium.

«Actualizar pagos» solicita conciliación y refresca ambas consultas. La cancelación requiere
confirmación; si falla, se conserva el mensaje de baja pendiente y el tiempo pagado. Se muestra
el fin del período pago. Las mutaciones no se reintentan automáticamente y bloquean doble envío.
Las respuestas se reconsultan usando la sesión actual para no trasladar datos entre cuentas.

MSW completo informa contratación deshabilitada por defecto; los tests de pagos configuran handlers
locales explícitos. MSW parcial y ninguno usan los endpoints reales del backend para este módulo.
No hay credenciales de Mercado Pago ni datos de tarjetas en el frontend.
[Guía de configuración](../docs/mercado-pago-configuracion-local.md).

## Estructura de carpetas

```
src/
├── routes/        rutas file-based de TanStack Router: finas (guard + loader + <Page/>)
├── pages/         una carpeta por pantalla
├── layouts/       PublicLayout, AppLayout
├── components/    ui/ (átomos), AuthForm/, Navbar/, Landing/ y sistema/ (el mockup)
├── hooks/<dominio>/  queries.ts (queryOptions + keys + useSuspenseQuery), mutations.ts
├── api/           cliente fetch (CSRF, errores) + un archivo por recurso con schemas zod
├── stores/        estado de cliente con zustand (sistemas, taller, avisos, tutorial)
├── types/         schemas zod y tipos inferidos
├── lib/           utilidades puras (cn, formErrors, destino) y lib/sistema (motor del mockup)
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

## Marca

Tokens de color y tipografía en `src/styles/index.css`; guía en [../docs/marca.md](../docs/marca.md).

Las pantallas de la aplicación son un port calcado de `Ensambla_Mockup`, con su CSS acotado bajo `.mk-root`.
Ver [cómo se mapea y cómo se verifica](../docs/ui-mockup.md).

## Validación de planes y suscripciones (21/10/2026)

Tras integrar los cambios remotos de configuración del negocio y ubicación, el frontend pasó
lint, tipos, **344 pruebas en 32 archivos** y build de producción. Incluye la consulta de planes,
activación de prueba, contratación, cancelación y restricciones comerciales con sesión y CSRF.
Las verificaciones externas pendientes de Mercado Pago se detallan en [ENS-98](../docs/plan-ENS-98.md).

<!-- prueba: dispara ci-frontend sin Vercel -->

## Home con métricas y metas (ENS-244)

Cuando hay tablas, Inicio muestra primero el resumen del sistema elegido: ventas registradas,
pedidos pendientes, productos con stock bajo, clientes y filas, según las tablas disponibles.
Las filas de ejemplo no se cuentan. Ventas usa cantidad por precio y no se suma con Pedidos;
no se presenta como ganancia o cobros. El resumen usa el estado actual y el acumulado registrado.

Cada indicador admite una meta local, con mínimo a alcanzar o máximo a reducir. Se separan por
cuenta y UUID de sistema (que identifica su negocio); no se guardan con nombres o correos.
No se sincronizan entre dispositivos ni envían notificaciones con la aplicación cerrada.
Si el navegador impide guardar, se avisa y la meta sigue disponible durante esa visita.

Planes muestra solo modalidad mensual; la anual espera una definición comercial. Las tarjetas
comparten estructura entre visitantes y cuentas, y la renovación se gestiona en una sección aparte.
Los reportes filtrados conservan el control Premium del servidor. Los mensajes de acceso gratuito
informan cupo y renovación; el enlace a opciones de plan aparece cuando hay contenido bloqueado.
La tarjeta de sugerencias para la home depende de ENS-245.
