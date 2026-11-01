# Sami por WhatsApp con Evolution API (ENS-122 / ENS-146)

El comerciante verifica su número de WhatsApp en la app y desde ahí le manda a Sami textos o fotos del cuaderno. Sami arma las
filas, las muestra por el chat y **recién las guarda cuando la persona responde que sí**. Es el mismo Sami de la app, con los
mismos límites de uso.

No usa la API de Meta. El puente es [Evolution API](https://github.com/EvolutionAPI/evolution-api): open source, gratis, se
levanta con Docker y se conecta escaneando un QR, como WhatsApp Web. **No es oficial**: WhatsApp puede bloquear el número si se
abusa, así que sirve para el TPI y la demo, no para producción masiva. Todo el acceso al proveedor está detrás del puerto
`MensajeriaWhatsappPort` y del parser `EvolutionWebhookParser`; para pasar a YCloud o a la API oficial se reemplazan esos dos.

## Cómo funciona

### 1. Verificar el número (desde la app)

1. En **Tu cuenta › WhatsApp** la persona escribe su número con código de país (`POST /api/whatsapp/verificacion`).
2. Ensambla le manda por WhatsApp un código de 6 dígitos. Vence a los 10 minutos, se puede pedir otro cada 60 segundos (hasta 5
   por hora) y alcanzan 5 intentos. De la base sólo se guarda la huella (SHA-256), nunca el código. Además, a un mismo número no
   se le piden más de 3 códigos por hora entre todas las cuentas.
3. La persona escribe el código en la app (`POST /api/whatsapp/verificacion/confirmar`). Si es correcto el número queda vinculado
   a su negocio y Sami le da la bienvenida por WhatsApp.
4. `GET /api/whatsapp/vinculacion` devuelve el número vinculado y el del bot (404 si no hay) y `DELETE` lo desvincula.

El camino anterior sigue funcionando: `POST /api/whatsapp/vinculacion/codigo` da un código de 6 caracteres y la persona le manda
`VINCULAR ABC234` al bot desde su teléfono. Un número sin vincular no carga nada en ningún negocio y recibe un aviso.

### 2. Cargar registros por el chat

Cada mensaje de un número vinculado se guarda en `whatsapp_mensaje` y un proceso (`AtencionWhatsappWorker`) los atiende de a
uno y en orden, así el webhook contesta enseguida y nada se pierde si el servidor se reinicia.

| Mensaje | Qué hace Sami |
|---|---|
| Texto («gasté 5000 en harina») | Descuenta una consulta de Sami del día, la IA elige la tabla y arma las filas. |
| Foto del cuaderno | Descuenta una página del cupo mensual del plan (10 en el gratuito, 100 en el premium) y del límite diario. Si el pie de la foto nombra una tabla, lee contra esa. |
| Nota de voz | Responde que por ahora sólo entiende textos y fotos (la transcripción es ENS-147). |

Después muestra el resumen («Para *Gastos* entendí: 1) Concepto: harina · Monto: 5000 …») con `(?)` en lo que no pudo leer bien:

- **SÍ** guarda las filas, con origen «WhatsApp». Valida el límite de registros del plan.
- **Productos que ya existen.** En las tablas de cosas (inventario, clientes y formularios propios) Sami busca el producto
  entre las filas que ya hay, aunque la persona lo escriba como pueda (sin tildes, en plural, con una letra mal o con
  sólo parte del nombre). Si lo encuentra, el resumen dice «Actualizo *Harina 000*: Monto: 5000» y al confirmar le cambia
  sólo los datos que vinieron, sin tocar el nombre. Si no hay un producto parecido, o hay dos igual de parecidos, agrega
  una fila nueva («Agrego: …»). En las tablas de movimientos (ventas, gastos, pedidos) cada mensaje es una fila nueva.
- **NO** descarta. Para corregir, se responde NO y se manda de nuevo.
- Si no está segura de la tabla (o la foto no se parece a ninguna) manda una lista numerada; se responde con el número y no se
  vuelve a leer la foto ni se cobra otra página.
- Si ninguna tabla corresponde, pide aclaración en lugar de guardar.
- Un mensaje nuevo con una propuesta abierta la descarta (y se lo dice). Las propuestas vencen a los 30 minutos.

Si falla la IA el mensaje queda `FALLIDO` y la persona lo vuelve a mandar: no hay reintentos automáticos para no cobrar dos veces.

## Webhook

Evolution entrega los mensajes a `POST /api/whatsapp/webhook`. Ese endpoint es público y sin CSRF, así que se protege con el
secreto `ENSAMBLA_WHATSAPP_WEBHOOK_SECRETO`, que se acepta de dos formas:

- **`?token=<secreto>`** en la URL. Es lo que usan los `docker-compose`, porque el webhook *global* de Evolution no permite
  agregar headers.
- Header `X-Ensambla-Webhook-Token`, para el webhook por instancia (ver abajo).

Si el secreto está vacío rechaza todo. Con un token válido siempre responde 200, también cuando ignora el evento, para que
Evolution no reintente. Como el token va en la URL queda en los logs de Evolution; por eso Evolution tiene que estar en la red
interna (en el VPS sólo escucha en el loopback).

## Puesta en marcha local

Necesitás un número de WhatsApp para el bot (un chip o línea aparte) y otro teléfono para probar.

1. Copiá `backend/.env.example` como `backend/.env` y completá las variables de WhatsApp (el secreto y la clave de Evolution).
   Las mismas variables tienen que llegar al backend (por ejemplo en `application-ensambla-secrets.properties`):

   ```properties
   ENSAMBLA_WHATSAPP_PROVEEDOR=evolution
   ENSAMBLA_WHATSAPP_NUMERO_BOT=549XXXXXXXXXX
   ENSAMBLA_WHATSAPP_WEBHOOK_SECRETO=un-secreto-largo
   ENSAMBLA_WHATSAPP_EVOLUTION_API_KEY=otra-clave-larga
   ```

2. Levantá Postgres y Evolution juntos:

   ```bash
   docker compose -f backend/docker-compose.yml up -d
   ```

   Evolution queda en `http://localhost:8081`, guarda sus tablas en el esquema `evolution` de la misma base y manda los
   mensajes al backend (puerto 8080) por `host.docker.internal`, ya con `?token=`.

3. Levantá el backend con las variables de arriba.
4. Creá la instancia (reemplazá la clave):

   ```bash
   curl -X POST http://localhost:8081/instance/create \
     -H "apikey: otra-clave-larga" -H "Content-Type: application/json" \
     -d '{"instanceName":"ensambla","integration":"WHATSAPP-BAILEYS","qrcode":true}'
   ```

5. Escaneá el QR con el WhatsApp del bot: abrí `http://localhost:8081/manager`, entrá con la clave y conectá la instancia
   `ensambla`.
6. Probá el flujo: en la app andá a Tu cuenta › WhatsApp, cargá tu número, escribí el código que te llega y mandale a Sami
   «gasté 5000 en harina» o una foto del cuaderno. Revisá la cola y las propuestas:

   ```sql
   SELECT tipo, texto, estado, intentos, error FROM whatsapp_mensaje ORDER BY recibido_en;
   SELECT origen, estado, bloque_id, expira_en FROM whatsapp_propuesta ORDER BY creado_en;
   ```

Con `ENSAMBLA_WHATSAPP_PROVEEDOR=simulado` (el valor por defecto) el backend no envía nada: las respuestas del bot quedan en el
log y las fotos se «descargan» de un archivo de ejemplo. Sirve para desarrollar y es lo que usan los tests.

### Webhook por instancia (alternativa)

Si preferís un header en vez del `?token=`, sacá el webhook global del compose y configurá la instancia al crearla:

```bash
curl -X POST http://localhost:8081/instance/create \
  -H "apikey: otra-clave-larga" -H "Content-Type: application/json" \
  -d '{"instanceName":"ensambla","integration":"WHATSAPP-BAILEYS","qrcode":true,
       "webhook":{"url":"http://host.docker.internal:8080/api/whatsapp/webhook",
                  "byEvents":false,"events":["MESSAGES_UPSERT"],
                  "headers":{"X-Ensambla-Webhook-Token":"un-secreto-largo"}}}'
```

## Producción (VPS con Docker Compose)

`deploy/docker-compose.yml` ya define Evolution (con Redis) detrás del perfil `whatsapp`. Para encenderlo, en el `.env` del VPS:

```bash
COMPOSE_PROFILES=whatsapp
ENSAMBLA_WHATSAPP_PROVEEDOR=evolution
EVOLUTION_API_KEY=<openssl rand -hex 32>
ENSAMBLA_WHATSAPP_WEBHOOK_SECRETO=<openssl rand -hex 32>
ENSAMBLA_WHATSAPP_NUMERO_BOT=549XXXXXXXXXX
```

`deploy.sh` no cambia: `docker compose` lee `COMPOSE_PROFILES` del `.env`. El backend le habla a Evolution por la red interna
(`http://evolution-api:8080`) y Evolution le entrega los mensajes a `http://backend:8080/api/whatsapp/webhook?token=…`. Evolution
no se publica en internet: se entra a su panel con un túnel SSH (`ssh -L 8081:localhost:8081 usuario@vps`) para crear la
instancia y escanear el QR, igual que en local.

## Detalles a tener en cuenta

- **Números de Argentina.** WhatsApp los identifica como `549…`. Si la persona escribe `+54 11 2233 4455` (sin el 9) se lo
  agregamos al verificar; si no, el número verificado nunca coincidiría con el remitente de los mensajes.
- **Número `@lid`.** WhatsApp está pasando a identificar los chats con un `@lid` en vez del teléfono. El parser usa `senderPn` o
  `remoteJidAlt` para obtener el número real; si no vienen, ignora el mensaje.
- **Fotos.** Sólo se guarda el id del mensaje y el tipo; la imagen se baja de Evolution
  (`/chat/getBase64FromMediaMessage`) al atender el mensaje, antes de cobrar la página. Para eso Evolution tiene que guardar los
  mensajes (`DATABASE_SAVE_DATA_NEW_MESSAGE=true`, ya puesto en los compose).
- **Duplicados.** `id_externo` es único: si Evolution reintenta el webhook, el mensaje no se guarda dos veces.
- **Grupos, estados y mensajes propios** se ignoran.
- **Pestaña abierta.** La app guarda cada sistema completo; para que no borre las filas cargadas por WhatsApp, el `GET` de
  sistemas devuelve la hora de lectura (`X-Leido-En`), el `PUT` la manda de vuelta y el servidor conserva las filas creadas
  después. Además la app recarga los sistemas al volver a la pestaña.
