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
**Gastos** las sembró `V9`, porque con las tres originales no había adónde apuntar «vendo velas».

### Cómo se mide

`AciertoSamiTest` corre `src/test/resources/sami/frases-prueba.json` (48 frases con la plantilla y las columnas
esperadas) y exige ≥ 80 % de acierto de plantilla, ≥ 90 % de precisión en lo que da por resuelto y que lo vago no se
resuelva. Imprime el reporte en cada corrida. Resultado actual: **45/45 plantillas (100 %)**, 25 de 48 frases se
resuelven sin preguntar, 22 de 23 columnas esperadas acertadas.

> **Ojo.** Las frases las escribió el mismo equipo que armó el diccionario, así que ese 100 % es optimista. Falta
> sumar descripciones reales de comerciantes (el Jira pide 40 o más) antes de creerle la cifra.

## IA con Amazon Bedrock

Se activa con `ensambla.sami.ia.proveedor=bedrock` (variables `ENSAMBLA_SAMI_*`, ver
`application-ensambla-secrets.properties.example`).

| Propiedad | Por defecto | Para qué |
|---|---|---|
| `proveedor` | `simulado` | `simulado` elige la primera plantilla sin costo; `bedrock` usa Claude |
| `region` | `us-east-1` | región donde están habilitados los modelos |
| `perfil-aws` | vacío | perfil de `~/.aws`; vacío usa la cadena por defecto (variables `AWS_*`, rol de la instancia) |
| `modelo-rapido` | `us.anthropic.claude-haiku-4-5-20251001-v1:0` | atiende todo |
| `modelo-avanzado` | `us.anthropic.claude-sonnet-4-6` | sólo si el rápido no está seguro; vacío desactiva el escalado |
| `umbral-escalar` | `0.6` | confianza que tiene que alcanzar un modelo para no escalar |
| `limite-diario-ia` | `20` | consultas con IA por negocio y por día (UTC) |

- **Costo medido** con Haiku 4.5 real: ≈ 830 tokens de entrada y 100 de salida por consulta.
- **Consumo**: `consumo_sami_diario` (migración `V10`) guarda, por negocio y día, las consultas y los tokens. La
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

El taller del mockup (`/taller`) **no se tocó**: sigue con su modelo de «sistema» y su API simulada
(`/sami/propuesta`). Son dos caminos distintos hasta que se decida cuál es la fuente de verdad.

- **Modo `completo`** (demo sin backend): `mocks/handlers/samiProyecto.ts` simula las tres rutas con una versión
  chica del diccionario. En `parcial` y `ninguno` responde el backend de verdad.
- Para probarla con el backend real: levantar el backend y correr el frontend con `VITE_API_MOCKS=parcial`.
  Ojo: en `parcial` el resto de la app (sistemas, avisos) sigue simulado y la sesión es la del backend.
- Todavía no hay una pantalla donde abrir el proyecto creado (el frontend de `dev` no tiene pantallas de
  proyectos): al terminar se ofrece volver al inicio.

## Pendiente

- Frases reales para medir el acierto (ver arriba).
- Que `RespaldoIaSamiService` tenga un tope de gasto en dinero además del de consultas, si hace falta.
