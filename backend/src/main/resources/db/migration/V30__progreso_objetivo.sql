-- V30: el monitoreo silencioso de los objetivos (ENS-268). Cada hora el servidor mide el resultado del negocio y la
-- métrica de cada mini-objetivo en curso y guarda una foto por día; no crea avisos ni manda mensajes. Lo que se ve
-- de ahí es sólo lo que la persona consulta al entrar.

-- Desde qué día el valor de un mini-objetivo en curso no mejora; vacío si viene mejorando.
ALTER TABLE mini_objetivo ADD COLUMN sin_avance_desde date;

CREATE TABLE progreso_objetivo (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    objetivo_id      uuid NOT NULL REFERENCES objetivo (id) ON DELETE CASCADE,
    -- Vacío: la foto es del objetivo mayor (el resultado del negocio de los últimos 30 días).
    mini_objetivo_id uuid REFERENCES mini_objetivo (id) ON DELETE CASCADE,
    -- Día argentino de la foto: la última pasada del día pisa a la anterior.
    dia              date NOT NULL,
    valor            numeric(14, 2) NOT NULL,
    medido_en        timestamptz NOT NULL,
    CONSTRAINT ux_progreso_dia UNIQUE NULLS NOT DISTINCT (objetivo_id, mini_objetivo_id, dia)
);

COMMENT ON TABLE progreso_objetivo IS 'Una foto por día del avance de un objetivo y de sus mini-objetivos, tomada por el monitoreo.';
