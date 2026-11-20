-- V32: el cierre del domingo de cada objetivo (ENS-270). Cada semana (de lunes a domingo, hora argentina) queda un
-- resumen por objetivo con el resultado del negocio al cierre, las decisiones de la semana y el valor de cada
-- mini-objetivo; y los reajustes que ese cierre hizo en los mini-objetivos, con su motivo. El índice único por objetivo
-- y semana hace que una segunda corrida del cierre no deje nada duplicado.

CREATE TABLE resumen_semanal_objetivo (
    id                     uuid PRIMARY KEY,
    objetivo_id            uuid NOT NULL REFERENCES objetivo (id) ON DELETE CASCADE,
    semana_inicio          date NOT NULL,
    resultado              numeric(14, 2) NOT NULL,
    avance                 numeric(5, 4) NOT NULL,
    decisiones_aplicadas   integer NOT NULL,
    decisiones_descartadas integer NOT NULL,
    decisiones_aceptadas   integer NOT NULL,
    decisiones_ignoradas   integer NOT NULL,
    minis                  jsonb NOT NULL,
    que_funciono           text NOT NULL,
    texto_con_ia           boolean NOT NULL DEFAULT false,
    cerrado_en             timestamptz NOT NULL,
    CONSTRAINT ux_resumen_semana UNIQUE (objetivo_id, semana_inicio)
);

CREATE TABLE reajuste_mini_objetivo (
    id               uuid PRIMARY KEY,
    resumen_id       uuid NOT NULL REFERENCES resumen_semanal_objetivo (id) ON DELETE CASCADE,
    mini_anterior_id uuid NOT NULL REFERENCES mini_objetivo (id) ON DELETE CASCADE,
    mini_nuevo_id    uuid REFERENCES mini_objetivo (id) ON DELETE SET NULL,
    tipo             text NOT NULL,
    motivo           text NOT NULL,
    titulo_anterior  text NOT NULL,
    meta_anterior    numeric(14, 2) NOT NULL,
    titulo_nuevo     text,
    meta_nueva       numeric(14, 2),
    creado_en        timestamptz NOT NULL
);

CREATE INDEX ix_reajuste_mini_objetivo_resumen ON reajuste_mini_objetivo (resumen_id);
