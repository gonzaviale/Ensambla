-- V22: historial de los precios de venta de los productos (lo que el motor de avisos necesita para saber cuándo hubo un
-- aumento) y un tipo de aviso nuevo: REVISAR_PRECIO, para cuando las ventas caen después de subir un precio.

CREATE TABLE historial_precio (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id      uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    registro_id     uuid NOT NULL REFERENCES registro (id) ON DELETE CASCADE,
    -- null cuando es el primer precio que se carga.
    precio_anterior numeric(14, 2) CHECK (precio_anterior IS NULL OR precio_anterior > 0),
    precio_nuevo    numeric(14, 2) NOT NULL CHECK (precio_nuevo > 0),
    cambiado_en     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_historial_precio_negocio_fecha ON historial_precio (negocio_id, cambiado_en);

COMMENT ON TABLE historial_precio IS 'Cada cambio del precio de un producto del inventario, con el precio anterior y el nuevo.';

ALTER TABLE aviso DROP CONSTRAINT aviso_tipo_check;

ALTER TABLE aviso
    ADD CONSTRAINT aviso_tipo_check
        CHECK (tipo IN ('STOCK', 'PRECIO_ZONA', 'ESTANCADO', 'MUY_VENDIDO', 'COMBO', 'CONSUMO_ZONA', 'REVISAR_PRECIO'));
