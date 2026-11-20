-- V34: objetivos con Sami (H-60, ENS-339). Un objetivo ahora tiene un tipo (MONETARIO es el de siempre; CON_DATOS,
-- SIN_DATOS e HIBRIDO se arman con Sami y no tienen meta en pesos) y un mini-objetivo tiene un modo (MEDIDO lo mide el
-- servidor con una métrica; MANUAL es una acción que la persona responde). Los que ya existen quedan MONETARIO y MEDIDO.

ALTER TABLE objetivo
    ADD COLUMN tipo text NOT NULL DEFAULT 'MONETARIO'
        CHECK (tipo IN ('MONETARIO', 'CON_DATOS', 'SIN_DATOS', 'HIBRIDO')),
    -- Si lo armó Sami con IA: es lo que cuenta para la cuota mensual (T-242).
    ADD COLUMN armado_con_sami boolean NOT NULL DEFAULT false,
    ALTER COLUMN meta DROP NOT NULL;

-- Sólo un monetario tiene meta en pesos, y la tiene siempre.
ALTER TABLE objetivo
    ADD CONSTRAINT ck_objetivo_meta_segun_tipo CHECK ((tipo = 'MONETARIO') = (meta IS NOT NULL));

ALTER TABLE mini_objetivo
    ADD COLUMN modo text NOT NULL DEFAULT 'MEDIDO' CHECK (modo IN ('MEDIDO', 'MANUAL')),
    ALTER COLUMN tipo DROP NOT NULL,
    ALTER COLUMN meta DROP NOT NULL,
    ALTER COLUMN valor_inicial DROP NOT NULL,
    ALTER COLUMN valor_actual DROP NOT NULL,
    ALTER COLUMN aporte_estimado DROP NOT NULL;

-- Un medido tiene su métrica y todos sus números; un manual no tiene ninguno.
ALTER TABLE mini_objetivo
    ADD CONSTRAINT ck_mini_objetivo_segun_modo CHECK (
        (modo = 'MEDIDO' AND tipo IS NOT NULL AND meta IS NOT NULL AND valor_inicial IS NOT NULL
            AND valor_actual IS NOT NULL AND aporte_estimado IS NOT NULL)
        OR
        (modo = 'MANUAL' AND tipo IS NULL AND registro_id IS NULL AND referencia IS NULL AND meta IS NULL
            AND valor_inicial IS NULL AND valor_actual IS NULL AND aporte_estimado IS NULL));

COMMENT ON COLUMN objetivo.tipo IS 'MONETARIO (con meta en pesos) o, armado con Sami, CON_DATOS, SIN_DATOS o HIBRIDO.';
COMMENT ON COLUMN mini_objetivo.modo IS 'MEDIDO (métrica del catálogo, la mide el servidor) o MANUAL (acción que la persona responde).';
