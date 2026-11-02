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

## PRECIO_ZONA · Precio frente a la competencia de la zona (`ReglaPrecioZona`)

Compara el precio de cada producto con el **rango** (mínimo y máximo) que cobran los comercios de la zona por el mismo producto
(mismo nombre, sin importar tildes ni mayúsculas). Si el precio queda fuera del rango, avisa:

- **Por debajo del mínimo:** muestra el rango y la diferencia ($ y %), y sugiere **revisar costos y margen** (con el costo cargado,
  dice cuánto queda por unidad o cuánto se pierde). No cambia el precio: sólo ofrece «Ver producto».
- **Por encima del máximo:** muestra el rango y la diferencia y sugiere **bajarlo**. La acción «Bajar a $X» es un `EDITAR_REGISTRO`
  que cambia `price` al máximo de la zona redondeado a la decena (o al máximo exacto si el redondeo no lo deja por debajo del precio actual).

**Anonimato:** con menos de 3 comercios en la zona (`AnalisisZona.MINIMO_COMERCIOS`) o menos de 3 que cobren ese producto no se calcula
ni se muestra rango. Los textos sólo traen el mínimo, el máximo y la cantidad de comercios. Se compara aunque el producto no tenga
stock ni ventas. La entidad es `registro:<id>` y los datos son `{precio, minimo, maximo}`: si cambia el precio o el rango de la zona,
un aviso descartado vuelve a aparecer. Puede convivir con `ESTANCADO` sobre el mismo producto (cada tipo explica su causa).

## MUY_VENDIDO · Producto de alta rotación con precio bajo (`ReglaProductoAltaRotacion`)

Avisa de un producto que **se vende más que de costumbre** y está **por debajo del rango de la zona**:

- Las unidades del período (`periodo-dias`) superan su promedio por período (`MetricaProducto.promedioPorPeriodo`, calculado desde la
  primera venta; con menos de un período de historia el promedio es igual a lo vendido y no avisa).
- Su precio es menor al **mínimo** que cobran los comercios de la zona (mismo nombre, sin importar tildes ni mayúsculas), con el mínimo de
  3 comercios en la zona y en el producto (`AnalisisZona.productoConRango`).

El aumento sugerido es el mínimo de la zona redondeado a la decena (o el mínimo exacto si el redondeo no lo deja por encima del precio
actual). El aviso muestra las ventas del período, el promedio, el precio actual y el rango de la zona. La acción «Subir a $X» es un
`EDITAR_REGISTRO` que cambia `price`. La entidad es `registro:<id>` y los datos son `{precio, sugerido}`.

## COMBO · Productos que se llevan juntos y rotan poco (`ReglaComboBajaRotacion`)

Propone armar un combo con dos productos que **se venden juntos** y de los que **al menos uno rota poco**:

- Una **canasta** es lo que un mismo cliente se llevó el mismo día (las ventas no tienen ticket; sin cliente no hay canastas). Se cuentan
  las canastas del último período (`periodo-dias`).
- El par cuenta si apareció en al menos `combo.frecuencia-minima` canastas y uno de los dos tiene baja rotación (`MetricasVentas.esBajaRotacion`).
- Los dos tienen que estar en la misma tabla de inventario, con precio. Si el inventario ya tiene un producto cuyo nombre contiene «combo» y
  los nombres de los dos, no se propone de nuevo.
- Precio sugerido: la suma de los precios menos `combo.descuento`, redondeada a la decena; si no queda por debajo de la suma, no avisa.
  El costo del combo es la suma de los costos, sólo si los dos lo tienen.

La acción «Crear el combo» es un `CREAR_REGISTRO`: al aplicarla, `AplicarAviso` suma a la tabla de inventario una fila «Combo A + B» con
cantidad 0, el precio sugerido y el costo (con las mismas validaciones y el mismo límite de filas del plan que cargarla a mano; la fila queda
con origen «Aviso de Sami»). La entidad es `combo:<idA>:<idB>` (ids ordenados) y los datos `{precioA, precioB, sugerido}`: las canastas no
entran en la huella.

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `ensambla.avisos.combo.frecuencia-minima` | `3` | Canastas en las que tiene que aparecer el par |
| `ensambla.avisos.combo.descuento` | `0.1` | Fracción que se descuenta de la suma de los precios (0,1 = 10 %) |

## Historial de precio y REVISAR_PRECIO · Las ventas cayeron después de un aumento (`ReglaRevisarPrecioTrasAumento`)

Cada vez que cambia el precio (`price`) de un producto **del inventario**, `RegistroDatosService.editar` anota el cambio en la tabla
`historial_precio` (precio anterior y nuevo, con la fecha). Sólo se anota lo que pasa por ahí (editar una fila, aplicar un aviso); no hay
historial de lo anterior a esta función. Las filas de ventas, aunque tengan precio, no se anotan.

La regla toma el **último** cambio de cada producto y avisa si:

- Fue un **aumento** de los últimos `periodo-dias` días (el primer precio que se carga no es un aumento).
- Ya pasó la ventana posterior (`revision-precio.ventana-dias`): no se puede comparar antes.
- Las unidades por día de la ventana posterior cayeron al menos `revision-precio.caida-minima` respecto de la ventana anterior
  (mismo cálculo que el impacto de un aviso, `ImpactoAviso.medir`) y antes se vendía.

La acción «Volver a $anterior» es un `EDITAR_REGISTRO`; al aplicarla queda anotada una baja, el último cambio deja de ser un aumento y el
aviso vence solo. La entidad es `registro:<id>` y los datos `{precioAnterior, precioNuevo, fecha}`: un aviso descartado sólo vuelve si hay un
aumento nuevo. Es un tipo propio (`REVISAR_PRECIO`) porque el motor vence los avisos por tipo y entidad, y dos reglas del mismo tipo se
pisarían.

**No es el impacto de los avisos:** el impacto (más abajo) mide después de aplicar un aviso de stock o estancado y nunca genera un aviso;
esta regla genera un aviso por cualquier aumento de precio, venga de un aviso o de una edición a mano.

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `ensambla.avisos.revision-precio.ventana-dias` | `7` | Días antes y después del aumento que se comparan |
| `ensambla.avisos.revision-precio.caida-minima` | `0.2` | Fracción mínima que tienen que caer las unidades por día (0,2 = 20 %) |

## Impacto de los avisos aplicados (`MedirImpactoAvisos`)

Cuando la persona aplica un aviso de `STOCK` o de `ESTANCADO` sobre un producto, el servidor mide después qué pasó con las ventas de
ese producto. Un worker (`ImpactoAvisosWorker`, cada 6 horas) busca los avisos `APLICADO` que todavía no se midieron y, cuando ya pasó
la ventana completa, compara:

- **Antes:** los `ventana-dias` días anteriores al día en que se aplicó.
- **Después:** los `ventana-dias` días que empiezan ese día. El día de hoy no cuenta mientras la ventana siga abierta.

Calcula unidades e ingreso por día en cada lado y guarda el resultado en `datos.impacto` del aviso, sin cambiar su huella ni su estado
(no hay migración). Las ventas sin precio cuentan en las unidades pero no en el ingreso. `GET /api/avisos/impacto` devuelve los
aplicados y descartados, el cambio de ingreso semanal total y el detalle de cada aviso medido.

**Es una comparación, no una causa.** Otras cosas también mueven las ventas (la época del año, un feriado, un faltante). Por eso la
pantalla dice «vendés $X más por semana que antes de aplicar los avisos» y nunca atribuye ese dinero al aviso.

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `ensambla.avisos.impacto.habilitado` | `true` | Prende o apaga el worker (`ENSAMBLA_AVISOS_IMPACTO`) |
| `ensambla.avisos.impacto.intervalo` | `21600000` | Milisegundos entre mediciones (`ENSAMBLA_AVISOS_IMPACTO_INTERVALO`) |
| `ensambla.avisos.impacto.ventana-dias` | `14` | Días antes y días después que se comparan (`ENSAMBLA_AVISOS_IMPACTO_VENTANA_DIAS`) |

## Qué ve la persona: avisos del servidor y del navegador

El frontend (`useMotorDeAvisos`) junta los avisos del servidor con los que Sami calcula en el navegador. Para no duplicar ni resucitar
avisos descartados:

- Con el **backend real** (`VITE_API_MOCKS` distinto de `completo`), los tipos `stock`, `estancado`, `precio_zona`, `muy_vendido`, `revisar_precio` y `combo` son **siempre del servidor**, también
  mientras todavía no contestó: el navegador no lo calcula. Así un aviso descartado o aplicado no reaparece como copia local, y no hay un
  parpadeo de avisos duplicados al abrir la pantalla.
- Los demás tipos que el servidor trae (por ejemplo `stock`) también se omiten en el navegador mientras haya alguno pendiente.
- En **modo demo** (todo simulado) o si el servidor **falla**, el navegador sigue calculándolos él mismo.
