-- Cada pestaña de Mi zona tiene su propio cupo gratuito: Precios sigue contando como ZONA y se suman
-- ZONA_CERCA (Qué se vende cerca) y ZONA_OTRA (Otra zona). Los desbloqueos ZONA ya registrados quedan en Precios.
ALTER TABLE contenido_plan_desbloqueado DROP CONSTRAINT contenido_plan_desbloqueado_tipo_check;

ALTER TABLE contenido_plan_desbloqueado
    ADD CONSTRAINT contenido_plan_desbloqueado_tipo_check
    CHECK (tipo IN ('AVISOS', 'ZONA', 'ZONA_CERCA', 'ZONA_OTRA'));

COMMENT ON TABLE contenido_plan_desbloqueado IS
    'Seis avisos por mes y seis recomendaciones de zona por pestaña (Precios, Qué se vende cerca y Otra zona) en Gratuito; claves estables por negocio.';
