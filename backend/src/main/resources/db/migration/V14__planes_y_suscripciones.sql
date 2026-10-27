-- ENS-94 / T-066: catálogo global y una suscripción actual por negocio.
-- Los valores comerciales pendientes no se interpretan como permisos ilimitados.
CREATE TABLE plan (
    id                          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo                      text NOT NULL,
    nombre                      text NOT NULL,
    configurado                 boolean NOT NULL DEFAULT false,
    precio_mensual              numeric(14,2),
    moneda                      text,
    limite_proyectos            integer,
    limite_registros            bigint,
    limite_paginas_ia_mensuales  integer,
    funciones                   jsonb NOT NULL DEFAULT '[]'::jsonb,
    creado_en                   timestamptz NOT NULL DEFAULT now(),
    actualizado_en              timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_plan_codigo UNIQUE (codigo),
    CONSTRAINT ck_plan_codigo CHECK (codigo IN ('GRATUITO', 'PREMIUM')),
    CONSTRAINT ck_plan_nombre CHECK (length(btrim(nombre)) BETWEEN 1 AND 120),
    CONSTRAINT ck_plan_precio CHECK (precio_mensual >= 0),
    CONSTRAINT ck_plan_moneda CHECK (moneda ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_plan_proyectos CHECK (limite_proyectos >= 0),
    CONSTRAINT ck_plan_registros CHECK (limite_registros >= 0),
    CONSTRAINT ck_plan_paginas CHECK (limite_paginas_ia_mensuales >= 0),
    CONSTRAINT ck_plan_funciones CHECK (jsonb_typeof(funciones) = 'array'),
    CONSTRAINT ck_plan_gratuito CHECK (
        codigo <> 'GRATUITO' OR (precio_mensual IS NOT NULL AND precio_mensual = 0)
    ),
    CONSTRAINT ck_plan_configurado CHECK (
        NOT configurado OR (
            precio_mensual IS NOT NULL AND moneda IS NOT NULL
            AND (codigo <> 'PREMIUM' OR precio_mensual > 0)
            AND (codigo <> 'GRATUITO' OR (
                limite_proyectos IS NOT NULL AND limite_paginas_ia_mensuales IS NOT NULL
            ))
        )
    )
);

CREATE TABLE suscripcion (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id      uuid NOT NULL,
    plan_id         uuid NOT NULL,
    estado          text NOT NULL DEFAULT 'ACTIVA',
    inicio_en       timestamptz NOT NULL,
    fin_en          timestamptz,
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_suscripcion_negocio UNIQUE (negocio_id),
    CONSTRAINT fk_suscripcion_negocio FOREIGN KEY (negocio_id) REFERENCES negocio (id) ON DELETE CASCADE,
    CONSTRAINT fk_suscripcion_plan FOREIGN KEY (plan_id) REFERENCES plan (id) ON DELETE RESTRICT,
    CONSTRAINT ck_suscripcion_estado CHECK (estado IN ('ACTIVA', 'PRUEBA', 'VENCIDA', 'CANCELADA')),
    CONSTRAINT ck_suscripcion_periodo CHECK (fin_en IS NULL OR fin_en > inicio_en)
);

CREATE INDEX ix_suscripcion_plan ON suscripcion (plan_id);

INSERT INTO plan (id, codigo, nombre, precio_mensual)
VALUES
    (md5('ensambla:plan:gratuito')::uuid, 'GRATUITO', 'Gratuito', 0),
    (md5('ensambla:plan:premium')::uuid, 'PREMIUM', 'Premium', NULL);

INSERT INTO suscripcion (negocio_id, plan_id, estado, inicio_en)
SELECT negocio.id, plan.id, 'ACTIVA', now()
FROM negocio
CROSS JOIN plan
WHERE plan.codigo = 'GRATUITO';

COMMENT ON COLUMN plan.configurado IS
    'false: catálogo pendiente de definición comercial; no otorga capacidades ilimitadas.';
COMMENT ON COLUMN plan.limite_proyectos IS
    'NULL: pendiente si configurado=false; sin límite sólo en un plan configurado.';
COMMENT ON COLUMN plan.limite_registros IS
    'NULL: pendiente si configurado=false; sin límite sólo en un plan configurado.';
COMMENT ON COLUMN plan.limite_paginas_ia_mensuales IS
    'NULL: pendiente si configurado=false; sin límite sólo en un plan configurado.';
COMMENT ON TABLE suscripcion IS
    'Suscripción actual del negocio. Prueba, cobro y control efectivo se implementan en ENS-95/96/98.';
