-- V11: el aviso sabe sobre qué entidad habla (un producto, por ejemplo) y guarda los datos que lo originaron.
-- La tabla todavía no tiene filas, así que las columnas nuevas pueden ser NOT NULL sin relleno.

ALTER TABLE aviso
    ADD COLUMN entidad text NOT NULL CHECK (length(btrim(entidad)) > 0),
    ADD COLUMN datos jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(datos) = 'object');

COMMENT ON COLUMN aviso.entidad IS 'Entidad afectada, con la forma "registro:<uuid>".';
COMMENT ON COLUMN aviso.datos IS 'Datos de origen con los que el motor armó el aviso; la huella sale de ellos.';

-- No puede haber dos avisos pendientes del mismo tipo sobre la misma entidad.
CREATE UNIQUE INDEX ux_aviso_pendiente_entidad ON aviso (negocio_id, tipo, entidad) WHERE estado = 'PENDIENTE';
