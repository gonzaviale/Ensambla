# Métricas a medida: investigación y decisión (T-171, ENS-259)

Rama: `feat/ENS-253-metricas-a-medida` · Fecha de la decisión: 2026-11-19

## 1. La pregunta y la recomendación

**La pregunta:** ¿cómo dejamos que cada negocio arme sus propias métricas ("¿cuánto vendí esta semana?", "¿qué producto
vendo más?") sin que el número pueda salir mal o inventado?

**La recomendación:** el número lo calcula **siempre el servidor**, a partir de una **receta declarativa**. La receta no
es una fórmula: son cinco datos elegidos de listas cerradas.

| Dato | Qué dice | Valores posibles |
|---|---|---|
| `fuente` | de qué tabla sale | `VENTAS`, `GASTOS`, `INVENTARIO` |
| `calculo` | qué cuenta se hace | `SUMA`, `PROMEDIO`, `CONTEO`, `UNIDADES` |
| `agrupar` | cómo se reparte el número | `NINGUNA`, `PRODUCTO`, `CLIENTE`, `CATEGORIA`, `DIA_DE_LA_SEMANA` |
| `periodo` | qué días mira | `HOY` (1 día), `SEMANA` (7), `MES` (30), `TODO` |
| `soloFiado` | sólo las ventas fiadas | `true` / `false` (sólo para ventas) |

Como todo sale de listas cerradas, nadie (ni la persona ni un programa) puede pedir algo que el servidor no sepa
calcular: la receta se valida (`DefinicionMetrica.validar()`) y, si no tiene sentido, se rechaza con un motivo claro.

**Lo que comparamos:**

- **Que la IA escriba la fórmula o la consulta (SQL, JS, una expresión).** Descartado. Es riesgoso: puede leer datos de
  otro negocio, hacer una cuenta que parece bien y está mal, o colgar el servidor. Además, probar que una fórmula
  libre es correcta es mucho más difícil que probar cinco valores de una lista.
- **Un catálogo fijo y nada más.** Seguro y barato, pero rígido: la persona sólo puede elegir lo que pensamos nosotros.
- **Receta declarativa (lo elegido).** Tiene la seguridad del catálogo y deja combinar libremente las piezas.

## 2. La decisión final del equipo

Decidido por Tomy el **2026-11-19**: por **costo** y porque **ya usamos IA en otras secciones** de la app, esta parte se
hizo **sin IA**.

- **Catálogo por rubro** en `backend/src/main/resources/metricas/catalogo-metricas.json`: las métricas que se sugieren
  según el rubro del negocio (`GET /api/proyectos/{id}/metricas/sugeridas`).
- **"Pedila con tus palabras":** la persona escribe lo que quiere y un **diccionario de frases**
  (`backend/src/main/resources/metricas/diccionario-pedidos.json`) la traduce a una receta. Endpoint:
  `POST /api/proyectos/{id}/metricas/interpretar`. Es un algoritmo fijo (`InterpreteDePedidos`): busca qué tabla, qué
  cuenta, qué reparto y qué período menciona el texto; nunca pone un número.
- La receta que propone se muestra con su resultado (`/previsualizar`) y la persona decide si la guarda.
- **T-173 (herramienta de Sami)** queda **afuera por ahora**.

## 3. Qué se puede calcular hoy y qué no

El motor (`MotorMetricas`) **lee los datos por la clave semántica del campo** (`price`, `quantity`, `credit`, `cost`,
`name`, `category`, `date`, `total`), no por la etiqueta de la columna. Así no importa si en un cuaderno la columna se
llama "Precio" y en otro "Valor". Las filas de ejemplo quedan afuera.

**Se puede:**

| Tabla | Cálculos | Repartos | Período |
|---|---|---|---|
| Ventas | suma (precio × cantidad), promedio, conteo, unidades | producto, cliente, día de la semana | sí |
| Gastos | suma (total), promedio, conteo — **no** unidades | categoría, día de la semana | sí |
| Inventario | suma (cantidad × costo), promedio, conteo, unidades | producto | **no**: siempre "todo" (es lo que hay hoy) |

Además: lo fiado (`soloFiado`) sólo en ventas; un reparto muestra hasta 10 partes, las más grandes primero (el día de la
semana va de lunes a domingo); y un cuaderno guarda hasta 12 métricas, sin repetir recetas.

**No se puede (y por qué):**

- **Ventas por categoría:** el inventario no tiene campo de categoría.
- **Ventas por hora:** la fecha de la venta es sólo el día, sin hora.
- **Clientes nuevos:** la tabla de clientes no tiene fecha de alta.
- **Margen histórico:** el costo que guardamos es el actual; no sabemos cuánto costaba el producto el día que se vendió.

**Hay que tener en cuenta:**

- Las ventas se guardan **una fila por producto, sin número de ticket**. Por eso "contar ventas" cuenta renglones y el
  **promedio por venta es aproximado** (promedia renglones, no tickets).
- Los períodos son días corridos hasta hoy (hora argentina): "este mes" son los últimos 30 días, no el mes calendario.

## 4. El catálogo por rubro

Todos los negocios reciben la **base**:

- Lo que vendiste esta semana (ventas · suma · semana)
- Lo que gastaste este mes (gastos · suma · mes)
- Lo que te deben (ventas · suma · por cliente · todo · sólo fiado)
- Producto más vendido del mes (ventas · unidades · por producto · mes)

Y además, según el rubro:

| Rubro | Métricas extra |
|---|---|
| Almacén | Ventas por día de la semana · Gastos por categoría del mes · Plata parada en mercadería · Importe promedio por venta |
| Kiosco | Ventas por día de la semana · Cantidad de ventas de hoy · Ventas por producto del mes · Gastos por categoría del mes |
| Minimercado | Ventas por producto del mes · Clientes que más te compraron · Unidades en stock · Plata parada en mercadería |
| Dietética | Ventas por producto del mes · Clientes que más te compraron · Unidades en stock por producto · Plata parada por producto |
| Papelería | Ventas por día de la semana · Ventas por producto del mes · Gastos por categoría del mes · Plata parada en mercadería |

Sólo se sugieren las que el cuaderno todavía no tiene.

## 5. Los pedidos de ejemplo (tests de `InterpreteDePedidos`)

Los diez pedidos de un almacén que el test comprueba:

| Pedido | Receta que arma |
|---|---|
| ¿Cuánto vendí esta semana? | ventas · suma · sin repartir · semana |
| qué producto vendo más este mes | ventas · unidades · por producto · mes |
| ¿Cuánto me deben por cliente? | ventas · suma · por cliente · todo · sólo fiado |
| En qué gasté más este mes | gastos · suma · por categoría · mes |
| cuántas ventas hice hoy | ventas · conteo · sin repartir · hoy |
| Venta promedio de esta semana | ventas · promedio · sin repartir · semana |
| ¿Cuánto stock me queda por producto? | inventario · unidades · por producto · todo |
| ¿Qué día de la semana vendo más? | ventas · suma · por día de la semana · mes |
| cuánto gasté en total | gastos · suma · sin repartir · todo |
| ¿Cuánto vale mi mercadería? | inventario · suma · sin repartir · todo |

Cómo decide:

- No importan las tildes, las mayúsculas, los plurales ni los signos. El nombre sugerido es el pedido sin signos en los
  bordes, con mayúscula y hasta 80 letras.
- Lo que el pedido no dice toma el valor de siempre: **sumar, sin repartir y el último mes** ("ventas" → ventas · suma ·
  mes). Lo fiado y el inventario, sin período dicho, miran **todo**.
- Si hay varias coincidencias, gana la primera en el orden de cada lista (por ejemplo, "más vendido" da unidades por
  producto).

Lo que **no entiende**:

- Un texto que no nombra ventas, gastos ni stock ("hola, ¿cómo andás?"): responde *"No me di cuenta de qué querés
  mirar. Decime si es de tus ventas, tus gastos o tu stock…"*.
- Una combinación que no se puede calcular ("gastos por cliente"): responde *"No se puede repartir gastos de esa
  manera"*.
- Cualquier frase que no esté en el diccionario: sinónimos nuevos, regionalismos, errores de tipeo ("bendí"), cosas
  de la sección 3 que no se pueden calcular (por hora, por categoría de producto, margen) o comparaciones ("esta semana
  contra la pasada").

## 6. "De dónde sale" y seguridad

- **"De dónde sale":** cada resultado trae la tabla de la que salió, cuántas filas entraron en la cuenta, cuántas quedaron
  afuera por no tener el dato (por ejemplo, ventas sin precio) y las fechas desde/hasta del período.
- **Nunca un cero inventado:** si falta la tabla, no hay filas en el período o ninguna fila tiene el dato, el resultado
  viene **sin valor y con el motivo** ("No hay ventas en este período.", "Cargá el precio de tus ventas para poder
  calcularlo.").
- **Aislamiento por negocio:** el negocio sale de la sesión; pedir las métricas de un cuaderno ajeno responde **404**,
  igual que en el resto de la app.
- **Límite del pedido:** el texto de `/interpretar` es obligatorio y tiene **hasta 200 caracteres**.
- El servidor nunca ejecuta lo que escribe la persona: sólo busca frases en una lista.

## 7. Costo

- **Referencia (borrador):** si lo hiciera la IA, un pedido con Haiku 4.5 costaría unos **US$ 0,001 a 0,003**. Es una
  **estimación, no lo medimos**.
- **Sin IA, el costo por pedido es cero**: es una búsqueda de frases en memoria.

## 8. Riesgos y cómo ampliarlo

**Riesgos:**

- El diccionario no entiende todo: algunas personas van a escribir cosas que no reconoce. El mensaje les sugiere cómo
  decirlo.
- Una frase ambigua puede dar una receta distinta a la esperada. Por eso siempre se muestra la vista previa antes de
  guardar.
- El promedio por venta es aproximado mientras no haya número de ticket.

**Cómo ampliarlo:**

- **Frases nuevas:** agregarlas a `diccionario-pedidos.json` (fuentes, cálculos, agrupaciones, períodos o fiado) y sumar
  el pedido de ejemplo al test de `InterpreteDePedidos`. No hace falta tocar código.
- **Métricas sugeridas nuevas:** agregarlas a `catalogo-metricas.json`, en la base o en el rubro.
- **Cálculos nuevos** (por hora, por categoría de producto, margen): primero hay que guardar el dato que falta (hora,
  categoría, costo del día) y después sumar el valor a la lista correspondiente.
- **Si en el futuro se usa IA**, que **sólo complete la receta** (elegir entre los valores de las listas), nunca que
  escriba fórmulas ni consultas, y que el servidor la siga validando y calculando igual que hoy.
