-- V31: las decisiones de la persona frente a un objetivo (ENS-269). Cada vez que aplica o descarta un aviso, o acepta o
-- ignora el paso de un mini-objetivo, queda una fila por objetivo activo del negocio para dibujarla en el gráfico de la
-- semana y, cuando se pueda, mostrar qué efecto tuvo.

CREATE TABLE decision_objetivo (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id       uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    objetivo_id      uuid NOT NULL REFERENCES objetivo (id) ON DELETE CASCADE,
    -- El mini-objetivo en curso del producto del aviso, o el que se aceptó o ignoró; vacío si ninguno coincide.
    mini_objetivo_id uuid REFERENCES mini_objetivo (id) ON DELETE SET NULL,
    tipo             text NOT NULL CHECK (tipo IN ('AVISO_APLICADO', 'AVISO_DESCARTADO', 'PASO_ACEPTADO', 'PASO_IGNORADO')),
    -- El aviso que se resolvió, si es una decisión sobre un aviso. Sigue ahí aunque el aviso se borre.
    aviso_id         uuid REFERENCES aviso (id) ON DELETE SET NULL,
    -- El producto del aviso o del mini-objetivo. Sin FK: el producto puede borrarse del cuaderno.
    registro_id      uuid,
    -- El título del aviso o del paso, para el gráfico.
    descripcion      text NOT NULL CHECK (length(btrim(descripcion)) > 0),
    tomada_en        timestamptz NOT NULL,
    -- Lunes de la semana (hora argentina) en que se tomó.
    semana_inicio    date NOT NULL
);

CREATE INDEX ix_decision_objetivo_semana ON decision_objetivo (objetivo_id, semana_inicio);

-- Un aviso se registra una sola vez por objetivo.
CREATE UNIQUE INDEX ux_decision_aviso ON decision_objetivo (objetivo_id, aviso_id) WHERE aviso_id IS NOT NULL;

COMMENT ON TABLE decision_objetivo IS 'Lo que la persona decidió (avisos aplicados o descartados, pasos aceptados o ignorados) frente a cada objetivo.';
