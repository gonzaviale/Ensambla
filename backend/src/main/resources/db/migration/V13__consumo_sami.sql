-- ENS-178 / T-117: consultas de Sami a la IA por negocio y día (UTC), para respetar el límite y medir el gasto.
CREATE TABLE consumo_sami_diario (
    negocio_id uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    fecha date NOT NULL,
    consultas integer NOT NULL DEFAULT 0 CHECK (consultas >= 0),
    tokens_entrada bigint NOT NULL DEFAULT 0 CHECK (tokens_entrada >= 0),
    tokens_salida bigint NOT NULL DEFAULT 0 CHECK (tokens_salida >= 0),
    modelo text,
    creado_en timestamptz NOT NULL DEFAULT now(),
    actualizado_en timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (negocio_id, fecha)
);
