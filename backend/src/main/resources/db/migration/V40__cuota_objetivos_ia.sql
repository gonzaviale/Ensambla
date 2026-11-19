-- V36: cuota mensual de objetivos armados con Sami con IA (H-60, ENS-338). Gratuito 5 y Premium 20 por mes calendario
-- argentino. Cuenta cada objetivo que se guarda armado con Sami con IA, con todas sus renovaciones semanales; un
-- objetivo monetario del formulario clásico o un seguimiento manual no cuentan.

ALTER TABLE plan ADD COLUMN limite_objetivos_ia_mensuales integer;
ALTER TABLE plan ADD CONSTRAINT ck_plan_objetivos_ia CHECK (limite_objetivos_ia_mensuales >= 0);

UPDATE plan SET limite_objetivos_ia_mensuales = 5, actualizado_en = now() WHERE codigo = 'GRATUITO';
UPDATE plan SET limite_objetivos_ia_mensuales = 20, actualizado_en = now() WHERE codigo = 'PREMIUM';

-- Una fila por objetivo con IA guardado. `objetivo_id` no tiene clave foránea: borrar el objetivo no devuelve la cuota
-- del mes en que se guardó.
CREATE TABLE consumo_objetivo_ia (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id   uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    objetivo_id  uuid NOT NULL,
    consumido_en timestamptz NOT NULL
);

CREATE INDEX ix_consumo_objetivo_ia_negocio ON consumo_objetivo_ia (negocio_id, consumido_en);

COMMENT ON COLUMN plan.limite_objetivos_ia_mensuales IS 'Objetivos con IA que se pueden guardar por mes; vacío es sin límite.';
COMMENT ON TABLE consumo_objetivo_ia IS 'Objetivos armados con Sami con IA que ya gastaron cuota del mes en que se guardaron.';
