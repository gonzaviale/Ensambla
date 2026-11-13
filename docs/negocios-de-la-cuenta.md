# Negocios de la cuenta · contrato de API (H-55 · T-194)

Cómo se hablan el front y el back cuando una cuenta tiene **varios negocios** y una **vista conjunta**. Este documento
es el acuerdo entre quien haga el backend (T-198 a T-201) y quien haga el frontend (T-202 a T-206); el front ya lo
tiene simulado en `frontend/src/mocks/handlers/negocios.ts` (modo `completo`), con los mismos JSON, errores y códigos.

> **Estado:** propuesta. Las rutas y los JSON de abajo son lo que se acuerda; si al implementar alguno tiene que
> cambiar, se cambia acá, en los mocks (`frontend/src/mocks`) y en `frontend/src/types/negocios.ts` en el mismo PR.

## Vocabulario

| Palabra | Qué es |
|---|---|
| **Cuenta** | La persona que se registra (`usuario`). Tiene el plan y la suscripción. |
| **Negocio** | El emprendimiento. Una cuenta tiene uno o más (Gratuito 1, Premium hasta 6). |
| **Negocio activo** | El que la cuenta eligió para trabajar. Es el que usan todas las pantallas y los endpoints de siempre. |
| **Sección** | Lo que antes era un «cuaderno» (Ventas, Stock, Clientes…). Cuelga de un negocio. |
| **Vista conjunta** | El resumen de todos los negocios de la cuenta juntos. |

## Reglas que no cambian

- **El negocio nunca viaja en el body ni en la URL de los demás endpoints.** Siguen resolviéndolo desde la sesión:
  el *negocio activo* de la cuenta. Las rutas nuevas de este documento son las únicas que reciben un id de negocio, y
  solo para **elegir, crear o eliminar** (no para leer o escribir datos de otro negocio).
- **Aislamiento:** un negocio de otra cuenta responde **404**, igual que uno inexistente.
- Todas las rutas piden sesión; las que escriben (`POST`, `PUT`, `DELETE`) piden además el token CSRF
  (`X-CSRF-TOKEN`), como el resto de la API.
- Todas responden `Cache-Control: no-store`.
- Los errores usan el formato de siempre (`timestamp`, `status`, `error`, `message`, `path`, `traceId`) y, cuando es una
  restricción del plan, el campo `codigo`.

## Endpoints

Prefijo `/api`.

| Método y ruta | Qué hace | Respuesta |
|---|---|---|
| `GET /negocios` | Los negocios de la cuenta, cuál está activo y cuántos permite el plan | 200 |
| `POST /negocios` | Crea un negocio (no lo activa) | 201 |
| `PUT /negocios/{id}/activo` | Elige el negocio activo; es idempotente | 200 (la lista ya actualizada) |
| `DELETE /negocios/{id}` | Elimina el negocio con todo lo suyo | 204 |
| `GET /negocios/resumen?dias=30` | Resumen conjunto de todos los negocios | 200 |

### `GET /api/negocios`

Los negocios van del más antiguo al más nuevo. `limite` sale del plan de la cuenta (Gratuito 1, Premium 6) y
`puedeCrear` es `negocios.length < limite`, para que el front no tenga que repetir la cuenta.

```json
{
  "activoId": "0c0f9b6e-2b3a-4a0e-9f5e-1d2a7c4e8b11",
  "limite": 6,
  "puedeCrear": true,
  "negocios": [
    {
      "id": "0c0f9b6e-2b3a-4a0e-9f5e-1d2a7c4e8b11",
      "nombre": "Almacén San Justo",
      "rubro": "ALMACEN",
      "activo": true,
      "creadoEn": "2026-10-02T14:03:11Z"
    },
    {
      "id": "7a1d5e90-6c4b-4c1f-8a3e-5b2f9d0c3e22",
      "nombre": "Kiosco de la estación",
      "rubro": "KIOSCO",
      "activo": false,
      "creadoEn": "2026-11-05T09:41:57Z"
    }
  ]
}
```

### `POST /api/negocios`

Body: `nombre` (1 a 120 caracteres, obligatorio) y `rubro` (un código de `GET /api/ubicacion/rubros`; si falta, `OTRO`).
**Crear no cambia el negocio activo**: el front lo activa después con `PUT` si quiere trabajar en el nuevo.

```json
{ "nombre": "Kiosco de la estación", "rubro": "KIOSCO" }
```

Responde **201** con el negocio creado (`activo` siempre `false`):

```json
{
  "id": "7a1d5e90-6c4b-4c1f-8a3e-5b2f9d0c3e22",
  "nombre": "Kiosco de la estación",
  "rubro": "KIOSCO",
  "activo": false,
  "creadoEn": "2026-11-05T09:41:57Z"
}
```

Errores: **400** (nombre vacío o de más de 120, rubro inexistente, campos desconocidos) y **403** con
`LIMITE_NEGOCIOS` (ver abajo).

### `PUT /api/negocios/{id}/activo`

Sin body. Deja ese negocio como activo y devuelve la lista como `GET /api/negocios`, con `activo` ya actualizado.
Activar el que ya es activo es válido y devuelve la misma lista. Un id ajeno o inexistente → **404**.

### `DELETE /api/negocios/{id}`

Sin body. Elimina el negocio y todo lo que cuelga de él (secciones, registros, avisos, objetivos, pedidos…).
Responde **204**. Si el eliminado era el activo, el backend deja como activo al **más antiguo de los que quedan**; el
front vuelve a pedir `GET /api/negocios` para enterarse. Errores: **404** (ajeno o inexistente) y **409** si es el
único negocio de la cuenta.

La confirmación («Vas a perder…») es del front; el backend no pide un segundo paso.

### `GET /api/negocios/resumen?dias=30`

La vista conjunta. `dias` son los últimos días contando hoy (por defecto 30; entre 1 y 366, si no **400**), con el
«hoy» de Argentina. Cada negocio trae las **mismas métricas, con el mismo significado** que el panel de un negocio
(`docs/panel-negocio.md`): `vendiste`, `gastos`, `queda` y `teDeben`. Un negocio sin ventas en el período viene con
`hayDatos: false` y esas cuatro en `null` (no se manda un resumen de ceros). Lo que no se puede calcular tampoco se
manda (`null`). `total` suma solo lo que tiene valor.

```json
{
  "desde": "2026-10-15",
  "hasta": "2026-11-13",
  "dias": 30,
  "total": {
    "negocios": 2,
    "vendiste": 1250000,
    "gastos": 410000,
    "queda": 840000,
    "teDeben": 60000,
    "avisosPendientes": 5
  },
  "negocios": [
    {
      "id": "0c0f9b6e-2b3a-4a0e-9f5e-1d2a7c4e8b11",
      "nombre": "Almacén San Justo",
      "rubro": "ALMACEN",
      "activo": true,
      "hayDatos": true,
      "vendiste": 1250000,
      "gastos": 410000,
      "queda": 840000,
      "teDeben": 60000,
      "avisosPendientes": 5
    },
    {
      "id": "7a1d5e90-6c4b-4c1f-8a3e-5b2f9d0c3e22",
      "nombre": "Kiosco de la estación",
      "rubro": "KIOSCO",
      "activo": false,
      "hayDatos": false,
      "vendiste": null,
      "gastos": null,
      "queda": null,
      "teDeben": null,
      "avisosPendientes": 0
    }
  ]
}
```

`avisosPendientes` cuenta los avisos en estado `PENDIENTE` de ese negocio. Una cuenta de un solo negocio recibe la
misma forma con un único elemento.

## Errores

| Caso | Status | `codigo` | `message` (ejemplo) |
|---|---|---|---|
| Límite del plan alcanzado al crear | 403 | `LIMITE_NEGOCIOS` | `Tu plan permite hasta 1 negocio. Pasate a Premium para sumar más.` |
| Negocio de otra cuenta o inexistente (`PUT`, `DELETE`) | 404 | — | `Negocio no encontrado` |
| Eliminar el único negocio de la cuenta | 409 | — | `No podés eliminar el único negocio de tu cuenta.` |
| Datos inválidos (`POST`) o `dias` fuera de rango | 400 | — | `Revisá los datos del negocio.` |
| Sin sesión | 401 | — | `Se requiere una sesión válida` |
| Falta el CSRF en una escritura | 403 | — | `Acceso no permitido` |

El `403` de límite lleva `codigo` a propósito: el cliente HTTP **no lo reintenta** como un CSRF vencido y la pantalla
puede llevar a Planes. El `message` ya viene redactado para mostrarse tal cual, con el límite del plan de la cuenta
(`negocio` en singular si es 1).

```json
{
  "timestamp": "2026-11-13T15:04:05",
  "status": 403,
  "error": "Forbidden",
  "message": "Tu plan permite hasta 1 negocio. Pasate a Premium para sumar más.",
  "path": "/api/negocios",
  "traceId": "3f8d4b0a-0f0c-4b0e-a4f3-0b6f6b8f2f10",
  "codigo": "LIMITE_NEGOCIOS"
}
```

## Cómo lo usa cada punta

- **Backend:** el negocio activo se guarda en la cuenta y es lo que hoy sale de la sesión; los demás endpoints no
  cambian de firma. El límite sale del plan de la cuenta (`limiteNegocios`, ver T-198). Crear y eliminar son
  transaccionales; eliminar borra en cascada todo lo del negocio.
- **Frontend:** los handlers de `frontend/src/mocks/handlers/negocios.ts` responden esto en modo `completo`; el cliente
  está en `frontend/src/api/negocios.ts` y los esquemas zod en `frontend/src/types/negocios.ts`. En el mock, los
  sistemas, avisos y demás datos siguen colgando de la cuenta y se atribuyen al primer negocio; los negocios que se
  crean arrancan vacíos. Cuando cada pantalla trabaje con el negocio activo, los datos pasarán a colgar de cada negocio.
- **Plan:** `GET /api/planes` (`limiteProyectos` → `limiteNegocios`) cambia con T-198; el límite de `GET /api/negocios`
  tiene que coincidir con ese valor.

## Preguntas abiertas para quien aprueba

1. **Activar:** ¿`PUT /negocios/{id}/activo` (idempotente, lo propuesto) o `POST /negocios/{id}/activar`?
2. **Crear y activar:** hoy crear no activa; si el front prefiere un solo paso, se agrega `activar: true` al body.
3. **Eliminar el activo:** se activa el más antiguo que queda; la alternativa es exigir que el front active otro antes.
4. **Rubro obligatorio:** hoy es opcional (`OTRO` por defecto) para no frenar el alta; se completa después en
   `/ubicacion`.
