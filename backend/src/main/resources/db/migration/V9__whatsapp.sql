-- WhatsApp (T-106): un número de WhatsApp se vincula con un negocio y lo que manda queda guardado para interpretarlo
-- después (T-142 los textos, T-107 las notas de voz).
--   * vinculo_whatsapp: una fila por negocio. Para vincular, la app genera un código de 6 dígitos que vence a
--     los 10 minutos y la persona lo manda por WhatsApp desde su número: así prueba que el número es suyo. Al
--     vincular se guarda el número y se borra el código. Un número está vinculado a un solo negocio.
--   * mensaje_whatsapp: cada texto o nota de voz que llegó desde un número vinculado. wa_mensaje_id es el id
--     que pone Meta: es único porque Meta reintenta el aviso y el mismo mensaje no se guarda dos veces. De una
--     nota de voz sólo se guarda el id del archivo en Meta (media_id); se descarga al transcribirla.
CREATE TABLE vinculo_whatsapp (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id       uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    telefono         text,
    vinculado_en     timestamptz,
    codigo           text,
    codigo_vence_en  timestamptz,
    creado_en        timestamptz NOT NULL DEFAULT now(),
    actualizado_en   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_vinculo_whatsapp_negocio UNIQUE (negocio_id),
    CONSTRAINT ux_vinculo_whatsapp_telefono UNIQUE (telefono),
    CONSTRAINT ux_vinculo_whatsapp_codigo UNIQUE (codigo),
    CONSTRAINT ck_vinculo_whatsapp_telefono CHECK (telefono ~ '^[0-9]{8,15}$'),
    CONSTRAINT ck_vinculo_whatsapp_codigo CHECK (codigo ~ '^[0-9]{6}$'),
    CONSTRAINT ck_vinculo_whatsapp_vinculado CHECK ((telefono IS NULL) = (vinculado_en IS NULL)),
    CONSTRAINT ck_vinculo_whatsapp_vence CHECK ((codigo IS NULL) = (codigo_vence_en IS NULL))
);

CREATE TABLE mensaje_whatsapp (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id      uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    telefono        text NOT NULL,
    wa_mensaje_id   text NOT NULL,
    tipo            text NOT NULL,
    texto           text,
    media_id        text,
    estado          text NOT NULL DEFAULT 'PENDIENTE',
    recibido_en     timestamptz NOT NULL,
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_mensaje_whatsapp_wa_mensaje UNIQUE (wa_mensaje_id),
    CONSTRAINT ck_mensaje_whatsapp_tipo CHECK (tipo IN ('TEXTO', 'AUDIO')),
    CONSTRAINT ck_mensaje_whatsapp_estado CHECK (estado IN ('PENDIENTE', 'INTERPRETADO', 'ERROR')),
    CONSTRAINT ck_mensaje_whatsapp_contenido CHECK (
        (tipo = 'TEXTO' AND texto IS NOT NULL AND media_id IS NULL)
        OR (tipo = 'AUDIO' AND media_id IS NOT NULL AND texto IS NULL))
);

-- Los pendientes de cada negocio en orden de llegada (lo que va a leer T-142).
CREATE INDEX ix_mensaje_whatsapp_pendiente ON mensaje_whatsapp (negocio_id, recibido_en) WHERE estado = 'PENDIENTE';
