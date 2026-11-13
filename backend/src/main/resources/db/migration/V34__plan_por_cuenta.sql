-- H-55 / T-198: el plan es de la cuenta (usuario) y fija cuántos negocios puede tener.
--
-- Antes la suscripción, la prueba Premium, la renovación y los pagos colgaban del negocio y el límite de los planes
-- era de «proyectos». Ahora cuelgan del usuario: las cuentas actuales heredan el plan de su negocio actual (el más
-- antiguo, que es el que la aplicación siempre usó) y el límite pasa a ser de negocios (Gratuito 1; Premium el propio
-- y hasta 5 adicionales, 6 en total).
--
-- Los consumos por negocio (cupos de contenido, Sami y fotos por día) NO se tocan: ver docs/modelo-datos.md.

-- El negocio actual de cada cuenta: hasta hoy una cuenta tiene un solo negocio, así que es el único.
CREATE TEMP TABLE negocio_actual ON COMMIT DROP AS
SELECT DISTINCT ON (duenio_id) duenio_id AS usuario_id, id AS negocio_id
FROM negocio
ORDER BY duenio_id, creado_en, id;

-- Una cuenta con más de un negocio nunca se pudo crear desde la aplicación. Si existiera, la suscripción y la prueba de
-- los negocios que no son el actual se descartan (son datos derivados), pero los cobros no: si hay renovaciones o
-- pagos en esos negocios se detiene la migración antes de perder cualquier dato de dinero.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM renovacion_premium WHERE negocio_id NOT IN (SELECT negocio_id FROM negocio_actual))
       OR EXISTS (SELECT 1 FROM pago_premium WHERE negocio_id NOT IN (SELECT negocio_id FROM negocio_actual)) THEN
        RAISE EXCEPTION 'V34: hay renovaciones o pagos Premium en un negocio que no es el actual de su cuenta; '
            'resolvelos a mano antes de migrar el plan a la cuenta';
    END IF;
END $$;

-- ===== suscripción: una por cuenta =====
ALTER TABLE suscripcion ADD COLUMN usuario_id uuid;

DELETE FROM suscripcion WHERE negocio_id NOT IN (SELECT negocio_id FROM negocio_actual);

UPDATE suscripcion s
SET usuario_id = a.usuario_id
FROM negocio_actual a
WHERE a.negocio_id = s.negocio_id;

-- Al quitar la columna se van su unique, su FK al negocio y el índice parcial de pruebas vencidas.
ALTER TABLE suscripcion DROP COLUMN negocio_id;

-- Una cuenta con negocio pero sin suscripción (no debería existir) arranca en Gratuito, como al registrarse.
INSERT INTO suscripcion (usuario_id, plan_id, estado, inicio_en)
SELECT a.usuario_id, p.id, 'ACTIVA', now()
FROM negocio_actual a
CROSS JOIN plan p
WHERE p.codigo = 'GRATUITO'
  AND NOT EXISTS (SELECT 1 FROM suscripcion s WHERE s.usuario_id = a.usuario_id);

ALTER TABLE suscripcion ALTER COLUMN usuario_id SET NOT NULL;
ALTER TABLE suscripcion
    ADD CONSTRAINT ux_suscripcion_usuario UNIQUE (usuario_id),
    ADD CONSTRAINT fk_suscripcion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE;
CREATE INDEX ix_suscripcion_pruebas_vencidas ON suscripcion (fin_en, usuario_id) WHERE estado = 'PRUEBA';

COMMENT ON TABLE suscripcion IS
    'Suscripción actual de la cuenta: el plan es del usuario y vale para todos sus negocios.';

-- ===== prueba Premium: una por cuenta =====
ALTER TABLE prueba_premium ADD COLUMN usuario_id uuid;

DELETE FROM prueba_premium WHERE negocio_id NOT IN (SELECT negocio_id FROM negocio_actual);

UPDATE prueba_premium p
SET usuario_id = a.usuario_id
FROM negocio_actual a
WHERE a.negocio_id = p.negocio_id;

-- Al quitar la columna se van su clave primaria y su FK al negocio.
ALTER TABLE prueba_premium DROP COLUMN negocio_id;
ALTER TABLE prueba_premium ALTER COLUMN usuario_id SET NOT NULL;
ALTER TABLE prueba_premium
    ADD CONSTRAINT pk_prueba_premium PRIMARY KEY (usuario_id),
    ADD CONSTRAINT fk_prueba_premium_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE;

-- ===== renovación y pagos Premium: de la cuenta =====
ALTER TABLE pago_premium DROP CONSTRAINT fk_pago_renovacion;
ALTER TABLE renovacion_premium DROP CONSTRAINT renovacion_premium_negocio_id_fkey;

UPDATE renovacion_premium r SET negocio_id = n.duenio_id FROM negocio n WHERE n.id = r.negocio_id;
UPDATE pago_premium p SET negocio_id = n.duenio_id FROM negocio n WHERE n.id = p.negocio_id;

ALTER TABLE renovacion_premium RENAME COLUMN negocio_id TO usuario_id;
ALTER TABLE pago_premium RENAME COLUMN negocio_id TO usuario_id;
ALTER TABLE renovacion_premium RENAME CONSTRAINT ux_renovacion_negocio_id TO ux_renovacion_usuario_id;
ALTER INDEX ix_renovacion_negocio_fecha RENAME TO ix_renovacion_usuario_fecha;

ALTER TABLE renovacion_premium
    ADD CONSTRAINT fk_renovacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE;
ALTER TABLE pago_premium
    ADD CONSTRAINT fk_pago_renovacion FOREIGN KEY (usuario_id, renovacion_id)
        REFERENCES renovacion_premium (usuario_id, id) ON DELETE CASCADE;

-- ===== plan: el límite pasa de proyectos a negocios =====
ALTER TABLE plan RENAME COLUMN limite_proyectos TO limite_negocios;
ALTER TABLE plan RENAME CONSTRAINT ck_plan_proyectos TO ck_plan_negocios;

UPDATE plan SET limite_negocios = 1, actualizado_en = now() WHERE codigo = 'GRATUITO';
UPDATE plan SET limite_negocios = 6, actualizado_en = now() WHERE codigo = 'PREMIUM';

-- Con los límites configurados todo plan dice cuántos negocios permite: ya no hay «sin límite».
ALTER TABLE plan ADD CONSTRAINT ck_plan_limites_negocios CHECK (
    NOT limites_configurados OR limite_negocios IS NOT NULL
);

COMMENT ON COLUMN plan.limite_negocios IS
    'Cuántos negocios puede tener la cuenta con este plan (Gratuito 1; Premium el propio y 5 adicionales).';
