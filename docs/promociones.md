# Promocioná tu comercio («Promocionar»)

Sami propone qué publicar a partir de lo que se vende, escribe el texto para el canal elegido y arma una imagen con la
marca. **La persona revisa y comparte: nada se publica solo.** Publicar directo en Instagram o Facebook exige una app
verificada por Meta, y quedó afuera a propósito.

## Cómo se usa

Pantalla **Promociones** (barra de arriba, `/promociones`), en tres pasos:

1. **Qué promocionar.** Las ideas que aplican hoy más la lista de precios.
2. **Dónde compartirlo.** Instagram, estado de WhatsApp o mensaje a clientes. Sami escribe el texto con el tono y el
   largo de cada lugar.
3. **Revisarla y compartirla.** El texto se puede editar. La imagen se arma en el navegador (cuadrada o historia; la lista
   de precios siempre es la lista). **Compartir** usa la hoja de compartir del celular; en la computadora baja la imagen y
   abre WhatsApp con el texto. También se puede copiar el texto o descargar la imagen.

Usa el cuaderno que se está mirando si tiene inventario; si no, el primero que lo tenga.

## Las ideas (`ReglaPromocion`)

Cada regla mira el inventario y las ventas de los últimos 30 días y propone una promoción, o nada. Sumar una idea es
sumar una clase y registrarla como bean en `PromocionUseCaseConfig`.

| Regla | Propone | No propone si |
|---|---|---|
| `PromocionProductoEstrella` | El producto que más unidades vendió | Nada se vendió en el período |
| `PromocionLiquidar` | Hasta 3 productos con stock que no se vendieron (primero los de más stock) | No hay ventas anotadas (no se sabe qué está parado) |

La **lista de precios** la arma el navegador con los productos propios que tienen precio (hasta 12, sin las filas de
ejemplo).

## Los canales (`CanalPromocion`)

Una clase base con un método plantilla (`redactar`: armar el texto del canal y ajustarlo al largo que admite) y una
clase por canal. Sumar un canal es sumar una clase registrada como bean.

| Canal | Clave | Largo máximo | Forma |
|---|---|---|---|
| `CanalInstagram` | `instagram` | 2200 | Título, gancho, lista, invitación al local y etiquetas |
| `CanalEstadoWhatsapp` | `estado` | 700 | Título en negrita, lista y el nombre del negocio |
| `CanalMensajeAClientes` | `clientes` | 1000 | Saludo, lista y «¿Te guardo alguno?» |

Si el texto pasa del largo del canal se corta y termina en «…».

## Las imágenes (`FormatoDeImagen`)

Clase base con un método plantilla (fondo → encabezado → cuerpo → pie) y tres formatos: `Cuadrado` (1080×1080, 5
productos), `Historia` (1080×1920, 7) y `ListaDePrecios` (1080×1350, 12). Los colores son los de `docs/marca.md`. Se dibujan
con `canvas` (sin dependencias nuevas) y las clases solo piden un `Lienzo` mínimo, así se prueban sin navegador.

## API

Sesión obligatoria; el negocio sale de la sesión.

| Método y ruta | Qué hace |
|---|---|
| `GET /api/proyectos/{proyectoId}/promociones` | Las ideas que aplican y los canales disponibles |
| `POST /api/promociones/texto` | El texto de una promoción en un canal. Cuerpo: `{ "canal", "clave", "titulo", "gancho", "productos": [{ "nombre", "precio" }] }` |

## Hoy el texto no usa IA

El texto sale de plantillas fijas por canal: es instantáneo, gratis y nunca falla, y alcanza para la demo. El siguiente
paso es que Sami lo redacte con el proveedor de IA ya integrado (`ClienteModeloTexto`), con las plantillas como respaldo y
con el límite diario de `consumo_sami_diario`. Para eso solo cambia `CanalPromocion.armarTexto`.

## Dónde está en el código

- `domain/model/promocion` y `domain/useCase/promocion/{sugerirPromociones,redactarPromocion}`; `PromocionController`.
- Front: `pages/Promociones`, `components/promociones`, `lib/promociones` (`formatosDeImagen`, `imagenDePromocion`,
  `sistemaParaPromocionar`), `lib/sistema/compartir.ts`, `api/promociones.ts`, `hooks/promociones`.
