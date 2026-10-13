-- Inventario por movimientos (T-074): cada fila de entrada o salida de un producto (un registro de cualquier bloque).
-- El stock no se guarda: es la suma de las entradas menos las salidas. Si se borra el producto, se borra su historial.

-- La FK compuesta por negocio necesita una clave única (negocio_id, id), igual que bloque y campo.
ALTER TABLE registro ADD CONSTRAINT ux_registro_negocio_id UNIQUE (negocio_id, id);

CREATE TABLE movimiento_stock (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id  uuid NOT NULL,
    registro_id uuid NOT NULL,
    tipo        text NOT NULL,
    cantidad    numeric(14, 3) NOT NULL,
    fecha       date NOT NULL,
    nota        text,
    creado_en   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA')),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_movimiento_nota CHECK (nota IS NULL OR length(nota) <= 300),
    CONSTRAINT fk_movimiento_registro FOREIGN KEY (negocio_id, registro_id)
        REFERENCES registro (negocio_id, id) ON DELETE CASCADE
);

CREATE INDEX ix_movimiento_registro ON movimiento_stock (negocio_id, registro_id, creado_en DESC);
