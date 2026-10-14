-- Campo de tipo RELACION: cada fila apunta a una fila de otro bloque (o del mismo) del mismo proyecto.
-- El valor guardado en registro.valores es el id del registro referenciado; si ese registro se borra, la
-- referencia queda colgando y la interfaz la muestra como "Fila eliminada".

-- El CHECK de V1 no tenía nombre: PostgreSQL lo llamó campo_tipo_check.
ALTER TABLE campo DROP CONSTRAINT campo_tipo_check;

ALTER TABLE campo
    ADD CONSTRAINT ck_campo_tipo CHECK (tipo IN ('TEXTO', 'NUMERO', 'FECHA', 'OPCION', 'SI_NO', 'RELACION')),
    -- Bloque al que apunta un campo RELACION; la FK compuesta impide apuntar a un bloque de otro negocio.
    ADD COLUMN bloque_relacionado_id uuid,
    ADD CONSTRAINT fk_campo_bloque_relacionado FOREIGN KEY (negocio_id, bloque_relacionado_id)
        REFERENCES bloque (negocio_id, id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_campo_relacion CHECK ((tipo = 'RELACION') = (bloque_relacionado_id IS NOT NULL));

CREATE INDEX ix_campo_bloque_relacionado ON campo (negocio_id, bloque_relacionado_id);
