# Guía de lenguaje de Ensambla

Actualizada: 2026-11-19

## Para qué sirve

Todo texto que ve el usuario en Ensambla (app, avisos, Sami, mails y landing) sigue esta guía. Es la subtarea T-209 de H-56 y va primero: el resto de las subtareas (T-210 a T-216) la usan para reescribir sus pantallas.

**A quién le hablamos:** dueños de almacenes, kioscos y comercios de barrio. Saben mucho de su negocio y poco de computadoras. Leen rápido, entre cliente y cliente, muchas veces desde el celular.

**El problema que arregla:** la revisión de H-56 marcó demasiado texto y dos tonos mezclados (frases muy de charla como "Para no quedarte corto, pedí 5" al lado de otras formales como "Ocurrió un error inesperado").

## Tono: cómo hablamos

Hablamos como una **aplicación profesional, clara y cordial**. Voseo rioplatense, pero sin coloquialismos.

1. **Voseo sí, siempre:** vos, tenés, querés, elegí, cargá, escribí. Nunca «tú», «tienes», «quieres» ni «usted».
2. **Sin informalidad.** Nada de muletillas, exclamaciones, diminutivos, humor, jerga ni expresiones de charla.
3. **Botones en infinitivo**, con verbo y objeto: «Guardar venta», «Agregar producto», «Crear sección». Los verbos en
   voseo imperativo se reservan para consignas directas dentro de un texto («Elegí un producto»).
4. **Textos de ayuda: neutros y directos.** Una idea por frase. Primero el dato, después la explicación.
5. **Impersonal cuando es una descripción** («Se muestra cuánto falta para tu meta») y **segunda persona cuando se le
   pide algo** («Elegí una fecha»).
6. **Ensambla** habla en plural: «No pudimos guardar los cambios». **Sami** habla en primera persona singular, con
   respeto y sin confianza excesiva: «Encontré 5 productos con poco stock», «¿En qué puedo ayudarte?».
7. Sin emojis, sin mayúsculas sostenidas, un signo de exclamación solo para un logro real.
8. Se mantiene el glosario de esta guía (negocio, sección, stock, aviso, fiado, ganancia…).

## Palabras: usar y evitar

Revisar cada coincidencia en contexto: si es un texto que ve la persona, se reescribe; si es código o un comentario, se
deja.

- **Tono:** `dale`, `che`, `mano` (en «dar una mano»), `manito`, `joya`, `genial`, `bárbaro`, `tranqui`, `acá`, `ahí`,
  `mirá`, `fijate`, `anotá`, `mandale`, `pasame`, `armalo`, `dejame`, `contame`, `decime`, `avisame`, `vamos a`,
  `de una`, `un toque`, `¡listo`, `tranquilo`, `ni idea`, diminutivos (`ito`, `ita`) y toda exclamación que no sea un
  logro.
- **Glosario:** `inventario` (en pantalla, `Stock`), `registro`, `campo`, `bloque`, `pieza`, `tabla`, `cuaderno`
  (cuando se habla del negocio), `alerta`, `notificación`, `Ingresá`, `Ocurrió un error`, `No se pudo`.

## Glosario de Ensambla

Estos nombres son fijos: se escriben siempre igual, con la misma mayúscula, en todas las pantallas.

| Término | Qué es | Cómo se escribe |
| --- | --- | --- |
| Ensambla | La app | Con mayúscula, sin artículo ("Ensambla te avisa") |
| negocio | Cada emprendimiento que manejás en Ensambla (tu kiosco, tu almacén), con sus datos, su Sami y sus avisos | En minúscula. "Mis negocios", "Tu negocio" |
| sección | Una parte del negocio donde cargás datos: Ventas, Stock, Clientes | En minúscula. Nunca "cuaderno", "bloque" ni "tabla" |
| cuaderno | Solo el cuaderno de papel que fotografiás para cargar datos | "Sacale una foto a tu cuaderno". Nunca para hablar del negocio ni de una sección |
| Inicio | La primera pantalla; si tenés varios negocios, muestra cómo van todos juntos | Con mayúscula. En pantalla nunca "vista conjunta" ni "dashboard" |
| Sami | El asistente de cada negocio. En el Inicio habla de todos tus negocios juntos | Con mayúscula. Habla en primera persona |
| aviso | Algo que Sami encontró y conviene mirar | Nunca "alerta" ni "notificación" |
| objetivo | Una meta chica y medible ("Vender $50.000 esta semana") | En minúscula |
| stock | Lo que tenés para vender | Nunca "inventario" |
| fiado | Lo que vendiste y todavía no te pagaron | "Vendiste fiado", "Te deben" |
| ganancia | Lo que te queda después de los costos | Nunca "margen" en pantalla |
| Mi zona | Cómo están tus precios y tu barrio | Con mayúscula en Mi |
| plantilla | Secciones ya armadas para empezar rápido | En minúscula |
| Planes | Lo que incluye cada opción de Ensambla. El plan es de tu cuenta, no de cada negocio | Plan Gratuito, Plan Premium (un tercer plan está en discusión) |

## Reglas por tipo de texto

**Botones**

- Verbo + qué cosa, en 1 a 3 palabras: "Guardar venta", "Subir foto", "Ver avisos".
- Nunca "Aceptar", "OK" ni "Enviar" solos: decí qué pasa al tocarlo.
- "Quitar" saca algo de una lista y se puede volver a agregar. "Borrar" es para siempre y siempre pide confirmación.
- Sin punto final ni signos.

**Mensajes de error** (qué pasó + qué hacer, en una o dos frases)

- Si falla algo nuestro: "No pudimos guardar la venta. Probá de nuevo en un rato."
- Si falta un dato: decilo en positivo y al lado del dato. "Escribí el precio" en vez de "Campo obligatorio".
- Aclará si se perdió algo o no: "Tus datos siguen guardados".
- Nunca códigos (500, 404), ni "inesperado", ni culpar al usuario.

**Confirmaciones (antes de algo que no se puede deshacer)**

- Título como pregunta con el nombre de la cosa: "¿Borrar el negocio Almacén Rosa?"
- Una línea con la consecuencia: "Se borran todas sus ventas y no se pueden recuperar."
- Botones: el que actúa repite el verbo ("Borrar negocio"); el otro es "Cancelar". Nunca "Sí" / "No".

**Mensajes de éxito**

- Cortos y en pasado: "Venta guardada", "Foto subida". Un "¡" solo para logros ("¡Cumpliste tu objetivo!").

**Estados vacíos (cuando todavía no hay nada)**

- Qué falta + un botón para hacerlo: "Esta semana todavía no anotaste ventas" + [Anotar venta].
- Sin culpa ni presión.

**Avisos de Sami**

- Una línea, línea y media como máximo, con ícono. El número adelante: "5 productos bajo el precio de la zona".
- Avisos del mismo tipo van juntos, con la lista adentro.
- Cuándo lo encontró Sami va arriba de todo, con ícono de reloj ("Hace 2 días"). El detalle va en el desplegable "Por qué de este aviso", no en la tarjeta.

**Cargando**

- Decir qué se está haciendo con palabras simples: "Leyendo tu foto…", "Guardando…". Nunca "Procesando".

## Números, plata y fechas

| Qué | Regla | Ejemplo |
| --- | --- | --- |
| Plata | Signo $ pegado, punto para miles, sin centavos salvo que importen | $12.500 |
| Plata con centavos | Coma decimal, solo en precios unitarios chicos | $850,50 |
| Cantidades | Siempre en número, aunque sea chico | 3 productos (no "tres") |
| Porcentajes | Sin decimales; si la base es muy chica, no mostrarlo | 15 % |
| Subas y bajas | Flecha o color + palabra, no solo el signo | Vendiste 20 % más que la semana pasada |
| Fechas cercanas | Palabras | Hoy, Ayer, El lunes |
| Fechas lejanas | Día y mes corto, año solo si no es el actual | 14 nov, 3 mar 2025 |
| Períodos | Siempre los mismos tres | Hoy, Esta semana, Este mes |
| Horas | 24 horas, con "h" | 18:30 h |

## Antes y después

### Tono profesional (2026-11-19)

| Antes | Después |
| --- | --- |
| ¿En qué te doy una mano? | ¿En qué puedo ayudarte? |
| Te ayudo a encontrar el próximo paso. | Elegí cómo querés continuar. |
| Crear una sección conmigo | Crear una sección con Sami |
| Encontrar una plantilla simple | Elegir una plantilla |
| ¿Necesitás una mano? (botón) | Ayuda |
| ¿Una mano con esto? | Consultar a Sami |
| Acá ves cuánto te falta para llegar. En «Ver mi semana» están los mini-objetivos de Sami. | Aquí se muestra cuánto falta para llegar a tu meta. Los mini-objetivos de Sami están en «Ver mi semana». |
| Cómo van tus objetivos | Seguimiento de tus objetivos |
| Cómo viene tu negocio | Estado de tu negocio |
| Los últimos 30 días, con lo que anotaste en tus secciones. | Últimos 30 días, según los datos de tus secciones. |
| Vendiste / La mercadería te costó / Te queda | Ventas / Costo de mercadería / Ganancia |
| Te quedan 20 de 20 objetivos con IA este mes | Objetivos con IA disponibles este mes: 20 de 20 |
| Resumen de la semana listo | Resumen semanal disponible |
| ¿Pensás abrir otro local? Sami te muestra en qué zonas conviene, con el mapa y las razones de cada una. | ¿Evaluás abrir otro local? Compará zonas con el mapa y los motivos de cada una. |
| Tu próxima venta empieza acá | Todavía no hay productos en esta venta |
| Agregá productos para revisar la compra. | Agregá productos para comenzar. |
| Alineá las barras dentro del marco horizontal. | Alineá el código de barras dentro del marco. |
| Mandale a Sami una foto de tu cuaderno | Enviá a Sami una foto de tu cuaderno |
| Mandale lo que anotás | Enviá lo que anotás |
| ¡Listo! Tu sistema está armado | Tu negocio quedó armado |
| ¡Listo! «X» ya está armado. | «X» quedó armado. |
| ¡Dale! Cuando quieras seguimos. | De acuerdo. Cuando quieras, continuamos. |
| Vamos a pedir la cancelación a Mercado Pago. | Solicitaremos la cancelación a Mercado Pago. |
| Vamos a buscar dónde abrir… | Buscaremos dónde abrir… |
| Armalo con Sami | Crear con Sami |
| Escanear Inventario | Escanear stock |
| Tus favoritos aparecerán aquí | Todavía no hay favoritos |
| Tocá Organizar para elegirlos o crear uno nuevo. | Usá «Organizar» para elegirlos o crear uno nuevo. |
| Probá otro nombre o agregá un artículo. | Probá con otro nombre o agregá un artículo. |
| Lo vamos a intentar de nuevo. | Reintentaremos en unos segundos. |

### Palabras y reglas

Casi todos son textos que hoy están en la app; el aviso largo de Sami y el "Sí / No" son ejemplos armados para mostrar la regla.

| Antes | Después | Regla |
| --- | --- | --- |
| Mis cuadernos | Mis negocios | Hablamos de negocio |
| Nombre de tu proyecto | Nombre de tu negocio | Hablamos de negocio |
| Ocurrió un error inesperado. Probá de nuevo en unos minutos. | No pudimos cargar esta pantalla. Probá de nuevo en un rato. | Decir qué falló |
| No se pudo eliminar el registro. | No pudimos borrar la venta. Probá de nuevo. | Plural + palabra concreta |
| Ingresá un importe mayor a cero. | Escribí un importe mayor a $0. | Escribí, no Ingresá |
| Los campos con \* son obligatorios. | Los datos con \* los necesitamos para guardar. | Sin "campos" |
| Ningún registro coincide con la búsqueda y los filtros. | No encontramos nada con esa búsqueda. | Sin "registro" |
| Dejá al menos un registro. | Tiene que quedar al menos una venta. | Palabra concreta |
| Elegí una imagen JPG, PNG o WEBP. | Elegí una foto (las de cámara o capturas sirven). | Sin formatos técnicos |
| La imagen supera los 10 MB. Elegí una más liviana. | Esa foto es muy pesada. Elegí otra o sacá una nueva. | Sin "MB" |
| Para no quedarte corto, pedí 5. | Te conviene pedir 5 para no quedarte sin stock. | Sami sugiere, sin informalidad |
| Por qué te lo aviso | Por qué de este aviso | Menos informal |
| Detectado hace 2 días (al final del texto) | [reloj] Hace 2 días, arriba de la tarjeta | Visual antes que texto |
| Sami detectó que Coca 500, Galletitas Oreo, Yerba Playadito, Azúcar y Fideos están por debajo del precio promedio de tu zona. | [ícono] 5 productos bajo el precio de la zona → Ver lista | Número adelante, agrupar |
| Confirmar cancelación / Sí / No | ¿Cancelar tu plan? / Cancelar plan / Volver | Botón con verbo |
