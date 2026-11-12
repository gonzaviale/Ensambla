-- V29: los mini-objetivos de un objetivo (ENS-267). Al definir un objetivo, el servidor propone hasta 3 cambios o metas
-- con números del negocio; cada uno mide una métrica y tiene un estado. Los reemplazados, logrados y abandonados quedan
-- como historia. `objetivo.alcanzado_en` marca el objetivo cuyo resultado ya llegó a la meta (sin mini-objetivos).

ALTER TABLE objetivo ADD COLUMN alcanzado_en timestamptz;

CREATE TABLE mini_objetivo (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    objetivo_id     uuid NOT NULL REFERENCES objetivo (id) ON DELETE CASCADE,
    negocio_id      uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    -- Lugar en la pantalla: 1 a 3. Sólo los EN_CURSO ocupan lugar (índice de abajo).
    posicion        smallint NOT NULL CHECK (posicion BETWEEN 1 AND 3),
    tipo            text NOT NULL CHECK (tipo IN ('SUBIR_PRECIO', 'MOVER_PARADO', 'PONER_A_LA_VISTA',
                                                  'RECORTAR_GASTO', 'MEJORAR_MARGEN', 'VENTAS_SEMANALES')),
    -- El producto (registro de inventario) si el tipo es de producto. Sin FK: el producto puede borrarse del cuaderno.
    registro_id     uuid,
    -- Nombre del producto o de la categoría de gasto, para mostrar.
    referencia      text,
    meta            numeric(14, 2) NOT NULL,
    valor_inicial   numeric(14, 2) NOT NULL,
    valor_actual    numeric(14, 2) NOT NULL,
    -- Pesos por mes que suma al resultado del negocio si se cumple.
    aporte_estimado numeric(14, 2) NOT NULL,
    estado          text NOT NULL CHECK (estado IN ('EN_CURSO', 'LOGRADO', 'ABANDONADO', 'REEMPLAZADO')),
    -- Lunes de la semana en que se propuso (hora argentina).
    semana_inicio   date NOT NULL,
    titulo          text NOT NULL CHECK (length(btrim(titulo)) > 0),
    justificacion   text NOT NULL CHECK (length(btrim(justificacion)) > 0),
    texto_con_ia    boolean NOT NULL DEFAULT false,
    -- El mini-objetivo que lo reemplazó. Diferida: al reemplazar, el viejo se actualiza antes de insertar el nuevo.
    reemplazado_por uuid REFERENCES mini_objetivo (id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED,
    creado_en       timestamptz NOT NULL,
    actualizado_en  timestamptz NOT NULL
);

-- Como máximo 3 en curso por objetivo, uno por posición.
CREATE UNIQUE INDEX ux_mini_objetivo_en_curso ON mini_objetivo (objetivo_id, posicion) WHERE estado = 'EN_CURSO';

CREATE INDEX ix_mini_objetivo_objetivo ON mini_objetivo (objetivo_id, creado_en);

COMMENT ON TABLE mini_objetivo IS 'Los 3 cambios, métricas o metas semanales que Sami propone para llegar a un objetivo.';
