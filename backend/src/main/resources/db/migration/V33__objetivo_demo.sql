-- Ensambla · un objetivo que se autogestiona para la cuenta demo (demo@ensambla.test, H-52 / T-184)
-- Le suma al almacén de ejemplo 12 semanas de ventas que van mejorando, hasta el día en que se aplica la migración, y
-- un objetivo creado hace 8 semanas con una meta del doble de lo que dejan esas ventas hoy (así queda lejos aunque
-- se sumen las ventas viejas de V10). El resto lo arma el sistema solo:
-- al arrancar, el cierre del domingo cierra las semanas pasadas y, a los pocos minutos, el monitoreo propone los 3
-- mini-objetivos. Desde el domingo siguiente aparecen los reajustes.
--
-- Busca todo por el email y por la clave de cada columna, no por ids: si la cuenta demo no existe (por ejemplo en
-- producción) o la persona cambió su almacén y ya no tiene ventas ni inventario, no hace nada.

CREATE TEMP TABLE tmp_demo AS
SELECT n.id AS negocio_id,
       bv.id AS ventas_id,
       (SELECT c.id FROM campo c WHERE c.bloque_id = bv.id AND c.clave = 'product')  AS c_producto,
       (SELECT c.id FROM campo c WHERE c.bloque_id = bv.id AND c.clave = 'quantity') AS c_cantidad,
       (SELECT c.id FROM campo c WHERE c.bloque_id = bv.id AND c.clave = 'price')    AS c_precio,
       (SELECT c.id FROM campo c WHERE c.bloque_id = bv.id AND c.clave = 'date')     AS c_fecha,
       bi.id AS inventario_id,
       (SELECT c.id FROM campo c WHERE c.bloque_id = bi.id AND c.clave = 'name')     AS i_nombre,
       (SELECT c.id FROM campo c WHERE c.bloque_id = bi.id AND c.clave = 'quantity') AS i_stock,
       (SELECT c.id FROM campo c WHERE c.bloque_id = bi.id AND c.clave = 'price')    AS i_precio,
       (SELECT c.id FROM campo c WHERE c.bloque_id = bi.id AND c.clave = 'cost')     AS i_costo
FROM usuario u
JOIN negocio n ON n.duenio_id = u.id
JOIN LATERAL (SELECT b.id FROM bloque b WHERE b.negocio_id = n.id AND b.tipo = 'VENTAS' ORDER BY b.posicion LIMIT 1) bv
    ON true
JOIN LATERAL (SELECT b.id FROM bloque b WHERE b.negocio_id = n.id AND b.tipo = 'INVENTARIO' ORDER BY b.posicion LIMIT 1) bi
    ON true
WHERE u.email = 'demo@ensambla.test';

DELETE FROM tmp_demo
WHERE c_producto IS NULL OR c_cantidad IS NULL OR c_precio IS NULL OR c_fecha IS NULL
   OR i_nombre IS NULL OR i_stock IS NULL OR i_precio IS NULL OR i_costo IS NULL;

-- Los productos con precio, costo y stock; cada uno vende por día entre 1 y 4 unidades de base. El azúcar queda
-- afuera: el demo la tiene sin ventas hace más de un mes para el aviso de rebajar el precio (V10).
CREATE TEMP TABLE tmp_demo_producto AS
SELECT d.negocio_id, r.id AS producto_id,
       (r.valores ->> d.i_precio::text)::numeric AS precio,
       (r.valores ->> d.i_costo::text)::numeric AS costo,
       1 + (row_number() OVER (ORDER BY r.orden, r.id) % 4) AS base
FROM tmp_demo d
JOIN registro r ON r.bloque_id = d.inventario_id
WHERE (r.valores ->> d.i_precio::text) ~ '^[0-9]+(\.[0-9]+)?$'
  AND (r.valores ->> d.i_costo::text) ~ '^[0-9]+(\.[0-9]+)?$'
  AND (r.valores ->> d.i_stock::text) ~ '^[0-9]+(\.[0-9]+)?$'
  AND (r.valores ->> d.i_stock::text)::numeric > 0
  AND coalesce(r.valores ->> d.i_nombre::text, '') NOT ILIKE 'az_car%';

-- 84 días hacia atrás (las 4 semanas antes del objetivo llenan sus primeros 30 días); la semana más vieja vende el
-- 80 % de la base y cada semana un 4 % más. Un producto de cada cuatro vende siempre lo mismo, para que el cierre
-- tenga algo que no avanza.
CREATE TEMP TABLE tmp_demo_venta AS
SELECT p.negocio_id, p.producto_id, p.precio, current_date - d AS dia,
       greatest(1, round(p.base * CASE WHEN p.base = 1 THEN 1 ELSE 0.8 + 0.04 * ((83 - d) / 7) END))::integer
           AS cantidad
FROM tmp_demo_producto p
CROSS JOIN generate_series(0, 83) AS d
WHERE (d + p.base) % 2 = 0 OR p.base > 2;

INSERT INTO registro (negocio_id, bloque_id, valores, creado_en, actualizado_en, orden, origen)
SELECT d.negocio_id, d.ventas_id,
       jsonb_build_object(
           d.c_producto::text, v.producto_id::text,
           d.c_cantidad::text, v.cantidad,
           d.c_precio::text, v.precio,
           d.c_fecha::text, to_char(v.dia, 'YYYY-MM-DD')),
       v.dia::timestamptz + interval '12 hours',
       v.dia::timestamptz + interval '12 hours',
       (SELECT coalesce(max(r.orden), 0) FROM registro r WHERE r.bloque_id = d.ventas_id)
           + row_number() OVER (ORDER BY v.dia, v.producto_id),
       'Demo de objetivos'
FROM tmp_demo d
JOIN tmp_demo_venta v ON v.negocio_id = d.negocio_id;

-- La meta: el doble de lo que dejaron estas ventas en los últimos 30 días, redondeado a $10.000 (mínimo $100.000).
INSERT INTO objetivo (negocio_id, texto, meta, creado_en)
SELECT d.negocio_id,
       'Quiero ganar más sin subir demasiado los precios',
       greatest(100000, round(sum(v.cantidad * (p.precio - p.costo)) * 2 / 10000) * 10000),
       now() - interval '56 days'
FROM tmp_demo d
JOIN tmp_demo_venta v ON v.negocio_id = d.negocio_id AND v.dia > current_date - 30
JOIN tmp_demo_producto p ON p.producto_id = v.producto_id
GROUP BY d.negocio_id;
