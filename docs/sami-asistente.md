# Sami: asistente para armar el sistema (H-33 / ENS-170)

Sami lee cómo la persona describe su negocio («vendo velas y anoto quién me debe») y propone la **plantilla** base
y las **columnas extra** que le faltan. La persona confirma antes de crear nada. Está pensado para gastar lo menos
posible: cada etapa sólo corre si la anterior no alcanzó.

## Cómo decide (embudo)

| Etapa | Qué hace | Costo | Código |
|---|---|---|---|
| 1. Diccionario | Normaliza el texto (minúsculas, tildes, plurales) y suma puntos por palabras y frases de cada plantilla. Detecta columnas por disparadores. | $0 | `InterpretacionSamiService` |
| 2. Preguntas | Si la confianza no llega al umbral, elige la pregunta que mejor separa a las candidatas, con respuestas rápidas. Máximo 3. | $0 | `ConversacionSamiService` |
| 3. IA | Sólo si lo anterior no alcanzó. Claude en Amazon Bedrock: primero el modelo rápido (Haiku) y, si su propuesta no es válida o llega con poca confianza, el avanzado (Sonnet). | centavos | `RespaldoIaSamiService`, `SamiIaAdapter` |

**Confianza** = `fuerza × separación`. La fuerza crece con los puntos de la mejor plantilla hasta 5; la separación
baja si la segunda anda cerca (un empate da 0 y obliga a preguntar). El umbral es `0.7`.

Si la persona responde **«Otra cosa»** en la pregunta general, Sami no sigue preguntando por plantillas que ya
descartó: pasa directo a la IA.

**La IA no puede inventar.** Se le manda un vocabulario cerrado (las plantillas reales con sus columnas y los tipos
permitidos) y su respuesta se valida: la plantilla tiene que existir, las columnas inválidas, repetidas o que la
plantilla ya trae se descartan, y como mucho se conservan 6. Una respuesta ilegible o con una plantilla inexistente
se rechaza, pero el gasto igual queda registrado.

## API

Todo requiere sesión; las escrituras llevan `X-CSRF-TOKEN`. El negocio sale de la sesión, nunca del body.

**`POST /api/sami/conversacion`**: sin estado. El cliente manda el texto y las respuestas dadas hasta ahora, y el
servidor reconstruye la conversación.

```json
{ "texto": "quiero un stock y llevar los pedidos",
  "respuestas": [ { "preguntaId": "principal", "opcionId": "pedidos" } ] }
```

Responde `tipo`:

- `PREGUNTA`: viene `pregunta { id, texto, opciones[{id, texto}] }`.
- `PROPUESTA`: viene `propuesta { plantillaId, plantilla, columnas[{nombre, tipo, opciones}], confianza, origen }`
  con `origen` `ALGORITMO` o `IA`.
- `RESPALDO_IA`: no se usa con el flujo completo; queda para clientes que quieran saber que el algoritmo no alcanzó.

**`POST /api/sami/proyectos`**: crea el proyecto con la propuesta final y los ajustes de la persona
(`plantillaId`, `nombre` opcional, `columnas`). Es **una sola transacción**: si una columna es inválida no queda
ningún proyecto. Los errores llegan ubicados (`columnas[1].nombre`). Sólo admite columnas de texto, número, fecha,
opción y sí/no.

## El diccionario

`backend/src/main/resources/ia/sami-diccionario.json`. Se edita sin tocar código:

- `plantillas[].terminos`: palabra o frase con su peso. Se escribe de forma natural; el texto se normaliza igual que
  el de la persona. **Los verbos se listan conjugados** (`vendo`, `vendemos`, `vender`): no hay conjugación
  automática. Formas con la misma raíz cuentan una sola vez.
- `columnas`: nombre, tipo, opciones, `disparadores` y las `plantillas` donde aplica (vacío = cualquiera).
- `preguntas`: opciones que eligen una plantilla (`null` = «ninguna de estas»). Elegir una opción suma 5 puntos a
  su plantilla y descarta a las otras que esa pregunta separaba.

Los nombres de plantilla tienen que existir en la base (hay un test contra Postgres). Las plantillas **Ventas** y
**Gastos** las sembró `V12`, porque con las tres originales no había adónde apuntar «vendo velas».

### Cómo se mide

`AciertoSamiTest` corre `src/test/resources/sami/frases-prueba.json` (48 frases con la plantilla y las columnas
esperadas) y exige ≥ 80 % de acierto de plantilla, ≥ 90 % de precisión en lo que da por resuelto y que lo vago no se
resuelva. Imprime el reporte en cada corrida. Resultado actual: **45/45 plantillas (100 %)**, 26 de 48 frases se
resuelven sin preguntar, 22 de 23 columnas esperadas acertadas.

> **Ojo.** Las frases las escribió el mismo equipo que armó el diccionario, así que ese 100 % es optimista. Falta
> sumar descripciones reales de comerciantes (el Jira pide 40 o más) antes de creerle la cifra.

## Proveedor de IA intercambiable (Anthropic o Bedrock)

Sami y la lectura del cuaderno piden una **puerta** y la configuración decide por atrás qué proveedor la atiende:

| Puerta | La usa | Simulado | API de Anthropic | Amazon Bedrock |
|---|---|---|---|---|
| `ClienteModeloVision` (imágenes → JSON) | lectura del cuaderno | ✅ | ✅ | ✅ |
| `ClienteModeloTexto` (texto → JSON) | asistente de plantillas | ✅ | ✅ | ✅ |
| `ClienteModeloConHerramientas` (charla con herramientas e imágenes) | taller de Sami | ✅ | ✅ | ✅ |

- **Un solo selector:** `ensambla.ia.proveedor = simulado | anthropic | bedrock` (`ENSAMBLA_IA_PROVEEDOR`). Las
  credenciales y los modelos de cada proveedor van en `ensambla.ia.proveedores.<nombre>.*`:
  - Anthropic: `ENSAMBLA_IA_API_KEY` y `ENSAMBLA_IA_ANTHROPIC_MODELO_RAPIDO` / `_AVANZADO` / `_VISION`.
  - Bedrock: `ENSAMBLA_IA_BEDROCK_REGION`, `ENSAMBLA_IA_BEDROCK_PERFIL_AWS` (vacío = credenciales por defecto) y
    `ENSAMBLA_IA_BEDROCK_MODELO_RAPIDO` / `_AVANZADO` / `_VISION`.
  - `ENSAMBLA_IA_MODELO` sigue pisando el modelo de visión.
- **Sami puede usar otro:** cada valor de `ensambla.sami.ia.*` vacío sale del global; con valor, manda el de Sami
  (`ENSAMBLA_SAMI_IA_PROVEEDOR`, `_PERFIL_AWS`, `_MODELO_RAPIDO`, …). Ojo: un archivo de secretos viejo con
  `ENSAMBLA_SAMI_IA_PROVEEDOR=simulado` deja a Sami simulado aunque el global sea `bedrock`.
- **Sumar un proveedor:** implementar `ProveedorIa` (arma sus tres clientes), registrarlo como bean en `IaConfig` y
  poner sus datos en `ensambla.ia.proveedores.<nombre>.*`. `IaConfig` y `SamiConfig` sólo piden puertas a
  `ProveedoresIa`; un nombre desconocido no deja arrancar la app.

### Ajustes de Sami

| Propiedad | Por defecto | Para qué |
|---|---|---|
| `proveedor` | el global | `simulado` elige la primera plantilla sin costo; `bedrock` o `anthropic` usan Claude |
| `region` / `perfil-aws` | los del proveedor | región y perfil de AWS (sólo Bedrock) |
| `modelo-rapido` | Haiku 4.5 del proveedor | atiende todo |
| `modelo-avanzado` | Sonnet 4.6 del proveedor | sólo si el rápido no está seguro; para apagar el escalado, dejar vacío también el avanzado del proveedor |
| `umbral-escalar` | `0.6` | confianza que tiene que alcanzar un modelo para no escalar |
| `limite-diario-ia` | `20` | consultas con IA por negocio y por día (UTC) |

- **Costo medido** con Haiku 4.5 real: ≈ 830 tokens de entrada y 100 de salida por consulta.
- **Consumo**: `consumo_sami_diario` (migración `V13`) guarda, por negocio y día, las consultas y los tokens. La
  reserva es atómica en una sola sentencia, así que pedidos simultáneos no pasan el límite; un rechazo por límite no
  llama a la IA.
- **Prueba contra Bedrock real**: `SAMI_BEDROCK_PERFIL=<perfil> ./mvnw test -Dtest=SamiBedrockRealTest`. Se salta sola
  si no está la variable, así que `verify` nunca gasta.
- **Modelos habilitados**: con la cuenta usada para desarrollar, Haiku 4.5 y Sonnet 4.6 responden en `us-east-1`;
  los modelos 5.5 figuran en la lista pero dan «not available for this account». Si se habilitan, alcanza con
  cambiar `modelo-rapido` y `modelo-avanzado`.
- **Credenciales**: en desarrollo se usó un perfil con credenciales de la cuenta *root*. Para cualquier despliegue
  conviene un usuario o rol de IAM que sólo pueda `bedrock:InvokeModel` sobre esos modelos.

## Dictado por voz (ENS-216)

El botón del micrófono del taller usa el reconocimiento de voz del navegador (`SpeechRecognition`, `es-AR`): lo
dictado se va escribiendo en el campo para revisarlo antes de enviar y **no sale audio del navegador**. Sin
permiso, sin micrófono o en un navegador sin soporte (por ejemplo Firefox), el botón avisa con un mensaje simple
(cómo habilitar el permiso) y se puede seguir escribiendo. No hay respaldo que envíe el audio a un servicio de
transcripción: el Jira lo menciona como alternativa y queda pendiente si hace falta cubrir esos navegadores.

## «Armalo con Sami» en el frontend (ENS-180)

Pantalla nueva en `/sami` (`pages/SamiProyecto`), con un acceso en el inicio («Armalo con Sami»). Habla con el
backend real: `POST /api/sami/conversacion`, `POST /api/sami/proyectos` y `GET /api/plantillas` (para mostrar la
plantilla con sus columnas). Recorrido: descripción (escrita o dictada) → preguntas con respuestas rápidas →
propuesta con vista previa → la persona puede quitar columnas y cambiar el nombre → «Sí, crear mi proyecto».
Sin estado en el servidor: `hooks/sistemas/useSamiProyecto.ts` guarda el texto y las respuestas y las manda en cada
pedido. Si el servidor rechaza algo (por ejemplo el límite diario de IA) se muestra su mensaje y se conserva lo
escrito.

Esta pantalla crea un proyecto real (`proyecto`, `bloque`, `campo`), que ninguna pantalla de `dev` muestra todavía.
Para conversar con Sami y armar un sistema que sí queda en «Mis sistemas», el camino es el **taller conversacional**
(sección siguiente). Conviven hasta decidir cuál es la fuente de verdad.

- **Modo `completo`** (demo sin backend): `mocks/handlers/samiProyecto.ts` simula las tres rutas con una versión
  chica del diccionario. En `parcial` y `ninguno` responde el backend de verdad.
- Para probarla con el backend real: levantar el backend y correr el frontend con `VITE_API_MOCKS=parcial`.
  Ojo: en `parcial` el resto de la app (sistemas, avisos) sigue simulado y la sesión es la del backend.
- Todavía no hay una pantalla donde abrir el proyecto creado (el frontend de `dev` no tiene pantallas de
  proyectos): al terminar se ofrece volver al inicio.

## Taller conversacional con herramientas (`POST /api/sami/taller`)

El taller (`/taller`) es una conversación: la persona escribe lo que quiera y Sami contesta, cambia la propuesta o
pregunta con una botonera. **El modelo decide y el sistema ejecuta**: Haiku no escribe el sistema, invoca
herramientas de vocabulario cerrado; el backend valida lo que pidió y el taller lo aplica con las funciones que ya
tiene (`lib/sistema/core.ts`). El resultado es el mismo sistema del taller, así que se guarda y se usa en «Mis
sistemas».

**Sami ensambla, no inventa.** Todo mensaje (también el primero) va a la IA; no hay reglas locales ni ajustes fijos en
la pantalla. Sami arma con los **componentes del editor** y los adapta al rubro: los elige, les pone los nombres de la
persona, suma datos propios, carga ejemplos del rubro y los conecta. Como sólo elige y completa, cada turno es barato y
el resultado es siempre un sistema que el editor sabe mostrar y editar a mano.

- **Catálogo único.** Los componentes son los bloques de `frontend/src/lib/sistema/catalogo.ts` (los 10 del editor).
  `npm run catalogo:sami` los exporta a `backend/src/main/resources/ia/catalogo-componentes.json` y un test del
  frontend (`src/test/lib/catalogoSami.test.ts`) falla si el archivo quedó viejo. Si el editor suma un bloque o un
  campo, Sami lo puede usar sin tocar el backend.
- **Tabla a medida.** El componente `form` («Formulario») es la pieza genérica: lo que no tiene componente propio
  (turnos, trabajos, arreglos, materiales…) se arma como una tabla con nombre y campos. Es el único que se repite.
- **Kits por rubro** (`ia/kits-rubro.json`, del backend): qué componentes lleva cada rubro, con qué nombres, qué datos
  extra (con opciones), 2 o 3 filas de ejemplo, conexiones y una pregunta para seguir. Hay 10: almacén, dietética,
  velas, panadería, viandas, ropa, peluquería, taller mecánico, ferretería y librería. Un test valida que cada kit use
  sólo componentes, tipos de dato, etiquetas y conexiones que existen.
- **Detección del rubro sin costo** (`RubroTallerService`): busca los términos de cada kit en el mensaje, la charla y
  el nombre del sistema. Con el sistema vacío, el contexto lleva el resumen del kit reconocido o, si no hay, la lista
  de kits; con piezas ya armadas no se nombran kits (un pedido puntual no arma el kit).
- **Conexiones.** `wireSystem` ya vincula ventas y pedidos con clientes, y ventas, entradas y salidas con el
  inventario; el backend descarta un `conectar` entre esos tipos para no duplicar el vínculo. `conectar` es para el
  resto (turnos con clientes, velas con materiales, pedidos con platos).

**Un turno = una llamada.** Haiku responde con texto y con las herramientas que quiere invocar en la misma respuesta;
no hay segundo viaje. Cada turno manda un contexto chico: el prompt fijo con la lista de componentes, el sistema
resumido (piezas y datos, sin filas), el kit si corresponde, los últimos 6 mensajes y el mensaje. Cada turno se
registra en `consumo_sami_diario` y respeta el límite `ensambla.sami.limite-diario-taller` (60 por negocio y día), que
comparte contador con el asistente de plantillas.

| Herramienta | Efecto en el taller |
|---|---|
| `usar_kit(rubro)` | **sólo backend**: se expande en `agregar_pieza` y `conectar` con todo el kit; cuenta como una acción |
| `agregar_pieza(tipo, nombre?, campos?, ejemplos?, seccion?)` | suma un componente con su nombre, datos extra (≤ 8) y filas de ejemplo (≤ 5) |
| `quitar_pieza(pieza)` | quita una pieza |
| `cargar_ejemplos(pieza, filas)` | reemplaza las filas de ejemplo (las reales quedan) |
| `cargar_registros(pieza, filas)` | suma **registros reales** (por ejemplo, lo que se lee de una foto), hasta 30; se guardan al crear el sistema |
| `editar_registro(pieza, registro, cambios)` / `quitar_registro(pieza, registro)` | cambia datos de un registro que ya está (`cambios` = `{etiqueta: valor}`; un valor vacío borra el dato, menos el principal) o lo saca. `registro` es su nombre o su id; en la apertura de cada pieza Sami ve sus primeras 15 filas con 3 datos. Si el nombre se repite, el contexto le da el id |
| `conectar(desde, hacia, etiqueta?)` | suma en `desde` un dato vínculo a `hacia` (o convierte en vínculo un dato con esa etiqueta) |
| `renombrar_pieza(pieza, nombre)` / `renombrar_sistema(nombre)` | cambia nombres |
| `agregar_campo(pieza, etiqueta, tipo, opciones?)` / `quitar_campo(pieza, campo)` | suma o quita un dato; `select` necesita 2 opciones |
| `editar_campo(pieza, campo, etiqueta?, tipo?, opciones?, obligatorio?, posicion?)` | cambia un dato que ya existe: nombre, tipo, opciones, si es obligatorio y su lugar (desde 1); no cambia el tipo de un vínculo ni del primer dato |
| `separar_en_seccion(pieza, seccion)` | pasa la pieza a una sección aparte |
| `cambiar_distribucion(layout)` | mosaic, single, two o three |
| `ofrecer_opciones(pregunta, opciones)` | **no cambia nada**: Sami pregunta con una botonera |

**Mandarle una foto a Sami.** La cámara del taller (también en «Ajustar con Sami») adjunta una foto del cuaderno, una
lista o una planilla al próximo mensaje. El navegador la achica (lado mayor 1600 px) y viaja en
`POST /api/sami/taller` como `imagen: {tipo, datos}` (JPEG, PNG o WEBP en base64, hasta 5 MB; si no, 400 con un mensaje
para la persona). El modelo la ve en el mismo turno, arma o ajusta las piezas según sus columnas y carga lo que lee
con `cargar_registros`. Si el modelo arma las piezas y se olvida de cargar lo que lee (pasa con los kits), se le hace
un segundo pedido sólo para los registros, mostrándole las piezas ya armadas para que use sus etiquetas; los registros
se suman a la respuesta. Al crear el sistema se descartan sólo las filas de ejemplo: los registros reales quedan. La
foto no se guarda ni viaja en el historial.

**Toda foto lleva a Sami.** «Traer datos de una foto» (inicio, editor, ayuda) ya no tiene pantalla de revisión propia: elige
la imagen, abre el taller (sobre el sistema, si venía de uno) con la foto adjunta y se la manda sola. Sami cuenta qué leyó,
nombra las dudas y cierra con una botonera: ¿está bien?, ¿hay algo mal?, ¿es de otro cuaderno?, ¿sumamos otra tabla?
(prompt v4, `CON_FOTO`).

**Ajustar con Sami un sistema ya creado.** En la pantalla de un sistema, «Ajustar con Sami» abre el taller con ese
sistema (`/taller?sistema=<id>`): Sami lo ve con sus piezas y datos y lo cambia con las mismas herramientas. La
acción principal pasa a ser «Guardar cambios», que reemplaza el sistema (`actualizarSistema`, y de ahí
`PUT /sistemas/{id}`); «Volver» sale sin guardar. Los registros reales se conservan; las filas de ejemplo que se
sumaron en el taller no se guardan (`ajusteParaGuardar` en `lib/sistema/acciones.ts`). Como un sistema creado no
tiene ejemplos, si Sami carga un ejemplo que nombra un registro de otra pieza («Vela lavanda» en una venta), ese
registro se suma como ejemplo en la pieza vinculada, así el vínculo se ve.

Las piezas se nombran por id, tipo o nombre; una pieza sumada en el mismo turno, por tipo o por nombre. Si Sami armó
con un kit y no preguntó nada, la respuesta lleva la pregunta del kit en la botonera, así la charla siempre sigue.

Si el modelo sólo invoca herramientas y no escribe nada (pasa seguido con Haiku), Sami cuenta lo que quedó hecho a
partir de las acciones ya validadas («Sumé gastos. Quité clientes. Mirá cómo queda.»).

**Sami no dice que hizo algo que no hizo.** Si el texto cuenta un cambio («Armé…», «Moví…», «Renombré…») y no llegó
ninguna acción válida, `TallerSamiService` le pide al modelo una vez más que lo haga con las herramientas (no reserva
otra consulta del día; los tokens se registran). Si tampoco lo hace, la respuesta es «No pude hacer eso…». La
evaluación mide al modelo sin este reintento.

**Reglas fijas del ensamblado** (no dependen del modelo): las conexiones que el taller hace solo se descartan;
renombrar una pieza a su mismo nombre no cuenta; y si un kit trae clientes, el dato Cliente de sus ventas va al lugar 2,
porque la vista de cada tabla muestra sólo los primeros 4 datos.

`ofrecer_opciones` llega en el campo `opciones` de la respuesta (dato estructurado, sin parsear texto). La pantalla
dibuja una botonera con 2 a 6 respuestas y **siempre** un último botón «Escribir otra cosa…» que enfoca el campo,
como en Claude. Elegir una opción equivale a escribir su texto. Cada cambio de Sami se puede deshacer con «Deshacer lo
último».

**Validación (`HerramientasTallerService`).** El modelo nunca escribe el sistema: se descarta toda herramienta
desconocida, pieza o dato inexistente o repetido, tipo fuera del catálogo y lo que pase los topes (12 acciones del
modelo y 24 contando los kits expandidos, 12 datos por pieza, 5 filas de ejemplo, nombres de 60 caracteres y valores
de 80). Un dato extra o una celda inválidos se caen de a uno sin descartar la llamada. Las llamadas se validan en orden
sobre una copia del estado. El frontend (`aplicarAcciones` en `lib/sistema/acciones.ts`) vuelve a defenderse y
convierte cada valor al tipo del dato.

**Contrato.** Request: `{mensaje, estado: {nombre, distribucion, piezas[{id, tipo, nombre, seccion, campos[{etiqueta,
tipo}]}]}, historial[{quien, texto}]}`. Response: `{respuesta, acciones[{herramienta, argumentos}], opciones}`.

### Evaluación

`TallerBedrockEvaluacionTest` (opt-in con `SAMI_BEDROCK_PERFIL`) corre 45 conversaciones de
`sami/conversaciones-taller.json` con el prompt, el catálogo, los kits, las herramientas y la validación de
producción: pedidos puntuales, preguntas, pedidos fuera del catálogo, armado desde cero con y sin kit, conexiones,
edición de datos, charlas reales de prueba (velas) y dos fotos (un cuaderno de ventas y una lista de stock).
`SAMI_EVAL_CASOS=foto` corre sólo los casos cuyo id contiene «foto»; los fallos muestran las llamadas crudas del modelo.

```bash
SAMI_BEDROCK_PERFIL=mi-perfil ./mvnw test -Dtest=TallerBedrockEvaluacionTest
```

Resultado con Haiku 4.5 (prompt v3, 14 herramientas, 2026-10-20): **93–96 %** según la corrida, ≈ **2680 tokens de
entrada sin foto y ≈ 3500 con foto**, ≈ 1,8 s de latencia: unos 0,3 centavos de dólar por turno sin foto y 0,35 con
foto. La meta es ≥ 85 % y < 2800 tokens sin foto. La evaluación mide al modelo solo: no pasa por los reintentos de
`TallerSamiService` (si dice que hizo algo sin hacerlo, o si con una foto no cargó registros), que en la app corrigen
justamente los casos que más fallan (bicicletas y el cuaderno de velas).

La primera versión del ensamblado dio 80 %: conectaba de más, hacía más de lo pedido y preguntaba el rubro en vez de
armar; lo corrigieron reglas concretas en el prompt y reglas fijas en la validación. La meta de tokens empezó en 2000 y
subió con cada herramienta (2300, 2600 con `editar_campo`, 2800 con `cargar_registros`): cerca de la mitad del turno son
las herramientas que el proveedor manda siempre, y el prompt caching no aplica porque el prefijo es más corto que el
mínimo que pide Haiku.

> **Ojo.** Las conversaciones y el prompt los escribió el mismo equipo, así que el 100 % es optimista; hay que probar
> con lo que escribe la gente de verdad. Las cuentas nuevas de Bedrock tienen una cuota baja de pedidos por minuto
> (respondió 429 a ráfagas de 25 pedidos): el test espera entre casos y reintenta.

## Pendiente

- Frases reales para medir el acierto (ver arriba) y conversaciones reales para el taller.
- Aviso a Gonzalo: el taller del mockup (`/taller`) ahora habla con el backend de Sami y se retiró su API simulada
  `/sami/propuesta`; también se sacaron `createGuided`, `refineDraft` y `recommend` de `lib/sistema/core.ts`, que sólo
  usaba esa API simulada.
- Guardar el sistema en el backend: el taller crea el sistema en el navegador y `useSincronizarSistemas` lo manda a
  `PUT /api/sistemas/{id}`, que todavía no existe en el backend (lo simula MSW en `parcial` y `completo`). Cuando
  exista, lo que arma Sami se guarda sin cambios en Sami: Sami no tiene estado.
- Unificar con el catálogo de piezas por rubro de la rama `feat/ensambla-se-ensambla-solo`, que no está mergeada y
  usa la API vieja de proyectos.
- Que `RespaldoIaSamiService` tenga un tope de gasto en dinero además del de consultas, si hace falta.
