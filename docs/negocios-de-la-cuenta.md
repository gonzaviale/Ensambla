# Negocios de la cuenta (H-55 · ENS-284)

Cómo una cuenta trabaja con **varios negocios** y los mira juntos. Reemplaza al contrato de API que acompañó a las
olas 1 y 2: ese modelo (varias filas de `negocio` por usuario y el plan colgando del usuario) se descartó.

## El modelo

- **`negocio` es el perfil único de la cuenta.** Hay uno por usuario y de él cuelga todo: los proyectos, el plan, la
  prueba, la renovación y los pagos. No se multiplica. Ver [modelo-datos.md](modelo-datos.md).
- **Cada negocio que ve la persona es un cuaderno**: un `proyecto` del backend, un `Sistema` en el frontend. El backend
  ya permitía varios por cuenta y el plan ya los limita (`limite_proyectos`: Gratuito 1, Premium sin límite).
- **No hay endpoints de negocios.** La lista sale de `GET /api/sistemas`; crear y eliminar un negocio es guardar o borrar
  un cuaderno con `PUT` y `DELETE /api/sistemas/{id}`. Con el cupo agotado el servidor responde 403 `LIMITE_PLAN`.

## Vocabulario

| Palabra | Qué es |
|---|---|
| **Cuenta** | La persona que se registra (`usuario`) con su perfil (`negocio`). Tiene el plan. |
| **Negocio** | Cada emprendimiento de la cuenta. Técnicamente, un cuaderno (`proyecto` / `Sistema`). |
| **Negocio activo** | El cuaderno con que se está trabajando. Es el que muestran las pantallas. |
| **Pestaña** | Cada grupo de tablas dentro de un negocio (`seccion` en la base). |
| **Vista conjunta** | La comparación de todos los negocios de la cuenta. |

Varios textos del frontend todavía dicen «sección» para referirse al cuaderno (herencia de la ola 1). Unificarlos queda
para un cambio aparte.

## Cómo lo resuelve el frontend

| Qué | Dónde | Cómo |
|---|---|---|
| Lista de negocios | `lib/negocios/lista.ts`, `hooks/negocios/queries.ts` | Un negocio por cuaderno; el límite es `limiteProyectos` del plan actual. |
| Negocio activo | `stores/sistemaStore.ts` (`negocioActivoId`) | Se recuerda en `localStorage` (`ensambla.negocio.activo`). Si el guardado ya no existe, vale el primero. |
| Pantallas filtradas | `seleccionarSistemasDelNegocio` | Inicio, métricas, zona y los avisos que calcula el navegador leen sólo el cuaderno activo. |
| Elegir al entrar | `/elegir-negocio` | Con más de un cuaderno se pregunta con cuál trabajar antes de entrar a la app. |
| Agregar | `ModalAgregarNegocio` | Pide el nombre y crea un cuaderno vacío, que queda activo. |
| Eliminar | `ModalEliminarNegocio` | Borra el cuaderno; el activo pasa a ser otro. |
| Comparar | `/negocios/comparar` | Pide `GET /api/negocio/panel?proyectoId=…&dias=30` por cada cuaderno y suma los resultados. |

## Lo que este enfoque no separa

El servidor no sabe cuál es el negocio activo, así que lo que él calcula sigue siendo de toda la cuenta:

- **Avisos del motor, objetivos y zona.** Son del perfil. Por eso la comparación y el pop-up no dicen cuántos avisos
  tiene cada negocio.
- **Rubro y ubicación.** Son del perfil: todos los negocios de la cuenta los comparten.
- **WhatsApp.** El número se vincula a la cuenta, no a un negocio en particular.
- **El negocio activo no viaja entre dispositivos.** Se recuerda en cada navegador.

Separar cualquiera de estos puntos por negocio necesita cambios en el backend.
