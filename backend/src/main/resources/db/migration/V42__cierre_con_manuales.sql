-- V38: el cierre del domingo con mini-objetivos manuales (H-60, ENS-343). Un manual no tiene meta: el reajuste que lo
-- renueva guarda el título anterior pero no una meta. Y cada resumen semanal guarda lo que la persona respondió de sus
-- manuales (o que no respondió) en `manuales`: [{"miniObjetivoId": "…", "titulo": "…", "hizo": "SI"|null, "quePaso": …}].

ALTER TABLE reajuste_mini_objetivo ALTER COLUMN meta_anterior DROP NOT NULL;

ALTER TABLE resumen_semanal_objetivo ADD COLUMN manuales jsonb NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN resumen_semanal_objetivo.manuales IS 'Lo respondido de cada mini-objetivo manual al cierre; hizo vacío es sin respuesta.';
