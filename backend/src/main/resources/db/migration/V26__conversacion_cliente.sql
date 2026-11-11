-- V26: la conversación de un cliente con Sami por WhatsApp. En qué paso está (elegir el comercio o pedirle) y a qué comercio
-- le está pidiendo. Hay una sola por cliente: si pasa un rato sin hablar, la misma fila se reinicia.

CREATE TABLE conversacion_cliente (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id     uuid NOT NULL REFERENCES cliente_whatsapp (id) ON DELETE CASCADE,
    estado         text NOT NULL CHECK (estado IN ('ELIGIENDO_COMERCIO', 'PIDIENDO')),
    -- Eligiendo: el comercio que Sami le ofrece seguir (el último); pidiendo: el comercio elegido.
    negocio_id     uuid REFERENCES negocio (id) ON DELETE CASCADE,
    -- Comercios entre los que la persona tiene que elegir con un número, en el orden en que se le mostraron.
    opciones       jsonb NOT NULL DEFAULT '[]'::jsonb,
    actualizado_en timestamptz NOT NULL,
    CONSTRAINT ux_conversacion_cliente_cliente UNIQUE (cliente_id),
    CONSTRAINT ck_conversacion_cliente_pidiendo CHECK (estado <> 'PIDIENDO' OR negocio_id IS NOT NULL)
);

COMMENT ON TABLE conversacion_cliente IS 'Paso en el que está la conversación de un cliente con Sami y el comercio al que le pide.';
