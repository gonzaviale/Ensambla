-- V35: responder un mini-objetivo manual (H-60, ENS-342). Un mini-objetivo manual es una acción que la persona
-- responde: si la hizo (SI, EN_PARTE o NO) y qué pasó (una respuesta rápida, con un comentario opcional). Se responde una
-- sola vez y la respuesta queda guardada con su fecha, sin modificarse. Cada respuesta también deja una decisión en el
-- historial del objetivo (decision_objetivo), con tres tipos nuevos.

CREATE TABLE respuesta_mini_objetivo (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id       uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    objetivo_id      uuid NOT NULL REFERENCES objetivo (id) ON DELETE CASCADE,
    -- Una sola respuesta por mini-objetivo: no se pisa.
    mini_objetivo_id uuid NOT NULL UNIQUE REFERENCES mini_objetivo (id) ON DELETE CASCADE,
    hizo             text NOT NULL CHECK (hizo IN ('SI', 'EN_PARTE', 'NO')),
    que_paso         text NOT NULL CHECK (que_paso IN ('FUNCIONO', 'NO_TUVE_TIEMPO', 'NO_FUNCIONO', 'OTRA')),
    comentario       text CHECK (comentario IS NULL OR (length(btrim(comentario)) > 0 AND length(comentario) <= 300)),
    -- Lunes de la semana (hora argentina) en que se respondió.
    semana_inicio    date NOT NULL,
    respondida_en    timestamptz NOT NULL,
    -- Si lo hizo, tuvo tiempo.
    CHECK (NOT (hizo = 'SI' AND que_paso = 'NO_TUVE_TIEMPO'))
);

CREATE INDEX ix_respuesta_mini_objetivo_objetivo ON respuesta_mini_objetivo (objetivo_id, respondida_en);

COMMENT ON TABLE respuesta_mini_objetivo IS 'Lo que la persona respondió de un mini-objetivo manual: si lo hizo y qué pasó. Una por mini-objetivo.';

ALTER TABLE decision_objetivo DROP CONSTRAINT decision_objetivo_tipo_check;

ALTER TABLE decision_objetivo ADD CONSTRAINT decision_objetivo_tipo_check CHECK (tipo IN (
    'AVISO_APLICADO', 'AVISO_DESCARTADO', 'PASO_ACEPTADO', 'PASO_IGNORADO',
    'MINI_HECHO', 'MINI_EN_PARTE', 'MINI_NO_HECHO'));
