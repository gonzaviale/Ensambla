-- V25: pedidos por WhatsApp, base. Un cliente (número de la lista de prueba) le escribe a Sami para pedirle a un comercio.
-- El cliente no tiene cuenta en Ensambla: queda registrado por su teléfono y lo que escribe espera en su propia cola,
-- separada de la de los comercios porque todavía no pertenece a ningún negocio.

CREATE TABLE cliente_whatsapp (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Sólo dígitos, con código de país y sin "+" (formato E.164).
    telefono          text NOT NULL CHECK (telefono ~ '^[0-9]{8,15}$'),
    -- El último comercio al que le pidió, para ofrecerle seguir con él.
    ultimo_negocio_id uuid REFERENCES negocio (id) ON DELETE SET NULL,
    creado_en         timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_cliente_whatsapp_telefono UNIQUE (telefono)
);

CREATE TABLE mensaje_cliente (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    telefono       text NOT NULL CHECK (telefono ~ '^[0-9]{8,15}$'),
    -- Id del mensaje en WhatsApp: si el proveedor reintenta el webhook, no se duplica.
    id_externo     text NOT NULL CHECK (length(btrim(id_externo)) > 0),
    texto          text NOT NULL,
    estado         text NOT NULL DEFAULT 'RECIBIDO' CHECK (estado IN ('RECIBIDO', 'ATENDIENDO', 'ATENDIDO', 'FALLIDO')),
    recibido_en    timestamptz NOT NULL,
    intentos       integer NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    reclamado_hasta timestamptz,
    atendido_en    timestamptz,
    error          text,
    creado_en      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_mensaje_cliente_id_externo UNIQUE (id_externo)
);

CREATE INDEX ix_mensaje_cliente_cola ON mensaje_cliente (estado, recibido_en);

COMMENT ON TABLE cliente_whatsapp IS 'Cliente que le pide a un comercio por WhatsApp, identificado por su teléfono.';
COMMENT ON TABLE mensaje_cliente IS 'Mensaje de texto de un cliente, en la cola para que Sami lo atienda.';
