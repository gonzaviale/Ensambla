# Carteles y promociones con la marca del negocio (H-62 · ENS-360)

Diseño de la historia (T-262 · ENS-361). Es la fuente para las demás subtareas: kit de marca (T-263), plantillas y motor
de render (T-264), textos con IA (T-265), qué promocionar (T-266), pantalla (T-267), fotos (T-268), descarga y
compartir (T-269), variante libre (T-270) y pruebas (T-271).

## Idea de fondo: que la IA decida lo menos posible

Un modelo que diseña con libertad cae siempre en los mismos patrones y todos los carteles se parecen. Por eso el reparto
es fijo:

| Quién | Qué decide | Qué no decide |
|---|---|---|
| **Nosotros** (plantillas) | disposición, jerarquía, márgenes, tamaños de letra, armonía | — |
| **El negocio** (kit de marca) | logo, 2 colores, tipografía, tono | — |
| **El sistema** (datos reales) | productos, precios, descuentos, fechas, horarios, fotos | — |
| **La IA** | qué plantilla conviene, un titular y una bajada cortos, el texto del posteo | cualquier número, color, fuente, medida o imagen |

Si el texto de la IA trae un número que no coincide con los datos, se descarta y se usa el texto de reserva de la
plantilla. Claude no genera imágenes: las fotos salen del catálogo (incluidas las del escáner) o de lo que suba la persona.

## Formatos de salida

Cada cartel se dibuja una sola vez como composición vectorial y se exporta a cuatro formatos. La plantilla declara cómo
se acomoda en cada uno; no se escala a ojo.

| Formato | Medida | Uso | Margen seguro |
|---|---|---|---|
| `A4` | 210 × 297 mm (PDF) | imprimir y colgar | 10 mm |
| `A5` | 148 × 210 mm (PDF) | mostrador, vidriera chica | 8 mm |
| `CUADRADO` | 1080 × 1080 px (PNG) | feed de redes | 60 px |
| `VERTICAL` | 1080 × 1350 px (PNG, 4:5) | feed vertical y WhatsApp | 60 px |

- Los PDF llevan el margen de impresión: nada importante (texto, logo, precio) cae fuera del margen seguro, que es
  más ancho que la zona que las impresoras hogareñas no alcanzan.
- Orientación: A4 y A5 son verticales. Una plantilla que no tenga composición para un formato no se ofrece en él.
- Se comparte por WhatsApp (el PNG vertical o cuadrado más el texto del posteo) y el texto del posteo se copia con un botón.
- Fuera de alcance: publicar directo en Instagram o Facebook, video, quitar fondos y generar imágenes con IA.

## Kit de marca

Un kit por negocio (T-263). Todo lo que la persona puede elegir es un conjunto cerrado, así no puede romper la armonía.

| Elemento | Regla |
|---|---|
| Logo | PNG o JPG, hasta 2 MB; opcional. Sin logo, se usa el nombre del negocio en tipografía. |
| Color principal y secundario | hex de 6 dígitos. Se **sugieren desde el logo sin IA**: se cuantizan los píxeles, se descartan blancos, negros y grises, y se eligen los dos tonos más frecuentes y más separados entre sí. |
| Tipografía | una de una lista cerrada (ver abajo); por defecto Plus Jakarta Sans. |
| Tono | `CERCANO`, `PROFESIONAL` o `DIVERTIDO`. Lo lee la IA para escribir; no cambia el diseño. |

Tipografías permitidas (todas autoalojadas, ninguna por CDN): Plus Jakarta Sans, Montserrat, Poppins, Playfair Display,
Roboto Slab, Nunito.

### Colores derivados

De los dos colores del negocio el sistema calcula el resto, siempre igual:

- **Tinta:** el texto principal. Es el oscuro de la paleta de Ensambla (Pizarra `#263443`) o blanco, el que dé más contraste
  sobre el fondo del bloque.
- **Fondo:** blanco Papel `#FFFDFA` o el color principal, según la variante.
- **Acento:** el color secundario.
- Si un color del negocio no alcanza el contraste mínimo con la tinta, se **oscurece o aclara en pasos** (misma tonalidad)
  hasta alcanzarlo. La persona ve el color ajustado y el motivo.

## Reglas de armonía

Reusan el criterio de `feat/armonia-visual-paleta` (`frontend/src/styles/sistema/armonia.css`) aplicado al cartel:

1. **60-30-10.** 60 % fondo (Papel o color principal muy dominante), 30 % color principal, 10 % acento (el precio o el
   llamado a la acción). Un solo elemento lleva el acento.
2. **Una jerarquía por cartel:** el protagonista (el precio o el titular) ocupa al menos el doble de altura de letra que el
   segundo nivel; los niveles son 3 como máximo (protagonista, apoyo, detalle).
3. **Contraste:** texto normal ≥ 4,5 : 1; texto grande (≥ 24 pt en impreso o ≥ 32 px en pantalla) ≥ 3 : 1. Se mide con la
   fórmula WCAG de luminancia relativa.
4. **Tamaño mínimo:** nada de texto informativo menor a 9 pt en A4/A5 ni a 28 px en redes.
5. **Márgenes y alineación:** una retícula de 12 columnas por formato, un único eje de alineación (izquierda o centro) y
   el mismo margen exterior en todos los lados. Los espacios son múltiplos de una unidad base.
6. **Máximo 2 familias de letra** (la del negocio y, en la variante con serif, una de apoyo); sin más de 2 pesos por cartel.
7. **Sin efectos:** sin sombras, degradados ni contornos de texto. Formas planas, igual que la marca de Ensambla.
8. **Texto largo no rompe nada:** cada campo tiene un máximo de caracteres y, si lo supera, se achica hasta un piso y
   después se corta con puntos suspensivos; nunca se superpone.
9. **Sin foto, funciona:** cada plantilla trae su composición tipográfica; la foto es un extra.

Las reglas 3, 4 y 8 se verifican automáticamente (T-271); las demás las garantiza la plantilla por construcción.

## Plantillas

Seis plantillas y tres variantes de composición por cada una (18 composiciones). Las 3 opciones que ve la persona
son siempre de **plantilla elegida con variantes distintas** y se arman con la regla de variedad de abajo.

| Código | Para qué | Datos que usa | Foto |
|---|---|---|---|
| `PROMO_PRECIO` | un producto con precio rebajado | producto, precio anterior, precio nuevo, porcentaje, vigencia | sí |
| `DOS_POR_UNO` | llevá 2 y pagá 1 (o NxM) | producto, precio unitario, cantidades, vigencia | sí |
| `COMBO` | varios productos a un precio | 2 a 4 productos, precio suelto, precio del combo, ahorro | hasta 4 |
| `NOVEDAD` | producto o servicio nuevo | producto, precio, fecha de ingreso | sí |
| `HORARIOS` | días y horarios de atención | horarios del negocio, dirección | no |
| `LISTA_PRECIOS` | lista de hasta 10 productos | productos con precio | no |

### Variantes de composición

Cada plantilla tiene tres, nombradas por su estructura y no por su look:

| Variante | Estructura |
|---|---|
| `BLOQUE` | un gran bloque de color principal con el protagonista adentro; el resto sobre fondo claro |
| `FRANJA` | fondo claro con una franja diagonal u horizontal del color principal que atraviesa el cartel |
| `MARCO` | fondo del color principal con una tarjeta clara centrada que contiene la información |

### Regla de variedad (las 3 opciones)

Dadas las 3 opciones que se muestran:

1. Las tres tienen **variantes distintas** (`BLOQUE`, `FRANJA`, `MARCO`).
2. Al menos una **intercambia** qué color es fondo y cuál acento.
3. Si la plantilla admite foto, al menos una usa la foto grande y otra la usa chica; las tres funcionan igual sin foto.
4. Las tres comparten marca, datos y textos de la IA; sólo cambia la composición. Eso es lo que hace que sean
   coherentes con la marca y distintas entre sí.

## Qué calcula el sistema y qué escribe la IA

### El sistema (nunca la IA)

- Precio anterior, precio nuevo, descuento en pesos y en porcentaje (redondeado a entero), con `BigDecimal`.
- Para 2x1 / NxM: precio por unidad efectivo y ahorro.
- Combo: suma de precios sueltos, precio del combo, ahorro.
- Fechas de vigencia (desde, hasta) en formato `dd/MM` con el huso del negocio, y los horarios.
- Elección de **qué promocionar** a partir de los datos (T-266): productos con stock alto o poca rotación, precios
  recientemente bajados, productos nuevos del catálogo.

### La IA (T-265)

Recibe un pedido chico y devuelve **un JSON**; se lo valida contra un esquema. Sólo propone:

- `plantilla`: una de las seis, entre las que el sistema le ofrece como posibles para ese pedido.
- `titular`: hasta 40 caracteres.
- `bajada`: hasta 90 caracteres.
- `textoPosteo`: hasta 280 caracteres, para copiar en redes.

### Esquema de lo que devuelve la IA

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "additionalProperties": false,
  "required": ["plantilla", "titular", "bajada", "textoPosteo"],
  "properties": {
    "plantilla": {
      "type": "string",
      "enum": ["PROMO_PRECIO", "DOS_POR_UNO", "COMBO", "NOVEDAD", "HORARIOS", "LISTA_PRECIOS"]
    },
    "titular": { "type": "string", "minLength": 1, "maxLength": 40 },
    "bajada": { "type": "string", "minLength": 1, "maxLength": 90 },
    "textoPosteo": { "type": "string", "minLength": 1, "maxLength": 280 }
  }
}
```

### Validación del texto de la IA

Se aplica a `titular`, `bajada` y `textoPosteo`; si falla **cualquiera**, se descarta todo y se usa el texto de reserva
de la plantilla (`"¡Oferta en {producto}!"`, `"Llevate {producto} a {precio}"`…).

1. **Números:** se extraen todos los números del texto (con o sin `$`, `%`, `x`, separadores de miles y decimales). Cada
   uno debe estar en el conjunto de números permitidos que arma el sistema para ese cartel (precios, porcentajes,
   cantidades, días y horas). Un número que no está, o que está con otro valor, descarta el texto.
2. **Sin marcado:** nada de HTML, enlaces, `@`, ni caracteres de control.
3. **Largos** dentro de los máximos del esquema.
4. **Idioma:** español rioplatense; el tono viene del kit.
5. **El texto de la persona es dato:** si el nombre de un producto contiene instrucciones («ignorá lo anterior…»), se
   envía entre delimitadores como valor y el prompt dice explícitamente que son datos, no órdenes.

## Qué datos se mandan a la IA y cuáles no

| Se manda | No se manda nunca |
|---|---|
| Rubro del negocio | Nombres, teléfonos, direcciones o mails de clientes |
| Nombre comercial del negocio | Deudas, fiados, saldos o datos de pago |
| Tono del kit de marca | Ventas, ganancias, costos y márgenes |
| Plantillas posibles para el pedido | Dirección del negocio ni coordenadas |
| Nombre de cada producto a promocionar | Logo, fotos ni colores (el diseño no pasa por la IA) |
| Los números ya calculados (precios, porcentajes, vigencias) | Identificadores internos (`NegocioId`, `UsuarioId`) |

La IA recibe los números calculados para poder nombrarlos en el texto, pero no los puede cambiar: la validación de
arriba lo comprueba. El pedido nunca incluye datos de personas.

## Cuota

La cuota de carteles con IA es **aparte** de la de objetivos y de la de análisis de fotos. Cifras provisorias: 10 por
mes en Gratuito y 40 en Premium (`LIMITE_CARTELES_IA_MENSUAL`, mes argentino). Generar las 3 opciones con IA cuenta
**uno**; regenerar el texto cuenta otro. Sin cuota, o con la IA apagada o fallando, el cartel se arma igual con el texto
de reserva y no se descuenta nada.

## Edición

La persona edita **textos** (titular, bajada, nombres, texto del posteo), **elementos permitidos** (mostrar u ocultar la
foto, el logo y la vigencia, y cambiar el producto) y elige **variante** y **formato**. Quedan bloqueados colores, fuentes,
tamaños y márgenes. Cada texto editado se valida con los mismos máximos; un número escrito a mano es suyo y no pasa por el
control de la IA, pero el precio de la plantilla sólo cambia si cambia el dato de origen.

## Nivel 2 (opcional, T-270)

La IA propone una variante en HTML dentro de reglas fijas: sólo etiquetas y propiedades de una lista blanca, sin scripts,
sin recursos externos, sólo variables de la paleta del negocio y de la retícula. Se sanea y se valida contra las mismas
reglas de armonía antes de guardarse; si falla, se descarta. Es de Premium y queda al final: si no alcanza para el 22,
pasa a MVP 5.

## Cinco ejemplos de punta a punta

Moneda en pesos argentinos, mes de noviembre de 2026. Los números salen del sistema; la IA sólo escribe lo que figura como
«texto».

### 1. Almacén «Don Rufino» · promo de precio

- **Kit:** principal `#00506A`, secundario `#EE8000`, tipografía Plus Jakarta Sans, tono `CERCANO`.
- **Pedido:** Aceite de girasol 900 ml, precio anterior `$2.500`, nuevo `$2.000`, vigencia 15/11 al 22/11, con foto del catálogo.
- **Sistema calcula:** descuento `$500` = `20 %`; vigencia `15/11 al 22/11`. Números permitidos: `900`, `2500`, `2000`, `500`, `20`, `15`, `11`, `22`.
- **IA devuelve:**
  ```json
  {"plantilla":"PROMO_PRECIO","titular":"Aceite a precio de vecino","bajada":"20 % menos hasta el 22/11","textoPosteo":"Esta semana el aceite de girasol de 900 ml pasa de $2.500 a $2.000. 20 % menos hasta el 22/11. ¡Pasá por Don Rufino!"}
  ```
- **Validación:** `20`, `22`, `11`, `900`, `2500`, `2000` están permitidos → se acepta.
- **Variantes mostradas:** `BLOQUE` (foto grande), `FRANJA` (colores intercambiados), `MARCO` (foto chica).
- **Salida elegida:** A4 vertical para la vidriera.

### 2. Panadería «La Espiga» · 2x1

- **Kit:** principal `#B5651D` (se ajusta a `#9A5418` por contraste), secundario `#F4C95D`, tipografía Playfair Display, tono `CERCANO`.
- **Pedido:** Medialunas de manteca, `$1.800` la docena, 2x1 los sábados, sin foto.
- **Sistema calcula:** pagás `1 de 2`, ahorro `$1.800`, precio efectivo por docena `$900`.
- **IA devuelve:** titular «Sábado de medialunas dobles», bajada «Llevás 2 docenas y pagás 1», posteo con `$900` por docena.
- **Validación:** `2`, `1`, `900` permitidos → se acepta.
- **Sin foto:** la composición tipográfica pone «2×1» como protagonista (acento secundario).
- **Salida elegida:** A5 para el mostrador.

### 3. Kiosco «Esquina» · combo

- **Kit:** principal `#4EB2D4`, secundario `#F85F5A` (la tinta se calcula en Pizarra por contraste), tipografía Poppins, tono `DIVERTIDO`.
- **Pedido:** alfajor `$900` + gaseosa 500 ml `$1.600` + chicle `$400` = `$2.900` sueltos; combo `$2.500`.
- **Sistema calcula:** suma `$2.900`, ahorro `$400`, 3 fotos del catálogo (una de ellas viene del escáner).
- **IA devuelve:** «Combo recreo» / «Los tres por $2.500»; el posteo menciona `$400` de ahorro.
- **Intento inválido:** si la IA escribe «ahorrás $500», el número `500` no está permitido → se descarta y se usa «Combo: alfajor + gaseosa + chicle a $2.500».
- **Salida elegida:** cuadrado para Instagram y mensaje por WhatsApp con el texto del posteo copiado.

### 4. Ferretería «El Tornillo» · novedad

- **Kit:** sin logo; el sistema pide subirlo o usa el nombre. Principal `#263443`, secundario `#EBCE32`, tipografía Roboto Slab, tono `PROFESIONAL`.
- **Pedido:** Taladro percutor 650 W, `$78.000`, ingresó el 10/11, con foto subida.
- **Sistema calcula:** precio `$78.000`, fecha de ingreso `10/11`.
- **IA devuelve:** «Llegó el taladro de 650 W» / «Ya disponible en el local».
- **Validación:** `650` es un número del nombre del producto → permitido. Si dijera «en 12 cuotas», `12` no está → se descarta.
- **Salida elegida:** vertical 4:5 para redes.

### 5. Peluquería «Corte&Color» · horarios

- **Kit:** principal `#7B2D5B`, secundario `#EBCE32`, tipografía Nunito, tono `CERCANO`.
- **Pedido:** horarios del negocio: lunes a viernes `9 a 19`, sábados `9 a 14`, domingos cerrado.
- **Sistema calcula:** las líneas de horarios ya formateadas; no hay precios ni foto.
- **IA devuelve:** «Te esperamos» / «De lunes a sábado, con turno o sin turno».
- **Validación:** el texto no trae números → siempre válido. Si dijera «hasta las 20», `20` no está → se descarta.
- **Salida elegida:** A4 vertical para la puerta.

## Decisiones tomadas

- La cuota de carteles con IA es aparte de la de objetivos; las cifras (10 y 40) son provisorias.
- No se publica directo en redes: sólo se descarga y se comparte.
- El nivel 2 queda como subtarea opcional (T-270).
- Los textos siguen la guía de tono de H-56.

## Cómo quedó el motor de render (T-264 · ENS-363)

- El dominio compone el cartel como una lista de piezas (`Rectangulo` y `TextoDeCartel`) en unidades de dibujo; no
  conoce ningún formato de salida. Un puerto (`DibujanteDeCartelPort`) lo convierte: hoy a SVG (vista previa) y en T-269
  a PDF y PNG recorriendo las mismas piezas.
- Cada variante (`BLOQUE`, `FRANJA`, `MARCO`) es una subclase de `DisenoDeVariante`; el reparto de los textos
  (`RolDeTexto`: titular, protagonista, bajada, detalle y líneas) es común y se acomoda en la zona de texto de la variante.
- `PaletaDeCartel` ajusta los colores del negocio (oscurece o aclara en pasos) hasta que todo texto cumpla 4,5 : 1.
- Un texto largo se achica hasta el tamaño mínimo del formato y después se corta con «…». Si las líneas no entran con
  altura legible (por ejemplo 10 productos en el cuadrado), se rechaza con un mensaje en vez de superponerlas.
- Endpoint: `POST /api/carteles/opciones` devuelve las 3 composiciones con su dibujo, con la marca del negocio de la sesión.
