-- Reclamos con token: un intento viejo no puede finalizar el trabajo de uno nuevo.
ALTER TABLE lote_subida
    ADD COLUMN reclamo_id uuid,
    ADD COLUMN reclamo_hasta timestamptz,
    ADD COLUMN proximo_intento_en timestamptz,
    ADD COLUMN paginas_reservadas integer NOT NULL DEFAULT 0 CHECK (paginas_reservadas >= 0),
    ADD COLUMN dia_reserva date,
    ADD CONSTRAINT ck_lote_reclamo CHECK ((reclamo_id IS NULL) = (reclamo_hasta IS NULL)),
    ADD CONSTRAINT ck_lote_reserva CHECK ((paginas_reservadas = 0) = (dia_reserva IS NULL));

CREATE TABLE consumo_ia_diario (
    negocio_id uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    fecha date NOT NULL,
    paginas integer NOT NULL DEFAULT 0 CHECK (paginas >= 0),
    reservadas integer NOT NULL DEFAULT 0 CHECK (reservadas >= 0),
    creado_en timestamptz NOT NULL DEFAULT now(),
    actualizado_en timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (negocio_id, fecha)
);

ALTER TABLE lectura_ia
    ADD COLUMN reclamo_id uuid UNIQUE,
    ADD COLUMN propuesta jsonb CHECK (propuesta IS NULL OR jsonb_typeof(propuesta) = 'object'),
    ADD COLUMN dia_consumo date,
    ADD COLUMN consumo_completo boolean NOT NULL DEFAULT true;

UPDATE lectura_ia SET dia_consumo = (creado_en AT TIME ZONE 'UTC')::date;
ALTER TABLE lectura_ia ALTER COLUMN dia_consumo SET NOT NULL;
ALTER TABLE lectura_ia ALTER COLUMN dia_consumo SET DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date;

CREATE INDEX ix_lote_reclamo_vencido ON lote_subida (reclamo_hasta) WHERE estado = 'PROCESANDO';

-- Respetar el consumo ya registrado antes de activar el worker.
INSERT INTO consumo_ia_diario (negocio_id, fecha, paginas)
SELECT negocio_id, (creado_en AT TIME ZONE 'UTC')::date, SUM(paginas)
FROM lectura_ia
GROUP BY negocio_id, (creado_en AT TIME ZONE 'UTC')::date;
