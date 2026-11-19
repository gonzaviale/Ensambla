ALTER TABLE venta_caja ADD COLUMN inventario_id uuid;
UPDATE venta_caja
SET inventario_id = split_part(datos->>'huella', ':', 2)::uuid,
    datos = datos || jsonb_build_object(
        'inventarioId', split_part(datos->>'huella', ':', 2),
        'pagos', jsonb_build_array(jsonb_build_object('medio', datos->>'medio', 'importe', datos->'total'))
    );
ALTER TABLE venta_caja ALTER COLUMN inventario_id SET NOT NULL;

CREATE TABLE cierre_caja (
    id uuid PRIMARY KEY,
    negocio_id uuid NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
    proyecto_id uuid NOT NULL,
    inventario_id uuid NOT NULL,
    datos jsonb NOT NULL,
    creado_en timestamptz NOT NULL,
    actualizado_en timestamptz NOT NULL,
    CONSTRAINT ck_cierre_caja_datos CHECK (jsonb_typeof(datos) = 'object')
);
ALTER TABLE venta_caja ADD COLUMN cierre_id uuid REFERENCES cierre_caja(id);
CREATE INDEX ix_venta_caja_abierta ON venta_caja (negocio_id, proyecto_id, inventario_id, creado_en)
    WHERE cierre_id IS NULL;
CREATE INDEX ix_cierre_caja_historial ON cierre_caja (negocio_id, proyecto_id, inventario_id, creado_en DESC);

