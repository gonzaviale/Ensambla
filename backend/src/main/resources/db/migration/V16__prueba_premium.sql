-- Una prueba por negocio; su historial sobrevive al cambio de plan y al vencimiento.
CREATE TABLE prueba_premium (
    negocio_id uuid PRIMARY KEY,
    inicio_en timestamptz NOT NULL,
    fin_en timestamptz NOT NULL,
    paginas_ia integer NOT NULL DEFAULT 0,
    CONSTRAINT fk_prueba_premium_negocio FOREIGN KEY (negocio_id) REFERENCES negocio(id) ON DELETE CASCADE,
    CONSTRAINT ck_prueba_premium_duracion CHECK (fin_en = inicio_en + interval '168 hours'),
    CONSTRAINT ck_prueba_premium_paginas CHECK (paginas_ia BETWEEN 0 AND 10)
);

CREATE INDEX ix_suscripcion_pruebas_vencidas ON suscripcion(fin_en, negocio_id)
    WHERE estado = 'PRUEBA';

ALTER TABLE suscripcion ADD CONSTRAINT ck_suscripcion_prueba_periodo CHECK (
    estado <> 'PRUEBA' OR (fin_en IS NOT NULL AND fin_en = inicio_en + interval '168 hours')
);
