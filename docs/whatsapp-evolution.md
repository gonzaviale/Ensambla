# Carga por WhatsApp con Evolution API (ENS-146)

El comerciante vincula su número de WhatsApp con su negocio y desde ahí le manda al bot textos o notas de voz. Esta primera
parte **recibe y guarda** los mensajes (`whatsapp_mensaje`); transcribir (ENS-147), interpretar (ENS-207) y confirmar (ENS-208)
vienen después.

No usa la API de Meta. El puente es [Evolution API](https://github.com/EvolutionAPI/evolution-api): open source, gratis, se
levanta con Docker y se conecta escaneando un QR, como WhatsApp Web. **No es oficial**: WhatsApp puede bloquear el número si se
abusa, así que sirve para el TPI y la demo, no para producción masiva. Todo el acceso al proveedor está detrás del puerto
`MensajeriaWhatsappPort` y del parser `EvolutionWebhookParser`; para pasar a YCloud o a la API oficial se reemplazan esos dos.

## Cómo funciona

1. En la app el usuario pide un código: `POST /api/whatsapp/vinculacion/codigo` (6 caracteres, vigente 10 minutos). La respuesta
   incluye `numeroBot`, el número al que tiene que escribir.
2. Desde su WhatsApp le manda al bot `VINCULAR ABC234`. El bot contesta que el número quedó vinculado.
3. Desde ese momento cada texto o nota de voz que mande queda guardado en el negocio del usuario. Un número sin vincular no carga
   nada en ningún negocio y recibe un aviso.
4. `GET /api/whatsapp/vinculacion` muestra el número vinculado (404 si no hay) y `DELETE` lo desvincula.

Evolution entrega los mensajes a `POST /api/whatsapp/webhook`. Ese endpoint es público y sin CSRF, así que se protege con el
header `X-Ensambla-Webhook-Token`, que tiene que coincidir con `ENSAMBLA_WHATSAPP_WEBHOOK_SECRETO`. Si el secreto está vacío,
rechaza todo. Si el token es válido siempre responde 200, también cuando ignora el evento, para que Evolution no reintente.

## Puesta en marcha local

Necesitás un número de WhatsApp para el bot (un chip o línea aparte) y otro teléfono para probar.

1. En `application-ensambla-secrets.properties` (copiá el `.example`) completá:

   ```properties
   ENSAMBLA_WHATSAPP_PROVEEDOR=evolution
   ENSAMBLA_WHATSAPP_NUMERO_BOT=549XXXXXXXXXX
   ENSAMBLA_WHATSAPP_WEBHOOK_SECRETO=un-secreto-largo
   ENSAMBLA_WHATSAPP_EVOLUTION_API_KEY=otra-clave-larga
   ```

2. Evolution manda el secreto como header: configuralo al crear la instancia (paso 4). Levantá Postgres y Evolution:

   ```bash
   docker compose -f backend/docker-compose.yml --profile whatsapp up -d
   ```

   Evolution queda en `http://localhost:8081` y guarda sus tablas en el esquema `evolution` de la misma base. El backend corre en
   el puerto 8080 y Evolution lo alcanza por `host.docker.internal`.

3. Levantá el backend con las variables de arriba.
4. Creá la instancia con el webhook y el token (reemplazá la clave):

   ```bash
   curl -X POST http://localhost:8081/instance/create \
     -H "apikey: otra-clave-larga" -H "Content-Type: application/json" \
     -d '{"instanceName":"ensambla","integration":"WHATSAPP-BAILEYS","qrcode":true,
          "webhook":{"url":"http://host.docker.internal:8080/api/whatsapp/webhook",
                     "byEvents":false,"events":["MESSAGES_UPSERT"],
                     "headers":{"X-Ensambla-Webhook-Token":"un-secreto-largo"}}}'
   ```

5. Escaneá el QR con el WhatsApp del bot: abrí `http://localhost:8081/manager`, entrá con la clave y conectá la instancia
   `ensambla`.
6. Probá el flujo: iniciá sesión en la app, pedí el código (`POST /api/whatsapp/vinculacion/codigo`), mandá `VINCULAR <código>`
   desde otro teléfono y después «gasté 5000 en harina» o una nota de voz. Revisá la tabla:

   ```sql
   SELECT tipo, texto, mime_audio, duracion_seg, estado FROM whatsapp_mensaje ORDER BY recibido_en;
   ```

Con `ENSAMBLA_WHATSAPP_PROVEEDOR=simulado` (el valor por defecto) el backend no envía nada: las respuestas del bot quedan en el
log. Sirve para desarrollar y es lo que usan los tests.

## Detalles a tener en cuenta

- **Número `@lid`.** WhatsApp está pasando a identificar los chats con un `@lid` en vez del teléfono. El parser usa `senderPn` o
  `remoteJidAlt` para obtener el número real; si no vienen, ignora el mensaje.
- **Audio.** Sólo se guarda el id del mensaje, el tipo y la duración. ENS-147 va a descargar el archivo desde Evolution
  (`/chat/getBase64FromMediaMessage`) cuando lo transcriba.
- **Duplicados.** `id_externo` es único: si Evolution reintenta el webhook, el mensaje no se guarda dos veces.
- **Grupos, estados y mensajes propios** se ignoran.
- **Producción.** Evolution no está en `render.yaml`; para usarlo en Render haría falta desplegarlo aparte y cargar las variables
  `ENSAMBLA_WHATSAPP_*` (ya están declaradas con `sync: false`).
