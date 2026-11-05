# Pedido a proveedor en PDF («Reponer»)

Arma el pedido de lo que falta, agrupado por proveedor y listo para mandar. No lleva precios ni costos: es un pedido, no
una cuenta. Es gratis (no es una función Premium).

## Cómo se usa

- En un cuaderno, **Agregar y organizar › Pedido a proveedor**. El modal muestra lo que falta con una solapa por
  proveedor, la cantidad sugerida en cada producto (se puede cambiar) y una casilla para sacar lo que no se pide.
- **Descargar PDF** baja el documento. **Enviar por WhatsApp** usa la hoja de compartir del celular (el PDF cae directo
  en WhatsApp); en la computadora baja el PDF y abre WhatsApp Web con el mensaje, para adjuntarlo.
- El WhatsApp del proveedor se anota una vez y queda en este navegador (`localStorage`); no viaja al servidor.
- Para agrupar por proveedor, el inventario tiene una columna **Proveedor** (`supplier`). Los sistemas que ya existían no
  la tienen: sin ella todo cae en «Sin proveedor» y el modal explica cómo agregarla.

## Qué es «lo que falta»

Un producto está en falta si se **agotó** (`Agotado`) o si quedan unidades pero **no más que su mínimo** (`BajoMinimo`),
ambas variantes de `SituacionStock`. Se sugiere pedir lo necesario para llegar al **doble del mínimo** (o a 10 si no hay
mínimo), nunca menos de 1. Es la misma cuenta del aviso de stock (`SituacionStock.pedidoPara`).

## API

Los dos endpoints exigen sesión; el negocio sale de la sesión y un sistema de otro negocio responde 404.

| Método y ruta | Qué hace |
|---|---|
| `GET /api/proyectos/{proyectoId}/reposicion` | Lo que hay que reponer, agrupado por proveedor y con la cantidad sugerida |
| `POST /api/proyectos/{proyectoId}/reposicion/pdf` | El PDF del pedido. Cuerpo: `{ "proveedor": "Molinos", "productos": [{ "productoId": "…", "cantidad": 12 }] }` |

El `POST` rechaza (400) un pedido sin productos, una cantidad que no sea positiva o un producto que ya no está en falta.

## Dónde está en el código

- `domain/model/reposicion`: `SituacionStock` (`Agotado`, `BajoMinimo`), `ItemReposicion`, `Faltantes`,
  `GrupoDeProveedor`, `PedidoReposicion`.
- `domain/service/reposicion/ReposicionService` (lo comparten los dos casos de uso) y
  `domain/useCase/reposicion/{consultarReposicion,armarPedidoProveedor}`.
- Puerto `GeneradorPedidoPdfPort` y adaptador `infrastructure/reposicion/GeneradorPedidoPdfAdapter` (OpenPDF).
- `ReposicionController`. Front: `components/sistema/modales/ModalPedidoProveedor.tsx`, `lib/sistema/pedidoProveedor.ts`,
  `lib/sistema/compartir.ts`, `api/reposicion.ts`, `hooks/reposicion`.

## Fuera de alcance por ahora

Mandar el PDF desde el chat de WhatsApp con un comando (`REPONER`, `PEDIDO`): hace falta que el puerto de mensajería
envíe documentos. Registrar el pedido como una entrada de stock también queda para después.
