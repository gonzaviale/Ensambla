# Reglas de avisos del motor de monitoreo

El motor (`MotorAvisosService`) corre cada regla (`ReglaAviso`) sobre un `ContextoNegocio` que arma `ContextoNegocioService`:
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

## Configuración

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `ensambla.avisos.periodo-dias` | `30` | Período en que se cuentan las ventas (el día de hoy y los 29 anteriores) |
| `ensambla.avisos.baja-rotacion-factor` | `0.5` | Un producto rota poco si vendió, en el período, esta fracción de la mediana de los demás o menos |

Se pueden cambiar con las variables `ENSAMBLA_AVISOS_PERIODO_DIAS` y `ENSAMBLA_AVISOS_BAJA_ROTACION_FACTOR`.

## Métricas por producto (`MetricasVentasService`)

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

## «Misma venta»: mismo cliente y mismo día

Como las ventas no tienen ticket, una **canasta** (lo que se llevó una persona en una compra) es el conjunto de productos vendidos al
mismo `customer` el mismo `date`. Las ventas sin cliente no forman canasta. Es la misma definición que usa el frontend para calcular
combos en el navegador (`lib/sistema/alertas.ts`). Un producto anotado varias veces en la misma canasta cuenta una vez.

## COMBO · Combos de productos de baja rotación (`ReglaCombo`)

Avisa de dos productos de la misma tabla de inventario que estuvieron juntos en una canasta al menos
`ensambla.avisos.combo-minimo-veces` veces (3 por defecto) dentro del período, y de los que **al menos uno rota poco**.

- **Precio del combo**: la suma de los dos precios por 0,9 (10 % menos), redondeada a la decena; si no queda por debajo de la suma, no hay
  aviso. El costo es la suma de los costos cargados (se omite si no hay ninguno).
- **No avisa** si el inventario ya tiene un producto «combo» que nombra a los dos (por la primera palabra significativa de cada nombre).
- La entidad es `combo:<idA>:<idB>` (ids ordenados) y los datos son `{precioA, precioB, sugerido}`: las veces que salieron juntos no
  entran en la huella.
- La acción «Crear el combo» es un `CREAR_REGISTRO`: `AccionAvisoService` toma la primera acción `EDITAR_REGISTRO` o `CREAR_REGISTRO` y,
  para crear, traduce los valores por clave de campo (`name`, `quantity`, `price`, `cost`) y crea la fila en la tabla de la acción. Una
  tabla de otro negocio no devuelve campos y se rechaza. Al correr de nuevo el motor, el combo ya existe y el aviso vence.
- En el demo (V10) alfajor y galletitas salen 4 veces juntos; con el factor 0,5 ambos quedan justo en el borde de baja rotación (4
  unidades contra una mediana de 8 del resto), por eso el factor no se tocó.
