-- V44: cupo mensual de carteles redactados con IA (ENS-364 / T-265), aparte del de análisis de fotos y del de
-- objetivos. Cifras provisorias: 10 por mes en Gratuito y 40 en Premium. Un límite nulo no limita.

ALTER TABLE plan ADD COLUMN limite_carteles_ia_mensuales integer CHECK (limite_carteles_ia_mensuales >= 0);

UPDATE plan SET limite_carteles_ia_mensuales = 10 WHERE codigo = 'GRATUITO';
UPDATE plan SET limite_carteles_ia_mensuales = 40 WHERE codigo = 'PREMIUM';

COMMENT ON COLUMN plan.limite_carteles_ia_mensuales IS
    'Carteles redactados con IA por mes argentino; NULL no limita. Provisorio: 10 Gratuito, 40 Premium.';

CREATE TABLE consumo_cartel_ia (
    negocio_id     uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    mes            date NOT NULL CHECK (EXTRACT(DAY FROM mes) = 1),
    cantidad       integer NOT NULL DEFAULT 0 CHECK (cantidad >= 0),
    creado_en      timestamptz NOT NULL DEFAULT now(),
    actualizado_en timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (negocio_id, mes)
);
