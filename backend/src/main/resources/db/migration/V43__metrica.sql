-- V43: las métricas a medida de un cuaderno (ENS-260). La persona guarda en la home las que le sugiere su rubro o las
-- que arma ella. Lo que se guarda es la receta (qué tabla, qué cálculo, cómo repartir, qué período), nunca el número:
-- el servidor lo calcula cada vez con los datos de hoy.

CREATE TABLE metrica (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id  uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    proyecto_id uuid NOT NULL REFERENCES proyecto (id) ON DELETE CASCADE,
    nombre      text NOT NULL CHECK (length(btrim(nombre)) > 0),
    fuente      text NOT NULL CHECK (fuente IN ('VENTAS', 'GASTOS', 'INVENTARIO')),
    calculo     text NOT NULL CHECK (calculo IN ('SUMA', 'PROMEDIO', 'CONTEO', 'UNIDADES')),
    agrupar     text NOT NULL CHECK (agrupar IN ('NINGUNA', 'PRODUCTO', 'CLIENTE', 'CATEGORIA', 'DIA_DE_LA_SEMANA')),
    periodo     text NOT NULL CHECK (periodo IN ('HOY', 'SEMANA', 'MES', 'TODO')),
    solo_fiado  boolean NOT NULL DEFAULT false,
    -- Lugar en el panel de la home, de menor a mayor.
    orden       integer NOT NULL CHECK (orden >= 0),
    -- SUGERIDA: salió del catálogo del rubro. PROPIA: la armó la persona.
    autoria     text NOT NULL CHECK (autoria IN ('SUGERIDA', 'PROPIA')),
    creado_en   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_metrica_proyecto_orden ON metrica (proyecto_id, orden);

COMMENT ON TABLE metrica IS 'Métricas a medida de cada cuaderno: la receta de cálculo, no el resultado.';
