-- V24: los objetivos del negocio (lo que la persona le cuenta a Sami y la meta mensual que busca). Hasta ahora vivían
-- en el navegador; cada negocio ve solo los suyos. El historial de avance no se guarda: lo calcula el frontend.

CREATE TABLE objetivo (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    texto      text NOT NULL CHECK (length(btrim(texto)) > 0),
    -- Resultado mensual que se quiere alcanzar, en pesos.
    meta       numeric(14, 2) NOT NULL CHECK (meta > 0),
    creado_en  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_objetivo_negocio_creado ON objetivo (negocio_id, creado_en);

COMMENT ON TABLE objetivo IS 'Objetivos que cada negocio le cuenta a Sami, con su meta mensual.';
