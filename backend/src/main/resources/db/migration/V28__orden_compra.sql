-- V28: la orden de compra que nace cuando el cliente confirma la lista que Sami le armó con el stock del comercio, y el
-- renglón de la lista que el cliente está cambiando de marca. El dueño la gestiona desde la app (aceptar, rechazar, en
-- preparación, lista para retirar); el stock se descuenta al aceptarla, no al confirmar.

ALTER TABLE conversacion_cliente
    ADD COLUMN renglon_a_cambiar integer CHECK (renglon_a_cambiar IS NULL OR renglon_a_cambiar > 0);

COMMENT ON COLUMN conversacion_cliente.renglon_a_cambiar IS 'Número (desde 1) del renglón de la lista para el que se ofrecieron otras marcas; vacío si no está cambiando ninguno.';

CREATE TABLE orden_compra (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id     uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    cliente_id     uuid NOT NULL REFERENCES cliente_whatsapp (id),
    estado         text NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'EN_PREPARACION', 'LISTA')),
    -- Lo que confirmó el cliente: cada ítem guarda el producto, su nombre, la cantidad y el precio de ese momento.
    items          jsonb NOT NULL,
    total          numeric(14, 2) NOT NULL CHECK (total >= 0),
    motivo_rechazo text,
    creado_en      timestamptz NOT NULL,
    actualizado_en timestamptz NOT NULL,
    CONSTRAINT ck_orden_compra_motivo CHECK (motivo_rechazo IS NULL OR estado = 'RECHAZADA')
);

CREATE INDEX ix_orden_compra_negocio_estado ON orden_compra (negocio_id, estado, creado_en DESC);

COMMENT ON TABLE orden_compra IS 'Orden de compra que un cliente confirmó por WhatsApp y que el dueño del comercio gestiona desde la app.';
