-- ENS-95: límites aprobados. Precio y moneda premium siguen pendientes de ENS-98.
ALTER TABLE consumo_ia_diario ADD COLUMN paginas_inmediatas integer NOT NULL DEFAULT 0;
ALTER TABLE consumo_ia_diario ADD CONSTRAINT ck_consumo_ia_paginas_inmediatas
    CHECK (paginas_inmediatas >= 0 AND paginas_inmediatas <= paginas);

ALTER TABLE plan ADD COLUMN limites_configurados boolean NOT NULL DEFAULT false;
ALTER TABLE plan ADD CONSTRAINT ck_plan_limites_configurados CHECK (
    NOT limites_configurados OR (
        limite_paginas_ia_mensuales IS NOT NULL
        AND (codigo <> 'GRATUITO' OR (limite_proyectos IS NOT NULL AND limite_registros IS NOT NULL))
    )
);

UPDATE plan SET limite_proyectos = 1, limite_registros = 1000, limite_paginas_ia_mensuales = 10,
    funciones = '[]'::jsonb, limites_configurados = true, actualizado_en = now()
WHERE codigo = 'GRATUITO';

UPDATE plan SET limite_proyectos = NULL, limite_registros = NULL, limite_paginas_ia_mensuales = 100,
    funciones = '["RECETAS_COSTOS", "REPORTES_FILTRADOS", "EXPORTAR_EXCEL"]'::jsonb,
    limites_configurados = true, actualizado_en = now()
WHERE codigo = 'PREMIUM';

COMMENT ON COLUMN plan.limites_configurados IS
    'Capacidades listas para control del servidor; independiente de configurado (precio y moneda).';
