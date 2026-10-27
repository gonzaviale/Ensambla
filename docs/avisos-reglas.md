# Reglas de avisos del motor de monitoreo

El motor (el caso de uso `EjecutarMotorAvisos`) corre cada regla (`ReglaAviso`, registrada como `@Bean` en `AvisoUseCaseConfig`)
sobre un `ContextoNegocio` que arma él mismo:
las tablas de **inventario** y de **ventas** del negocio (con sus campos por clave semántica), los **agregados de su zona**
y el **día de hoy en hora argentina** (`America/Argentina/Buenos_Aires`). Si una regla necesita `hoy` y no viene, no avisa.

## De dónde salen las ventas

Las ventas **no** son movimientos de stock: son las filas de la tabla de tipo `VENTAS`, con estas claves semánticas.

| Clave | Contenido |
|---|---|
| `product` | id del registro de inventario vendido (texto) |
| `quantity` | unidades (número mayor a 0) |
| `price` | precio unitario al que se vendió (opcional; sólo cuenta si es mayor a 0) |
| `date` | fecha `YYYY-MM-DD` |
| `customer` | cliente (opcional) |

Se ignoran las filas sin producto válido, sin cantidad positiva o sin fecha válida, y las ventas con fecha posterior a hoy.
Una fila es **un producto**: no hay ticket.

## Las filas de ejemplo no cuentan

Las plantillas traen filas marcadas como **ejemplo** (`es_ejemplo`): no son datos del negocio. El motor las deja afuera al
armar el contexto, tanto en el inventario como en las ventas, así que **ninguna regla** avisa sobre ellas. Cuando la persona
las reemplaza o carga productos propios, esas filas sí se evalúan.

## Configuración

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `ensambla.avisos.periodo-dias` | `30` | Período en que se cuentan las ventas (el día de hoy y los 29 anteriores) |
| `ensambla.avisos.baja-rotacion-factor` | `0.5` | Un producto rota poco si vendió, en el período, esta fracción de la mediana de los demás o menos |

Se pueden cambiar con las variables `ENSAMBLA_AVISOS_PERIODO_DIAS` y `ENSAMBLA_AVISOS_BAJA_ROTACION_FACTOR`.

## Métricas por producto (`MetricasVentas`, en `domain/model/aviso`)

- **Unidades del período**: suma de las cantidades vendidas en el período.
- **Promedio por período**: unidades totales dividido la cantidad de períodos desde la primera venta (mínimo 1).
- **Precio promedio histórico**: precio de venta ponderado por cantidad, sobre las filas que tienen precio.
- **Última venta**: fecha de la venta más reciente.
- **Baja rotación**: no vendió nada en el período, o vendió `factor` x la mediana (de los demás productos que sí vendieron) o menos.
  Si ningún otro producto vendió, no hay con qué comparar y sólo cuenta el caso de «no vendió nada».

## Huella

La huella de un aviso es `tipo:entidad:hash(datos)`. En `datos` sólo van los valores que, si cambian, deben hacer reaparecer un aviso
descartado (los precios), **nunca** las ventas del período: si no, el aviso reaparecería cada día. Cada aviso necesita una `entidad`
distinta (índice único de avisos pendientes por negocio, tipo y entidad).

## ESTANCADO · Producto caro y estancado (`ReglaProductoEstancado`)

Avisa de un producto con stock y precio que **rota poco** y está **caro**:

- Está más de un 5 % por encima del precio promedio de la zona (mismo nombre, sin importar tildes ni mayúsculas), o
- más de un 5 % por encima del precio promedio al que él mismo lo vendió. Si se cumplen las dos, manda la zona.

El precio sugerido es la referencia redondeada a la decena; si no queda por debajo del precio actual, no hay aviso. Si el negocio no
anotó ninguna venta, no avisa (no se puede saber qué se mueve). La entidad es `registro:<id>` y los datos son `{precio, sugerido}`.
La acción «Rebajar a $X» es un `EDITAR_REGISTRO` que cambia `price`; al aplicarla el precio baja, la regla deja de detectar el
producto y el aviso vence solo.

## Qué ve la persona: avisos del servidor y del navegador

El frontend (`useMotorDeAvisos`) junta los avisos del servidor con los que Sami calcula en el navegador. Para no duplicar ni resucitar
avisos descartados:

- Con el **backend real** (`VITE_API_MOCKS` distinto de `completo`), los tipos `estancado` y `combo` son **siempre del servidor**, también
  mientras todavía no contestó: el navegador no los calcula. Así un aviso descartado o aplicado no reaparece como copia local, y no hay un
  parpadeo de avisos duplicados al abrir la pantalla.
- Los demás tipos que el servidor trae (por ejemplo `stock`) también se omiten en el navegador mientras haya alguno pendiente.
- En **modo demo** (todo simulado) o si el servidor **falla**, el navegador sigue calculándolos él mismo.
