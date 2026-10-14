-- Campo de tipo FORMULA: valor calculado al leer a partir de otros campos NUMERO del mismo bloque.
-- La expresión se guarda como texto con los campos referenciados por id ({uuid}); el resultado no se persiste en
-- registro.valores.

ALTER TABLE campo DROP CONSTRAINT ck_campo_tipo;

ALTER TABLE campo
    ADD CONSTRAINT ck_campo_tipo
        CHECK (tipo IN ('TEXTO', 'NUMERO', 'FECHA', 'OPCION', 'SI_NO', 'RELACION', 'FORMULA')),
    ADD COLUMN formula text,
    ADD CONSTRAINT ck_campo_formula CHECK ((tipo = 'FORMULA') = (formula IS NOT NULL));
