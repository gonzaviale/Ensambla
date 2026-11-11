# Panel de la home: métricas, flujo de caja y sugerencias

Los números que la home le muestra a quien ya tiene un cuaderno con datos: cuánto vendió, cuánto le queda y cómo se movió la plata
semana a semana. Se calculan en el servidor con las mismas tablas y las mismas reglas que el [resumen en PDF](resumen-negocio.md), con las
diferencias que se explican abajo.

**Qué muestra hoy la home.** La bienvenida compacta arriba; después el tablero «Cómo te va» (lo vendido y el aviso económico, con el selector
de cuaderno: `ObtenerResumenInicio` y `ObtenerAvisoEconomico`); y debajo «Tu plata y lo que conviene hacer», con las sugerencias de Sami y el
flujo de caja de este endpoint, del mismo cuaderno que se eligió arriba. Las métricas de este endpoint (`vendiste`, `queda`, `teDeben`,
`margen`…) siguen en la respuesta pero la home ya no las dibuja: para vendido y rentabilidad manda el tablero.

## Cómo se pide

`GET /api/negocio/panel?proyectoId=<id>&dias=30`

- `proyectoId` es el sistema (cuaderno) que se quiere mirar. Tiene que ser del negocio de la sesión; uno ajeno responde 404, igual que uno
  que no existe. El negocio sale siempre de la sesión.
- `dias` son los últimos días contando hoy. Sin valor son 30; tiene que ser mayor a cero y no pasar de 366 (si no, 400).
- El «hoy» y las fechas son los de Argentina (`ZonaNegocio`). No es una función de plan: lo ven todos los planes.

## Qué devuelve

| Campo | Qué es |
|---|---|
| `desde`, `hasta`, `dias` | El período mirado |
| `hayDatos` | `false` si el período no tiene ventas |
| `metricas` | Ver abajo; no viene si no hay datos |
| `flujoDeCaja` | Una entrada por semana; vacío si no hay datos |

Sin ventas en el período la respuesta es explícita: `hayDatos: false`, sin métricas y sin semanas. No se manda un panel de ceros. Los campos
que no se pueden calcular no vienen en el JSON (el front los trata como «sin dato»).

### Métricas

| Métrica | Cómo se calcula |
|---|---|
| `vendiste` | Precio por cantidad de todas las ventas del período con precio, también las fiadas. Es el «ingresos» del PDF |
| `mercaderia` | Lo comprado en el período (ver abajo). No viene si no hay tabla de entradas ni de gastos |
| `gastos` | Gastos del período **sin** la categoría «Mercadería». No viene si no hay tabla de gastos con importe |
| `queda` | Lo cobrado menos gastos y mercadería. Es el saldo con el que cierra el flujo de caja |
| `teDeben` | Lo vendido a fiado en el período (campo `credit` de las ventas) |
| `margen` | Ganancia sobre lo vendido, en %, **solo de los productos que tienen costo** (los demás no entran ni arriba ni abajo). No viene si ninguno lo tiene |
| `ticketPromedio` | Lo vendido dividido las ventas **con precio**. No viene si ninguna tiene precio |
| `variacion` | Cuánto cambió `vendiste` contra el período anterior de igual largo, en %. No viene si antes no hubo ventas |

`vendiste`, `teDeben` y `variacion` salen de `ResumenNegocio`, la misma clase que arma el PDF, y coinciden con él.

## Flujo de caja

El spec del laboratorio dice que un flujo de caja «real» necesita un circuito de cobros y pagos que todavía no existe. Esta versión se arma
con lo que ya se carga:

- **Entradas:** ventas con precio que no están marcadas como fiado.
- **Salidas:** los gastos (sin «Mercadería») más las compras de mercadería.
- **Saldo** de cada semana: entradas menos salidas. **Saldo acumulado:** lo que lleva el período hasta esa semana. El último coincide con `queda`.
- Las semanas son de 7 días corridos desde el primer día del período; la última puede ser más corta.

### Compras de mercadería (y por qué «Mercadería» no es un gasto)

Una misma compra puede estar anotada de dos maneras: como una **entrada** de inventario o como un **gasto** de categoría «Mercadería» (la
plantilla de gastos la trae). Si se sumaran las dos, la compra se restaría dos veces. Por eso:

- Si el sistema tiene tabla de **entradas**, las compras salen de ahí. La tabla guarda producto, cantidad y fecha, pero no el precio de
  compra: cada entrada se valúa con el **costo actual** del producto (cantidad por costo). No se cuenta si el producto no se encuentra o no
  tiene costo, si la cantidad no es positiva o si la fecha falta o cae fuera del período.
- Si **no** tiene tabla de entradas, las compras son los gastos de categoría «Mercadería».
- En ningún caso esa categoría se suma como gasto. Por eso `gastos` puede ser menor que el total de gastos del PDF, que los suma todos.

## Límites que hay que tener presentes

- **El fiado nunca pasa a ser entrada.** No hay forma de registrar el cobro de una venta fiada, así que se informa aparte en `teDeben` y no
  entra al flujo. Cuando exista un circuito de cobros, el cobro tendría que sumar a las entradas de la semana en que se cobra.
- **Un fiado de un período anterior que se cobró en este tampoco se ve**, por lo mismo.
- **La mercadería es una aproximación.** Si el costo de un producto cambió desde que se compró, la compra se valúa al costo de hoy.
- **`queda` no es el «resultado» del PDF.** El PDF resta solo los gastos (todos) a lo vendido; `queda` resta además la mercadería, no cuenta dos
  veces la categoría «Mercadería» y deja afuera lo fiado.
- **Home y Objetivos todavía no dan lo mismo.** «Cómo viene tu negocio» en Objetivos se calcula en el navegador y usa el costo de lo vendido en
  lugar de lo comprado; unificarlos es parte de ENS-249.
- Las ventas sin precio cuentan en la cantidad de ventas pero no en el dinero.
- Las filas de ejemplo de las plantillas no cuentan.

## Sugerencias de Sami en la home

Debajo de las cifras, «Para decidir hoy» muestra hasta tres sugerencias, una línea cada una, con el botón de lo que propone. Salen del motor de
avisos que ya corre en el navegador (los del servidor más los que calcula Sami, con el límite del plan aplicado): no hay llamada nueva.

- Se elige la más urgente de cada tema que pide la tarjeta: **algo que falta** (stock), **un producto que no se mueve** (estancado) y **un
  precio frente a la zona** (precio frente a la zona, «se vende mucho» o revisar precio). Si algún tema no tiene avisos, se completa con los
  siguientes más urgentes. Así, tres faltantes de stock no tapan a los demás.
- «Ver por qué» abre el detalle y, dentro, «No me interesa, descartar»: descartar queda un toque más adentro para no hacerlo sin querer.
- «Ver las otras N» lleva a la pantalla de Avisos. Sin avisos la sección no aparece.
- `lib/sistema/sugerenciasDeLaHome.ts` elige las sugerencias; `components/inicio/DecisionesRapidas.tsx` las dibuja.

## Dónde está en el código

- Backend: `domain/model/reporte/PanelNegocio` (cálculo puro), `BloqueEntradas`, `PeriodoResumen.ultimosDias`,
  `domain/service/reporte/DatosDeProyectoService` (carga también las entradas), `domain/useCase/reporte/obtenerPanelNegocio/*`,
  `PanelNegocioController` y `PanelNegocioPresentationMapper`. La prueba de punta a punta con base real es `PanelNegocioIntegrationTest`.
- Front: `components/inicio/*` (`PanelDelNegocio`, `FlujoDeCajaPorSemana`, `DecisionesRapidas`), `components/sistema/MetricasDelInicio`, `api/panel.ts`, `hooks/panel/queries.ts`,
  `lib/sistema/panelDelNegocio.ts` (qué cuaderno mirar y el formato de pesos y porcentajes) y, para el modo demo, `mocks/db/panel.ts`.
