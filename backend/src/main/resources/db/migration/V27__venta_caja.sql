CREATE TABLE venta_caja (
    id uuid PRIMARY KEY,
    negocio_id uuid NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
    proyecto_id uuid NOT NULL,
    datos jsonb NOT NULL,
    creado_en timestamptz NOT NULL,
    actualizado_en timestamptz NOT NULL,
    CONSTRAINT ck_venta_caja_datos CHECK (jsonb_typeof(datos) = 'object')
);
CREATE INDEX ix_venta_caja_negocio ON venta_caja (negocio_id, proyecto_id, creado_en DESC);
