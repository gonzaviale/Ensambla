-- Receta de un producto (T-075): qué insumos (otros registros) y cuánto de cada uno lleva. El costo no se guarda:
-- se calcula al leer con el precio actual de cada insumo, tomado de la columna numérica `campo_precio_id`.
-- Las líneas viajan en un arreglo JSON [{insumoId, cantidad, unidad}]; si se borra un insumo, la línea queda
-- colgando y la interfaz la muestra como eliminada (igual que una conexión). Si se borra el producto o la columna
-- del precio, se borra la receta.

CREATE TABLE receta (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id      uuid NOT NULL,
    producto_id     uuid NOT NULL,
    campo_precio_id uuid NOT NULL,
    lineas          jsonb NOT NULL DEFAULT '[]'::jsonb,
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_receta_producto UNIQUE (producto_id),
    CONSTRAINT ck_receta_lineas CHECK (jsonb_typeof(lineas) = 'array'),
    CONSTRAINT fk_receta_producto FOREIGN KEY (negocio_id, producto_id)
        REFERENCES registro (negocio_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_receta_campo_precio FOREIGN KEY (negocio_id, campo_precio_id)
        REFERENCES campo (negocio_id, id) ON DELETE CASCADE
);

CREATE INDEX ix_receta_negocio_campo_precio ON receta (negocio_id, campo_precio_id);
