-- Ensambla · el sistema que arma la persona en el frontend se guarda entero (PUT /api/sistemas/{id}).
-- El documento del front admite cosas que el modelo anterior prohibía: nombres repetidos de bloques y de columnas,
-- claves de columna generadas por la pantalla y filas en el orden que la persona las dejó.

-- Dos bloques o dos columnas pueden llamarse igual (por ejemplo, dos notas): se distinguen por id, no por nombre.
ALTER TABLE bloque DROP CONSTRAINT ux_bloque_proyecto_nombre;
ALTER TABLE campo DROP CONSTRAINT ux_campo_bloque_nombre;

-- La clave de una columna es la que le puso la pantalla (name, price, o un id propio de hasta 64 caracteres).
-- V9 la creó sin nombre, así que PostgreSQL la llamó campo_clave_check.
ALTER TABLE campo DROP CONSTRAINT campo_clave_check;
ALTER TABLE campo
    ADD CONSTRAINT ck_campo_clave CHECK (clave IS NULL OR clave ~ '^[A-Za-z][A-Za-z0-9_-]{0,63}$');

-- Orden de las filas dentro de su bloque; las que ya existían conservan el orden de carga.
ALTER TABLE registro ADD COLUMN orden integer NOT NULL DEFAULT 0 CHECK (orden >= 0);
