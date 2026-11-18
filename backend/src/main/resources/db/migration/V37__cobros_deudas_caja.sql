CREATE TABLE cobro_caja (
    id uuid PRIMARY KEY,
    negocio_id uuid NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
    proyecto_id uuid NOT NULL,
    inventario_id uuid NOT NULL,
    cierre_id uuid REFERENCES cierre_caja(id),
    datos jsonb NOT NULL,
    creado_en timestamptz NOT NULL,
    actualizado_en timestamptz NOT NULL,
    CONSTRAINT ck_cobro_caja_datos CHECK (jsonb_typeof(datos) = 'object')
);

CREATE INDEX ix_cobro_caja_dia ON cobro_caja (negocio_id, proyecto_id, inventario_id, creado_en, id);
CREATE INDEX ix_cobro_caja_abierto ON cobro_caja (negocio_id, proyecto_id, inventario_id, creado_en, id)
    WHERE cierre_id IS NULL;
