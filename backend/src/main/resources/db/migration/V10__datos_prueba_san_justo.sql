-- Ensambla · datos de prueba de la zona de la UNLaM (San Justo, La Matanza)
-- Sirve para probar el frontend y el motor de avisos con datos parecidos a los reales. Los comercios, las direcciones
-- y las personas son FICTICIOS (las calles son de la zona y las coordenadas, aproximadas); los precios, el stock y
-- las ventas se generan de forma determinística y las fechas son relativas al día en que se aplica la migración.
--
-- Cuentas de prueba (todas con la misma contraseña):  Ensambla-Test-2026
--   comercio01@ensambla.test … comercio10@ensambla.test   comercios que aportan a los agregados de la zona
--   demo@ensambla.test                                    almacén de ejemplo con 60 días de ventas pensado para
--                                                         que Sami tenga avisos (no aporta a la zona)
--
-- Los ids se derivan de un texto con md5, así el script es repetible en cualquier base y los tests los conocen.

CREATE FUNCTION pg_temp.uid(texto text) RETURNS uuid AS $fn$
    SELECT md5('sanjusto:' || texto)::uuid
$fn$ LANGUAGE sql IMMUTABLE;

-- ===== Zona =====
INSERT INTO zona (id, nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES (pg_temp.uid('zona'), 'San Justo - UNLaM', 'San Justo', 'La Matanza', 'Buenos Aires', -34.670300, -58.562800, 1500);

-- ===== Comercios (n = 1..10 aportan a la zona; n = 11 es el demo) =====
CREATE TEMP TABLE tmp_negocio AS
SELECT * FROM (VALUES
    (1,  'comercio01@ensambla.test', 'Pepe Rodríguez',   'Almacén Don Pepe',            'ALMACEN',     'Arieta 3120',           -34.668900, -58.560100),
    (2,  'comercio02@ensambla.test', 'Julia Benítez',    'Kiosco El Estudiante',        'KIOSCO',      'Florencio Varela 1850', -34.670100, -58.564000),
    (3,  'comercio03@ensambla.test', 'Hugo Sosa',        'Minimercado Los Tres Hermanos','MINIMERCADO','Juan Manuel de Rosas 3620', -34.674500, -58.556000),
    (4,  'comercio04@ensambla.test', 'Marta Giménez',    'Almacén Doña Marta',          'ALMACEN',     'Almafuerte 2250',       -34.666000, -58.567500),
    (5,  'comercio05@ensambla.test', 'Carlos Ledesma',   'Autoservicio San Justo',      'MINIMERCADO', 'Perú 1650',             -34.672500, -58.570000),
    (6,  'comercio06@ensambla.test', 'Romina Aguirre',   'Kiosco 24 Horas Venezuela',   'KIOSCO',      'Venezuela 2480',        -34.664000, -58.559000),
    (7,  'comercio07@ensambla.test', 'Nélida Cabrera',   'Almacén El Progreso',         'ALMACEN',     'Entre Ríos 2910',       -34.677500, -58.563000),
    (8,  'comercio08@ensambla.test', 'Diego Paz',        'Minimercado Buen Precio',     'MINIMERCADO', 'Arieta 3580',           -34.669000, -58.558000),
    (9,  'comercio09@ensambla.test', 'Estela Molina',    'Almacén Los Pibes',           'ALMACEN',     'Almafuerte 1700',       -34.671000, -58.565500),
    (10, 'comercio10@ensambla.test', 'Raúl Ibarra',      'Kiosco Don Bosco',            'KIOSCO',      'Perú 2100',             -34.668000, -58.571000),
    (11, 'demo@ensambla.test',       'Lucía Gómez',      'Almacén de ejemplo',          'ALMACEN',     'Florencio Varela 1990', -34.670600, -58.562000)
) AS t (n, email, usuario, nombre, rubro, direccion, latitud, longitud);

INSERT INTO usuario (id, email, nombre, hash_contrasenia)
SELECT pg_temp.uid('usuario:' || n), email, usuario, '$2a$10$WoGXjJ5plQUWQIsZGYzEK.iHYEoQD.RQA5g2.W6NjRJ6y1h48QXxm'
FROM tmp_negocio;

INSERT INTO negocio (id, nombre, duenio_id, rubro, direccion, localidad, latitud, longitud, zona_id, comparte_datos_zona)
SELECT pg_temp.uid('negocio:' || n), nombre, pg_temp.uid('usuario:' || n), rubro, direccion, 'San Justo',
       latitud, longitud, pg_temp.uid('zona'), n <> 11
FROM tmp_negocio;

INSERT INTO preferencia_usuario (usuario_id, bienvenida_vista)
VALUES (pg_temp.uid('usuario:11'), false);

-- ===== Sistema de cada comercio: proyecto, sección y bloques =====
INSERT INTO proyecto (id, negocio_id, nombre, descripcion, plantilla_clave, es_demo)
SELECT pg_temp.uid('proyecto:' || n), pg_temp.uid('negocio:' || n),
       CASE WHEN n = 11 THEN 'Almacén de ejemplo' ELSE 'Mi almacén' END,
       'Vendé, descontá stock y mirá qué se mueve.', 'ALMACEN', n = 11
FROM tmp_negocio;

INSERT INTO seccion (id, negocio_id, proyecto_id, nombre, posicion)
SELECT pg_temp.uid('seccion:' || n), pg_temp.uid('negocio:' || n), pg_temp.uid('proyecto:' || n), 'Vista general', 0
FROM tmp_negocio;

CREATE TEMP TABLE tmp_bloque AS
SELECT * FROM (VALUES
    ('INVENTARIO', 'Inventario', 'Productos, cantidades, costos y precios.', 'NARANJA',  'ANCHO', NULL::smallint, 0),
    ('VENTAS',     'Ventas',     'Lo que vendés, producto por producto.',    'AMARILLO', 'ANCHO', -1::smallint,   1),
    ('CLIENTES',   'Clientes',   'Contactos y fiado, siempre a mano.',       'CELESTE',  'MEDIO', NULL::smallint, 2),
    ('ENTRADAS',   'Entradas',   'Compras, ingresos y reposición.',          'CELESTE',  'MEDIO', 1::smallint,    3)
) AS t (tipo, nombre, descripcion, color, ancho, direccion_stock, posicion);

INSERT INTO bloque (id, negocio_id, proyecto_id, seccion_id, nombre, posicion, tipo, descripcion, color, ancho, direccion_stock)
SELECT pg_temp.uid('bloque:' || n.n || ':' || b.tipo), pg_temp.uid('negocio:' || n.n), pg_temp.uid('proyecto:' || n.n),
       pg_temp.uid('seccion:' || n.n), b.nombre, b.posicion, b.tipo, b.descripcion, b.color, b.ancho, b.direccion_stock
FROM tmp_negocio n CROSS JOIN tmp_bloque b;

-- ===== Campos (las claves son las del catálogo del frontend) =====
CREATE TEMP TABLE tmp_campo AS
SELECT * FROM (VALUES
    ('INVENTARIO', 0, 'name',     'Producto',     'TEXTO',       true,  NULL::numeric),
    ('INVENTARIO', 1, 'quantity', 'Cantidad',     'NUMERO',      true,  0::numeric),
    ('INVENTARIO', 2, 'price',    'Precio',       'MONEDA',      false, NULL::numeric),
    ('INVENTARIO', 3, 'cost',     'Costo',        'MONEDA',      false, NULL::numeric),
    ('INVENTARIO', 4, 'minimum',  'Stock mínimo', 'NUMERO',      false, NULL::numeric),
    ('VENTAS',     0, 'product',  'Producto',     'RELACION',    true,  NULL::numeric),
    ('VENTAS',     1, 'quantity', 'Cantidad',     'NUMERO',      true,  1::numeric),
    ('VENTAS',     2, 'price',    'Precio',       'MONEDA',      false, NULL::numeric),
    ('VENTAS',     3, 'date',     'Fecha',        'FECHA',       false, NULL::numeric),
    ('VENTAS',     4, 'customer', 'Cliente',      'TEXTO',       false, NULL::numeric),
    ('VENTAS',     5, 'credit',   'Fiado',        'SI_NO',       false, NULL::numeric),
    ('CLIENTES',   0, 'name',     'Nombre',       'TEXTO',       true,  NULL::numeric),
    ('CLIENTES',   1, 'phone',    'Teléfono',     'TELEFONO',    false, NULL::numeric),
    ('CLIENTES',   2, 'email',    'Correo',       'EMAIL',       false, NULL::numeric),
    ('CLIENTES',   3, 'notes',    'Notas',        'TEXTO_LARGO', false, NULL::numeric),
    ('ENTRADAS',   0, 'product',  'Producto',     'RELACION',    true,  NULL::numeric),
    ('ENTRADAS',   1, 'quantity', 'Cantidad',     'NUMERO',      true,  1::numeric),
    ('ENTRADAS',   2, 'date',     'Fecha',        'FECHA',       false, NULL::numeric),
    ('ENTRADAS',   3, 'notes',    'Referencia',   'TEXTO',       false, NULL::numeric)
) AS t (bloque, posicion, clave, nombre, tipo, obligatorio, minimo);

INSERT INTO campo (id, negocio_id, bloque_id, nombre, tipo, obligatorio, posicion, clave, minimo, bloque_relacionado_id)
SELECT pg_temp.uid('campo:' || n.n || ':' || c.bloque || ':' || c.clave), pg_temp.uid('negocio:' || n.n),
       pg_temp.uid('bloque:' || n.n || ':' || c.bloque), c.nombre, c.tipo, c.obligatorio, c.posicion, c.clave, c.minimo,
       CASE WHEN c.tipo = 'RELACION' THEN pg_temp.uid('bloque:' || n.n || ':INVENTARIO') END
FROM tmp_negocio n CROSS JOIN tmp_campo c;

-- ===== Catálogo de productos del barrio (precio y costo base; frecuencia y cantidad de cada venta) =====
CREATE TEMP TABLE tmp_producto AS
SELECT * FROM (VALUES
    (1,  'Yerba Playadito 1 kg',        3900, 2800, 1, 4),
    (2,  'Aceite Natura 900 ml',        2800, 2000, 1, 2),
    (3,  'Coca-Cola 2,25 L',            2750, 2000, 1, 6),
    (4,  'Azúcar Ledesma 1 kg',         1150,  800, 1, 3),
    (5,  'Fideos Matarazzo 500 g',      1300,  900, 1, 4),
    (6,  'Lavandina Ayudín 1 L',        1350,  950, 1, 1),
    (7,  'Alfajor Jorgito',              800,  520, 1, 3),
    (8,  'Galletitas Criollitas 3 U',   1250,  880, 2, 3),
    (9,  'Leche La Serenísima 1 L',     1650, 1250, 1, 8),
    (10, 'Pan lactal Bimbo 360 g',      2500, 1900, 1, 4),
    (11, 'Agua Villavicencio 2 L',      1100,  750, 1, 4),
    (12, 'Cerveza Quilmes 1 L',         2300, 1700, 1, 3),
    (13, 'Fernet Branca 750 ml',       10400, 8000, 2, 1),
    (14, 'Arroz Gallo Oro 1 kg',        1700, 1200, 1, 2),
    (15, 'Harina Pureza 000 1 kg',      1100,  750, 2, 2),
    (16, 'Puré de tomate Arcor 520 g',   900,  620, 1, 1),
    (17, 'Detergente Magistral 750 ml', 2900, 2100, 2, 1),
    (18, 'Papel higiénico Higienol x4', 3200, 2300, 1, 2),
    (19, 'Mayonesa Hellmanns 475 g',    2600, 1900, 3, 1),
    (20, 'Sprite 2,25 L',               2650, 1950, 1, 3)
) AS t (p, nombre, precio, costo, frecuencia, cantidad);

-- Qué lleva cada comercio. Los 10 primeros llevan ~75 % del catálogo, con precios entre -10 % y +10 % del base;
-- el Fernet sólo lo llevan dos (menos del mínimo de 3: no debe aparecer en la zona).
CREATE TEMP TABLE tmp_carga AS
SELECT n.n, p.p,
       (round(p.precio * (0.90 + ((n.n * 5 + p.p * 3) % 21) / 100.0) / 10) * 10)::integer AS precio,
       (round(p.costo * (0.95 + ((n.n + p.p) % 11) / 100.0) / 10) * 10)::integer AS costo,
       5 + (n.n * p.p * 7) % 40 AS stock,
       6 + p.p % 7 AS minimo
FROM tmp_negocio n
JOIN tmp_producto p ON n.n <= 10
    AND ((p.p = 13 AND n.n IN (2, 5)) OR (p.p <> 13 AND (n.n + p.p) % 4 <> 0));

-- El demo replica el almacén de ejemplo del frontend (lib/sistema/demo.ts).
INSERT INTO tmp_carga (n, p, precio, costo, stock, minimo)
VALUES (11, 1, 3600, 2700,  4, 10),   -- yerba: se vende mucho y barata, stock bajo
       (11, 2, 2600, 1900,  0,  6),   -- aceite: agotado
       (11, 3, 2700, 1950, 18, 12),
       (11, 4, 1500,  900, 25, 10),   -- azúcar: caro y sin ventas hace más de un mes
       (11, 5, 1100,  800, 30, 10),
       (11, 6, 1350,  950,  2,  6),   -- lavandina: stock bajo
       (11, 7,  800,  520, 40, 12),   -- alfajor y galletitas: salen poco pero siempre juntos
       (11, 8, 1250,  880, 25, 10);

-- Ventas de los últimos 60 días.
CREATE TEMP TABLE tmp_venta AS
SELECT c.n, c.p, d AS dia, p.cantidad, NULL::integer AS cliente
FROM tmp_carga c
JOIN tmp_producto p ON p.p = c.p
CROSS JOIN generate_series(0, 59) AS d
WHERE c.n <= 10 AND (d + c.n + c.p) % p.frecuencia = 0;

INSERT INTO tmp_venta (n, p, dia, cantidad, cliente)
SELECT 11, 1, d, 2, NULL::integer FROM generate_series(0, 29) AS d              -- la yerba vuela
UNION ALL SELECT 11, 3, d, 1, NULL FROM generate_series(0, 29, 2) AS d   -- la gaseosa, un día por medio
UNION ALL SELECT 11, 2, d, 1, NULL FROM generate_series(1, 29, 3) AS d   -- el aceite
UNION ALL SELECT 11, 5, d, 1, NULL FROM unnest(ARRAY[4, 11, 17, 24]) AS d
UNION ALL SELECT 11, 6, d, 1, NULL FROM unnest(ARRAY[1, 5, 9, 14, 19, 26]) AS d
UNION ALL SELECT 11, 4, d, 2, NULL FROM unnest(ARRAY[45, 52, 58]) AS d
UNION ALL SELECT 11, 7, d, 1, (((i - 1) % 3) + 1)::integer FROM unnest(ARRAY[3, 9, 15, 22]) WITH ORDINALITY AS t (d, i)
UNION ALL SELECT 11, 8, d, 1, (((i - 1) % 3) + 1)::integer FROM unnest(ARRAY[3, 9, 15, 22]) WITH ORDINALITY AS t (d, i);

-- Clientes (3 por comercio; el demo, los del frontend).
CREATE TEMP TABLE tmp_cliente AS
SELECT n.n, c.c,
       (ARRAY['Lucía Pérez', 'Martín García', 'Ana López', 'Doña Rosa', 'Bar La Esquina', 'Club Social'])[((n.n + c.c) % 6) + 1] AS nombre,
       '11 5555-' || lpad((n.n * 10 + c.c)::text, 4, '0') AS telefono
FROM tmp_negocio n CROSS JOIN generate_series(1, 3) AS c(c)
WHERE n.n <= 10;

INSERT INTO tmp_cliente (n, c, nombre, telefono)
VALUES (11, 1, 'Doña Rosa', '11 5555-0111'),
       (11, 2, 'Bar La Esquina', '11 5555-0122'),
       (11, 3, 'Club Social', '11 5555-0133');

-- ===== Registros =====
INSERT INTO registro (id, negocio_id, bloque_id, valores)
SELECT pg_temp.uid('producto:' || c.n || ':' || c.p), pg_temp.uid('negocio:' || c.n),
       pg_temp.uid('bloque:' || c.n || ':INVENTARIO'),
       jsonb_build_object(
           pg_temp.uid('campo:' || c.n || ':INVENTARIO:name')::text, p.nombre,
           pg_temp.uid('campo:' || c.n || ':INVENTARIO:quantity')::text, c.stock,
           pg_temp.uid('campo:' || c.n || ':INVENTARIO:price')::text, c.precio,
           pg_temp.uid('campo:' || c.n || ':INVENTARIO:cost')::text, c.costo,
           pg_temp.uid('campo:' || c.n || ':INVENTARIO:minimum')::text, c.minimo)
FROM tmp_carga c JOIN tmp_producto p ON p.p = c.p;

INSERT INTO registro (id, negocio_id, bloque_id, valores)
SELECT pg_temp.uid('cliente:' || n || ':' || c), pg_temp.uid('negocio:' || n),
       pg_temp.uid('bloque:' || n || ':CLIENTES'),
       jsonb_build_object(
           pg_temp.uid('campo:' || n || ':CLIENTES:name')::text, nombre,
           pg_temp.uid('campo:' || n || ':CLIENTES:phone')::text, telefono)
FROM tmp_cliente;

INSERT INTO registro (id, negocio_id, bloque_id, valores, creado_en, actualizado_en)
SELECT pg_temp.uid('venta:' || v.n || ':' || v.p || ':' || v.dia), pg_temp.uid('negocio:' || v.n),
       pg_temp.uid('bloque:' || v.n || ':VENTAS'),
       jsonb_strip_nulls(jsonb_build_object(
           pg_temp.uid('campo:' || v.n || ':VENTAS:product')::text, pg_temp.uid('producto:' || v.n || ':' || v.p)::text,
           pg_temp.uid('campo:' || v.n || ':VENTAS:quantity')::text, v.cantidad,
           pg_temp.uid('campo:' || v.n || ':VENTAS:price')::text, c.precio,
           pg_temp.uid('campo:' || v.n || ':VENTAS:date')::text, to_char(current_date - v.dia, 'YYYY-MM-DD'),
           pg_temp.uid('campo:' || v.n || ':VENTAS:customer')::text,
               CASE WHEN v.cliente IS NOT NULL THEN pg_temp.uid('cliente:' || v.n || ':' || v.cliente)::text END,
           pg_temp.uid('campo:' || v.n || ':VENTAS:credit')::text, false)),
       (current_date - v.dia)::timestamptz + interval '12 hours',
       (current_date - v.dia)::timestamptz + interval '12 hours'
FROM tmp_venta v JOIN tmp_carga c ON c.n = v.n AND c.p = v.p;

-- ===== Agregados de la zona, calculados con lo que se acaba de sembrar =====
-- Sólo entran los negocios que comparten datos; un producto necesita al menos 3 comercios que lo tengan y 3 que lo vendan.
WITH inventario AS (
    SELECT r.id AS registro_id, r.negocio_id,
           r.valores ->> cn.id::text AS nombre,
           (r.valores ->> cp.id::text)::numeric AS precio
    FROM registro r
    JOIN bloque b ON b.id = r.bloque_id AND b.tipo = 'INVENTARIO'
    JOIN negocio n ON n.id = b.negocio_id AND n.zona_id = pg_temp.uid('zona') AND n.comparte_datos_zona
    JOIN campo cn ON cn.bloque_id = b.id AND cn.clave = 'name'
    JOIN campo cp ON cp.bloque_id = b.id AND cp.clave = 'price'
),
vendido AS (
    SELECT (r.valores ->> cp.id::text) AS registro_id,
           sum((r.valores ->> cq.id::text)::numeric) AS unidades
    FROM registro r
    JOIN bloque b ON b.id = r.bloque_id AND b.tipo = 'VENTAS'
    JOIN campo cp ON cp.bloque_id = b.id AND cp.clave = 'product'
    JOIN campo cq ON cq.bloque_id = b.id AND cq.clave = 'quantity'
    JOIN campo cd ON cd.bloque_id = b.id AND cd.clave = 'date'
    WHERE (r.valores ->> cd.id::text)::date >= current_date - 27
    GROUP BY 1
),
producto AS (
    SELECT lower(translate(i.nombre, 'áéíóúÁÉÍÓÚñÑ', 'aeiouAEIOUnN')) AS normalizado,
           i.nombre, i.negocio_id, i.precio, coalesce(v.unidades, 0) AS unidades
    FROM inventario i LEFT JOIN vendido v ON v.registro_id = i.registro_id::text
)
INSERT INTO agregado_zona_producto
    (zona_id, producto_normalizado, nombre, comercios, comercios_venden, precio_minimo, precio_promedio, precio_maximo,
     unidades_semana, periodo_desde, periodo_hasta)
SELECT pg_temp.uid('zona'), normalizado, min(nombre),
       count(DISTINCT negocio_id),
       count(DISTINCT negocio_id) FILTER (WHERE unidades > 0),
       min(precio), round(avg(precio), 2), max(precio),
       round(sum(unidades) / 4.0 / count(DISTINCT negocio_id) FILTER (WHERE unidades > 0), 2),
       current_date - 27, current_date
FROM producto
GROUP BY normalizado
HAVING count(DISTINCT negocio_id) >= 3
   AND count(DISTINCT negocio_id) FILTER (WHERE unidades > 0) >= 3;

DROP TABLE tmp_cliente;
DROP TABLE tmp_venta;
DROP TABLE tmp_carga;
DROP TABLE tmp_producto;
DROP TABLE tmp_campo;
DROP TABLE tmp_bloque;
DROP TABLE tmp_negocio;
DROP FUNCTION pg_temp.uid(text);
