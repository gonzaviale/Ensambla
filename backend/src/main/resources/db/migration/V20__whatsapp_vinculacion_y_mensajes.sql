-- V12: carga por WhatsApp. El comerciante vincula su número con un código que manda por el chat;
-- los mensajes que llegan de un número vinculado quedan guardados para interpretarlos después.

CREATE TABLE whatsapp_vinculo (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id   uuid NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    negocio_id   uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    -- Sólo dígitos, con código de país y sin "+" (formato E.164).
    telefono     text NOT NULL CHECK (telefono ~ '^[0-9]{8,15}$'),
    vinculado_en timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_whatsapp_vinculo_usuario UNIQUE (usuario_id),
    CONSTRAINT ux_whatsapp_vinculo_telefono UNIQUE (telefono)
);

CREATE TABLE whatsapp_codigo_vinculacion (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id uuid NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    codigo     text NOT NULL CHECK (length(btrim(codigo)) > 0),
    expira_en  timestamptz NOT NULL,
    usado_en   timestamptz,
    creado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_whatsapp_codigo UNIQUE (codigo)
);

CREATE INDEX ix_whatsapp_codigo_usuario ON whatsapp_codigo_vinculacion (usuario_id);

CREATE TABLE whatsapp_mensaje (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id   uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    telefono     text NOT NULL,
    -- Id del mensaje en WhatsApp: si el proveedor reintenta el webhook, no se duplica.
    id_externo   text NOT NULL CHECK (length(btrim(id_externo)) > 0),
    tipo         text NOT NULL CHECK (tipo IN ('TEXTO', 'AUDIO')),
    texto        text,
    mime_audio   text,
    duracion_seg integer CHECK (duracion_seg IS NULL OR duracion_seg >= 0),
    estado       text NOT NULL DEFAULT 'RECIBIDO' CHECK (estado IN ('RECIBIDO')),
    recibido_en  timestamptz NOT NULL,
    creado_en    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_whatsapp_mensaje_id_externo UNIQUE (id_externo),
    CONSTRAINT ck_whatsapp_mensaje_contenido CHECK (
        (tipo = 'TEXTO' AND texto IS NOT NULL) OR (tipo = 'AUDIO' AND mime_audio IS NOT NULL))
);

CREATE INDEX ix_whatsapp_mensaje_negocio_estado ON whatsapp_mensaje (negocio_id, estado);

COMMENT ON TABLE whatsapp_vinculo IS 'Número de WhatsApp vinculado a un usuario y a su negocio.';
COMMENT ON TABLE whatsapp_codigo_vinculacion IS 'Código de un solo uso que el usuario manda por WhatsApp para vincular su número.';
COMMENT ON TABLE whatsapp_mensaje IS 'Mensaje recibido de un número vinculado, listo para transcribir e interpretar.';
