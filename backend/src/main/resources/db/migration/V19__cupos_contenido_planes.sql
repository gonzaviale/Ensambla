CREATE TABLE contenido_plan_desbloqueado (
    negocio_id uuid NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
    tipo varchar(10) NOT NULL CHECK (tipo IN ('AVISOS', 'ZONA')),
    periodo date NOT NULL CHECK (EXTRACT(DAY FROM periodo) = 1),
    clave varchar(240) NOT NULL,
    creado_en timestamptz NOT NULL,
    PRIMARY KEY (negocio_id, tipo, periodo, clave)
);

COMMENT ON TABLE contenido_plan_desbloqueado IS
    'Tres avisos y tres recomendaciones de zona por mes UTC en Gratuito; claves estables por negocio.';

UPDATE plan SET funciones = funciones || '["AVISOS_COMPLETOS","ZONA_COMPLETA"]'::jsonb,
    actualizado_en = now() WHERE codigo = 'PREMIUM';
