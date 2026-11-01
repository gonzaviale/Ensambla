-- V21: verificación del número desde la app y carga de registros con Sami por WhatsApp.
-- La persona carga su número en la app, le llega un código por WhatsApp y lo confirma; después los mensajes que
-- manda (textos y fotos) se atienden en orden y cada carga se confirma en el chat antes de guardarse.

CREATE TABLE whatsapp_verificacion (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id     uuid NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    telefono       text NOT NULL CHECK (telefono ~ '^[0-9]{8,15}$'),
    -- SHA-256 en hexadecimal del código de 6 dígitos: el código en sí nunca se guarda.
    codigo_hash    text NOT NULL CHECK (length(btrim(codigo_hash)) > 0),
    intentos       integer NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    enviado_en     timestamptz NOT NULL,
    expira_en      timestamptz NOT NULL,
    envios_ventana integer NOT NULL DEFAULT 1 CHECK (envios_ventana >= 0),
    ventana_desde  timestamptz NOT NULL,
    CONSTRAINT ux_whatsapp_verificacion_usuario UNIQUE (usuario_id)
);

CREATE INDEX ix_whatsapp_verificacion_telefono ON whatsapp_verificacion (telefono, enviado_en);

-- Fotos y cola de atención. Los CHECK de V20 no tenían nombre: PostgreSQL los llamó <tabla>_<columna>_check.
ALTER TABLE whatsapp_mensaje DROP CONSTRAINT IF EXISTS whatsapp_mensaje_tipo_check;
ALTER TABLE whatsapp_mensaje DROP CONSTRAINT IF EXISTS whatsapp_mensaje_estado_check;
ALTER TABLE whatsapp_mensaje DROP CONSTRAINT IF EXISTS ck_whatsapp_mensaje_contenido;
ALTER TABLE whatsapp_mensaje RENAME COLUMN mime_audio TO mime_media;
ALTER TABLE whatsapp_mensaje
    ADD COLUMN intentos        integer NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    ADD COLUMN reclamado_hasta timestamptz,
    ADD COLUMN atendido_en     timestamptz,
    ADD COLUMN error           text,
    ADD CONSTRAINT ck_whatsapp_mensaje_tipo CHECK (tipo IN ('TEXTO', 'AUDIO', 'IMAGEN')),
    ADD CONSTRAINT ck_whatsapp_mensaje_estado CHECK (estado IN ('RECIBIDO', 'ATENDIENDO', 'ATENDIDO', 'FALLIDO')),
    ADD CONSTRAINT ck_whatsapp_mensaje_contenido CHECK (
        (tipo = 'TEXTO' AND texto IS NOT NULL) OR (tipo IN ('AUDIO', 'IMAGEN') AND mime_media IS NOT NULL));

-- Lo que ya estaba guardado se da por atendido: no se reprocesa con IA.
UPDATE whatsapp_mensaje SET estado = 'ATENDIDO', atendido_en = recibido_en WHERE estado = 'RECIBIDO';

DROP INDEX ix_whatsapp_mensaje_negocio_estado;
CREATE INDEX ix_whatsapp_mensaje_cola ON whatsapp_mensaje (estado, recibido_en);

CREATE TABLE whatsapp_propuesta (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    telefono   text NOT NULL CHECK (telefono ~ '^[0-9]{8,15}$'),
    origen     text NOT NULL CHECK (origen IN ('TEXTO', 'FOTO')),
    estado     text NOT NULL CHECK (estado IN ('ELIGIENDO_TABLA', 'PENDIENTE', 'CONFIRMADA', 'DESCARTADA', 'VENCIDA')),
    -- Sin FK: si la tabla se borra antes de confirmar, se avisa al confirmar.
    bloque_id  uuid,
    -- Mensaje original: sirve para volver a interpretarlo cuando la persona elige la tabla.
    texto      text,
    -- Lectura de la foto: sirve para asignarla a la tabla elegida sin volver a llamar a la IA.
    lectura    jsonb,
    -- Ids de las tablas ofrecidas, en el orden de la lista numerada.
    opciones   jsonb NOT NULL DEFAULT '[]'::jsonb,
    filas      jsonb NOT NULL DEFAULT '[]'::jsonb,
    creado_en  timestamptz NOT NULL,
    expira_en  timestamptz NOT NULL,
    cerrado_en timestamptz
);

-- Una sola propuesta abierta por teléfono.
CREATE UNIQUE INDEX ux_whatsapp_propuesta_abierta ON whatsapp_propuesta (telefono)
    WHERE estado IN ('ELIGIENDO_TABLA', 'PENDIENTE');
CREATE INDEX ix_whatsapp_propuesta_negocio ON whatsapp_propuesta (negocio_id);

COMMENT ON TABLE whatsapp_verificacion IS 'Código enviado por WhatsApp para verificar el número que la persona cargó en la app.';
COMMENT ON TABLE whatsapp_propuesta IS 'Carga que Sami armó desde un texto o una foto y que espera confirmación en el chat.';
