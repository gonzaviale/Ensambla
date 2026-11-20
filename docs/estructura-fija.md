# Estructura fija del negocio

Actualizado: 2026-11-19

## Para qué sirve

Cada negocio de la cuenta es un `Sistema` (un `proyecto` en el backend) con secciones, y cada sección es un bloque con
columnas. Caja, Objetivos, Avisos y Mi negocio no buscan una sección por su nombre: buscan **secciones de cierto tipo**
y leen sus **columnas por clave** (`quantity`, `price`, `product`…). Si alguien quita una de esas columnas o cambia su
tipo de dato, esas pantallas dejan de funcionar.

Por eso hay una estructura mínima que **no se puede quitar ni cambiar**. Todo lo demás es libre: la persona puede
crear las secciones y columnas que quiera, y esas siguen siendo editables y borrables.

Esta tabla es la **única fuente**. El backend la implementa en `domain/model/bloque` (`TipoBloque`, `ColumnasFijas`,
`EstructuraFija`, `ProteccionDeEstructura`) y la pantalla la refleja con las marcas `delSistema`, `eliminable` y
`tipoBloqueado` que le manda el servidor. El backend es la defensa final; la pantalla solo mejora la experiencia.

## Secciones obligatorias

Todo negocio tiene **al menos una** sección de cada uno de estos tipos: **Stock** (`inventory`), **Ventas**
(`sales`), **Clientes** (`clients`) y **Gastos** (`expenses`). Si hay más de una del mismo tipo, las extra se pueden
borrar; la última no. No se les puede cambiar el tipo. El nombre, el color y el ancho sí se pueden cambiar.

Entradas, Salidas, Pedidos, Formulario, Nota y Resumen no son obligatorias. Entradas y Salidas, si existen, llevan
columnas fijas.

## Columnas fijas por tipo de sección

Fuente: lecturas del código al 2026-11-19 (backend: `CobrarVenta`, `MovimientoDeStock`, `PanelNegocio`,
`ResumenNegocio`, `ReglaStockBajo`, `MedidorMiniObjetivo`, `ComoVieneElNegocio`, `CodigosInventario`).

| Tipo | Clave | Nombre inicial | Tipo de dato | Obligatoria | Para qué la necesita Ensambla |
|---|---|---|---|---|---|
| Stock | `name` | Producto | texto | sí | identificar cada producto |
| | `quantity` | Cantidad | número | sí | controlar el stock y descontarlo en Caja |
| | `price` | Precio | moneda | no | cobrar en Caja y calcular la ganancia |
| | `cost` | Costo | moneda | no | calcular la ganancia de Objetivos y Mi negocio |
| | `minimum` | Stock mínimo | número | no | avisar cuando queda poco stock |
| | `barcode` | Código de barras | texto | no | escanear productos |
| | `saleMode` | Venta por | lista: unidad, peso | no | elegir si Caja vende por unidad o por peso |
| | `favorite` | Favorito en caja | sí/no | no | mostrar los favoritos en Caja |
| | `stockControl` | Controlar stock | sí/no | no | decidir si Caja descuenta el stock |
| Ventas | `product` | Producto | conexión a una sección Stock | sí | saber qué producto se vendió |
| | `quantity` | Cantidad | número | sí | descontar el stock y medir las ventas |
| | `price` | Precio | moneda | no | calcular lo vendido |
| | `date` | Fecha | fecha | no | ubicar cada venta en el período |
| | `customer` | Cliente | texto, o conexión a una sección Clientes | no | registrar a quién se fió |
| | `credit` | Fiado | sí/no | no | calcular lo que te deben |
| Clientes | `name` | Nombre | texto | sí | identificar a cada cliente en Caja |
| Gastos | `name` | Concepto | texto | sí | identificar cada gasto |
| | `date` | Fecha | fecha | no | ubicar cada gasto en el período |
| | `total` | Importe | moneda | sí | calcular costos y ganancia |
| | `category` | Categoría | lista (conserva «Mercadería») | no | separar la mercadería de los otros gastos |
| Entradas y Salidas | `product` | Producto | conexión a una sección Stock | sí | mover el stock del producto |
| | `quantity` | Cantidad | número | sí | sumar o restar unidades |

### Lo que la auditoría ajustó respecto del plan

- `phone` de Clientes **no es fija**: Caja solo lo muestra y lo usa en la búsqueda; si falta, nada se rompe.
- `date` de Entradas y Salidas **no es fija**: `MovimientoDeStock` solo lee `product` y `quantity`.
- `customer` de Ventas admite **texto o conexión**: `CobrarVenta` guarda el nombre si no hay conexión y el id si la hay.
- El mínimo de `quantity` no es parte del contrato: Caja lo cambia a 0,001 para vender por peso.

## Reglas

Se compara el sistema guardado con el propuesto. Las columnas se identifican por su **clave dentro de la sección**.

| Regla | Qué se rechaza (HTTP 409 con el motivo) | Qué se permite |
|---|---|---|
| E1 | Quitar la última sección de un tipo obligatorio | Quitar una sección extra del mismo tipo |
| E2 | Cambiar el tipo de una sección que lleva columnas fijas | Cambiarle el nombre, el color, el ancho o la pestaña |
| E3 | Quitar una columna fija | Quitar columnas propias |
| E4 | Cambiar el tipo de dato de una columna fija | Cambiarle el nombre visible y el orden |
| E5 | Cambiar el destino de una conexión fija a una sección de otro tipo | — |
| E6 | Dejar de pedir un dato que el contrato exige, o quitar «Mercadería» de la categoría | Agregar opciones a las listas |

Lo que **nunca existió** no se rechaza: se **completa**. Lo que **existía y ahora falta o cambió** se rechaza.

### Completar (negocios nuevos y viejos)

`EstructuraFija.completar` se aplica al guardar, al abrir un negocio y al crear uno nuevo:

1. Si falta una sección obligatoria, se agrega en la primera pestaña con su nombre inicial.
2. Si falta una columna fija en una sección de su tipo, se agrega al final.
3. Si la columna existe con el tipo correcto pero sin cumplir lo demás (no es obligatoria, le faltan opciones), se
   **ajusta en el lugar**: no se pierden datos.
4. Si la columna existe con **otro tipo de dato** (por ejemplo `price` como texto), se **conserva con sus datos** como
   `price_anterior` y nombre «Precio (anterior)», y se agrega la columna correcta. Nunca se cambia el tipo de una
   columna con datos.

Las secciones nuevas se agregan sin filas; agregar una columna no escribe valores en las filas que ya existían.

Una columna propia que quiera usar una clave reservada también se aparta (punto 4): la pantalla tiene que evitar
generar esas claves al crear una columna (ver la lista de claves reservadas en la pantalla).
