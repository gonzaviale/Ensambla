-- Snapshot de OpenStreetMap generado por scripts/importar-datos-zona.ps1.
-- Los datos se conservan en PostgreSQL para que la navegación no dependa de servicios externos.
-- Flyway ejecuta esta migración de PostgreSQL dentro de una única transacción.

UPDATE fuente_dato_zona SET consultado_en = '2026-11-10', actualizado_en = now() WHERE codigo = 'OSM';

INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Almagro', 'Ciudad Autónoma de Buenos Aires', 'CABA', 'CABA', -34.6087, -58.4195, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Boedo', 'Ciudad Autónoma de Buenos Aires', 'CABA', 'CABA', -34.629, -58.419, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Caballito Norte', 'Ciudad Autónoma de Buenos Aires', 'CABA', 'CABA', -34.612, -58.435, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Caballito Sur', 'Ciudad Autónoma de Buenos Aires', 'CABA', 'CABA', -34.623, -58.441, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Chacarita', 'Ciudad Autónoma de Buenos Aires', 'CABA', 'CABA', -34.5872, -58.4542, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Colegiales', 'Ciudad Autónoma de Buenos Aires', 'CABA', 'CABA', -34.5748, -58.4497, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Villa Ortúzar', 'Ciudad Autónoma de Buenos Aires', 'CABA', 'CABA', -34.5797, -58.4686, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Paternal', 'Ciudad Autónoma de Buenos Aires', 'CABA', 'CABA', -34.5964, -58.4701, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Villa Crespo Sur', 'Ciudad Autónoma de Buenos Aires', 'CABA', 'CABA', -34.5996, -58.4403, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Villa del Parque', 'Ciudad Autónoma de Buenos Aires', 'CABA', 'CABA', -34.6038, -58.493, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Flores Norte', 'Ciudad Autónoma de Buenos Aires', 'CABA', 'CABA', -34.6272, -58.4638, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('Ramos Mejía', 'Ramos Mejía', 'La Matanza', 'Buenos Aires', -34.6419, -58.5666, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();
INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)
VALUES ('San Justo - UNLaM', 'San Justo', 'La Matanza', 'Buenos Aires', -34.6703, -58.5628, 700)
ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,
    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();

DELETE FROM punto_interes_zona WHERE fuente_codigo = 'OSM' AND zona_id IN
    (SELECT id FROM zona WHERE nombre IN ('Almagro', 'Boedo', 'Caballito Norte', 'Caballito Sur', 'Chacarita', 'Colegiales', 'Villa Ortúzar', 'Paternal', 'Villa Crespo Sur', 'Villa del Parque', 'Flores Norte', 'Ramos Mejía', 'San Justo - UNLaM'));
DELETE FROM metrica_zona WHERE fuente_codigo = 'OSM' AND zona_id IN
    (SELECT id FROM zona WHERE nombre IN ('Almagro', 'Boedo', 'Caballito Norte', 'Caballito Sur', 'Chacarita', 'Colegiales', 'Villa Ortúzar', 'Paternal', 'Villa Crespo Sur', 'Villa del Parque', 'Flores Norte', 'Ramos Mejía', 'San Justo - UNLaM'));

INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:256246017', 'TRANSPORTE', NULL, 'Loria', -34.61084, -58.415549, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:286815382', 'TRANSPORTE', NULL, 'Castro Barros', -34.611718, -58.42151, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1436034192', 'COMPLEMENTARIO', NULL, 'Faraday', -34.60936, -58.416141, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1436034212', 'SALUD', NULL, 'Sanatorio Franchín', -34.609381, -58.416692, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1445066908', 'COMPLEMENTARIO', NULL, 'Coto', -34.611921, -58.420605, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1445066908', 'COMPETENCIA', 'ALMACEN', 'Coto', -34.611921, -58.420605, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2193739869', 'COMPLEMENTARIO', NULL, 'Galicia', -34.611169, -58.41852, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2193739871', 'COMPLEMENTARIO', NULL, 'Banco Nación', -34.611201, -58.418852, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2193739885', 'COMPLEMENTARIO', NULL, 'Supervielle', -34.608699, -58.421628, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2604948470', 'COMPETENCIA', 'PAPELERIA', 'Editorial Peniel', -34.611518, -58.418394, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2922349767', 'OFICINA', NULL, 'Federación Porteña de Patín Billinghurst 319', -34.606701, -58.416112, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301744', 'TRANSPORTE', NULL, 'Díaz Vélez y Gascón', -34.608633, -58.424883, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301745', 'TRANSPORTE', NULL, 'Díaz Vélez y Gascón', -34.608714, -58.42482, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301746', 'TRANSPORTE', NULL, 'Avenida Medrano', -34.608569, -58.421721, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301747', 'TRANSPORTE', NULL, 'Avenida Medrano', -34.608573, -58.421996, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301748', 'TRANSPORTE', NULL, 'Avenida Medrano', -34.608659, -58.421727, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301749', 'TRANSPORTE', NULL, 'Avenida Medrano', -34.608652, -58.421999, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301756', 'TRANSPORTE', NULL, 'Mitre y Salguero', -34.609726, -58.420141, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301757', 'TRANSPORTE', NULL, 'Mitre y Salguero', -34.609766, -58.420143, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301772', 'TRANSPORTE', NULL, 'Mitre y Bustamante', -34.609412, -58.415186, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301773', 'TRANSPORTE', NULL, 'Mitre y Bustamante', -34.609478, -58.415183, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301776', 'TRANSPORTE', NULL, 'Mitre y Bravo', -34.609568, -58.417741, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3367301777', 'TRANSPORTE', NULL, 'Mitre y Bravo', -34.609614, -58.417739, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786549', 'TRANSPORTE', NULL, 'Avenida Díaz Vélez', -34.608364, -58.421518, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786550', 'TRANSPORTE', NULL, 'Avenida Díaz Vélez', -34.608355, -58.421607, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786551', 'TRANSPORTE', NULL, 'Avenida Díaz Vélez', -34.608237, -58.421595, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786552', 'TRANSPORTE', NULL, 'Avenida Díaz Vélez', -34.608239, -58.421508, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786575', 'TRANSPORTE', NULL, 'Medrano y Perón', -34.605539, -58.421256, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786576', 'TRANSPORTE', NULL, 'Medrano y Perón', -34.60554, -58.421361, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786579', 'TRANSPORTE', NULL, 'Medrano y Perón', -34.605837, -58.421374, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786580', 'TRANSPORTE', NULL, 'Medrano y Perón', -34.605845, -58.421283, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786583', 'TRANSPORTE', NULL, 'Mitre y Bravo', -34.60958, -58.417941, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786584', 'TRANSPORTE', NULL, 'Mitre y Bravo', -34.609628, -58.417944, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786585', 'TRANSPORTE', NULL, 'Mitre y Bustamante', -34.609408, -58.415109, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786586', 'TRANSPORTE', NULL, 'Mitre y Bustamante', -34.609471, -58.415107, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436437413', 'TRANSPORTE', NULL, 'Sarmiento y Medrano', -34.604942, -58.420888, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436437414', 'TRANSPORTE', NULL, 'Sarmiento y Medrano', -34.604876, -58.420884, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436437415', 'TRANSPORTE', NULL, 'Plaza Almagro', -34.605357, -58.41785, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436437416', 'TRANSPORTE', NULL, 'Plaza Almagro', -34.605309, -58.417844, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436438088', 'TRANSPORTE', NULL, 'Sarmiento y Billinghurst', -34.605735, -58.415611, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436438089', 'TRANSPORTE', NULL, 'Sarmiento y Billinghurst', -34.605688, -58.415605, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3449178924', 'TRANSPORTE', NULL, 'Castro Barros', -34.611693, -58.421522, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3449178927', 'TRANSPORTE', NULL, 'Loria', -34.610813, -58.415552, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3449220577', 'TRANSPORTE', NULL, 'Castro Barros', -34.611706, -58.421518, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3449220578', 'TRANSPORTE', NULL, 'Loria', -34.610828, -58.41555, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887499', 'TRANSPORTE', NULL, 'Sarmiento y Medrano', -34.604942, -58.420349, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887500', 'TRANSPORTE', NULL, 'Sarmiento y Medrano', -34.605015, -58.420353, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887549', 'TRANSPORTE', NULL, 'Plaza Almagro', -34.605339, -58.417655, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887550', 'TRANSPORTE', NULL, 'Plaza Almagro', -34.605388, -58.417662, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887551', 'TRANSPORTE', NULL, 'Sarmiento y Billinghurst', -34.605638, -58.415867, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887552', 'TRANSPORTE', NULL, 'Sarmiento y Billinghurst', -34.605695, -58.415871, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3914718106', 'TRANSPORTE', NULL, 'Gascón y Mitre', -34.610302, -58.424552, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3914894188', 'TRANSPORTE', NULL, 'Gascón y Mitre', -34.610288, -58.42448, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3914950075', 'TRANSPORTE', NULL, 'Gascón y Díaz Vélez', -34.609033, -58.424555, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4006747413', 'TRANSPORTE', NULL, 'Gascón y Díaz Vélez', -34.60903, -58.424659, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4230147289', 'OFICINA', NULL, 'INSSSEP', -34.605518, -58.416545, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4309114503', 'COMPETENCIA', 'ALMACEN', 'Kalimnnos', -34.611698, -58.42118, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4309172337', 'COMPETENCIA', 'ALMACEN', 'Estancia San Francisco', -34.612985, -58.420516, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4312146518', 'COMPLEMENTARIO', NULL, 'Larocca', -34.611057, -58.417159, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4312146540', 'TRANSPORTE', NULL, 'Rivadavia y Bulnes', -34.610912, -58.418267, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4312146544', 'ESCUELA', NULL, 'Escuela Alamos', -34.60905, -58.419892, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4312146545', 'TRANSPORTE', NULL, 'Plaza Almagro', -34.60656, -58.419859, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4351823995', 'TRANSPORTE', NULL, 'Plaza Almagro', -34.606555, -58.419913, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4351823999', 'TRANSPORTE', NULL, 'Rivadavia y Bulnes', -34.611005, -58.41826, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4413381568', 'COMPLEMENTARIO', NULL, 'Banco Nación', -34.610898, -58.41884, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4435535414', 'TRANSPORTE', NULL, 'Medrano y Rivadavia', -34.611131, -58.421165, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4435535415', 'TRANSPORTE', NULL, 'Medrano y Rivadavia', -34.611262, -58.421147, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4441275729', 'TRANSPORTE', NULL, 'Medrano y Rivadavia', -34.611117, -58.421089, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4441275730', 'TRANSPORTE', NULL, 'Medrano y Rivadavia', -34.611241, -58.421066, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4560740228', 'TRANSPORTE', NULL, 'Boedo y Rivadavia', -34.611731, -58.41817, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4560740229', 'TRANSPORTE', NULL, 'Boedo y Rivadavia', -34.611754, -58.418213, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4560740230', 'TRANSPORTE', NULL, 'Rivadavia y Mario Bravo', -34.610834, -58.4171, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4560740231', 'TRANSPORTE', NULL, 'Rivadavia y Mario Bravo', -34.61088, -58.417692, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4560740232', 'TRANSPORTE', NULL, 'Rivadavia y Mario Bravo', -34.610914, -58.417093, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4560740233', 'TRANSPORTE', NULL, 'Rivadavia y Mario Bravo', -34.610961, -58.41768, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4560740254', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.610693, -58.414851, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4560740255', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.610695, -58.414929, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4560740256', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.610726, -58.415394, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4560740257', 'TRANSPORTE', NULL, 'Rivadavia y Billinghurst', -34.610726, -58.41557, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4560740258', 'TRANSPORTE', NULL, 'Boedo y Rivadavia', -34.61152, -58.418309, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4687888568', 'COMPLEMENTARIO', NULL, 'Latina', -34.611413, -58.421242, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4687888569', 'COMPETENCIA', 'ALMACEN', 'El Jevi', -34.611365, -58.421167, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4687888569', 'COMPETENCIA', 'KIOSCO', 'El Jevi', -34.611365, -58.421167, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4687888574', 'COMPLEMENTARIO', NULL, 'Galicia Más', -34.611729, -58.421995, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689892682', 'COMPETENCIA', 'ALMACEN', 'De la Esquina', -34.608385, -58.419979, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689892792', 'COMPLEMENTARIO', NULL, 'Díaz Vélez', -34.608408, -58.421382, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689892798', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.608466, -58.421716, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689937273', 'OFICINA', NULL, 'Asociación de Educadores Porteños', -34.608481, -58.424491, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4697318105', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.608645, -58.420262, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4697318110', 'COMPLEMENTARIO', NULL, 'Supermercado', -34.608674, -58.419647, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4697318110', 'COMPETENCIA', 'ALMACEN', 'Supermercado', -34.608674, -58.419647, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4700326054', 'COMPLEMENTARIO', NULL, 'Supermercado del Juguete', -34.611218, -58.41942, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4700326054', 'COMPETENCIA', 'ALMACEN', 'Supermercado del Juguete', -34.611218, -58.41942, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4853892923', 'COMPETENCIA', 'PAPELERIA', 'ArCaVe Centro Integral de Copiado', -34.611353, -58.419553, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4868903307', 'TRANSPORTE', NULL, 'Plaza Almagro', -34.60634, -58.419895, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4868903308', 'TRANSPORTE', NULL, 'Plaza Almagro', -34.606452, -58.419905, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4868903309', 'TRANSPORTE', NULL, 'Plaza Almagro', -34.606344, -58.419833, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4868903310', 'TRANSPORTE', NULL, 'Plaza Almagro', -34.606454, -58.419848, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4868903311', 'TRANSPORTE', NULL, 'Salguero y Díaz Vélez', -34.608905, -58.420007, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4868903312', 'TRANSPORTE', NULL, 'Salguero y Díaz Vélez', -34.608903, -58.419964, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4868903313', 'TRANSPORTE', NULL, 'Salguero y Díaz Vélez', -34.609035, -58.419992, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4868903314', 'TRANSPORTE', NULL, 'Salguero y Díaz Vélez', -34.609032, -58.41995, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5035956858', 'TRANSPORTE', NULL, 'Potosí y Medrano', -34.607254, -58.421124, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5035956859', 'TRANSPORTE', NULL, 'Potosí y Medrano', -34.60722, -58.421124, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5035956860', 'TRANSPORTE', NULL, 'Bulnes y Potosí', -34.607203, -58.418662, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5035956861', 'TRANSPORTE', NULL, 'Bulnes y Potosí', -34.607198, -58.418718, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5035956863', 'TRANSPORTE', NULL, 'Perón y Billinghurst', -34.606966, -58.416078, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5035956864', 'TRANSPORTE', NULL, 'Perón y Billinghurst', -34.606918, -58.416069, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741806', 'TRANSPORTE', NULL, 'Medrano - Castro Barros', -34.611336, -58.420573, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741807', 'TRANSPORTE', NULL, 'Medrano - Castro Barros', -34.611247, -58.420596, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741808', 'TRANSPORTE', NULL, 'Medrano - Castro Barros', -34.611219, -58.420122, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741809', 'TRANSPORTE', NULL, 'Medrano - Castro Barros', -34.61113, -58.420151, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117172779', 'ESCUELA', NULL, 'Instituto de Formación Técnica Superior 28', -34.610733, -58.416812, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117172780', 'ESCUELA', NULL, 'Centro de Formación Profesional 14 Eduardo Vicente - UTE', -34.610722, -58.41681, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117348071', 'ESCUELA', NULL, 'Mio Bambino', -34.609021, -58.419892, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117379859', 'ESCUELA', NULL, 'Centro Educativo de Nivel Primario 29 - CESAC 38', -34.606667, -58.42148, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117379872', 'ESCUELA', NULL, 'Centro Educativo de Nivel Primario 04', -34.609328, -58.416587, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117413134', 'ESCUELA', NULL, 'Instituto Tierra Santa', -34.609306, -58.415184, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117427848', 'ESCUELA', NULL, 'Jardín Maternal y Jardín de Infantes Dulce de Leche', -34.605334, -58.417249, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117454432', 'ESCUELA', NULL, 'Colegio Universitario "Doctor Pierre Fauchard"', -34.607846, -58.421643, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117546530', 'ESCUELA', NULL, 'Escuela de Educación Media 01/02° E.Reingreso', -34.607928, -58.417562, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117565966', 'ESCUELA', NULL, 'Escuela de la Joya', -34.610915, -58.422267, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117565967', 'ESCUELA', NULL, 'Centro Educativo de Nivel Secundario 20', -34.610896, -58.422259, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117565968', 'ESCUELA', NULL, 'Escuela Superior de Educación Artística en Arte Cerámico 01', -34.61039, -58.418444, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117576222', 'ESCUELA', NULL, 'ESEA en Danza Ramicone Curso Vocacional de Folklore 10', -34.610374, -58.418428, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117576313', 'ESCUELA', NULL, 'Instituto de Formación Técnica Superior 19', -34.611491, -58.42029, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117590881', 'ESCUELA', NULL, 'Escuela de Enfermería del Htal. Italiano', -34.607407, -58.423411, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117591921', 'ESCUELA', NULL, 'Centro de Formación Profesional 08 (anexo)', -34.609078, -58.421582, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117619915', 'ESCUELA', NULL, 'Escuela Primaria Común 22 Carlos Javier Benielli', -34.606368, -58.415055, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117619916', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado D (EPCjs 22/02°)', -34.606388, -58.415054, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117621824', 'ESCUELA', NULL, 'Escuela Primaria Común 18 Alejandro Carbo', -34.607949, -58.417561, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117621825', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado D (EPCjc 18/02°)', -34.607909, -58.417571, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117621864', 'ESCUELA', NULL, 'Centro de Formación Profesional 29 Asociación de Educadores Porteños', -34.608474, -58.424491, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5300234029', 'TRANSPORTE', NULL, 'Colombres y Rivadavia', -34.611463, -58.419603, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5300234030', 'TRANSPORTE', NULL, 'Colombres y Rivadavia', -34.611456, -58.419561, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5300234031', 'TRANSPORTE', NULL, 'Colombres y Rivadavia', -34.611302, -58.419655, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5300234032', 'TRANSPORTE', NULL, 'Colombres y Rivadavia', -34.611295, -58.419619, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5300250583', 'TRANSPORTE', NULL, 'Medrano y Sarmiento', -34.604612, -58.421149, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5300250584', 'TRANSPORTE', NULL, 'Medrano y Sarmiento', -34.604608, -58.421213, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5341790085', 'COMPLEMENTARIO', NULL, 'Oribe', -34.605797, -58.416242, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5341790181', 'COMPLEMENTARIO', NULL, 'Almacén Natural', -34.606548, -58.416082, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5341790181', 'COMPETENCIA', 'ALMACEN', 'Almacén Natural', -34.606548, -58.416082, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397354906', 'TRANSPORTE', NULL, 'Perón y Medrano', -34.605947, -58.421051, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5411463182', 'TRANSPORTE', NULL, 'Perón y Medrano', -34.606013, -58.421044, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5424446837', 'OFICINA', NULL, 'APLA', -34.611161, -58.423891, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5465713819', 'OFICINA', NULL, 'Grupo Coda', -34.609386, -58.421531, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5514598126', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.610831, -58.414772, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5727130915', 'TRANSPORTE', NULL, 'Díaz Vélez y Billinghurst', -34.608549, -58.416222, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5727130916', 'TRANSPORTE', NULL, 'Díaz Vélez y Billinghurst', -34.608613, -58.416227, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6032757536', 'OFICINA', NULL, 'Mala Junta', -34.612343, -58.419434, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6032764368', 'OFICINA', NULL, 'CASA FUSA', -34.61098, -58.421294, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6250918647', 'TRANSPORTE', NULL, 'Salguero y Díaz Vélez', -34.608775, -58.41998, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6250918648', 'TRANSPORTE', NULL, 'Salguero y Díaz Vélez', -34.608778, -58.420024, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6250929173', 'COMPLEMENTARIO', NULL, 'Sanyo', -34.61045, -58.419646, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6250929173', 'COMPETENCIA', 'ALMACEN', 'Sanyo', -34.61045, -58.419646, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6250929486', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.60506, -58.419441, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6250929486', 'COMPETENCIA', 'KIOSCO', 'Carrefour Express', -34.60506, -58.419441, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6283718954', 'SALUD', NULL, 'Centro Médico Mevaterapia', -34.605902, -58.422095, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6551856403', 'TRANSPORTE', NULL, 'Medrano - Castro Barros', -34.611202, -58.420437, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6551856404', 'TRANSPORTE', NULL, 'Medrano - Castro Barros', -34.611295, -58.420413, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6766479383', 'PARQUE', NULL, 'Lugar sin nombre', -34.609611, -58.420719, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6874579483', 'TRANSPORTE', NULL, 'Boedo y Rivadavia', -34.6115, -58.418272, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6874579484', 'TRANSPORTE', NULL, 'Rivadavia y Billinghurst', -34.61081, -58.415551, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7502414724', 'SALUD', NULL, 'Centro de Salud Nivel 1 Almagro - CeSAC 38', -34.606702, -58.421487, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8253361782', 'COMPLEMENTARIO', NULL, 'Nuevos Rumbos', -34.610287, -58.419688, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8253361782', 'COMPETENCIA', 'ALMACEN', 'Nuevos Rumbos', -34.610287, -58.419688, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8253364001', 'ESCUELA', NULL, 'Escuela Álamos', -34.609105, -58.419893, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8459826168', 'TRANSPORTE', NULL, 'Mitre y Bustamante', -34.609458, -58.4149, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8459826169', 'TRANSPORTE', NULL, 'Mitre y Bustamante', -34.609395, -58.414902, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8459826171', 'TRANSPORTE', NULL, 'Mitre y Bravo', -34.609604, -58.417607, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8459826172', 'TRANSPORTE', NULL, 'Mitre y Bravo', -34.609559, -58.417611, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8459826175', 'TRANSPORTE', NULL, 'Mitre y Salguero', -34.609766, -58.420058, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8459826176', 'TRANSPORTE', NULL, 'Mitre y Salguero', -34.609721, -58.420057, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8459826182', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.611872, -58.421735, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8459826183', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.611732, -58.421803, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8459826184', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.611562, -58.42142, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8459826185', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.611686, -58.421338, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8459826186', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.611635, -58.421216, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8508723576', 'COMPLEMENTARIO', NULL, 'FAURE - Almacén Saludable', -34.606268, -58.419681, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8508723576', 'COMPETENCIA', 'ALMACEN', 'FAURE - Almacén Saludable', -34.606268, -58.419681, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10736150968', 'COMPLEMENTARIO', NULL, 'Dia', -34.605876, -58.42351, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10736150968', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.605876, -58.42351, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11126708011', 'TRANSPORTE', NULL, 'Sarmiento y Billinghurst', -34.605664, -58.415735, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11126708012', 'TRANSPORTE', NULL, 'Sarmiento y Billinghurst', -34.605715, -58.415741, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11695267269', 'COMPLEMENTARIO', NULL, 'El Baquiano Almacén', -34.610478, -58.421259, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11695267269', 'COMPETENCIA', 'ALMACEN', 'El Baquiano Almacén', -34.610478, -58.421259, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11875163928', 'COMPLEMENTARIO', NULL, 'Super Chino', -34.611198, -58.421202, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11875163928', 'COMPETENCIA', 'ALMACEN', 'Super Chino', -34.611198, -58.421202, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11875168929', 'SALUD', NULL, 'Lugar sin nombre', -34.607548, -58.42133, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11875185386', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.607995, -58.421615, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11875185386', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.607995, -58.421615, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11875214984', 'COMPETENCIA', 'ALMACEN', 'Maxikiosco La Estrella', -34.609286, -58.42154, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11875214984', 'COMPETENCIA', 'KIOSCO', 'Maxikiosco La Estrella', -34.609286, -58.42154, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11875216487', 'COMPLEMENTARIO', NULL, 'Farmacity', -34.608724, -58.421325, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12543100819', 'TRANSPORTE', NULL, 'Avenida Díaz Vélez', -34.608306, -58.421513, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12543100820', 'TRANSPORTE', NULL, 'Avenida Díaz Vélez', -34.6083, -58.421601, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12543100821', 'TRANSPORTE', NULL, 'Medrano - Castro Barros', -34.611392, -58.42079, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12543100822', 'TRANSPORTE', NULL, 'Medrano - Castro Barros', -34.611306, -58.420815, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12543100823', 'TRANSPORTE', NULL, 'Avenida Rivadavia', -34.61113, -58.417252, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12543100824', 'TRANSPORTE', NULL, 'Avenida Rivadavia', -34.611116, -58.417202, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12543100825', 'TRANSPORTE', NULL, 'Rivadavia y Mario Bravo', -34.610839, -58.417166, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12543100826', 'TRANSPORTE', NULL, 'Rivadavia y Mario Bravo', -34.610918, -58.417154, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13091744733', 'COMPLEMENTARIO', NULL, 'Dia', -34.611131, -58.420382, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13091744733', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.611131, -58.420382, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13326819601', 'COMPETENCIA', 'ALMACEN', 'Verdulería y Frutería Primavera', -34.61106, -58.416847, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13420335753', 'TRANSPORTE', NULL, 'Mario Bravo y Valentín Gómez', -34.604947, -58.416972, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13420335754', 'TRANSPORTE', NULL, 'Mario Bravo y Valentín Gómez', -34.604953, -58.416927, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13682216901', 'COMPLEMENTARIO', NULL, 'Carrefour Express', -34.605894, -58.421373, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13682216901', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.605894, -58.421373, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:19217173', 'PARQUE', NULL, 'Plaza Almagro', -34.605925, -58.419081, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:337870289', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.61167, -58.421531, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:337870291', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.610871, -58.415539, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:337870298', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.610791, -58.415547, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:337870299', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.611736, -58.4215, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:440879857', 'PARQUE', NULL, 'Jardín Mario Jorge de Lellis', -34.611037, -58.42309, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:440888971', 'PARQUE', NULL, 'Lugar sin nombre', -34.605847, -58.418674, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:474933905', 'COMPETENCIA', 'ALMACEN', 'Alakoa', -34.61072, -58.416284, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:474934033', 'ESCUELA', NULL, 'Colegio 03 Mariano Moreno', -34.610714, -58.416832, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:474934036', 'COMPLEMENTARIO', NULL, 'Banco Santander', -34.610782, -58.418218, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:474934328', 'COMPLEMENTARIO', NULL, 'Las Nieves', -34.610808, -58.418414, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475206702', 'COMPETENCIA', 'ALMACEN', 'Carrefour Market', -34.608349, -58.421811, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475206702', 'COMPETENCIA', 'KIOSCO', 'Carrefour Market', -34.608349, -58.421811, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475214352', 'COMPLEMENTARIO', NULL, 'Banco Santander', -34.608419, -58.424837, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476443982', 'COMPLEMENTARIO', NULL, 'Dia Market', -34.61121, -58.416695, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476443982', 'COMPETENCIA', 'ALMACEN', 'Dia Market', -34.61121, -58.416695, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476444746', 'COMPETENCIA', 'ALMACEN', 'La Sud America', -34.612362, -58.422662, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:596235607', 'PARQUE', NULL, 'Parque de la Estación', -34.607894, -58.413117, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:637969657', 'PARQUE', NULL, 'Lugar sin nombre', -34.60963, -58.420905, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1051206062', 'ESCUELA', NULL, 'Instituto Nuestro Tiempo', -34.609647, -58.417184, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1051444925', 'ESCUELA', NULL, 'Instituto Superior Mitre', -34.609966, -58.420003, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:1380262', 'TRANSPORTE', NULL, 'Estación Castro Barros', -34.611704, -58.421514, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:4439266', 'SALUD', NULL, 'Hospital Italiano de Buenos Aires', -34.607914, -58.423639, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6495503', 'TRANSPORTE', NULL, 'Plaza Almagro', -34.60645, -58.419873, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6495507', 'TRANSPORTE', NULL, 'Rivadavia y Bulnes', -34.610958, -58.418263, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6639308', 'TRANSPORTE', NULL, 'Avenida Rivadavia', -34.611189, -58.421116, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6639396', 'TRANSPORTE', NULL, 'Estación Loria', -34.6108, -58.415471, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6801723', 'TRANSPORTE', NULL, 'Boedo al 50', -34.611742, -58.418191, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6801724', 'TRANSPORTE', NULL, 'Bravo - Maza', -34.610874, -58.417097, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6801725', 'TRANSPORTE', NULL, 'Bravo - Maza', -34.610921, -58.417686, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7268548', 'TRANSPORTE', NULL, 'Avenida Díaz Vélez', -34.608969, -58.419979, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7474075', 'TRANSPORTE', NULL, 'Avenida Medrano', -34.607237, -58.421124, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7474076', 'TRANSPORTE', NULL, 'Bulnes y Potosí', -34.607201, -58.41869, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7474077', 'TRANSPORTE', NULL, 'Perón y Billinghurst', -34.606942, -58.416073, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7543687', 'TRANSPORTE', NULL, 'Medrano - Castro Barros', -34.611233, -58.420359, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7834035', 'TRANSPORTE', NULL, 'Avenida Rivadavia', -34.611379, -58.419608, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8019718', 'TRANSPORTE', NULL, 'Perón y Medrano', -34.60598, -58.421048, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8416162', 'TRANSPORTE', NULL, 'Díaz Vélez y Billinghurst', -34.608581, -58.416225, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:20030732', 'TRANSPORTE', NULL, 'Mario Bravo y Valentín Gómez', -34.60495, -58.41695, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 26, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 7, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 143, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 6, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 5, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 28, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 24, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 2, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 7, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Almagro' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:256242013', 'TRANSPORTE', NULL, 'Boedo', -34.62548, -58.416116, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3769671013', 'TRANSPORTE', NULL, 'Boedo', -34.625403, -58.416111, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3772176438', 'TRANSPORTE', NULL, 'Boedo', -34.625533, -58.416095, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483326976', 'TRANSPORTE', NULL, 'Castro Barros', -34.624512, -58.418721, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483326977', 'TRANSPORTE', NULL, 'Castro Barros', -34.624526, -58.418924, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483326978', 'TRANSPORTE', NULL, 'Castro Barros', -34.624556, -58.418573, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483326979', 'TRANSPORTE', NULL, 'Castro Barros', -34.624565, -58.418716, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483326980', 'TRANSPORTE', NULL, 'Castro Barros', -34.624577, -58.418913, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4591103082', 'OFICINA', NULL, 'Seguros', -34.632142, -58.42172, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4591289226', 'COMPETENCIA', 'ALMACEN', 'El Central', -34.626076, -58.421437, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4598961683', 'COMPETENCIA', 'ALMACEN', 'Frutas para Todos', -34.630901, -58.414608, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4598961684', 'COMPETENCIA', 'ALMACEN', 'Express', -34.630922, -58.414665, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4600209582', 'COMPLEMENTARIO', NULL, 'Macro', -34.63118, -58.415769, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4600209586', 'COMPLEMENTARIO', NULL, 'Farmacias', -34.631994, -58.416149, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4600271139', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.630189, -58.417379, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4600271139', 'COMPETENCIA', 'KIOSCO', 'Dia', -34.630189, -58.417379, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4600271141', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.629742, -58.414616, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4600271141', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.629742, -58.414616, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4600271155', 'COMPETENCIA', 'ALMACEN', 'Amor', -34.632195, -58.415836, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4600271155', 'COMPETENCIA', 'KIOSCO', 'Amor', -34.632195, -58.415836, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4746370630', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.632244, -58.422129, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5001045506', 'TRANSPORTE', NULL, 'Castro Barros e Inclán', -34.632636, -58.417887, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5001045507', 'TRANSPORTE', NULL, 'Boedo y Garay', -34.631125, -58.416037, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5004211059', 'TRANSPORTE', NULL, 'Castro Barros e Inclán', -34.632635, -58.417822, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5004211061', 'TRANSPORTE', NULL, 'Boedo y Garay', -34.631127, -58.415938, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5113951276', 'ESCUELA', NULL, 'Escuela de Maestros (anexo 02)', -34.629886, -58.422502, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5113951277', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado C (EPCjs 25/06°)', -34.629992, -58.42254, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5113951278', 'ESCUELA', NULL, 'ESEA en Danza Mastrazzi Curso Vocacional de Danza y Expresión Corporal Infantil 10', -34.62993, -58.422556, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5116062583', 'ESCUELA', NULL, 'Escuela Primaria p/Adultos 26 República de Colombia', -34.632042, -58.420148, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117420007', 'ESCUELA', NULL, 'Escuela de Comercio 22 Gustavo Adolfo Martínez Zuviría', -34.62905, -58.422939, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117441255', 'ESCUELA', NULL, 'Centro de Formación Profesional 30', -34.630564, -58.423087, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117448212', 'ESCUELA', NULL, 'Escuela Infantil Meterete', -34.625937, -58.418647, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117571507', 'ESCUELA', NULL, 'Escuela Generación del Futuro', -34.62475, -58.419764, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6135674142', 'TRANSPORTE', NULL, 'Garay y Boedo', -34.631206, -58.41565, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6135674143', 'TRANSPORTE', NULL, 'Garay y Boedo', -34.631283, -58.415634, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6135674144', 'TRANSPORTE', NULL, 'Garay y Castro Barros', -34.631478, -58.417608, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6135674145', 'TRANSPORTE', NULL, 'Garay y Castro Barros', -34.631577, -58.417593, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6135674146', 'TRANSPORTE', NULL, 'Garay y José Mármol', -34.632014, -58.42271, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6135674147', 'TRANSPORTE', NULL, 'Garay y José Mármol', -34.632097, -58.422697, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6135674148', 'TRANSPORTE', NULL, 'Garay y Quintino Bocayuva', -34.631794, -58.420313, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6135674149', 'TRANSPORTE', NULL, 'Garay y Quintino Bocayuva', -34.631853, -58.420307, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6191408872', 'TRANSPORTE', NULL, 'Avenida San Juan', -34.625936, -58.417579, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6191408873', 'TRANSPORTE', NULL, 'Avenida San Juan', -34.625951, -58.417648, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6191408874', 'TRANSPORTE', NULL, 'Avenida San Juan', -34.626244, -58.417527, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6191408875', 'TRANSPORTE', NULL, 'Avenida San Juan', -34.626255, -58.417589, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6709238018', 'TRANSPORTE', NULL, 'Colombres y San Juan', -34.625257, -58.417634, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6709238019', 'TRANSPORTE', NULL, 'Colombres y San Juan', -34.625257, -58.417714, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6709238020', 'TRANSPORTE', NULL, 'San Juan y Castro Barros', -34.625893, -58.419208, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6709238021', 'TRANSPORTE', NULL, 'San Juan y Castro Barros', -34.625962, -58.419194, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6709238022', 'TRANSPORTE', NULL, 'San Juan y Bocayuva', -34.626281, -58.421899, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6709238023', 'TRANSPORTE', NULL, 'San Juan y Bocayuva', -34.626303, -58.422046, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6709238024', 'TRANSPORTE', NULL, 'San Juan y Bocayuva', -34.626378, -58.421884, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6709238025', 'TRANSPORTE', NULL, 'San Juan y Bocayuva', -34.626401, -58.422034, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6709238026', 'TRANSPORTE', NULL, 'San Juan y Bocayuva', -34.626325, -58.422187, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6709238027', 'TRANSPORTE', NULL, 'San Juan y Bocayuva', -34.62642, -58.422166, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7215532512', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.625454, -58.416362, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7215532513', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.625498, -58.416654, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7215532514', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.625538, -58.416349, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7215532515', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.625583, -58.416643, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7215532516', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.625559, -58.417061, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7215532517', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.625648, -58.417041, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7418366934', 'TRANSPORTE', NULL, 'Avenida Pavón', -34.630472, -58.416501, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7418366935', 'TRANSPORTE', NULL, 'Avenida Pavón', -34.630473, -58.41646, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8522882992', 'TRANSPORTE', NULL, 'Maza y Tarija', -34.629002, -58.41454, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8522882993', 'TRANSPORTE', NULL, 'Maza y Tarija', -34.628993, -58.414462, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8522882994', 'TRANSPORTE', NULL, 'Maza y Tarija', -34.629235, -58.414534, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8522882995', 'TRANSPORTE', NULL, 'Maza y Tarija', -34.629233, -58.414457, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8522882998', 'TRANSPORTE', NULL, 'Avenida Juan de Garay', -34.631281, -58.414497, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8522882999', 'TRANSPORTE', NULL, 'Avenida Juan de Garay', -34.631279, -58.414435, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8522883000', 'TRANSPORTE', NULL, 'Avenida Juan de Garay', -34.631441, -58.414493, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8522883001', 'TRANSPORTE', NULL, 'Avenida Juan de Garay', -34.63144, -58.414421, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8999266891', 'TRANSPORTE', NULL, 'Cochabamba y Marmol', -34.627881, -58.423583, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8999266892', 'TRANSPORTE', NULL, 'Cochabamba y Marmol', -34.627833, -58.423594, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8999266898', 'TRANSPORTE', NULL, 'Cochabamba y Bocayuva', -34.627523, -58.421011, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8999266899', 'TRANSPORTE', NULL, 'Cochabamba y Bocayuva', -34.627479, -58.421019, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8999266900', 'TRANSPORTE', NULL, 'Cochabamba y Castro', -34.62735, -58.41952, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8999266901', 'TRANSPORTE', NULL, 'Cochabamba y Castro', -34.627295, -58.419529, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8999266905', 'TRANSPORTE', NULL, 'Cochabamba y Castro Barros', -34.627213, -58.418285, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8999266906', 'TRANSPORTE', NULL, 'Cochabamba y Castro Barros', -34.627153, -58.418295, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8999266907', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.626915, -58.41573, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8999266908', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.626844, -58.415738, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9896504284', 'TRANSPORTE', NULL, 'Boedo y Garay', -34.630962, -58.415932, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9896504285', 'TRANSPORTE', NULL, 'Boedo y Garay', -34.630833, -58.415928, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9896504286', 'TRANSPORTE', NULL, 'Boedo y Garay', -34.630957, -58.416025, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9896504287', 'TRANSPORTE', NULL, 'Boedo y Garay', -34.630831, -58.416021, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9896504288', 'TRANSPORTE', NULL, 'Boedo y Garay', -34.630718, -58.415924, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9896504289', 'TRANSPORTE', NULL, 'Boedo y Garay', -34.630716, -58.416017, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9896504290', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.630192, -58.41614, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9896504291', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.630279, -58.416133, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9896504292', 'TRANSPORTE', NULL, 'Boedo y Tarija', -34.62871, -58.415919, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9896504293', 'TRANSPORTE', NULL, 'Boedo y Tarija', -34.628712, -58.415996, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9906953417', 'TRANSPORTE', NULL, 'Pavón y Marmol', -34.631038, -58.422866, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9906953418', 'TRANSPORTE', NULL, 'Pavón y Marmol', -34.630965, -58.422883, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9906953419', 'OFICINA', NULL, 'Unión Obreros y Empleados Plásticos', -34.630834, -58.422392, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9906953420', 'TRANSPORTE', NULL, 'Pavón y Castro', -34.630506, -58.418929, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9906953421', 'TRANSPORTE', NULL, 'Pavón y Castro', -34.630412, -58.418944, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10154888117', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.630752, -58.417852, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11458509921', 'TRANSPORTE', NULL, 'Pavón y Castro', -34.630643, -58.419418, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11458509922', 'TRANSPORTE', NULL, 'Pavón y Castro', -34.630563, -58.419424, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11873885872', 'OFICINA', NULL, 'Comunarte Boedo', -34.626455, -58.422125, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11873888186', 'OFICINA', NULL, 'Andriana Gentile', -34.626327, -58.421374, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11873895207', 'COMPETENCIA', 'ALMACEN', 'PANADERIA CONFITERIA SAN CAYETANO', -34.626363, -58.421528, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11873899186', 'COMPETENCIA', 'KIOSCO', 'Maxikiosco Marco', -34.626399, -58.421742, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11873900460', 'OFICINA', NULL, 'Pro Comuna 5 Boedo', -34.626413, -58.421828, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11873905606', 'OFICINA', NULL, 'RE/MAX', -34.626434, -58.421993, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11873906384', 'COMPETENCIA', 'PAPELERIA', 'Magu Librería', -34.62652, -58.422493, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11873915015', 'COMPETENCIA', 'ALMACEN', 'Panaderia San Julian', -34.62622, -58.422546, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12980365201', 'COMPETENCIA', 'ALMACEN', 'San Cayetano', -34.630519, -58.423343, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13266173801', 'SALUD', NULL, 'Centro médico Boedo', -34.628855, -58.415214, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13541495000', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.625194, -58.416214, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13542444360', 'TRANSPORTE', NULL, 'Castro e Inclán', -34.633204, -58.419247, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13542444361', 'TRANSPORTE', NULL, 'Castro e Inclán', -34.633205, -58.419194, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13542444372', 'TRANSPORTE', NULL, 'Colombres e Inclán', -34.632558, -58.416628, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13542444373', 'TRANSPORTE', NULL, 'Colombres e Inclán', -34.63256, -58.416573, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13542444380', 'TRANSPORTE', NULL, 'Avenida San Juan', -34.625158, -58.416208, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:136792934', 'PARQUE', NULL, 'Plaza Mario Abel Amaya', -34.627777, -58.416637, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:284082921', 'ESCUELA', NULL, 'Escuela Primaria Común 25 Paula Albarracín de Sarmiento', -34.630062, -58.422475, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:373809358', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.625464, -58.416075, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:445079551', 'PARQUE', NULL, 'Lugar sin nombre', -34.627802, -58.41621, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463908095', 'ESCUELA', NULL, 'Escuela Primaria Común 26 República de Colombia', -34.632161, -58.420191, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463908099', 'COMPLEMENTARIO', NULL, 'Farmacia', -34.632244, -58.42213, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463931014', 'COMPLEMENTARIO', NULL, 'Supermercado Tendy', -34.626033, -58.42215, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463931014', 'COMPETENCIA', 'ALMACEN', 'Supermercado Tendy', -34.626033, -58.42215, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463934127', 'OFICINA', NULL, 'La Caja', -34.625598, -58.418724, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463934129', 'OFICINA', NULL, 'Radio Taxi', -34.630213, -58.417657, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463949633', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.626116, -58.419651, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463949633', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.626116, -58.419651, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463955656', 'COMPLEMENTARIO', NULL, 'Supermercado Hua Long', -34.630107, -58.418395, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463955656', 'COMPETENCIA', 'ALMACEN', 'Supermercado Hua Long', -34.630107, -58.418395, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463955657', 'OFICINA', NULL, 'Lugar sin nombre', -34.630835, -58.422849, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463955658', 'COMPLEMENTARIO', NULL, 'Farmacia', -34.630855, -58.422941, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463955659', 'COMPLEMENTARIO', NULL, 'Supermercado Mega', -34.63094, -58.423761, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:463955659', 'COMPETENCIA', 'ALMACEN', 'Supermercado Mega', -34.63094, -58.423761, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6691914', 'TRANSPORTE', NULL, 'Castro Barros', -34.624539, -58.418749, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7430991', 'TRANSPORTE', NULL, 'Castro Barros e Inclán', -34.632636, -58.417854, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7430992', 'TRANSPORTE', NULL, 'Boedo y Juan de Garay', -34.631126, -58.415987, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7430994', 'TRANSPORTE', NULL, 'Estación Boedo', -34.625387, -58.416075, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9109900', 'TRANSPORTE', NULL, 'Garay y Boedo', -34.631245, -58.415642, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9109901', 'TRANSPORTE', NULL, 'Garay y Castro Barros', -34.631528, -58.4176, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9109902', 'TRANSPORTE', NULL, 'Garay y Quintino Bocayuva', -34.631824, -58.42031, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9109903', 'TRANSPORTE', NULL, 'Garay y José Mármol', -34.632055, -58.422703, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9918228', 'TRANSPORTE', NULL, 'San Juan y Castro Barros', -34.625928, -58.419201, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9918229', 'TRANSPORTE', NULL, 'San Juan y Bocayuva', -34.626373, -58.422176, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10716558', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.625603, -58.417051, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:11012145', 'TRANSPORTE', NULL, 'Avenida Pavón', -34.630473, -58.416481, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:12451099', 'TRANSPORTE', NULL, 'Maza y Tarija', -34.629234, -58.414496, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:12451101', 'TRANSPORTE', NULL, 'Avenida Juan de Garay', -34.631441, -58.414457, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13079721', 'TRANSPORTE', NULL, 'Cochabamba y Marmol', -34.627857, -58.423588, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13079722', 'TRANSPORTE', NULL, 'Cochabamba y Bocayuva', -34.627501, -58.421015, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13079723', 'TRANSPORTE', NULL, 'Cochabamba y Castro', -34.627323, -58.419524, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13079724', 'TRANSPORTE', NULL, 'Cochabamba y Castro Barros', -34.627183, -58.41829, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13079725', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.62688, -58.415734, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14367311', 'TRANSPORTE', NULL, 'Avenida Boedo', -34.630236, -58.416136, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14367312', 'TRANSPORTE', NULL, 'Boedo y Tarija', -34.628711, -58.415957, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14381402', 'TRANSPORTE', NULL, 'Pavón y Marmol', -34.631001, -58.422875, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14381403', 'TRANSPORTE', NULL, 'Pavón y Castro', -34.630459, -58.418936, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:20199262', 'TRANSPORTE', NULL, 'Castro e Inclán', -34.633204, -58.41922, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:20199265', 'TRANSPORTE', NULL, 'Colombres e Inclán', -34.632559, -58.416601, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 10, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 9, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 108, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 2, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 1, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 9, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 13, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 1, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 5, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Boedo' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1393060485', 'ESCUELA', NULL, 'Escuela del Parque', -34.613023, -58.433306, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1393147615', 'ESCUELA', NULL, 'Escuela del Nuevo Sol', -34.615379, -58.436062, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2364860583', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.615718, -58.436759, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2364860583', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.615718, -58.436759, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2384907561', 'TRANSPORTE', NULL, 'Acoyte y Vallese', -34.611311, -58.439176, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2384907565', 'TRANSPORTE', NULL, 'Acoyte y Vallese', -34.611153, -58.439251, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2384907566', 'TRANSPORTE', NULL, 'Acoyte y Vallese', -34.611103, -58.439074, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2384907568', 'TRANSPORTE', NULL, 'Acoyte y Vallese', -34.61204, -58.438651, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2636745415', 'TRANSPORTE', NULL, 'Acoyte y Vallese', -34.61112, -58.439165, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2715927477', 'TRANSPORTE', NULL, 'Acoyte y Vallese', -34.61207, -58.438728, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3264602292', 'TRANSPORTE', NULL, 'Acoyte y Vallese', -34.611281, -58.439094, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531429', 'TRANSPORTE', NULL, 'Avenida Díaz Vélez', -34.608938, -58.434662, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531430', 'TRANSPORTE', NULL, 'Avenida Díaz Vélez', -34.608952, -58.434711, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531437', 'TRANSPORTE', NULL, 'Avellaneda y Río de Janeiro', -34.611024, -58.430563, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531438', 'TRANSPORTE', NULL, 'Avellaneda y Río de Janeiro', -34.611077, -58.430526, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531452', 'TRANSPORTE', NULL, 'Avellaneda y Otamendi', -34.611989, -58.432862, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531453', 'TRANSPORTE', NULL, 'Avellaneda y Otamendi', -34.612038, -58.432827, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058875', 'TRANSPORTE', NULL, 'Hospital Durand', -34.608463, -58.437307, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058876', 'TRANSPORTE', NULL, 'Hospital Durand', -34.608505, -58.437381, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058877', 'TRANSPORTE', NULL, 'Parque Centenario', -34.60875, -58.43401, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058878', 'TRANSPORTE', NULL, 'Parque Centenario', -34.608834, -58.434016, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3370076117', 'TRANSPORTE', NULL, 'Neuquén - Aráoz Alfaro', -34.615636, -58.437623, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3370076118', 'TRANSPORTE', NULL, 'Neuquén - Aráoz Alfaro', -34.615637, -58.437696, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3370076129', 'TRANSPORTE', NULL, 'Sanatorio Méndez', -34.613874, -58.437914, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3370076130', 'TRANSPORTE', NULL, 'Sanatorio Méndez', -34.613892, -58.437966, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645766', 'TRANSPORTE', NULL, 'Acoyte y Vallese', -34.611136, -58.439155, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645809', 'TRANSPORTE', NULL, 'Acoyte y Avellaneda', -34.614321, -58.437669, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645810', 'TRANSPORTE', NULL, 'Av.Avellaneda', -34.614322, -58.437783, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645811', 'TRANSPORTE', NULL, 'Acoyte y Bogotá', -34.614745, -58.437633, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645812', 'TRANSPORTE', NULL, 'Acoyte y Bogotá', -34.614754, -58.437728, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645813', 'TRANSPORTE', NULL, 'Acoyte y Bogotá', -34.614909, -58.437595, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645814', 'TRANSPORTE', NULL, 'Acoyte y Bogotá', -34.615096, -58.437601, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645815', 'TRANSPORTE', NULL, 'Acoyte y Bogotá', -34.615099, -58.437687, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3931528550', 'TRANSPORTE', NULL, 'Hospital Durand', -34.608788, -58.438003, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3931528551', 'TRANSPORTE', NULL, 'Hospital Durand', -34.608789, -58.438149, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3931528552', 'TRANSPORTE', NULL, 'Hospital Durand', -34.608867, -58.438011, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3931528553', 'TRANSPORTE', NULL, 'Hospital Durand', -34.608867, -58.438138, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3931528556', 'TRANSPORTE', NULL, 'Parque Centenario', -34.608297, -58.433546, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3931530657', 'TRANSPORTE', NULL, 'Parque Centenario', -34.608275, -58.433382, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3931530658', 'TRANSPORTE', NULL, 'Parque Centenario', -34.608361, -58.433498, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4690601194', 'COMPLEMENTARIO', NULL, 'Farmática', -34.608657, -58.43839, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4699635370', 'COMPETENCIA', 'ALMACEN', 'Maxikiosco', -34.614736, -58.437868, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4699635370', 'COMPETENCIA', 'KIOSCO', 'Maxikiosco', -34.614736, -58.437868, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4699635377', 'COMPETENCIA', 'PAPELERIA', 'Aleca', -34.614874, -58.437857, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4699635382', 'COMPETENCIA', 'ALMACEN', 'Caballito', -34.615003, -58.437832, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4699635386', 'COMPETENCIA', 'ALMACEN', 'Diarco', -34.615085, -58.437818, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4699635386', 'COMPETENCIA', 'KIOSCO', 'Diarco', -34.615085, -58.437818, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4699635595', 'COMPLEMENTARIO', NULL, 'Dia', -34.615365, -58.437748, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4699635595', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.615365, -58.437748, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4699635606', 'COMPETENCIA', 'PAPELERIA', 'Acoyte', -34.615819, -58.437719, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4700029905', 'OFICINA', NULL, 'Diseño de Indumentaria', -34.61025, -58.439324, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4700029920', 'COMPETENCIA', 'ALMACEN', 'New Ola!', -34.611552, -58.438806, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4998983073', 'TRANSPORTE', NULL, 'Aranguren e Hidalgo', -34.613427, -58.439736, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4998983074', 'TRANSPORTE', NULL, 'Aranguren e Hidalgo', -34.613392, -58.439766, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5035956842', 'TRANSPORTE', NULL, 'Parque Centenario', -34.608295, -58.43729, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5035956843', 'TRANSPORTE', NULL, 'Parque Centenario', -34.608232, -58.43725, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5035956850', 'TRANSPORTE', NULL, 'Avenida Patricias Argentinas', -34.607945, -58.432828, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5035956851', 'TRANSPORTE', NULL, 'Avenida Patricias Argentinas', -34.607908, -58.432841, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117229816', 'ESCUELA', NULL, 'Centro de Formación Profesional 16 (sede) - SGBATOS', -34.612987, -58.433153, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117229817', 'ESCUELA', NULL, 'Instituto Privado del Sindicato GBA de Obras Sanitarias', -34.612972, -58.433154, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117379918', 'ESCUELA', NULL, 'Jardín La Casa del Bosque', -34.614387, -58.437945, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117390368', 'ESCUELA', NULL, 'Complejo Educativo Nuevo Sol', -34.615808, -58.436858, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117406344', 'ESCUELA', NULL, 'Escuela de la Paz', -34.613842, -58.431519, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117406383', 'ESCUELA', NULL, 'Instituto Privado Tierra Nueva', -34.609466, -58.432724, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117413155', 'ESCUELA', NULL, 'Instituto Tobar Garcia', -34.616236, -58.435314, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117420446', 'ESCUELA', NULL, 'Fundación Salottiana (Summa)', -34.61457, -58.431496, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117429128', 'ESCUELA', NULL, 'Escuela de Educación Media 01/07', -34.614195, -58.430738, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117441232', 'ESCUELA', NULL, 'Escuela Infantil Caminito de Colores', -34.610525, -58.436437, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117441234', 'ESCUELA', NULL, 'Escuela de Alta Costura, Arte y Diseño de Modas', -34.610526, -58.439198, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117448159', 'ESCUELA', NULL, 'Escuela Infantil Primeras Huellas', -34.609062, -58.433037, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117449279', 'ESCUELA', NULL, 'Instituto de Formación Técnica Superior 29', -34.614178, -58.430715, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117499421', 'ESCUELA', NULL, 'Centro Educativo Complementario de Idiomas Extranjeros 22', -34.612127, -58.44045, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117499422', 'ESCUELA', NULL, 'Escuela Primaria Común 20 Tte. Benjamín Matienzo', -34.612144, -58.440445, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117499423', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado B (EPCjs 20/07°) La Veleta de Caballito [Sede]', -34.612162, -58.440445, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117592454', 'ESCUELA', NULL, 'Centro Educativo de Nivel Secundario 57', -34.61105, -58.437997, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117592464', 'ESCUELA', NULL, 'Jardín de Infantes Integral 01/07° Andres Ferreyra', -34.608582, -58.432858, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117592465', 'ESCUELA', NULL, 'Jardín Maternal 05/07° Jacarandá', -34.610634, -58.438166, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117592472', 'ESCUELA', NULL, 'Escuela Primaria Común 15 Gral. José Gervasio de Artigas', -34.612846, -58.436225, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117592473', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado B (EPCjc 15/07°) La Veleta de Caballito', -34.612829, -58.436223, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451211', 'TRANSPORTE', NULL, 'Parque Centenario', -34.608224, -58.433444, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451212', 'TRANSPORTE', NULL, 'Instituto Pasteur', -34.608587, -58.434993, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451213', 'TRANSPORTE', NULL, 'Instituto Pasteur', -34.608628, -58.434984, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451214', 'TRANSPORTE', NULL, 'Instituto Pasteur', -34.608629, -58.435148, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451215', 'TRANSPORTE', NULL, 'Instituto Pasteur', -34.60859, -58.43516, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451216', 'TRANSPORTE', NULL, 'Campichuelo y Díaz Vélez', -34.609055, -58.43604, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451217', 'TRANSPORTE', NULL, 'Campichuelo y Díaz Vélez', -34.609039, -58.43599, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451218', 'TRANSPORTE', NULL, 'Campichuelo y Díaz Vélez', -34.609149, -58.436013, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451219', 'TRANSPORTE', NULL, 'Campichuelo y Díaz Vélez', -34.609136, -58.435962, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397454921', 'TRANSPORTE', NULL, 'Campichuelo y Avellaneda', -34.613034, -58.434572, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397454922', 'TRANSPORTE', NULL, 'Campichuelo y Avellaneda', -34.61301, -58.43452, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397454923', 'TRANSPORTE', NULL, 'Campichuelo y Rivadavia', -34.616101, -58.433123, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397454924', 'TRANSPORTE', NULL, 'Campichuelo y Rivadavia', -34.616081, -58.433073, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6488805562', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.608237, -58.437617, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6488805562', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.608237, -58.437617, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6566687246', 'TRANSPORTE', NULL, 'Acoyte y Bogotá', -34.614921, -58.437708, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6765175688', 'TRANSPORTE', NULL, 'Vallese y Acoyte', -34.610662, -58.438961, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6765175689', 'TRANSPORTE', NULL, 'Vallese y Acoyte', -34.610619, -58.438991, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7177927473', 'TRANSPORTE', NULL, 'Campichuelo y Avellaneda', -34.612855, -58.4346, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7177927474', 'TRANSPORTE', NULL, 'Campichuelo y Avellaneda', -34.612868, -58.434643, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7177927479', 'TRANSPORTE', NULL, 'Campichuelo y Rivadavia', -34.615958, -58.433191, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7177927480', 'TRANSPORTE', NULL, 'Campichuelo y Rivadavia', -34.615932, -58.433144, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8454166356', 'COMPETENCIA', 'ALMACEN', 'Caprice', -34.612876, -58.434533, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733855011', 'TRANSPORTE', NULL, 'Aranguren e Hidalgo', -34.613487, -58.439869, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733855012', 'TRANSPORTE', NULL, 'Aranguren e Hidalgo', -34.613449, -58.43989, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10038348966', 'OFICINA', NULL, 'Círculo Argentino de Odontología', -34.614878, -58.430985, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10874971207', 'COMPLEMENTARIO', NULL, 'La Santé Farmacia y Perfumería', -34.613853, -58.43758, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12308807801', 'COMPETENCIA', 'KIOSCO', 'Open 25hs', -34.608905, -58.437232, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12385163801', 'COMPETENCIA', 'KIOSCO', 'Maxikiosko', -34.613285, -58.434383, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12869877721', 'OFICINA', NULL, 'Torrici', -34.612478, -58.434203, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12869877723', 'COMPETENCIA', 'KIOSCO', 'The Best', -34.614487, -58.438676, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12869877724', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.614412, -58.438433, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12869877728', 'COMPETENCIA', 'ALMACEN', 'Dulce Ahora', -34.613082, -58.435622, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12884645953', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.610383, -58.439258, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12884645954', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.609654, -58.439633, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12884645954', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.609654, -58.439633, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13606891240', 'COMPETENCIA', 'ALMACEN', 'Mi Flor En Caballito', -34.611716, -58.431892, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13606898817', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.614332, -58.43881, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:14272457301', 'COMPLEMENTARIO', NULL, 'Carrefour Express', -34.609768, -58.438707, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:14272457301', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.609768, -58.438707, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:9939231', 'PARQUE', NULL, 'Parque Centenario', -34.606546, -58.435561, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:23634964', 'SALUD', NULL, 'Hospital General de Agudos Doctor Carlos G. Durand', -34.61003, -58.437764, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:148523278', 'SALUD', NULL, 'Sanatorio Julio Méndez', -34.614039, -58.438907, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:209144729', 'PARQUE', NULL, 'Plazoleta Federico Froebel', -34.608431, -58.433497, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:230967842', 'SALUD', NULL, 'Instituto de Zoonosis Luis Pasteur', -34.608166, -58.435034, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:283511296', 'ESCUELA', NULL, 'Escuela Primaria Común 03 Primera Junta', -34.616313, -58.433651, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:283511297', 'ESCUELA', NULL, 'Jardín de Infantes Común 02/07° Prof. Marina Margarita Ravioli', -34.615582, -58.434155, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:445565827', 'PARQUE', NULL, 'Cantero Central Omar Torrijos', -34.608328, -58.433917, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:445603828', 'PARQUE', NULL, 'Juegos de agua', -34.608101, -58.436161, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:447140696', 'PARQUE', NULL, 'Plazoleta Héctor Greslebin', -34.608612, -58.437033, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475655763', 'SALUD', NULL, 'Instituto de Medicina y Cirugia Cardiovascular', -34.608968, -58.434474, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475657694', 'COMPETENCIA', 'PAPELERIA', 'Magic', -34.608912, -58.431351, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476184968', 'COMPLEMENTARIO', NULL, 'Jumbo', -34.610484, -58.439939, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476184968', 'COMPETENCIA', 'ALMACEN', 'Jumbo', -34.610484, -58.439939, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476365266', 'SALUD', NULL, 'Deragopyan', -34.614816, -58.437984, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476365270', 'COMPETENCIA', 'ALMACEN', 'Kalimnos', -34.615158, -58.437912, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476365274', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.615736, -58.437776, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476365274', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.615736, -58.437776, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476384348', 'COMPLEMENTARIO', NULL, 'Hidalgo', -34.615135, -58.439061, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476384348', 'COMPETENCIA', 'ALMACEN', 'Hidalgo', -34.615135, -58.439061, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476409536', 'COMPETENCIA', 'ALMACEN', 'San Telmo', -34.614777, -58.437477, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:908967596', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.609115, -58.435968, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1059429135', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.613411, -58.439807, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1483982042', 'OFICINA', NULL, 'Sindicato Gran Buenos Aires de Trabajadores de Obras Sanitarias', -34.612438, -58.433421, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1543242007', 'ESCUELA', NULL, 'Escuela Superior de Enfermería Dra. Cecilia Grierson Sede', -34.610928, -58.437979, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7424498', 'TRANSPORTE', NULL, 'Aranguren e Hidalgo', -34.613423, -58.439807, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7474070', 'TRANSPORTE', NULL, 'Parque Centenario', -34.608263, -58.43727, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7474071', 'TRANSPORTE', NULL, 'Avenida Patricias Argentinas', -34.607927, -58.432834, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8000308', 'TRANSPORTE', NULL, 'Instituto Pasteur', -34.608609, -58.435154, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8000309', 'TRANSPORTE', NULL, 'Campichuelo y Díaz Vélez', -34.609094, -58.436001, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8000310', 'TRANSPORTE', NULL, 'Campichuelo y Avellaneda', -34.613022, -58.434546, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8000311', 'TRANSPORTE', NULL, 'Campichuelo y Rivadavia', -34.616091, -58.433098, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9717647', 'TRANSPORTE', NULL, 'Acoyte y Bogotá', -34.614915, -58.437652, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9991294', 'TRANSPORTE', NULL, 'Vallese y Acoyte', -34.61064, -58.438976, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 26, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 4, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 75, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 5, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 5, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 10, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 18, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 3, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 7, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:256241752', 'TRANSPORTE', NULL, 'Primera Junta', -34.620623, -58.441373, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:540460770', 'COMPLEMENTARIO', NULL, 'Farmacity', -34.625996, -58.437859, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1393177732', 'ESCUELA', NULL, 'Instituto Social Militar Doctor Dámaso Centeno', -34.621716, -58.443477, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1393189675', 'ESCUELA', NULL, 'La Ola', -34.621546, -58.443034, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1393189933', 'ESCUELA', NULL, 'Escuela de Estética', -34.61998, -58.439707, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1402327977', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.62273, -58.441463, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1402327980', 'COMPETENCIA', 'ALMACEN', 'La Milagrosa', -34.622473, -58.441006, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1402327981', 'COMPETENCIA', 'ALMACEN', 'Tutto Panne', -34.622304, -58.443878, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1458149227', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.620688, -58.441242, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1991074477', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.620353, -58.441287, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1991074479', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.620548, -58.441066, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1991074481', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.620609, -58.441341, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2291016433', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.620208, -58.441679, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2291016433', 'COMPETENCIA', 'KIOSCO', 'Carrefour Express', -34.620208, -58.441679, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343099046', 'TRANSPORTE', NULL, 'Rivadavia y Colpayo', -34.619811, -58.440097, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343099048', 'TRANSPORTE', NULL, 'Primera Junta', -34.620019, -58.440574, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343099049', 'TRANSPORTE', NULL, 'Primera Junta', -34.620253, -58.441078, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343099050', 'TRANSPORTE', NULL, 'Primera Junta', -34.620965, -58.442717, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343099240', 'TRANSPORTE', NULL, 'Primera Junta', -34.620086, -58.440709, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343100066', 'TRANSPORTE', NULL, 'Primera Junta', -34.620527, -58.441721, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343100067', 'TRANSPORTE', NULL, 'Primera Junta', -34.620622, -58.441943, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343100068', 'COMPLEMENTARIO', NULL, 'Coto', -34.621111, -58.445802, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343100068', 'COMPETENCIA', 'ALMACEN', 'Coto', -34.621111, -58.445802, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343100398', 'TRANSPORTE', NULL, 'Primera Junta', -34.620681, -58.442081, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343147506', 'TRANSPORTE', NULL, 'Primera Junta', -34.620103, -58.44053, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343147507', 'TRANSPORTE', NULL, 'Primera Junta', -34.620163, -58.440664, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343147508', 'TRANSPORTE', NULL, 'Primera Junta', -34.620332, -58.441042, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343147510', 'TRANSPORTE', NULL, 'Primera Junta', -34.620699, -58.441896, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343147511', 'TRANSPORTE', NULL, 'Primera Junta', -34.62076, -58.44204, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343147512', 'TRANSPORTE', NULL, 'Primera Junta', -34.621027, -58.442675, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343147514', 'TRANSPORTE', NULL, 'Rivadavia y Colpayo', -34.61989, -58.440051, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3449178933', 'TRANSPORTE', NULL, 'Primera Junta', -34.620551, -58.441418, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3449220580', 'TRANSPORTE', NULL, 'Primera Junta', -34.62059, -58.441392, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3456010437', 'TRANSPORTE', NULL, 'Caballito', -34.61924, -58.443659, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3835259992', 'TRANSPORTE', NULL, 'Primera Junta', -34.620041, -58.441697, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3835262362', 'TRANSPORTE', NULL, 'Primera Junta', -34.620299, -58.44159, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929826469', 'TRANSPORTE', NULL, 'Primera Junta', -34.620887, -58.441389, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929826470', 'TRANSPORTE', NULL, 'Primera Junta', -34.620944, -58.44163, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929826471', 'TRANSPORTE', NULL, 'Primera Junta', -34.620948, -58.441377, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929826472', 'TRANSPORTE', NULL, 'Primera Junta', -34.620991, -58.441826, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929826473', 'TRANSPORTE', NULL, 'Primera Junta', -34.62101, -58.441616, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929826474', 'TRANSPORTE', NULL, 'Primera Junta', -34.621053, -58.442072, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929826475', 'TRANSPORTE', NULL, 'Primera Junta', -34.621061, -58.441811, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929826476', 'TRANSPORTE', NULL, 'Primera Junta', -34.621119, -58.442056, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929826477', 'TRANSPORTE', NULL, 'Primera Junta', -34.621169, -58.442245, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929826478', 'TRANSPORTE', NULL, 'Primera Junta', -34.621229, -58.442467, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929842028', 'TRANSPORTE', NULL, 'Primera Junta', -34.621312, -58.44101, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929842029', 'TRANSPORTE', NULL, 'Primera Junta', -34.621326, -58.441057, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929842030', 'TRANSPORTE', NULL, 'Guayaquil y Calasanz', -34.621612, -58.438452, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929842031', 'TRANSPORTE', NULL, 'Guayaquil y Calasanz', -34.621637, -58.438638, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929842032', 'TRANSPORTE', NULL, 'Guayaquil y Calasanz', -34.621672, -58.438442, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3929842033', 'TRANSPORTE', NULL, 'Guayaquil y Calasanz', -34.621701, -58.438633, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4395753103', 'TRANSPORTE', NULL, 'Caballito', -34.619225, -58.443667, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4395753104', 'TRANSPORTE', NULL, 'Caballito', -34.619258, -58.443646, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483326981', 'TRANSPORTE', NULL, 'Goyena y Centenera', -34.626325, -58.440173, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483326982', 'TRANSPORTE', NULL, 'Goyena y Centenera', -34.626357, -58.440177, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483326983', 'TRANSPORTE', NULL, 'Pedro Goyena y Emilio Mitre', -34.626626, -58.442769, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483326984', 'TRANSPORTE', NULL, 'Emilio Mitre', -34.626712, -58.442748, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483327089', 'TRANSPORTE', NULL, 'Goyena y Calasanz', -34.626008, -58.437338, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483327090', 'TRANSPORTE', NULL, 'Goyena y Calasanz', -34.626064, -58.437356, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689796872', 'COMPLEMENTARIO', NULL, 'Farmacity', -34.620175, -58.441061, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689796880', 'COMPETENCIA', 'ALMACEN', 'Roma', -34.620291, -58.441257, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689800906', 'COMPLEMENTARIO', NULL, 'Columbia', -34.620418, -58.441549, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689800907', 'COMPLEMENTARIO', NULL, 'TKL Farmacia', -34.620511, -58.441809, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689804036', 'COMPETENCIA', 'ALMACEN', 'Exclusivos', -34.621528, -58.444108, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689804036', 'COMPETENCIA', 'KIOSCO', 'Exclusivos', -34.621528, -58.444108, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4700343827', 'COMPLEMENTARIO', NULL, 'BBVA', -34.620069, -58.440085, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4700343835', 'COMPLEMENTARIO', NULL, 'Farmacia Dr. Ahorro', -34.620471, -58.441033, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4702872894', 'COMPETENCIA', 'PAPELERIA', 'Código Arte', -34.623079, -58.443395, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741772', 'TRANSPORTE', NULL, 'Rivadavia y Espinosa', -34.622577, -58.446233, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741773', 'TRANSPORTE', NULL, 'Rivadavia y Espinosa', -34.622488, -58.446289, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741774', 'TRANSPORTE', NULL, 'Rivadavia y Espinosa', -34.622334, -58.445629, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741775', 'TRANSPORTE', NULL, 'Rivadavia y Espinosa', -34.622227, -58.445683, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741776', 'TRANSPORTE', NULL, 'Rivadavia y Espinosa', -34.62219, -58.445592, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741777', 'TRANSPORTE', NULL, 'Rivadavia y Espinosa', -34.622295, -58.445534, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741778', 'TRANSPORTE', NULL, 'Rivadavia y Gainza', -34.622259, -58.445445, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741779', 'TRANSPORTE', NULL, 'Rivadavia y Gainza', -34.622156, -58.445509, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741780', 'TRANSPORTE', NULL, 'Rivadavia y Gainza', -34.622218, -58.445342, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741781', 'TRANSPORTE', NULL, 'Rivadavia y Gainza', -34.622111, -58.445396, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741782', 'TRANSPORTE', NULL, 'Rivadavia y García Lorca', -34.621896, -58.444594, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5080741783', 'TRANSPORTE', NULL, 'Rivadavia y García Lorca', -34.621803, -58.444638, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117390651', 'ESCUELA', NULL, 'Escuela Infantil Mi Mundo Feliz', -34.623313, -58.43795, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117390664', 'ESCUELA', NULL, 'Escuela Infantil Hermanos Felices', -34.619497, -58.442188, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117390672', 'ESCUELA', NULL, 'El Jardín de los Abrazos', -34.625195, -58.444762, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117405296', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado E (EPCjs 14/08°) El Tranvia de Caballito', -34.626711, -58.441249, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117406370', 'ESCUELA', NULL, 'Instituto Gral. Justo José de Urquiza', -34.623576, -58.445254, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117420490', 'ESCUELA', NULL, 'Aquì estoy', -34.625246, -58.436356, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117441273', 'ESCUELA', NULL, 'Desde la Cuna', -34.625269, -58.439415, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117448156', 'ESCUELA', NULL, 'El Jardín de los Abrazos', -34.624008, -58.444662, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117448195', 'ESCUELA', NULL, 'Escuela Infantil Hermanos Felices', -34.619816, -58.442876, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117448220', 'ESCUELA', NULL, 'Escuela Infantil Jarpadimpi', -34.626514, -58.442153, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117565896', 'ESCUELA', NULL, 'Escuela para Niños, Niñas y Jóvenes con Discapacidad Visual 35 José Manuel Estrada', -34.622754, -58.443452, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117576271', 'ESCUELA', NULL, 'Escuela Primaria p/Adultos 14 Joaquín Víctor González', -34.626692, -58.441238, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117576272', 'ESCUELA', NULL, 'Escuela Primaria Común 14 Joaquín Víctor González', -34.626727, -58.441249, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117592462', 'ESCUELA', NULL, 'Escuela de Comercio 29 Marina Mercante Argentina', -34.618993, -58.438554, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117592463', 'ESCUELA', NULL, 'Escuela de Comercio 23 Doctor Luis Agote', -34.619028, -58.438581, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5544306519', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.623307, -58.438088, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5544306519', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.623307, -58.438088, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5544306520', 'TRANSPORTE', NULL, 'Alberdi y Calasanz', -34.623131, -58.437555, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5544307321', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.622755, -58.440664, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5553322877', 'TRANSPORTE', NULL, 'Alberdi y Calasanz', -34.62321, -58.437548, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5643225575', 'TRANSPORTE', NULL, 'Primera Junta', -34.621176, -58.442505, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5709810127', 'COMPETENCIA', 'ALMACEN', 'La boutique natural', -34.622788, -58.443929, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5709810128', 'COMPLEMENTARIO', NULL, 'SJ', -34.622455, -58.44416, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5709810129', 'COMPLEMENTARIO', NULL, 'Eneldo', -34.621563, -58.444189, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5709810129', 'COMPETENCIA', 'ALMACEN', 'Eneldo', -34.621563, -58.444189, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5709810130', 'COMPETENCIA', 'ALMACEN', 'Eneldo', -34.621595, -58.444275, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5709810130', 'COMPETENCIA', 'KIOSCO', 'Eneldo', -34.621595, -58.444275, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5838472679', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.620537, -58.441048, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5932586405', 'OFICINA', NULL, 'La Salvia', -34.623875, -58.444828, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5987261549', 'OFICINA', NULL, 'Fundacion Mision Marianista', -34.622249, -58.444871, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133681828', 'TRANSPORTE', NULL, 'Primera Junta', -34.620765, -58.440662, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133681829', 'TRANSPORTE', NULL, 'Primera Junta', -34.620704, -58.440679, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133681830', 'TRANSPORTE', NULL, 'Rosario y Riglos', -34.619876, -58.437519, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133681831', 'TRANSPORTE', NULL, 'Rosario y Riglos', -34.619819, -58.437534, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133713848', 'TRANSPORTE', NULL, 'Calasanz y Rosario', -34.620639, -58.438497, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133713849', 'TRANSPORTE', NULL, 'Calasanz y Rosario', -34.620647, -58.438561, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133713850', 'TRANSPORTE', NULL, 'Rivadavia y Gainza', -34.621949, -58.444983, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133713851', 'TRANSPORTE', NULL, 'Rivadavia y Gainza', -34.622048, -58.444921, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6175796648', 'TRANSPORTE', NULL, 'Guayaquil y Bertres', -34.621858, -58.439903, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6175796649', 'TRANSPORTE', NULL, 'Guayaquil y Bertres', -34.621798, -58.439907, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6182655087', 'TRANSPORTE', NULL, 'Primera Junta', -34.620608, -58.441677, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6182655088', 'TRANSPORTE', NULL, 'Calasanz y Rosario', -34.620803, -58.43845, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6193837651', 'TRANSPORTE', NULL, 'Primera Junta', -34.620687, -58.440401, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6193837652', 'TRANSPORTE', NULL, 'Primera Junta', -34.620634, -58.440417, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6193837653', 'TRANSPORTE', NULL, 'Rivadavia y Gainza', -34.621959, -58.444701, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6193837654', 'TRANSPORTE', NULL, 'Rivadavia y Gainza', -34.622078, -58.444651, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6432893903', 'COMPLEMENTARIO', NULL, 'Primera Junta', -34.618726, -58.442059, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6534704500', 'TRANSPORTE', NULL, 'Primera Junta', -34.620599, -58.440067, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6534704501', 'TRANSPORTE', NULL, 'Primera Junta', -34.620544, -58.440082, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6534704506', 'TRANSPORTE', NULL, 'Rivadavia y Víctor Martínez', -34.622465, -58.445956, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6534704507', 'TRANSPORTE', NULL, 'Rivadavia y Víctor Martínez', -34.62258, -58.445896, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6689761889', 'TRANSPORTE', NULL, 'Calasanz y Rosario', -34.620813, -58.438504, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6689761890', 'TRANSPORTE', NULL, 'Calasanz y Rosario', -34.620921, -58.438415, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6693618863', 'TRANSPORTE', NULL, 'Rivadavia y Víctor Martínez', -34.622728, -58.446263, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6693618864', 'TRANSPORTE', NULL, 'Rivadavia y Víctor Martínez', -34.622612, -58.44632, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6728511380', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.62175, -58.438521, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6728520671', 'SALUD', NULL, 'Centro Médico Accord', -34.620767, -58.4404, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6728534337', 'COMPETENCIA', 'ALMACEN', 'Granja y Fiambrería', -34.621744, -58.438478, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6728534337', 'COMPETENCIA', 'KIOSCO', 'Granja y Fiambrería', -34.621744, -58.438478, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6728548222', 'COMPETENCIA', 'ALMACEN', 'Del Rosario', -34.620694, -58.440135, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6728557304', 'COMPLEMENTARIO', NULL, 'Banco Santander', -34.620604, -58.439836, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6728560730', 'COMPLEMENTARIO', NULL, 'Family', -34.620742, -58.440292, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6728560730', 'COMPETENCIA', 'ALMACEN', 'Family', -34.620742, -58.440292, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6728655139', 'TRANSPORTE', NULL, 'Calasanz y Rosario', -34.620936, -58.438462, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6728655141', 'TRANSPORTE', NULL, 'Primera Junta', -34.620641, -58.440236, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6728655142', 'TRANSPORTE', NULL, 'Primera Junta', -34.620591, -58.440256, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6732503233', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.621323, -58.436659, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6745072313', 'TRANSPORTE', NULL, 'Primera Junta', -34.621456, -58.440994, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6745072314', 'TRANSPORTE', NULL, 'Primera Junta', -34.621462, -58.441043, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6753869597', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.620247, -58.441662, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6753869598', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.620313, -58.441624, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6753869599', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.620494, -58.44174, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7215532533', 'TRANSPORTE', NULL, 'Pedro Goyena y Calasanz', -34.626134, -58.438106, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7215532534', 'TRANSPORTE', NULL, 'Pedro Goyena y Calasanz', -34.626198, -58.438095, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7215532535', 'TRANSPORTE', NULL, 'Centenera y Valle', -34.62475, -58.440661, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7215532536', 'TRANSPORTE', NULL, 'Centenera y Valle', -34.624752, -58.44072, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8826036865', 'SALUD', NULL, 'Centro de diagnóstico Ecográfico Doctor Carlos Canetti', -34.619545, -58.438441, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9021551778', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.621701, -58.441101, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9021551778', 'COMPETENCIA', 'KIOSCO', 'Carrefour Express', -34.621701, -58.441101, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9025534901', 'TRANSPORTE', NULL, 'Avenida Alberdi', -34.623023, -58.436325, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9025534902', 'TRANSPORTE', NULL, 'Avenida Alberdi', -34.623033, -58.436371, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9025534903', 'TRANSPORTE', NULL, 'Alberdi y Calasanz', -34.623215, -58.437489, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9025534904', 'TRANSPORTE', NULL, 'Alberdi y Calasanz', -34.623131, -58.437495, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9025534909', 'TRANSPORTE', NULL, 'Alberdi y Del Barco Centenera', -34.622892, -58.440641, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9025534910', 'TRANSPORTE', NULL, 'Alberdi y Del Barco Centenera', -34.622807, -58.440637, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9268105914', 'COMPETENCIA', 'ALMACEN', 'Candy House', -34.621245, -58.443474, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9268105914', 'COMPETENCIA', 'KIOSCO', 'Candy House', -34.621245, -58.443474, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273111999', 'TRANSPORTE', NULL, 'Avenida Goyena', -34.626941, -58.443234, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273112000', 'TRANSPORTE', NULL, 'Avenida Goyena', -34.626939, -58.443177, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273112001', 'TRANSPORTE', NULL, 'Mitre y Valle', -34.625608, -58.443299, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273112002', 'TRANSPORTE', NULL, 'Mitre y Valle', -34.625605, -58.443246, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273112003', 'TRANSPORTE', NULL, 'Mitre y Valle', -34.625412, -58.443309, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273112004', 'TRANSPORTE', NULL, 'Mitre y Valle', -34.625409, -58.443258, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273112006', 'TRANSPORTE', NULL, 'Avenida Alberdi', -34.623206, -58.443533, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273112007', 'TRANSPORTE', NULL, 'Avenida Alberdi', -34.623204, -58.443465, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273112009', 'TRANSPORTE', NULL, 'Mitre y Rivadavia', -34.622359, -58.444071, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273112010', 'TRANSPORTE', NULL, 'Mitre y Rivadavia', -34.622323, -58.444013, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273112011', 'TRANSPORTE', NULL, 'Mitre y Rivadavia', -34.622219, -58.444177, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9273112012', 'TRANSPORTE', NULL, 'Mitre y Rivadavia', -34.622185, -58.444109, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10609415792', 'TRANSPORTE', NULL, 'Centenera y Alberdi', -34.622685, -58.440876, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10609415793', 'TRANSPORTE', NULL, 'Centenera y Alberdi', -34.622688, -58.440924, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11110960053', 'TRANSPORTE', NULL, 'Primera Junta', -34.620296, -58.441536, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11110960054', 'TRANSPORTE', NULL, 'Primera Junta', -34.620028, -58.441652, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11111026665', 'TRANSPORTE', NULL, 'Primera Junta', -34.6211, -58.442251, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11417419429', 'COMPLEMENTARIO', NULL, 'Banco Comafi', -34.619548, -58.438823, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11417419432', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.621271, -58.436047, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11417419432', 'COMPETENCIA', 'KIOSCO', 'Carrefour Express', -34.621271, -58.436047, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11417419435', 'COMPETENCIA', 'ALMACEN', 'Próspero Velazco', -34.626839, -58.442572, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11432304925', 'COMPLEMENTARIO', NULL, 'Openfarma Contemporánea', -34.621142, -58.441926, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11432357563', 'COMPLEMENTARIO', NULL, 'Dia', -34.621759, -58.440488, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11432357563', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.621759, -58.440488, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11432357568', 'COMPLEMENTARIO', NULL, 'ICBC', -34.619767, -58.439379, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11432608559', 'COMPETENCIA', 'ALMACEN', 'Las Medialunas Del Abuelo', -34.620967, -58.438336, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12639847873', 'TRANSPORTE', NULL, 'Primera Junta', -34.620292, -58.438923, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12639847874', 'TRANSPORTE', NULL, 'Primera Junta', -34.620238, -58.438942, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13148755666', 'TRANSPORTE', NULL, 'Rivadavia y Colpayo', -34.619679, -58.439571, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13148755667', 'TRANSPORTE', NULL, 'Rivadavia y Colpayo', -34.619604, -58.439616, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13241250702', 'ESCUELA', NULL, 'ISAS instituto superior andes del sur', -34.622112, -58.444577, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:26686023', 'ESCUELA', NULL, 'Instituto Cultural Marianista', -34.622448, -58.444811, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:26686024', 'ESCUELA', NULL, 'Colegio San Cirano', -34.622659, -58.445373, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:126514693', 'PARQUE', NULL, 'Plazoleta Crisólogo Larralde', -34.619488, -58.441569, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:139532061', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.619333, -58.443702, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:139532065', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.619221, -58.443768, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:188480313', 'PARQUE', NULL, 'Plaza Primera Junta', -34.620876, -58.441987, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:297726685', 'PARQUE', NULL, 'Lugar sin nombre', -34.619771, -58.441493, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:319501999', 'PARQUE', NULL, 'Plaza del Caballito', -34.619434, -58.443541, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:446708449', 'PARQUE', NULL, 'Lugar sin nombre', -34.619302, -58.443158, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:446739122', 'COMPETENCIA', 'PAPELERIA', 'Librerías', -34.620853, -58.441818, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475190493', 'COMPLEMENTARIO', NULL, 'Banco Nación', -34.619138, -58.438893, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475190494', 'COMPLEMENTARIO', NULL, 'Galicia', -34.619257, -58.439273, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476446212', 'COMPLEMENTARIO', NULL, 'Galicia', -34.620181, -58.440171, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:785871279', 'SALUD', NULL, 'Centro Médico Caballito Hospital Alemán', -34.624034, -58.44196, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1505156856', 'ESCUELA', NULL, 'Instituto Monseñor Sabelli', -34.623013, -58.445088, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:1779786', 'TRANSPORTE', NULL, 'Estación Primera Junta', -34.620521, -58.441242, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:1877839', 'TRANSPORTE', NULL, 'Estación Caballito', -34.619294, -58.443702, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6691915', 'TRANSPORTE', NULL, 'Del Barco Centenera', -34.626341, -58.440175, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6691916', 'TRANSPORTE', NULL, 'Emilio Mitre', -34.626669, -58.442759, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6691918', 'TRANSPORTE', NULL, 'Goyena y Calasanz', -34.626036, -58.437347, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7543681', 'TRANSPORTE', NULL, 'Espinosa - Martínez', -34.622164, -58.445369, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7543682', 'TRANSPORTE', NULL, 'Rivadavia y García Lorca', -34.621849, -58.444616, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8210242', 'TRANSPORTE', NULL, 'Alberdi y Calasanz', -34.623171, -58.437551, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9109895', 'TRANSPORTE', NULL, 'Rosario y Riglos', -34.619847, -58.437527, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9109896', 'TRANSPORTE', NULL, 'Calasanz y Rosario', -34.620643, -58.438529, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9671674', 'TRANSPORTE', NULL, 'Rivadavia y Víctor Martínez', -34.622523, -58.445926, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10716562', 'TRANSPORTE', NULL, 'Goyena y Calasanz', -34.626166, -58.438101, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10716563', 'TRANSPORTE', NULL, 'Centenera y Valle', -34.624751, -58.44069, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13114211', 'TRANSPORTE', NULL, 'Avenida Alberdi', -34.623028, -58.436348, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13114212', 'TRANSPORTE', NULL, 'Alberdi y Del Barco Centenera', -34.62285, -58.440639, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13466489', 'TRANSPORTE', NULL, 'Avenida Goyena', -34.62694, -58.443206, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13466490', 'TRANSPORTE', NULL, 'Mitre y Valle', -34.625411, -58.443284, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13466491', 'TRANSPORTE', NULL, 'Avenida Alberdi', -34.623205, -58.443499, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13466492', 'TRANSPORTE', NULL, 'Mitre y Rivadavia', -34.622202, -58.444143, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:15437970', 'TRANSPORTE', NULL, 'Centenera y Alberdi', -34.622687, -58.4409, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:16194666', 'TRANSPORTE', NULL, 'Primera Junta', -34.620034, -58.441674, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:19610820', 'TRANSPORTE', NULL, 'Rivadavia y Colpayo', -34.619642, -58.439594, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 22, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 2, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 148, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 5, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 3, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 21, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 23, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 2, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 10, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Caballito Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:256247568', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.587196, -58.455157, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1109841843', 'COMPLEMENTARIO', NULL, 'Banco Ciudad', -34.586594, -58.45536, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1109841850', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.585907, -58.454188, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1109841850', 'COMPETENCIA', 'KIOSCO', 'Carrefour Express', -34.585907, -58.454188, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1109841857', 'COMPLEMENTARIO', NULL, 'Faraday', -34.586627, -58.455239, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1111884105', 'COMPLEMENTARIO', NULL, 'Doctor Ahorro', -34.587076, -58.454449, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1137419664', 'TRANSPORTE', NULL, 'Lacroze', -34.586034, -58.454652, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1177972606', 'COMPLEMENTARIO', NULL, 'Provincia', -34.584889, -58.453554, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1570058058', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.586717, -58.455804, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1989838946', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.587153, -58.454913, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2025901952', 'TRANSPORTE', NULL, 'Lacroze', -34.587271, -58.454124, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2025901953', 'TRANSPORTE', NULL, 'Guzmán y Newbery', -34.590003, -58.451391, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2892599806', 'TRANSPORTE', NULL, 'Guzmán y Newbery', -34.590027, -58.451432, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2892599808', 'TRANSPORTE', NULL, 'Lacroze', -34.58731, -58.454139, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254582452', 'TRANSPORTE', NULL, 'Lacroze', -34.586249, -58.454787, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254582454', 'TRANSPORTE', NULL, 'Lacroze', -34.586788, -58.45514, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254582456', 'TRANSPORTE', NULL, 'Lacroze', -34.586887, -58.456144, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254582457', 'TRANSPORTE', NULL, 'Lacroze', -34.586951, -58.456166, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3264446345', 'TRANSPORTE', NULL, 'Lacroze', -34.586495, -58.454693, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3264457987', 'TRANSPORTE', NULL, 'Lacroze', -34.586698, -58.454819, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3264597002', 'TRANSPORTE', NULL, 'Lacroze', -34.586652, -58.454911, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3370076113', 'TRANSPORTE', NULL, 'Lacroze', -34.586131, -58.454709, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3370076114', 'TRANSPORTE', NULL, 'Lacroze', -34.586169, -58.454598, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3370076115', 'TRANSPORTE', NULL, 'Lacroze', -34.586519, -58.454973, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3370076123', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.588399, -58.452285, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3370076124', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.588484, -58.452324, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3370076125', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.588589, -58.451765, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3370076126', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.588675, -58.451812, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3407636969', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.587171, -58.455144, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3415331280', 'TRANSPORTE', NULL, 'Lacroze', -34.586092, -58.454547, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3415331916', 'TRANSPORTE', NULL, 'Lacroze y Fraga', -34.5841, -58.453273, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3415413094', 'TRANSPORTE', NULL, 'Lacroze', -34.586454, -58.454781, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3485531760', 'TRANSPORTE', NULL, 'Cementerio de la Chacarita', -34.586402, -58.457715, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3485531761', 'TRANSPORTE', NULL, 'Cementerio de la Chacarita', -34.586491, -58.457769, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3743606925', 'COMPLEMENTARIO', NULL, 'Farmacity', -34.583426, -58.452991, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3752928129', 'ESCUELA', NULL, 'Escuela Primaria p/Adultos 18 Cabildo de Buenos Aires', -34.583679, -58.453276, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3763312568', 'TRANSPORTE', NULL, 'Lacroze y Fraga', -34.584056, -58.453367, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3814463171', 'COMPLEMENTARIO', NULL, 'Macro', -34.583659, -58.45318, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3814467296', 'ESCUELA', NULL, 'Escuela Técnica 32 Gral. José de San Martín', -34.583727, -58.454721, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3814475846', 'SALUD', NULL, 'O.S.E.C.A.C', -34.58382, -58.452822, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645767', 'TRANSPORTE', NULL, 'Guzmán y Newbery', -34.58983, -58.451564, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645768', 'TRANSPORTE', NULL, 'Guzmán y Newbery', -34.589871, -58.451604, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645769', 'TRANSPORTE', NULL, 'Guzmán y Newbery', -34.590163, -58.451213, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645770', 'TRANSPORTE', NULL, 'Guzmán y Newbery', -34.590204, -58.451247, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645771', 'TRANSPORTE', NULL, 'Lacroze', -34.586317, -58.454691, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645772', 'TRANSPORTE', NULL, 'Lacroze', -34.586361, -58.45461, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645773', 'TRANSPORTE', NULL, 'Lacroze', -34.587044, -58.45473, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645774', 'TRANSPORTE', NULL, 'Lacroze', -34.587084, -58.454748, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645785', 'TRANSPORTE', NULL, 'Lacroze y Charlone', -34.582927, -58.452507, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645786', 'TRANSPORTE', NULL, 'Lacroze y Charlone', -34.582969, -58.452405, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645787', 'TRANSPORTE', NULL, 'Lacroze', -34.587427, -58.453671, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645788', 'TRANSPORTE', NULL, 'Lacroze', -34.587492, -58.45371, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836771620', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.586779, -58.455424, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4245700487', 'TRANSPORTE', NULL, 'Forest y Palpa', -34.583, -58.456041, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4245702298', 'TRANSPORTE', NULL, 'Cementerio La Chacarita', -34.587672, -58.454289, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4245702299', 'TRANSPORTE', NULL, 'Cementerio La Chacarita', -34.587738, -58.454319, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4245702307', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.588162, -58.453187, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4476025869', 'ESCUELA', NULL, 'Maternal Resurrección del Señor', -34.58725, -58.450188, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4516384702', 'TRANSPORTE', NULL, 'Terminal Chacarita', -34.586832, -58.450759, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4516384703', 'TRANSPORTE', NULL, 'Terminal Chacarita', -34.586845, -58.450729, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4516412372', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.58723, -58.449788, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4516412372', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.58723, -58.449788, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4549999680', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.586064, -58.451425, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4549999680', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.586064, -58.451425, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4669440740', 'OFICINA', NULL, 'Abogados Laboral', -34.584904, -58.454391, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4669440774', 'OFICINA', NULL, 'La Nestor Vive', -34.586535, -58.452895, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4669464618', 'COMPETENCIA', 'ALMACEN', 'Lily', -34.587648, -58.450291, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4669464618', 'COMPETENCIA', 'KIOSCO', 'Lily', -34.587648, -58.450291, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4672456183', 'OFICINA', NULL, 'Eleva-Hertz', -34.582918, -58.455721, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4672632743', 'COMPETENCIA', 'ALMACEN', 'Las Nenas', -34.585108, -58.453711, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4672632753', 'OFICINA', NULL, 'Escribanía', -34.585602, -58.453369, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4672632775', 'OFICINA', NULL, 'Nueva Comuna', -34.586529, -58.450202, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4672632778', 'OFICINA', NULL, 'Grupo G & Asociados', -34.586342, -58.450146, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4672632785', 'OFICINA', NULL, 'Escudo Seguros', -34.584667, -58.452455, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4672633395', 'COMPLEMENTARIO', NULL, 'Puma', -34.583493, -58.452639, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4672633395', 'COMPETENCIA', 'ALMACEN', 'Puma', -34.583493, -58.452639, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4679439085', 'COMPETENCIA', 'ALMACEN', 'Shell Select', -34.588535, -58.450415, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4679439085', 'COMPETENCIA', 'KIOSCO', 'Shell Select', -34.588535, -58.450415, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4680970978', 'TRANSPORTE', NULL, 'Lacroze y Fraga', -34.584576, -58.453582, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4680970979', 'TRANSPORTE', NULL, 'Lacroze y Fraga', -34.584613, -58.453488, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4680970980', 'TRANSPORTE', NULL, 'Lacroze y Fraga', -34.584691, -58.453655, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4680970981', 'TRANSPORTE', NULL, 'Lacroze y Fraga', -34.584721, -58.453557, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4680970984', 'TRANSPORTE', NULL, 'Lacroze', -34.587692, -58.452989, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4680999234', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.58853, -58.451927, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4680999235', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.588615, -58.45197, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4680999236', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.588458, -58.452124, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4680999237', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.58854, -58.452168, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4742293526', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.586752, -58.45549, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4742293527', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.586577, -58.455724, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392878', 'TRANSPORTE', NULL, 'Lacroze', -34.586707, -58.456642, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392879', 'TRANSPORTE', NULL, 'Lacroze', -34.586821, -58.456326, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392880', 'TRANSPORTE', NULL, 'Lacroze', -34.586882, -58.456343, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392881', 'TRANSPORTE', NULL, 'Centro de Trasbordo Federico Lacroze', -34.587532, -58.454119, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392882', 'TRANSPORTE', NULL, 'Centro de Trasbordo Federico Lacroze', -34.58746, -58.454315, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392883', 'TRANSPORTE', NULL, 'Lacroze', -34.587482, -58.454254, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392884', 'TRANSPORTE', NULL, 'Lacroze', -34.587437, -58.454378, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392885', 'TRANSPORTE', NULL, 'Lacroze', -34.587406, -58.454461, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392886', 'TRANSPORTE', NULL, 'Lacroze', -34.587389, -58.454508, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392887', 'TRANSPORTE', NULL, 'Lacroze', -34.587308, -58.45473, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392888', 'TRANSPORTE', NULL, 'Lacroze', -34.587381, -58.454755, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392889', 'TRANSPORTE', NULL, 'Lacroze', -34.587477, -58.454499, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392890', 'TRANSPORTE', NULL, 'Centro de Trasbordo Federico Lacroze', -34.587538, -58.454346, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392891', 'TRANSPORTE', NULL, 'Centro de Trasbordo Federico Lacroze', -34.58759, -58.454197, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392892', 'TRANSPORTE', NULL, 'Lacroze', -34.587416, -58.454223, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392893', 'TRANSPORTE', NULL, 'Lacroze', -34.58737, -58.454347, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392894', 'TRANSPORTE', NULL, 'Lacroze', -34.58732, -58.454477, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392895', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.588066, -58.453221, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392896', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.588137, -58.453258, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392897', 'TRANSPORTE', NULL, 'Lacroze', -34.587751, -58.453016, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392898', 'TRANSPORTE', NULL, 'Lacroze', -34.5877, -58.453154, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392899', 'TRANSPORTE', NULL, 'Lacroze', -34.58764, -58.453127, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392900', 'TRANSPORTE', NULL, 'Lacroze', -34.587587, -58.453453, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392901', 'TRANSPORTE', NULL, 'Lacroze', -34.587563, -58.453518, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392902', 'TRANSPORTE', NULL, 'Lacroze', -34.587518, -58.453436, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392903', 'TRANSPORTE', NULL, 'Lacroze', -34.587498, -58.453489, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392904', 'TRANSPORTE', NULL, 'Lacroze', -34.587448, -58.453617, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392905', 'TRANSPORTE', NULL, 'Lacroze', -34.587513, -58.453654, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392908', 'TRANSPORTE', NULL, 'Lacroze', -34.587252, -58.454295, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392909', 'TRANSPORTE', NULL, 'Lacroze', -34.587208, -58.454282, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392910', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.587095, -58.455608, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392911', 'TRANSPORTE', NULL, 'Lacroze', -34.587257, -58.454868, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812392912', 'TRANSPORTE', NULL, 'Lacroze', -34.58719, -58.454833, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812394923', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.58697, -58.454871, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812394924', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.587495, -58.454511, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812394925', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.587297, -58.454538, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812394926', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.587453, -58.454625, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812394927', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.587314, -58.454995, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4812753920', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.587183, -58.455151, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4814660186', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.586838, -58.455178, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4814660187', 'TRANSPORTE', NULL, 'Lacroze', -34.586821, -58.455079, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4814703849', 'TRANSPORTE', NULL, 'Cementerio de la Chacarita', -34.586501, -58.457434, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4814703850', 'TRANSPORTE', NULL, 'Cementerio de la Chacarita', -34.586473, -58.457514, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4814703851', 'TRANSPORTE', NULL, 'Cementerio de la Chacarita', -34.586589, -58.457477, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4814703852', 'TRANSPORTE', NULL, 'Cementerio de la Chacarita', -34.586563, -58.457559, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4814703853', 'TRANSPORTE', NULL, 'Cementerio de la Chacarita', -34.586445, -58.457594, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4814703854', 'TRANSPORTE', NULL, 'Cementerio de la Chacarita', -34.586535, -58.457641, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4875379103', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.586366, -58.455641, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4875379104', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.586456, -58.45567, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4875379105', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.586486, -58.45568, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4875379106', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.586603, -58.45574, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4875379107', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.586689, -58.455789, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4875379108', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.586811, -58.45586, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4875379109', 'TRANSPORTE', NULL, 'Federico Lacroze', -34.586864, -58.455879, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4875379111', 'TRANSPORTE', NULL, 'Forest y Palpa', -34.582954, -58.45596, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4896303967', 'TRANSPORTE', NULL, 'Forest y Teodoro García', -34.583862, -58.455131, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4896303968', 'TRANSPORTE', NULL, 'Forest y Teodoro García', -34.583917, -58.455218, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5114021169', 'ESCUELA', NULL, 'Escuela de Maestros (anexo 08)', -34.583757, -58.45475, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117230023', 'ESCUELA', NULL, 'Escuela Primaria Común 18 Cabildo de Buenos Aires', -34.583645, -58.453257, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117402542', 'ESCUELA', NULL, 'Jardín de Infantes Colmenitas', -34.586092, -58.453353, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117427879', 'ESCUELA', NULL, 'UGEE Nº 16 Bachillerato Popular de Adultos y Adolescentes Mocha Celis', -34.586466, -58.455214, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117429133', 'ESCUELA', NULL, 'Centro de Formación Profesional 10 Raúl Scalabrini Ortiz (anexo) - SEC', -34.584731, -58.452801, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117441303', 'ESCUELA', NULL, 'Primera Infancia Espacio El Jardín de Teresa', -34.58382, -58.456019, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117565934', 'ESCUELA', NULL, 'UGEE 15 Bachillerato Popular de Jóvenes y Adultos Osvaldo Bayer', -34.58642, -58.4552, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117592457', 'ESCUELA', NULL, 'Centro de Alfabetización, Educación Básica y Trabajo 24', -34.586405, -58.455184, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117619876', 'ESCUELA', NULL, 'Centro de Alfabetización, Educación Básica y Trabajo 09', -34.586436, -58.455208, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5311486494', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.587036, -58.455585, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5910755455', 'TRANSPORTE', NULL, 'Lacroze', -34.586307, -58.454685, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5910755456', 'TRANSPORTE', NULL, 'Lacroze', -34.586573, -58.454859, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5910755457', 'TRANSPORTE', NULL, 'Triunvirato y Zabala', -34.585069, -58.458662, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5910755458', 'TRANSPORTE', NULL, 'Triunvirato y Zabala', -34.585117, -58.45866, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6060276923', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.586896, -58.455364, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6060276925', 'COMPETENCIA', 'ALMACEN', 'Bimbo Outlet', -34.586419, -58.454998, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6331216625', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.584759, -58.453469, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6331216964', 'COMPETENCIA', 'ALMACEN', 'Gaby', -34.583029, -58.45236, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6331218303', 'COMPLEMENTARIO', NULL, 'Farmacia Silvy', -34.584712, -58.453434, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6332026128', 'OFICINA', NULL, 'Efectivo sí', -34.587132, -58.454252, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6371477130', 'COMPETENCIA', 'ALMACEN', 'Alfredo', -34.583059, -58.452377, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6791518002', 'OFICINA', NULL, 'Mudanzas El Neutral', -34.584365, -58.454488, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6791518430', 'OFICINA', NULL, 'Dimalex', -34.583139, -58.455593, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6839233084', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.588093, -58.453148, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7178440626', 'TRANSPORTE', NULL, 'Newbery y Guzmán', -34.590101, -58.452269, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7178440627', 'TRANSPORTE', NULL, 'Newbery y Guzmán', -34.590159, -58.452196, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7178440628', 'TRANSPORTE', NULL, 'Newbery y Rodney', -34.591116, -58.453378, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7178440629', 'TRANSPORTE', NULL, 'Newbery y Rodney', -34.591145, -58.45334, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7178440630', 'TRANSPORTE', NULL, 'Newbery y Rodney', -34.591193, -58.453458, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7178440631', 'TRANSPORTE', NULL, 'Newbery y Rodney', -34.591224, -58.453415, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7296061899', 'TRANSPORTE', NULL, 'Lacroze y Fraga', -34.584517, -58.453429, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7296061900', 'TRANSPORTE', NULL, 'Lacroze y Fraga', -34.584476, -58.453518, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8268761974', 'TRANSPORTE', NULL, 'Guevara y Newbery', -34.586955, -58.45003, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8268761975', 'TRANSPORTE', NULL, 'Guevara y Newbery', -34.586971, -58.449987, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9121577937', 'TRANSPORTE', NULL, 'Lacroze', -34.587959, -58.452466, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9121577938', 'TRANSPORTE', NULL, 'Lacroze', -34.587883, -58.452424, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9393418520', 'TRANSPORTE', NULL, 'Lacroze', -34.586766, -58.456663, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9625466623', 'TRANSPORTE', NULL, 'Newbery y Rodney', -34.590868, -58.453224, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9625466624', 'TRANSPORTE', NULL, 'Newbery y Rodney', -34.590913, -58.453161, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9625466626', 'TRANSPORTE', NULL, 'Triunvirato y Céspedes', -34.585057, -58.458098, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9625466627', 'TRANSPORTE', NULL, 'Triunvirato y Céspedes', -34.585103, -58.458096, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9627864739', 'OFICINA', NULL, 'Centro Único de Coordinación y Control', -34.590484, -58.451808, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9627882715', 'OFICINA', NULL, 'Ministerio Público Fiscal', -34.590715, -58.45094, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9719588059', 'TRANSPORTE', NULL, 'Triunvirato y Zabala', -34.58499, -58.458733, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9719588060', 'TRANSPORTE', NULL, 'Triunvirato y Zabala', -34.584949, -58.458737, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9719588061', 'TRANSPORTE', NULL, 'Triunvirato y Teodoro García', -34.585225, -58.455863, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9719588062', 'TRANSPORTE', NULL, 'Triunvirato y Teodoro García', -34.585257, -58.45587, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9719588063', 'TRANSPORTE', NULL, 'Triunvirato y Teodoro García', -34.585288, -58.455744, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9719588064', 'TRANSPORTE', NULL, 'Triunvirato y Teodoro García', -34.585257, -58.45574, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733897232', 'TRANSPORTE', NULL, 'Guevara y Newbery', -34.587012, -58.450051, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9793656594', 'COMPLEMENTARIO', NULL, 'Credicoop', -34.582887, -58.452668, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9921494440', 'TRANSPORTE', NULL, 'Olleros y Fraga', -34.585017, -58.45246, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9921494441', 'TRANSPORTE', NULL, 'Olleros y Fraga', -34.585194, -58.452499, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9921494442', 'TRANSPORTE', NULL, 'Olleros y Fraga', -34.585036, -58.45241, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9948624908', 'OFICINA', NULL, 'OPSCRA', -34.584568, -58.453348, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9948624909', 'OFICINA', NULL, 'SOECRA', -34.58453, -58.453323, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11101815977', 'TRANSPORTE', NULL, 'Guevara y Newbery', -34.587026, -58.450003, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11106536359', 'TRANSPORTE', NULL, 'Triunvirato y Teodoro García', -34.585396, -58.455855, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11106536360', 'TRANSPORTE', NULL, 'Triunvirato y Teodoro García', -34.58538, -58.455905, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11106536361', 'TRANSPORTE', NULL, 'Triunvirato y Teodoro García', -34.585343, -58.455893, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11106536362', 'TRANSPORTE', NULL, 'Triunvirato y Teodoro García', -34.585357, -58.455844, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12001191267', 'TRANSPORTE', NULL, 'Olleros y Fraga', -34.585178, -58.452554, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12084256251', 'COMPLEMENTARIO', NULL, 'Dia', -34.587559, -58.453114, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12084256251', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.587559, -58.453114, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12194506060', 'COMPETENCIA', 'PAPELERIA', 'Libreria El Garage', -34.584185, -58.454053, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12314555478', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.584865, -58.453553, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13612762802', 'SALUD', NULL, 'Kinev', -34.588746, -58.449995, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13612762901', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.589215, -58.449993, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13612934101', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.587736, -58.452418, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13612934401', 'COMPETENCIA', 'ALMACEN', 'El pan de cada día', -34.586878, -58.452353, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13619008902', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.583849, -58.455271, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13619037103', 'COMPLEMENTARIO', NULL, 'Sonrisas', -34.586979, -58.452497, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13619037103', 'COMPETENCIA', 'ALMACEN', 'Sonrisas', -34.586979, -58.452497, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13619043001', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.586481, -58.452962, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13619043301', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.587266, -58.451995, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13619043301', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.587266, -58.451995, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13634800701', 'COMPETENCIA', 'ALMACEN', 'Medialunas Point', -34.587467, -58.453484, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13929103775', 'COMPLEMENTARIO', NULL, 'Farmacity', -34.585502, -58.455755, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:14056991105', 'TRANSPORTE', NULL, 'LACROZE', -34.586636, -58.455518, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:102034200', 'PARQUE', NULL, 'Plazoleta Roberto Jorge Santoro', -34.584614, -58.454751, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:234486291', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.585973, -58.45703, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:333661084', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.587149, -58.455123, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:333661086', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.587211, -58.455158, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:380339087', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.585432, -58.459132, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:447216750', 'PARQUE', NULL, 'Jardín Paseo de la Chacarita de los Colegiales', -34.59321, -58.45597, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:447311427', 'PARQUE', NULL, 'Lugar sin nombre', -34.590241, -58.450439, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:447532917', 'PARQUE', NULL, 'Lugar sin nombre', -34.588607, -58.451293, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:455195541', 'OFICINA', NULL, 'Terminal Línea 39', -34.58712, -58.450752, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:455280894', 'COMPETENCIA', 'ALMACEN', 'El Galpón', -34.585506, -58.457118, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471512604', 'SALUD', NULL, 'Geriatrico Dorrego', -34.587058, -58.44925, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:472743046', 'OFICINA', NULL, 'Mega Letras', -34.583847, -58.455405, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:472743050', 'COMPLEMENTARIO', NULL, 'Galicia', -34.585522, -58.453922, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:472743051', 'COMPETENCIA', 'ALMACEN', 'Lanel', -34.585586, -58.453857, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:472743051', 'COMPETENCIA', 'KIOSCO', 'Lanel', -34.585586, -58.453857, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:472743055', 'COMPLEMENTARIO', NULL, 'Banco Nación', -34.586416, -58.453086, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:472743059', 'SALUD', NULL, 'Centro Medico Laboral', -34.587119, -58.452523, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:473087510', 'COMPLEMENTARIO', NULL, 'Dia', -34.585463, -58.453344, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:473087510', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.585463, -58.453344, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:473087525', 'ESCUELA', NULL, 'Escuela del Siglo Nuevo', -34.584403, -58.452274, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:488952754', 'TRANSPORTE', NULL, 'Centro de Trasbordo Federico Lacroze', -34.587387, -58.454299, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:488952756', 'TRANSPORTE', NULL, 'Centro de Trasbordo Federico Lacroze', -34.587529, -58.454357, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:488985405', 'TRANSPORTE', NULL, 'Centro de Trasbordo Federico Lacroze', -34.587215, -58.454764, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:488985409', 'TRANSPORTE', NULL, 'Centro de Trasbordo Federico Lacroze', -34.587354, -58.45483, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:519186312', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.586364, -58.456156, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:519186313', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.586475, -58.456222, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:519186314', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.58673, -58.456368, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:519186315', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.586597, -58.456277, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:519186319', 'PARQUE', NULL, 'Plazoleta Subcomisario Severino José Cirigliano', -34.585875, -58.453197, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1046732084', 'OFICINA', NULL, 'Edenor', -34.591288, -58.451033, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1134550220', 'PARQUE', NULL, 'Plaza Barrio Playón', -34.584728, -58.457356, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1134550221', 'PARQUE', NULL, 'Lugar sin nombre', -34.585021, -58.4561, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1259284675', 'PARQUE', NULL, 'Plaza Fraga Manzana C', -34.584524, -58.458512, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1268690214', 'PARQUE', NULL, 'Plaza Blanca Varela', -34.585491, -58.45687, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1317577534', 'ESCUELA', NULL, 'Escuela Técnica 32 DE 14 General José de San Martín', -34.58334, -58.454773, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1331521717', 'OFICINA', NULL, 'Cementerio de la Chacarita', -34.587145, -58.457063, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1349548460', 'SALUD', NULL, 'Centro de Cuidado Integral Playón Chacarita', -34.584807, -58.457355, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1349548462', 'PARQUE', NULL, 'Lugar sin nombre', -34.584858, -58.457117, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1525856867', 'PARQUE', NULL, 'Lugar sin nombre', -34.58465, -58.454674, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:533604', 'PARQUE', NULL, 'Parque Los Andes', -34.589968, -58.451624, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:2539278', 'TRANSPORTE', NULL, 'Estación Federico Lacroze', -34.587137, -58.455092, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:5664037', 'TRANSPORTE', NULL, 'Estación Federico Lacroze', -34.586215, -58.45703, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6730432', 'TRANSPORTE', NULL, 'Terminal Chacarita', -34.586838, -58.450744, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6969832', 'TRANSPORTE', NULL, 'Lacroze y Fraga', -34.584706, -58.453606, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6969833', 'TRANSPORTE', NULL, 'Centro de Trasbordo Federico Lacroze', -34.587398, -58.453958, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6969869', 'TRANSPORTE', NULL, 'Parque Los Andes', -34.588537, -58.452044, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7186048', 'TRANSPORTE', NULL, 'Lacroze', -34.586428, -58.454843, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7186142', 'TRANSPORTE', NULL, 'Cementerio de la Chacarita', -34.586495, -58.457602, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7280421', 'TRANSPORTE', NULL, 'Forest y Palpa', -34.582918, -58.456055, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7310021', 'TRANSPORTE', NULL, 'Forest y Teodoro García', -34.58389, -58.455175, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10662527', 'TRANSPORTE', NULL, 'Newbery y Guzmán', -34.59013, -58.452232, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10662528', 'TRANSPORTE', NULL, 'Newbery y Rodney', -34.591208, -58.453436, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13260756', 'TRANSPORTE', NULL, 'Lacroze', -34.587921, -58.452445, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:13987607', 'TRANSPORTE', NULL, 'Triunvirato y Céspedes', -34.58508, -58.458097, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14111330', 'TRANSPORTE', NULL, 'Triunvirato y Zabala', -34.584969, -58.458735, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14130765', 'TRANSPORTE', NULL, 'Guevara y Newbery', -34.586983, -58.45004, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14404359', 'TRANSPORTE', NULL, 'Olleros y Fraga', -34.585027, -58.452435, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 14, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 18, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 185, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 12, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 5, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 19, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 19, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 1, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 9, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Chacarita' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:84556661', 'TRANSPORTE', NULL, 'Colegiales', -34.57304, -58.448199, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1018297769', 'SALUD', NULL, 'Alexander Fleming', -34.571756, -58.451861, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1592676198', 'TRANSPORTE', NULL, 'Colegiales', -34.573005, -58.448181, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2011932655', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.577676, -58.449019, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2011932655', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.577676, -58.449019, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2578906122', 'COMPLEMENTARIO', NULL, 'Banco Nación', -34.572899, -58.446409, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2578919505', 'TRANSPORTE', NULL, 'Lacroze y Zapiola', -34.576379, -58.44845, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2578936601', 'COMPETENCIA', 'ALMACEN', 'Autoservicio Estela', -34.573161, -58.448415, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2578936601', 'COMPETENCIA', 'KIOSCO', 'Autoservicio Estela', -34.573161, -58.448415, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2578961172', 'COMPETENCIA', 'ALMACEN', 'Las Delicias', -34.573785, -58.449638, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2579154511', 'TRANSPORTE', NULL, 'Lacroze y Zapiola', -34.57581, -58.44839, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2579154512', 'TRANSPORTE', NULL, 'Conesa y Lacroze', -34.574918, -58.447426, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2579154513', 'TRANSPORTE', NULL, 'Conde y Lacroze', -34.57793, -58.449608, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2580886452', 'COMPLEMENTARIO', NULL, 'Green', -34.573849, -58.447034, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2580886453', 'COMPLEMENTARIO', NULL, 'Nueva Lacroze', -34.572264, -58.446409, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2580898124', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.577087, -58.451593, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2580898128', 'COMPLEMENTARIO', NULL, 'Lamaité', -34.577192, -58.450822, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2776463040', 'OFICINA', NULL, 'Online Marketing', -34.575746, -58.448767, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2871575601', 'TRANSPORTE', NULL, 'Lacroze y Zapiola', -34.575845, -58.448277, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2871575701', 'TRANSPORTE', NULL, 'Lacroze y Zapiola', -34.576346, -58.448537, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2871575801', 'TRANSPORTE', NULL, 'Conde y Lacroze', -34.577888, -58.449585, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3084057074', 'TRANSPORTE', NULL, 'Zapiola y Lacroze', -34.575924, -58.448591, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3084057075', 'TRANSPORTE', NULL, 'Zapiola y Lacroze', -34.575969, -58.448625, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3084064235', 'TRANSPORTE', NULL, 'Zapiola y Céspedes', -34.574037, -58.452226, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3084064236', 'TRANSPORTE', NULL, 'Zapiola y Céspedes', -34.574086, -58.452272, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3084064239', 'TRANSPORTE', NULL, 'Zapiola y Maure', -34.576658, -58.446862, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3084064240', 'TRANSPORTE', NULL, 'Zapiola y Maure', -34.576749, -58.446922, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3084064241', 'TRANSPORTE', NULL, 'Zapiola y Newbery', -34.577059, -58.445926, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3084064242', 'TRANSPORTE', NULL, 'Zapiola y Newbery', -34.577124, -58.44598, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3235487818', 'TRANSPORTE', NULL, 'Zapiola y Virrey Loreto', -34.572956, -58.454565, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3235495895', 'TRANSPORTE', NULL, 'Maure y Zapiola', -34.576646, -58.446683, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3235495896', 'TRANSPORTE', NULL, 'Newbery y Zapiola', -34.577293, -58.445793, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3235558342', 'TRANSPORTE', NULL, 'Maure y Zapiola', -34.576672, -58.44661, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3235558355', 'TRANSPORTE', NULL, 'Newbery y Zapiola', -34.577276, -58.445845, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3235558358', 'TRANSPORTE', NULL, 'Zapiola y Virrey Loreto', -34.572871, -58.454498, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343093428', 'TRANSPORTE', NULL, 'Lacroze y Conde', -34.577788, -58.449425, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343100399', 'TRANSPORTE', NULL, 'Lacroze y Conde', -34.577591, -58.449318, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3343147505', 'TRANSPORTE', NULL, 'Lacroze y Conde', -34.577833, -58.449338, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3404852452', 'TRANSPORTE', NULL, 'Lacroze y Conde', -34.577627, -58.44922, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786561', 'TRANSPORTE', NULL, 'Conde y Céspedes', -34.576116, -58.453424, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786562', 'TRANSPORTE', NULL, 'Conde y Céspedes', -34.576149, -58.453441, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786563', 'TRANSPORTE', NULL, 'Conde y Maure', -34.578668, -58.448016, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786564', 'TRANSPORTE', NULL, 'Conde y Maure', -34.578713, -58.448043, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786565', 'TRANSPORTE', NULL, 'Conde y Palpa', -34.576864, -58.451712, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3413786566', 'TRANSPORTE', NULL, 'Conde y Palpa', -34.576899, -58.451719, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3449432058', 'TRANSPORTE', NULL, 'Colegiales', -34.573024, -58.448191, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3542954440', 'TRANSPORTE', NULL, 'Conesa y Lacroze', -34.574963, -58.447446, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3579317702', 'COMPETENCIA', 'PAPELERIA', 'Librería Biblos', -34.573468, -58.448241, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645775', 'TRANSPORTE', NULL, 'Lacroze y Amenábar', -34.571801, -58.445961, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645776', 'TRANSPORTE', NULL, 'Lacroze y Amenábar', -34.571849, -58.445875, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645779', 'TRANSPORTE', NULL, 'Lacroze y Conde', -34.578275, -58.449601, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645780', 'TRANSPORTE', NULL, 'Lacroze y Conde', -34.578312, -58.449501, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887533', 'TRANSPORTE', NULL, 'Freire y Céspedes', -34.575355, -58.452369, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887534', 'TRANSPORTE', NULL, 'Freire y Céspedes', -34.57541, -58.452393, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887537', 'TRANSPORTE', NULL, 'Freire y Teodoro García', -34.576696, -58.449418, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887538', 'TRANSPORTE', NULL, 'Freire y Teodoro García', -34.576748, -58.449443, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887539', 'TRANSPORTE', NULL, 'Freire y Virrey Loreto', -34.574041, -58.454909, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887540', 'TRANSPORTE', NULL, 'Freire y Virrey Loreto', -34.574085, -58.454945, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887547', 'TRANSPORTE', NULL, 'Lacroze y Conde', -34.578166, -58.449536, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3909887548', 'TRANSPORTE', NULL, 'Lacroze y Conde', -34.57821, -58.449445, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4105423195', 'COMPETENCIA', 'PAPELERIA', 'Printplot', -34.574, -58.452588, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4654642529', 'OFICINA', NULL, 'Cobermed', -34.57847, -58.449911, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4654642529', 'SALUD', NULL, 'Cobermed', -34.57847, -58.449911, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4657022736', 'COMPETENCIA', 'PAPELERIA', 'Staples', -34.576872, -58.448614, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4657022749', 'COMPLEMENTARIO', NULL, 'Dia', -34.577774, -58.449097, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4657022749', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.577774, -58.449097, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4657022754', 'COMPETENCIA', 'PAPELERIA', 'Lugar sin nombre', -34.577842, -58.449136, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4657022770', 'COMPLEMENTARIO', NULL, 'Farmacia', -34.578582, -58.449592, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4657097738', 'COMPLEMENTARIO', NULL, 'Real', -34.579251, -58.449988, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4657707016', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.573068, -58.451101, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4657707016', 'COMPETENCIA', 'KIOSCO', 'Carrefour Express', -34.573068, -58.451101, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5114009386', 'ESCUELA', NULL, 'Escuela Primaria p/Adultos 15 Cap. Gral. Bernardo O''Higgins', -34.575547, -58.448365, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5114009387', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado B (EPCjs 15/09°)', -34.575569, -58.448376, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5115268321', 'ESCUELA', NULL, 'Escuela Primaria p/ Adultos 16 Cap. de Fragata Carlos María Moyano', -34.570669, -58.451377, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5116106908', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado A (EPCjc 07/09°)', -34.573683, -58.454579, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117402577', 'ESCUELA', NULL, 'Instituto Aequalis', -34.571559, -58.45223, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117402584', 'ESCUELA', NULL, 'La Escuelita', -34.576181, -58.44496, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117402587', 'ESCUELA', NULL, 'La Escuela del Arbol', -34.574504, -58.451066, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117420488', 'ESCUELA', NULL, 'Little Stars', -34.578917, -58.449006, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117427908', 'ESCUELA', NULL, 'Centro de Formación Profesional 19 (a)', -34.57762, -58.450464, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117429156', 'ESCUELA', NULL, 'Escuela Infantil Mundo del Sol', -34.574664, -58.448831, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117440712', 'ESCUELA', NULL, 'Centro Educativo de Nivel Secundario 28 SUTERH', -34.575677, -58.451402, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117441238', 'ESCUELA', NULL, 'Buenos Aires Montessori', -34.577807, -58.448041, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117441249', 'ESCUELA', NULL, 'Escuela de Enfermería del Sanatorio Colegiales', -34.577242, -58.450672, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117448185', 'ESCUELA', NULL, 'Los Almendros', -34.570611, -58.449669, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117449270', 'ESCUELA', NULL, 'Jardín del Prado', -34.575887, -58.445597, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117454943', 'ESCUELA', NULL, 'El Jardín de la Esquina', -34.571567, -58.452186, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117454958', 'ESCUELA', NULL, 'Colegio de Las Estrellas', -34.5789, -58.448992, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117454962', 'ESCUELA', NULL, 'Instituto Psicopedagógico Senderos', -34.572681, -58.450658, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117454975', 'ESCUELA', NULL, 'Escuela San Andrés', -34.575267, -58.447019, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117498914', 'ESCUELA', NULL, 'Centro Educativo de Nivel Primario 36 - SUTERH', -34.575696, -58.451348, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117498915', 'ESCUELA', NULL, 'Centro de Formación Profesional 28 Ministro José María Freire (anexo) - SUTERH', -34.575659, -58.451443, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117498916', 'ESCUELA', NULL, 'Centro Educativo de Nivel Secundario 17', -34.575639, -58.451475, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5269980472', 'COMPETENCIA', 'ALMACEN', 'Le Blé', -34.575697, -58.452772, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6252941969', 'TRANSPORTE', NULL, 'Conesa y Maure', -34.575662, -58.445697, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6331216969', 'COMPETENCIA', 'KIOSCO', 'Kiosco Dufis', -34.575451, -58.444299, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6331218236', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.57728, -58.446044, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6371477224', 'COMPETENCIA', 'ALMACEN', 'Bellaria', -34.571788, -58.445759, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7156325082', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.572184, -58.44597, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7156325082', 'COMPETENCIA', 'KIOSCO', 'Carrefour Express', -34.572184, -58.44597, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7722058685', 'SALUD', NULL, 'Delegación Belgrano de traumatología del Hospital Italiano', -34.571828, -58.448433, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7777094540', 'TRANSPORTE', NULL, 'Avenida Lacroze', -34.577096, -58.448718, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7777094541', 'TRANSPORTE', NULL, 'Freire y Lacroze', -34.577041, -58.448685, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8008077417', 'SALUD', NULL, 'Clínica Maisonave', -34.570794, -58.447909, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8008081568', 'COMPLEMENTARIO', NULL, 'Amenábar', -34.57189, -58.447554, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8008081568', 'COMPETENCIA', 'ALMACEN', 'Amenábar', -34.57189, -58.447554, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8008117994', 'COMPETENCIA', 'ALMACEN', 'Huerta Amenábar', -34.572828, -58.445995, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9625316171', 'COMPETENCIA', 'PAPELERIA', 'SMG Papelera', -34.576306, -58.448352, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9792926710', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.57708, -58.449114, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9921770634', 'TRANSPORTE', NULL, 'Freire y Céspedes', -34.575312, -58.452475, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9921770635', 'TRANSPORTE', NULL, 'Freire y Céspedes', -34.575363, -58.452499, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9921770636', 'TRANSPORTE', NULL, 'Freire y Teodoro García', -34.57673, -58.449354, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9921770637', 'TRANSPORTE', NULL, 'Freire y Teodoro García', -34.576783, -58.449373, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10789130082', 'COMPETENCIA', 'ALMACEN', 'Volito Kioscos', -34.571634, -58.44606, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10789130082', 'COMPETENCIA', 'KIOSCO', 'Volito Kioscos', -34.571634, -58.44606, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12112866323', 'COMPLEMENTARIO', NULL, 'Dia', -34.572194, -58.446382, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12112866323', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.572194, -58.446382, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13438729601', 'COMPETENCIA', 'KIOSCO', '365', -34.572427, -58.446226, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13685518988', 'COMPETENCIA', 'PAPELERIA', 'Gráfica 6', -34.577031, -58.448604, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:101885119', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.573012, -58.44833, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:101885124', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.57292, -58.448314, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:205207709', 'PARQUE', NULL, 'Plaza de proximidad Crámer', -34.574608, -58.444901, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:224820071', 'OFICINA', NULL, 'Datacenter Claro', -34.57392, -58.445012, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:251733486', 'SALUD', NULL, 'Sanatorio Colegiales', -34.576905, -58.451028, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:251961466', 'ESCUELA', NULL, 'Escuela Primaria Común 12 Gran Mariscal del Perú Ramón Castilla', -34.576312, -58.45217, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:251961467', 'ESCUELA', NULL, 'Escuela Primaria Común 07 Marcos Sastre', -34.573663, -58.454463, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:318764313', 'ESCUELA', NULL, 'Escuela Primaria Común 15 Cap. Gral. Bernardo O''Higgins', -34.575432, -58.448604, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471299673', 'COMPLEMENTARIO', NULL, 'Carrefour Market', -34.577165, -58.449417, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471299673', 'COMPETENCIA', 'ALMACEN', 'Carrefour Market', -34.577165, -58.449417, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471299680', 'COMPETENCIA', 'ALMACEN', 'Iris', -34.577602, -58.449465, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471299688', 'COMPLEMENTARIO', NULL, 'Universo', -34.578497, -58.450054, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471299688', 'COMPETENCIA', 'ALMACEN', 'Universo', -34.578497, -58.450054, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471299692', 'COMPLEMENTARIO', NULL, 'Farmacity', -34.578659, -58.450131, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471542454', 'ESCUELA', NULL, 'Instituto Patronato Español Virgen de Pilar', -34.575414, -58.44762, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471542455', 'ESCUELA', NULL, 'Centro de primera infancia: Mis Primeros Pasos', -34.575836, -58.448005, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:724155385', 'PARQUE', NULL, 'Lugar sin nombre', -34.574592, -58.444861, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1055154146', 'ESCUELA', NULL, 'Escuela del Sol', -34.575243, -58.444776, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1058462350', 'ESCUELA', NULL, 'Instituto Compañía de María', -34.575893, -58.452704, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1068810765', 'COMPLEMENTARIO', NULL, 'Supermercado Conesa', -34.574329, -58.449339, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1068810765', 'COMPETENCIA', 'ALMACEN', 'Supermercado Conesa', -34.574329, -58.449339, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1278654593', 'PARQUE', NULL, 'Parque Ferroviario Colegiales', -34.572029, -58.449168, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1518313124', 'ESCUELA', NULL, 'Escuela Primaria Común 16 Capitán de Fragata Carlos María Moyano', -34.570521, -58.451766, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:1302773', 'PARQUE', NULL, 'Plaza Juan José Paso', -34.570108, -58.452843, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:2790186', 'TRANSPORTE', NULL, 'Estación Colegiales', -34.572958, -58.448314, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:11372582', 'TRANSPORTE', NULL, 'Avenida Lacroze', -34.577068, -58.448701, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 30, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 3, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 58, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 4, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 5, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 15, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 17, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 6, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 7, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Colegiales' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1023775285', 'ESCUELA', NULL, 'Escuela Infantil 08/15º (a)', -34.57687, -58.4693, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1050234840', 'COMPLEMENTARIO', NULL, 'Farmacity', -34.578918, -58.469717, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1177972570', 'COMPLEMENTARIO', NULL, 'Leguizamón', -34.582411, -58.472213, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1231750243', 'COMPLEMENTARIO', NULL, 'Macro', -34.578981, -58.469878, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1231757964', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.578771, -58.469312, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1231757964', 'COMPETENCIA', 'KIOSCO', 'Carrefour Express', -34.578771, -58.469312, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1707518252', 'COMPETENCIA', 'ALMACEN', 'L''epi Boulangerie', -34.58153, -58.466871, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2025901930', 'TRANSPORTE', NULL, 'Triunvirato al 3600', -34.58195, -58.472778, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2025901950', 'TRANSPORTE', NULL, 'Avenida Chorroarín', -34.583058, -58.469293, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2892599800', 'TRANSPORTE', NULL, 'Avenida Chorroarín', -34.583142, -58.469315, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2892599813', 'TRANSPORTE', NULL, 'Triunvirato al 3600', -34.58202, -58.47281, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3071288955', 'TRANSPORTE', NULL, 'Los Incas y Acha', -34.580188, -58.472546, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3071289115', 'TRANSPORTE', NULL, 'Los Incas y Álvarez Thomas', -34.579142, -58.470663, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3071289642', 'TRANSPORTE', NULL, 'Los Incas y Heredia', -34.576164, -58.465489, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3071355994', 'TRANSPORTE', NULL, 'Los Incas y Acha', -34.580252, -58.472489, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3071356002', 'TRANSPORTE', NULL, 'Los Incas y Heredia', -34.57619, -58.465466, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3071356004', 'TRANSPORTE', NULL, 'Los Incas y Álvarez Thomas', -34.579209, -58.47061, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3207152461', 'TRANSPORTE', NULL, 'Avenida Los Incas', -34.578579, -58.469072, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3207152462', 'TRANSPORTE', NULL, 'Avenida Los Incas', -34.578648, -58.469086, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3207152468', 'TRANSPORTE', NULL, 'Los Incas y Plaza', -34.577602, -58.468026, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3207152469', 'TRANSPORTE', NULL, 'Los Incas y Plaza', -34.577638, -58.467994, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3864280395', 'TRANSPORTE', NULL, 'Los Incas y Acha', -34.580228, -58.472445, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3864280396', 'TRANSPORTE', NULL, 'Los Incas y Acha', -34.580282, -58.472408, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3864280401', 'TRANSPORTE', NULL, 'Los Incas y Estomba', -34.576691, -58.466021, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3864280402', 'TRANSPORTE', NULL, 'Los Incas y Estomba', -34.576732, -58.465993, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3864280405', 'TRANSPORTE', NULL, 'Avenida Álvarez Thomas', -34.57895, -58.470159, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3864280406', 'TRANSPORTE', NULL, 'Avenida Álvarez Thomas', -34.579007, -58.470127, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4080758989', 'TRANSPORTE', NULL, 'Avenida Chorroarín', -34.583208, -58.468876, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4080758990', 'TRANSPORTE', NULL, 'Avenida Chorroarín', -34.583277, -58.468913, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4080759003', 'TRANSPORTE', NULL, 'Triunvirato al 3600', -34.581818, -58.473185, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4080759004', 'TRANSPORTE', NULL, 'Triunvirato al 3600', -34.581889, -58.47321, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4080759005', 'TRANSPORTE', NULL, 'Triunvirato y Acha', -34.582345, -58.471487, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4080759006', 'TRANSPORTE', NULL, 'Triunvirato y Acha', -34.582428, -58.471511, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4222734563', 'COMPETENCIA', 'ALMACEN', 'Shop', -34.57871, -58.470192, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4222734563', 'COMPETENCIA', 'KIOSCO', 'Shop', -34.57871, -58.470192, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4227645726', 'TRANSPORTE', NULL, 'Los Incas y Plaza', -34.577905, -58.468174, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4227645727', 'TRANSPORTE', NULL, 'Los Incas y Plaza', -34.577935, -58.468148, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4413740225', 'COMPLEMENTARIO', NULL, 'Banco Nación', -34.578791, -58.4692, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4437298910', 'TRANSPORTE', NULL, 'Triunvirato y Chorroarín', -34.582838, -58.470259, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4437298911', 'TRANSPORTE', NULL, 'Triunvirato y Chorroarín', -34.582922, -58.470295, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4437298985', 'TRANSPORTE', NULL, 'Triunvirato y Acha', -34.582167, -58.472361, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4437298986', 'TRANSPORTE', NULL, 'Triunvirato y Acha', -34.582248, -58.472386, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4644719694', 'OFICINA', NULL, 'Piñeiro & Puy', -34.582751, -58.470967, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4658058995', 'COMPLEMENTARIO', NULL, 'Tendy', -34.577931, -58.471792, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4658058995', 'COMPETENCIA', 'ALMACEN', 'Tendy', -34.577931, -58.471792, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4668709332', 'COMPETENCIA', 'ALMACEN', 'Drugstore 24 Siete', -34.583942, -58.467246, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4668709332', 'COMPETENCIA', 'KIOSCO', 'Drugstore 24 Siete', -34.583942, -58.467246, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4670217250', 'COMPLEMENTARIO', NULL, 'Dia', -34.580302, -58.472906, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4670217250', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.580302, -58.472906, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4670217258', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.580101, -58.472583, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4670242194', 'COMPETENCIA', 'ALMACEN', 'Cifu', -34.579899, -58.469382, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4670242194', 'COMPETENCIA', 'KIOSCO', 'Cifu', -34.579899, -58.469382, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5115529980', 'ESCUELA', NULL, 'Escuela Primaria Común 26 José Mármol', -34.576839, -58.469264, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117406386', 'ESCUELA', NULL, 'Escuela Integral Interdisciplinaria 14', -34.582271, -58.472538, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117454518', 'ESCUELA', NULL, 'Instituto San Roque', -34.580958, -58.467415, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5910755459', 'TRANSPORTE', NULL, 'Triunvirato y Chorroarín', -34.582963, -58.469866, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5910755460', 'TRANSPORTE', NULL, 'Triunvirato y Chorroarín', -34.583052, -58.469893, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6575757378', 'COMPLEMENTARIO', NULL, 'Dia Market', -34.578075, -58.468023, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6575757378', 'COMPETENCIA', 'ALMACEN', 'Dia Market', -34.578075, -58.468023, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6957954103', 'COMPETENCIA', 'ALMACEN', 'M24', -34.579077, -58.469997, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6957954103', 'COMPETENCIA', 'KIOSCO', 'M24', -34.579077, -58.469997, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7137262441', 'COMPETENCIA', 'PAPELERIA', 'Multi Impresos', -34.578421, -58.472065, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7137274006', 'OFICINA', NULL, 'Expreso Albertino', -34.580963, -58.46934, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7152992577', 'TRANSPORTE', NULL, 'Avenida Chorroarín', -34.580487, -58.469755, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7152992578', 'TRANSPORTE', NULL, 'Avenida Chorroarín', -34.580524, -58.469759, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7152992579', 'TRANSPORTE', NULL, 'Donado y De Los Incas', -34.580485, -58.469887, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7152992581', 'TRANSPORTE', NULL, 'Avenida Chorroarín', -34.580484, -58.469939, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7152992582', 'TRANSPORTE', NULL, 'Avenida Chorroarín', -34.580524, -58.469937, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7152992583', 'TRANSPORTE', NULL, 'Avenida de los Incas', -34.579108, -58.470859, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7152992584', 'TRANSPORTE', NULL, 'Donado y De Los Incas', -34.579109, -58.470932, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7153015985', 'TRANSPORTE', NULL, 'Avenida de los Incas', -34.579173, -58.470859, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7153015986', 'TRANSPORTE', NULL, 'Avenida de los Incas', -34.579173, -58.470935, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7153015987', 'TRANSPORTE', NULL, 'Donado y Carbajal', -34.577706, -58.470921, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7153015988', 'TRANSPORTE', NULL, 'Donado y Carbajal', -34.577738, -58.470974, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7153015989', 'TRANSPORTE', NULL, 'Donado y Carbajal', -34.577776, -58.47086, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7153015990', 'TRANSPORTE', NULL, 'Donado y Carbajal', -34.577804, -58.470908, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7183338193', 'TRANSPORTE', NULL, 'Álvarez Thomas y Acha', -34.578051, -58.471854, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7183338194', 'TRANSPORTE', NULL, 'Álvarez Thomas y Acha', -34.577981, -58.471831, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7715435769', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.578785, -58.469178, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7715438244', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.578966, -58.469846, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7722629361', 'COMPETENCIA', 'ALMACEN', 'Dina', -34.580075, -58.472521, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7734184723', 'COMPETENCIA', 'ALMACEN', 'M24', -34.580154, -58.471999, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7734184723', 'COMPETENCIA', 'KIOSCO', 'M24', -34.580154, -58.471999, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7967780507', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.581022, -58.473541, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7967780507', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.581022, -58.473541, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8043418877', 'TRANSPORTE', NULL, 'Plaza 25 de Agosto', -34.580716, -58.46349, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8043418878', 'TRANSPORTE', NULL, 'Charlone y Estomba', -34.58069, -58.464367, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8043418880', 'TRANSPORTE', NULL, 'Charlone y Tronador', -34.58062, -58.466431, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8043418881', 'TRANSPORTE', NULL, 'Charlone y Tronador', -34.58061, -58.466721, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8304863570', 'TRANSPORTE', NULL, 'Álvarez Thomas y Virrey Del Pino', -34.577704, -58.473007, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8304863571', 'TRANSPORTE', NULL, 'Álvarez Thomas y Tronador', -34.578896, -58.465804, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8304863572', 'TRANSPORTE', NULL, 'Álvarez Thomas y Tronador', -34.578862, -58.465611, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9262504027', 'TRANSPORTE', NULL, 'Álvarez Thomas y Virrey Del Pino', -34.577784, -58.473025, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10035471644', 'TRANSPORTE', NULL, 'Charlone y Estomba', -34.580649, -58.464368, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10035471647', 'TRANSPORTE', NULL, 'Plaza 25 de Agosto', -34.580673, -58.463488, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10035471648', 'TRANSPORTE', NULL, 'Charlone y Estomba', -34.580646, -58.464464, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10035471649', 'TRANSPORTE', NULL, 'Charlone y Tronador', -34.580573, -58.466716, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10035471650', 'TRANSPORTE', NULL, 'Charlone y Tronador', -34.58058, -58.466432, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11002506847', 'PARQUE', NULL, 'Lugar sin nombre', -34.579649, -58.467038, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11005188883', 'OFICINA', NULL, 'Asociación Espiritista La Fraternidad', -34.582184, -58.470992, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11005188890', 'OFICINA', NULL, 'Casa de Oración María Reina de la Paz', -34.581151, -58.46742, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11005188891', 'OFICINA', NULL, 'Centro Pastoral María del Rosario', -34.58107, -58.467421, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11005188892', 'TRANSPORTE', NULL, 'Álvarez Thomas y Tronador', -34.578967, -58.46578, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11238340881', 'TRANSPORTE', NULL, 'Álvarez Thomas y Tronador', -34.578923, -58.465588, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11339138715', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.578938, -58.469833, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11702619314', 'COMPLEMENTARIO', NULL, 'Juan', -34.581943, -58.471888, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11702619314', 'COMPETENCIA', 'ALMACEN', 'Juan', -34.581943, -58.471888, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11702634981', 'COMPLEMENTARIO', NULL, 'Tendy', -34.580657, -58.466905, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11702634981', 'COMPETENCIA', 'ALMACEN', 'Tendy', -34.580657, -58.466905, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12749188960', 'OFICINA', NULL, 'Almundo', -34.57888, -58.470403, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:9967939', 'PARQUE', NULL, 'Plaza 25 de Agosto', -34.58015, -58.462543, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:333661075', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.584121, -58.466258, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:437232917', 'OFICINA', NULL, 'Embajada de Hungría', -34.575509, -58.470481, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:437232921', 'OFICINA', NULL, 'Consulado de Hungría', -34.575698, -58.470463, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:470302126', 'COMPLEMENTARIO', NULL, 'Santander Río', -34.581756, -58.472638, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:470302136', 'COMPLEMENTARIO', NULL, 'Dia', -34.582134, -58.471534, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:470302136', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.582134, -58.471534, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:472679202', 'OFICINA', NULL, 'Wärtsilä', -34.582161, -58.465801, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:472679203', 'ESCUELA', NULL, 'Jardín Motrichicos', -34.581482, -58.465983, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:472820090', 'OFICINA', NULL, 'Fletes Chorroarín', -34.579519, -58.469348, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:472820095', 'OFICINA', NULL, 'Partido Obrero', -34.580373, -58.469324, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:473076571', 'COMPLEMENTARIO', NULL, 'Dia', -34.583461, -58.467805, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:473076571', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.583461, -58.467805, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:473076575', 'COMPLEMENTARIO', NULL, 'Jumbo', -34.583343, -58.46556, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:473076575', 'COMPETENCIA', 'ALMACEN', 'Jumbo', -34.583343, -58.46556, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:492058952', 'OFICINA', NULL, 'Dirección de Captación de Comunicaciones', -34.577, -58.466199, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:827070608', 'PARQUE', NULL, 'Plazoleta Villa Ortúzar', -34.583895, -58.466546, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1055624014', 'ESCUELA', NULL, 'Escuela para Jóvenes y Adultos con Discapacidad Mental y Formación Integral 18 C M Estrada de Cano', -34.579149, -58.469077, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1184770928', 'PARQUE', NULL, 'Plazoleta La Vecindad', -34.579612, -58.467066, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:2402057', 'PARQUE', NULL, 'Jardín Paseo República de Filipinas', -34.574263, -58.463759, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6490745', 'PARQUE', NULL, 'Lugar sin nombre', -34.575514, -58.470564, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6634647', 'TRANSPORTE', NULL, 'Avenida Chorroarín', -34.58288, -58.470277, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6634683', 'TRANSPORTE', NULL, 'Triunvirato y Acha', -34.582208, -58.472373, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7204765', 'TRANSPORTE', NULL, 'Tronador - Villa Ortúzar', -34.584121, -58.466258, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8705652', 'TRANSPORTE', NULL, 'Avenida Chorroarín', -34.583008, -58.469879, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10618063', 'TRANSPORTE', NULL, 'Avenida Chorroarín', -34.580506, -58.469847, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10618064', 'TRANSPORTE', NULL, 'Avenida de los Incas', -34.579173, -58.470897, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10618065', 'TRANSPORTE', NULL, 'Donado y Carbajal', -34.57779, -58.470884, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10668934', 'PARQUE', NULL, 'Lugar sin nombre', -34.585131, -58.470116, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10668935', 'TRANSPORTE', NULL, 'Álvarez Thomas y Acha', -34.578016, -58.471842, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:16016823', 'TRANSPORTE', NULL, 'Álvarez Thomas y Tronador', -34.578931, -58.465792, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 6, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 12, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 74, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 7, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 0, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 17, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 18, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 1, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 6, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Ortúzar' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:310606744', 'TRANSPORTE', NULL, 'La Paternal', -34.597012, -58.467215, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2152507928', 'TRANSPORTE', NULL, 'La Paternal', -34.597105, -58.46715, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3077545863', 'TRANSPORTE', NULL, 'Trelles y Cucha Cucha', -34.599645, -58.466491, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254582466', 'TRANSPORTE', NULL, 'Trelles y Añasco', -34.597618, -58.465993, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254582467', 'TRANSPORTE', NULL, 'Trelles y Añasco', -34.597633, -58.465898, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254582470', 'TRANSPORTE', NULL, 'Trelles y Cucha Cucha', -34.599632, -58.466584, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3451071469', 'TRANSPORTE', NULL, 'La Paternal', -34.596992, -58.467227, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4677133333', 'COMPLEMENTARIO', NULL, 'San Luis', -34.599735, -58.466668, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391432', 'TRANSPORTE', NULL, 'Garmendia y Osorio', -34.594853, -58.465037, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391433', 'TRANSPORTE', NULL, 'Garmendia y Osorio', -34.594856, -58.464972, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391434', 'TRANSPORTE', NULL, 'Carrefour', -34.59667, -58.472417, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391435', 'TRANSPORTE', NULL, 'Carrefour', -34.596732, -58.472395, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391436', 'TRANSPORTE', NULL, 'Hospital Alvear', -34.597013, -58.47467, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391437', 'TRANSPORTE', NULL, 'Hospital Alvear', -34.597091, -58.474666, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376574', 'TRANSPORTE', NULL, 'Hospital Alvear', -34.597009, -58.474436, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376575', 'TRANSPORTE', NULL, 'Hospital Alvear', -34.596912, -58.474446, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376577', 'TRANSPORTE', NULL, 'Plaza Besteiro', -34.596379, -58.47101, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376578', 'TRANSPORTE', NULL, 'Plaza Besteiro', -34.596311, -58.471027, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117454429', 'ESCUELA', NULL, 'Centro de Alfabetización, Educación Básica y Trabajo 39 Centro Comunitario', -34.597214, -58.466094, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117619851', 'ESCUELA', NULL, 'Centro de Alfabetización, Educación Básica y Trabajo 75 Centro Comunitario', -34.597195, -58.466087, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9873520503', 'TRANSPORTE', NULL, 'La Paternal', -34.596878, -58.467268, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9873520504', 'TRANSPORTE', NULL, 'La Paternal', -34.59698, -58.467232, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13298747759', 'TRANSPORTE', NULL, 'Elcano y Ávalos', -34.592483, -58.468168, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13298747760', 'TRANSPORTE', NULL, 'Elcano y Ávalos', -34.592506, -58.468139, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13298747761', 'TRANSPORTE', NULL, 'Punta Arenas y Bauness', -34.594427, -58.470287, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13298747762', 'TRANSPORTE', NULL, 'Punta Arenas y Bauness', -34.594441, -58.470216, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:23634120', 'SALUD', NULL, 'Hospital de emergencias psiquiátricas Marcelo Torcuato de Alvear', -34.597834, -58.473187, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:31183661', 'PARQUE', NULL, 'Parque La Isla de la Paternal', -34.593843, -58.474566, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:55594880', 'COMPLEMENTARIO', NULL, 'Carrefour', -34.595731, -58.473855, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:55594880', 'COMPETENCIA', 'ALMACEN', 'Carrefour', -34.595731, -58.473855, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:106964125', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.597065, -58.467204, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:106964133', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.596931, -58.467271, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:205022708', 'PARQUE', NULL, 'Lugar sin nombre', -34.595748, -58.470709, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:205022709', 'PARQUE', NULL, 'Plazoleta Julián Besteiro', -34.596079, -58.470851, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:451202952', 'PARQUE', NULL, 'Lugar sin nombre', -34.598391, -58.466587, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:451262642', 'PARQUE', NULL, 'Lugar sin nombre', -34.596114, -58.470642, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1250615645', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.596901, -58.467286, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1250615646', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.596959, -58.467257, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1250615647', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.59704, -58.46722, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1250615648', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.597092, -58.467192, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:1738600', 'PARQUE', NULL, 'Plazoleta Eloy Alfaro', -34.598418, -58.466592, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:2728320', 'TRANSPORTE', NULL, 'La Paternal', -34.596995, -58.467238, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7192476', 'TRANSPORTE', NULL, 'Garmendia y Osorio', -34.594753, -58.464992, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7192478', 'TRANSPORTE', NULL, 'Hospital M.T. de Alvear', -34.597052, -58.474668, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7204741', 'TRANSPORTE', NULL, 'Hospital M.T. de Alvear', -34.596961, -58.474441, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7204742', 'TRANSPORTE', NULL, 'Plaza Besteiro', -34.596345, -58.471019, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:19825172', 'TRANSPORTE', NULL, 'Elcano y Ávalos', -34.592495, -58.468153, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:19825173', 'TRANSPORTE', NULL, 'Punta Arenas y Bauness', -34.594434, -58.470251, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 2, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 0, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 36, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 6, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 1, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 2, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 1, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 0, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 0, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Paternal' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:256247490', 'TRANSPORTE', NULL, 'Malabia - Osvaldo Pugliese', -34.599042, -58.439704, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1357149443', 'COMPLEMENTARIO', NULL, 'Banco Provincia', -34.598148, -58.440412, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659139467', 'COMPLEMENTARIO', NULL, 'Supermercado Vea', -34.601009, -58.438671, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659139467', 'COMPETENCIA', 'ALMACEN', 'Supermercado Vea', -34.601009, -58.438671, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659144502', 'COMPETENCIA', 'ALMACEN', 'Los Pibes', -34.600662, -58.438382, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659148593', 'COMPETENCIA', 'ALMACEN', 'Manin Hermanos', -34.598558, -58.43696, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659162344', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Ramírez de Velasco', -34.598685, -58.436733, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659207446', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Ramírez de Velasco', -34.598838, -58.436984, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659240540', 'COMPLEMENTARIO', NULL, 'Banco Piano', -34.599386, -58.437779, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659246269', 'COMPLEMENTARIO', NULL, 'Banco Santander', -34.600405, -58.437612, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659257017', 'COMPLEMENTARIO', NULL, 'BBVA', -34.600076, -58.437745, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659285696', 'COMPETENCIA', 'ALMACEN', 'Granja Santa María', -34.600252, -58.438568, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659285696', 'COMPETENCIA', 'KIOSCO', 'Granja Santa María', -34.600252, -58.438568, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659289208', 'COMPLEMENTARIO', NULL, 'Supermercado Diego', -34.598214, -58.435891, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1659289208', 'COMPETENCIA', 'ALMACEN', 'Supermercado Diego', -34.598214, -58.435891, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1663432796', 'COMPLEMENTARIO', NULL, 'Banco Nación', -34.597937, -58.440621, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1672859586', 'COMPETENCIA', 'KIOSCO', 'Maxikiosco Valentino', -34.599583, -58.438087, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1711860950', 'COMPLEMENTARIO', NULL, 'Dia', -34.599433, -58.437851, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1711860950', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.599433, -58.437851, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1989821068', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.598825, -58.439766, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1989821069', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.599055, -58.439518, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1989821070', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.598929, -58.439956, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1989821071', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.599186, -58.439652, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2025901951', 'TRANSPORTE', NULL, 'Camargo y Serrano', -34.597383, -58.443425, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2193739865', 'COMPLEMENTARIO', NULL, 'Banco Ciudad', -34.600522, -58.437332, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2193739872', 'COMPLEMENTARIO', NULL, 'Banco Patagonia', -34.598496, -58.440489, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2299645342', 'COMPLEMENTARIO', NULL, 'Banco Ciudad', -34.599143, -58.439367, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2299673347', 'OFICINA', NULL, 'Colombraro', -34.598797, -58.436761, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2299724276', 'TRANSPORTE', NULL, 'Aguirre y Scalabrini Ortiz', -34.597984, -58.43526, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2299724283', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortíz', -34.598692, -58.436317, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2299878021', 'OFICINA', NULL, 'Modem Propiedades', -34.597859, -58.435855, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2299878032', 'COMPETENCIA', 'ALMACEN', 'La cocina de Rosa', -34.598223, -58.438286, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2299878032', 'COMPETENCIA', 'KIOSCO', 'La cocina de Rosa', -34.598223, -58.438286, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2299878038', 'COMPETENCIA', 'ALMACEN', 'Dinastia Maisit', -34.598524, -58.439229, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2299878038', 'COMPETENCIA', 'KIOSCO', 'Dinastia Maisit', -34.598524, -58.439229, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2299878043', 'COMPLEMENTARIO', NULL, 'Farmacia Panorámica', -34.597383, -58.437501, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2301409892', 'COMPLEMENTARIO', NULL, 'Credicoop', -34.600263, -58.440447, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2483501638', 'COMPLEMENTARIO', NULL, 'Farmacia Cadefa', -34.601832, -58.442255, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2604945996', 'COMPLEMENTARIO', NULL, 'Supermercado Autoservicio "Juki"', -34.601037, -58.436038, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2604945996', 'COMPETENCIA', 'ALMACEN', 'Supermercado Autoservicio "Juki"', -34.601037, -58.436038, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2618564164', 'COMPETENCIA', 'ALMACEN', 'La Rosa de Villa Crespo', -34.598281, -58.435259, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2679839292', 'COMPLEMENTARIO', NULL, 'ICBC', -34.598441, -58.440553, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2679841178', 'OFICINA', NULL, 'Tarjeta Naranja', -34.596999, -58.442083, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2679854580', 'COMPLEMENTARIO', NULL, 'Diarco Barrio', -34.59925, -58.439683, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2679854580', 'COMPETENCIA', 'ALMACEN', 'Diarco Barrio', -34.59925, -58.439683, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2679860714', 'COMPLEMENTARIO', NULL, 'Carrefour Market', -34.596509, -58.442354, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2679860714', 'COMPETENCIA', 'ALMACEN', 'Carrefour Market', -34.596509, -58.442354, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2872814701', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortíz', -34.598737, -58.436364, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2872814901', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Ramírez de Velasco', -34.59861, -58.436807, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2892599799', 'TRANSPORTE', NULL, 'Camargo y Serrano', -34.597424, -58.443481, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2909399764', 'TRANSPORTE', NULL, 'Malabia y Corrientes', -34.598835, -58.439612, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3134580680', 'TRANSPORTE', NULL, 'Malabia y Corrientes', -34.598875, -58.439586, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3134580681', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Ramírez de Velasco', -34.598763, -58.437047, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3169552759', 'TRANSPORTE', NULL, 'Acevedo y Corrientes', -34.5983, -58.440787, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3194505199', 'COMPETENCIA', 'ALMACEN', 'Natur', -34.597493, -58.43766, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3194505199', 'COMPETENCIA', 'KIOSCO', 'Natur', -34.597493, -58.43766, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3232383761', 'TRANSPORTE', NULL, 'Acevedo y Corrientes', -34.598237, -58.440865, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3237430904', 'TRANSPORTE', NULL, 'Aguirre y Scalabrini Ortiz', -34.598015, -58.435313, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3261087671', 'TRANSPORTE', NULL, 'Warnes y Scalabrini Ortiz', -34.602082, -58.442839, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3261087672', 'TRANSPORTE', NULL, 'Warnes y Scalabrini Ortiz', -34.602137, -58.442868, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3261106257', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortiz', -34.599384, -58.439312, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3261106258', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortiz', -34.599445, -58.439388, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531435', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortiz', -34.599616, -58.439061, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531436', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortiz', -34.599682, -58.439137, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531444', 'TRANSPORTE', NULL, 'Corrientes y Gurruchaga', -34.596944, -58.441935, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531445', 'TRANSPORTE', NULL, 'Corrientes y Gurruchaga', -34.597034, -58.441991, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531446', 'TRANSPORTE', NULL, 'Corrientes y Gurruchaga', -34.597064, -58.441802, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531447', 'TRANSPORTE', NULL, 'Corrientes y Gurruchaga', -34.597125, -58.44189, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531448', 'TRANSPORTE', NULL, 'Drago y Julián Álvarez', -34.601289, -58.437388, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3267531449', 'TRANSPORTE', NULL, 'Drago y Julián Álvarez', -34.60134, -58.437454, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058868', 'TRANSPORTE', NULL, 'Avenida Corrientes', -34.599628, -58.438572, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058869', 'TRANSPORTE', NULL, 'Avenida Corrientes', -34.599707, -58.438503, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058873', 'TRANSPORTE', NULL, 'Marechal y Gandhi', -34.603464, -58.440762, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058874', 'TRANSPORTE', NULL, 'Marechal y Gandhi', -34.603488, -58.440803, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058881', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Ramírez de Velasco', -34.598195, -58.436332, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058882', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Ramírez de Velasco', -34.598273, -58.436277, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058891', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Warnes', -34.601681, -58.441848, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365058892', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Warnes', -34.601715, -58.441806, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365065893', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Warnes', -34.601813, -58.442091, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3365065894', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Warnes', -34.601851, -58.442075, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3407636974', 'TRANSPORTE', NULL, 'Malabia - Osvaldo Pugliese', -34.599025, -58.439675, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436433997', 'TRANSPORTE', NULL, 'Camargo y Acevedo', -34.599135, -58.441571, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436433998', 'TRANSPORTE', NULL, 'Camargo y Acevedo', -34.599092, -58.441523, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436433999', 'TRANSPORTE', NULL, 'Camargo y Scalabrini Ortiz', -34.60077, -58.439809, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436434000', 'TRANSPORTE', NULL, 'Camargo y Scalabrini Ortiz', -34.600728, -58.439761, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436435493', 'TRANSPORTE', NULL, 'Camargo y Lavalleja', -34.603162, -58.437616, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3436435494', 'TRANSPORTE', NULL, 'Camargo y Lavalleja', -34.603124, -58.437578, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822152842', 'TRANSPORTE', NULL, 'S. Ortiz y Corrientes', -34.600314, -58.439296, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822153427', 'TRANSPORTE', NULL, 'S. Ortiz y Corrientes', -34.600059, -58.438885, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822153457', 'TRANSPORTE', NULL, 'S. Ortiz y Corrientes', -34.600222, -58.43914, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822177714', 'TRANSPORTE', NULL, 'Malabia y Corrientes', -34.599124, -58.440025, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822177715', 'TRANSPORTE', NULL, 'Malabia y Corrientes', -34.599166, -58.439991, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822177719', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortiz', -34.599576, -58.439105, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822177720', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortiz', -34.599643, -58.439176, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822177721', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortiz', -34.599448, -58.439243, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822177722', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortiz', -34.599512, -58.439316, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822177736', 'TRANSPORTE', NULL, 'S. Ortiz y Corrientes', -34.599985, -58.438947, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822177737', 'TRANSPORTE', NULL, 'S. Ortiz y Corrientes', -34.600147, -58.439213, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822177738', 'TRANSPORTE', NULL, 'S. Ortiz y Corrientes', -34.600237, -58.439362, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822177752', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Ramírez de Velasco', -34.598677, -58.436912, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3822177753', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Ramírez de Velasco', -34.598749, -58.436842, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645793', 'TRANSPORTE', NULL, 'Muñecas y Gurruchaga', -34.600618, -58.445596, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645795', 'TRANSPORTE', NULL, 'Acoyte y Apolinario Figueroa', -34.602926, -58.443232, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3836645796', 'TRANSPORTE', NULL, 'Acoyte y Apolinario Figueroa', -34.602954, -58.443271, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4131908759', 'TRANSPORTE', NULL, 'Corrientes y Julián Álvarez', -34.600519, -58.436781, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4131908760', 'TRANSPORTE', NULL, 'Corrientes y Julián Álvarez', -34.600611, -58.436811, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4131908761', 'TRANSPORTE', NULL, 'Julián Álvarez y Ramírez de Velasco', -34.599905, -58.435071, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4131908762', 'TRANSPORTE', NULL, 'Julián Álvarez y Ramírez de Velasco', -34.599951, -58.435039, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4214670717', 'OFICINA', NULL, 'Dirección Nacional de Registros de Propiedad Automotor', -34.596133, -58.443195, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4214670723', 'COMPLEMENTARIO', NULL, 'Macro', -34.596732, -58.441953, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4214670726', 'OFICINA', NULL, 'Migliorisi', -34.597472, -58.44163, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4311916826', 'COMPLEMENTARIO', NULL, 'Dia Market', -34.59789, -58.442267, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4311916826', 'COMPETENCIA', 'ALMACEN', 'Dia Market', -34.59789, -58.442267, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4333695042', 'COMPLEMENTARIO', NULL, 'Súper Pampero', -34.595926, -58.440818, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4333695042', 'COMPETENCIA', 'ALMACEN', 'Súper Pampero', -34.595926, -58.440818, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4333696348', 'COMPETENCIA', 'ALMACEN', 'Gusto & Piacere', -34.597609, -58.439124, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4333696349', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.597554, -58.439197, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4333982568', 'COMPETENCIA', 'PAPELERIA', 'Staples', -34.599541, -58.438001, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4502995689', 'COMPETENCIA', 'ALMACEN', 'La Catalana', -34.596856, -58.442246, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4682611606', 'TRANSPORTE', NULL, 'Malabia y Warnes', -34.601755, -58.443174, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4682611607', 'TRANSPORTE', NULL, 'Malabia y Warnes', -34.601727, -58.443209, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4709301020', 'COMPLEMENTARIO', NULL, 'Farmacia Pami', -34.598853, -58.436863, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4709385385', 'OFICINA', NULL, 'E Bertolotti', -34.599686, -58.438764, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4858736103', 'COMPLEMENTARIO', NULL, 'La Santé', -34.599459, -58.438402, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4871207942', 'TRANSPORTE', NULL, 'Serrano y Corrientes', -34.596658, -58.442957, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4871207943', 'TRANSPORTE', NULL, 'Serrano y Corrientes', -34.596696, -58.44291, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4871207944', 'TRANSPORTE', NULL, 'Malabia - Osvaldo Pugliese', -34.599034, -58.439693, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4871207945', 'TRANSPORTE', NULL, 'Malabia y Corrientes', -34.598792, -58.439456, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4871207946', 'TRANSPORTE', NULL, 'Malabia y Corrientes', -34.598747, -58.439491, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4871207947', 'TRANSPORTE', NULL, 'Avenida Corrientes', -34.599583, -58.43831, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4871207948', 'TRANSPORTE', NULL, 'Avenida Corrientes', -34.599504, -58.438384, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4919955422', 'OFICINA', NULL, 'Sociedad Argentina de Hematologia', -34.602656, -58.439409, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4935690921', 'COMPETENCIA', 'ALMACEN', 'San Carmelo', -34.60015, -58.442509, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5093760998', 'COMPETENCIA', 'ALMACEN', 'Cartu 724', -34.595662, -58.439365, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5093760998', 'COMPETENCIA', 'KIOSCO', 'Cartu 724', -34.595662, -58.439365, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5093760999', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.596122, -58.440062, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5093761000', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.596133, -58.440088, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5093761002', 'COMPLEMENTARIO', NULL, 'Briut', -34.595777, -58.439489, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5093761003', 'COMPETENCIA', 'KIOSCO', 'Maxi Kiosco', -34.595683, -58.439359, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5114013953', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado A (EPCjs 02/07°)', -34.598653, -58.441895, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5114013954', 'ESCUELA', NULL, 'Conservatorio Superior de Música Manuel de Falla - Anexo 4', -34.598626, -58.441931, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5114021157', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado A (EPCjc 17/07°)', -34.601784, -58.438187, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5114021158', 'ESCUELA', NULL, 'Centro Educativo de Nivel Secundario 33 Leopoldo Marechal', -34.601837, -58.438225, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5116062581', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado A (EPCjc 01/07°) [Sede]', -34.598819, -58.440188, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5116062582', 'ESCUELA', NULL, 'Escuela Primaria p/Adultos 01 Tomasa de la Quintana de Escalada', -34.59877, -58.440245, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117379903', 'ESCUELA', NULL, 'Instituto Educativo Bereshit', -34.595278, -58.438819, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117406411', 'ESCUELA', NULL, 'Centro de Formación Profesional - ADEF', -34.599113, -58.440178, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117413154', 'ESCUELA', NULL, 'Colegio Paideia', -34.596767, -58.438792, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117419998', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado A (EPCjs 22/07°)', -34.601038, -58.439117, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117420020', 'ESCUELA', NULL, 'Escuela Terciaria de Estudios Radiofónicos', -34.599218, -58.442288, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117420429', 'ESCUELA', NULL, 'Escuela de Comercio 16 Gabriela Mistral', -34.602895, -58.439313, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117420447', 'ESCUELA', NULL, 'Instituto Doctor Herzl', -34.599157, -58.442198, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117441257', 'ESCUELA', NULL, 'Instituto de Formación Superior SEDEBA', -34.598995, -58.439482, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117441292', 'ESCUELA', NULL, 'Instituto de Formación Técnica Superior 20', -34.595467, -58.438584, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117441317', 'ESCUELA', NULL, 'Escuela Infantil Magic Room', -34.595823, -58.438927, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117565852', 'ESCUELA', NULL, 'Colegio Buenos Aires S.R.L.', -34.598759, -58.441286, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117565939', 'ESCUELA', NULL, 'Escuela Primaria Común 02 Australia', -34.595448, -58.43859, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117591993', 'ESCUELA', NULL, 'Escuela Primaria Común 22 Doctor Rómulo S. Naón', -34.601019, -58.439125, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117619862', 'ESCUELA', NULL, 'Instituto Educativo Alas', -34.601817, -58.436088, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117619863', 'ESCUELA', NULL, 'Instituto Sarmiento', -34.602414, -58.436985, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117619866', 'ESCUELA', NULL, 'Escuela Cooperativa Mundo Nuevo', -34.595989, -58.441702, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117619887', 'ESCUELA', NULL, 'Jardín de Infantes Común 03/09° Leopoldo Marechal', -34.595493, -58.438582, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5212462421', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Warnes', -34.602379, -58.442662, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5212462422', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Warnes', -34.602416, -58.442715, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5240262956', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Warnes', -34.602338, -58.442697, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5240262957', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Warnes', -34.602374, -58.442748, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451181', 'TRANSPORTE', NULL, 'Camargo y Acevedo', -34.599255, -58.441435, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451182', 'TRANSPORTE', NULL, 'Camargo y Acevedo', -34.599215, -58.441384, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451184', 'TRANSPORTE', NULL, 'Camargo y Scalabrini Ortiz', -34.600952, -58.439644, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451185', 'TRANSPORTE', NULL, 'Camargo y Scalabrini Ortiz', -34.600913, -58.439605, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451186', 'TRANSPORTE', NULL, 'Camargo y Julián Álvarez', -34.602383, -58.438315, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5397451187', 'TRANSPORTE', NULL, 'Camargo y Julián Álvarez', -34.602356, -58.438271, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5732949810', 'TRANSPORTE', NULL, 'Malabia y Warnes', -34.601676, -58.443073, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5732949811', 'TRANSPORTE', NULL, 'Malabia y Warnes', -34.601643, -58.443104, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5732949812', 'TRANSPORTE', NULL, 'Malabia y Murillo', -34.601356, -58.442686, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5732949813', 'TRANSPORTE', NULL, 'Malabia y Murillo', -34.601317, -58.442729, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5732949815', 'TRANSPORTE', NULL, 'Malabia y Padilla', -34.60054, -58.441652, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5732949816', 'TRANSPORTE', NULL, 'Malabia y Padilla', -34.600501, -58.441699, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5732949817', 'TRANSPORTE', NULL, 'Malabia y Padilla', -34.600488, -58.441589, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5732949818', 'TRANSPORTE', NULL, 'Malabia y Padilla', -34.600456, -58.441633, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5750292421', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.600667, -58.441981, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5757428355', 'COMPETENCIA', 'ALMACEN', 'Natgen', -34.596919, -58.438229, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5757428355', 'COMPETENCIA', 'KIOSCO', 'Natgen', -34.596919, -58.438229, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6005797829', 'OFICINA', NULL, 'Lugar sin nombre', -34.598493, -58.441341, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6005803977', 'COMPETENCIA', 'ALMACEN', 'Santa María', -34.601323, -58.439159, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6123250844', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.603853, -58.439194, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6123250845', 'TRANSPORTE', NULL, 'Lavalleja y Camargo', -34.60321, -58.437919, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6123250846', 'TRANSPORTE', NULL, 'Lavalleja y Vera', -34.601766, -58.43565, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6123250847', 'TRANSPORTE', NULL, 'Lavalleja y Vera', -34.601692, -58.435553, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6123250848', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.601475, -58.435485, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6565448042', 'TRANSPORTE', NULL, 'Lavalleja y Vera', -34.601715, -58.435688, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6614169178', 'TRANSPORTE', NULL, 'Lavalleja y Camargo', -34.603165, -58.437952, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6737859136', 'TRANSPORTE', NULL, 'Serrano y Padilla', -34.598157, -58.444805, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6737859137', 'TRANSPORTE', NULL, 'Serrano y Padilla', -34.598196, -58.44476, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6737859138', 'TRANSPORTE', NULL, 'Murillo y Serrano', -34.599021, -58.445562, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6737859139', 'TRANSPORTE', NULL, 'Murillo y Serrano', -34.598984, -58.445514, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6737859143', 'TRANSPORTE', NULL, 'Murillo y Gurruchaga', -34.599929, -58.444546, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6737859144', 'TRANSPORTE', NULL, 'Murillo y Gurruchaga', -34.599905, -58.444514, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6737859145', 'TRANSPORTE', NULL, 'Warnes y Scalabrini Ortíz', -34.602364, -58.442138, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6737859146', 'TRANSPORTE', NULL, 'Warnes y Scalabrini Ortíz', -34.602279, -58.442097, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6737859153', 'TRANSPORTE', NULL, 'Plazoleta Baron Hirsch', -34.603277, -58.439886, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6737859154', 'TRANSPORTE', NULL, 'Plazoleta Baron Hirsch', -34.603198, -58.439857, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6748864852', 'TRANSPORTE', NULL, 'Ramírez de Velazco y Aráoz', -34.599363, -58.435775, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6748864853', 'TRANSPORTE', NULL, 'Ramírez de Velazco y Aráoz', -34.599334, -58.435731, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6765175701', 'TRANSPORTE', NULL, 'Drago y Lavalleja', -34.601974, -58.436709, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6765175702', 'TRANSPORTE', NULL, 'Drago y Lavalleja', -34.60201, -58.436764, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7178440655', 'TRANSPORTE', NULL, 'Gurruchaga y Muñecas', -34.600403, -58.445534, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7178440656', 'TRANSPORTE', NULL, 'Gurruchaga y Muñecas', -34.600362, -58.445585, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7178440657', 'TRANSPORTE', NULL, 'Murillo y Acevedo', -34.600858, -58.443505, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7178440658', 'TRANSPORTE', NULL, 'Murillo y Acevedo', -34.600829, -58.443468, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7236235098', 'TRANSPORTE', NULL, 'Lavalleja y Vera', -34.601652, -58.435592, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8268761983', 'TRANSPORTE', NULL, 'Ramírez de Velasco y Acevedo', -34.59674, -58.438143, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8268761984', 'TRANSPORTE', NULL, 'Ramírez de Velasco y Acevedo', -34.596804, -58.438088, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8268761985', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortíz', -34.598769, -58.436242, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8268761986', 'TRANSPORTE', NULL, 'Julián Álvarez y Ramírez de Velasco', -34.599883, -58.435035, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9731335291', 'COMPLEMENTARIO', NULL, 'Bera', -34.599702, -58.443082, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9731372978', 'COMPETENCIA', 'ALMACEN', 'Buenas Costumbres', -34.600146, -58.435521, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9731399386', 'COMPLEMENTARIO', NULL, 'Farmacity', -34.599518, -58.439365, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9731401377', 'OFICINA', NULL, 'Experta ART', -34.597229, -58.441861, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9731402531', 'COMPLEMENTARIO', NULL, 'Anco', -34.596801, -58.441794, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9731408821', 'COMPETENCIA', 'ALMACEN', 'Mica', -34.599089, -58.443747, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733897237', 'TRANSPORTE', NULL, 'Acevedo y Padilla', -34.599926, -58.442951, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733897238', 'TRANSPORTE', NULL, 'Acevedo y Padilla', -34.599956, -58.442915, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733897244', 'TRANSPORTE', NULL, 'Corrientes y Gurruchaga', -34.5968, -58.442089, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733897245', 'TRANSPORTE', NULL, 'Corrientes y Gurruchaga', -34.596865, -58.442161, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733897249', 'TRANSPORTE', NULL, 'Corrientes y Drago', -34.600397, -58.437148, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733897250', 'TRANSPORTE', NULL, 'Corrientes y Drago', -34.600481, -58.437181, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733897251', 'TRANSPORTE', NULL, 'Corrientes y Julián Álvarez', -34.600645, -58.436703, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733897252', 'TRANSPORTE', NULL, 'Corrientes y Julián Álvarez', -34.600557, -58.436667, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9733897253', 'TRANSPORTE', NULL, 'Julián Álvarez y Ramírez de Velasco', -34.599925, -58.434998, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11126718829', 'TRANSPORTE', NULL, 'Malabia y Ramírez de Velasco', -34.597458, -58.437369, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11126718830', 'TRANSPORTE', NULL, 'Malabia y Ramírez de Velasco', -34.597416, -58.43741, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11155177895', 'COMPETENCIA', 'ALMACEN', 'Maxikiosco', -34.598429, -58.436819, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11155177895', 'COMPETENCIA', 'KIOSCO', 'Maxikiosco', -34.598429, -58.436819, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11467995599', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Warnes', -34.602304, -58.442645, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11467995600', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Warnes', -34.602345, -58.442609, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11747123277', 'TRANSPORTE', NULL, 'Acevedo y Acoyte', -34.601208, -58.444524, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11747123278', 'TRANSPORTE', NULL, 'Acevedo y Acoyte', -34.601244, -58.444491, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12344807002', 'COMPLEMENTARIO', NULL, 'Dia', -34.600928, -58.435324, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12344807002', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.600928, -58.435324, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12344808301', 'COMPETENCIA', 'ALMACEN', 'Dalla Terra', -34.600776, -58.43579, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12344819201', 'COMPETENCIA', 'ALMACEN', 'Vera', -34.599738, -58.436898, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12366190203', 'COMPETENCIA', 'ALMACEN', 'Autoservicio JE', -34.599008, -58.436225, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12999500201', 'COMPLEMENTARIO', NULL, 'Dia', -34.600273, -58.43966, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12999500201', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.600273, -58.43966, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12999760350', 'OFICINA', NULL, 'REMAX', -34.600759, -58.439644, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12999760352', 'COMPLEMENTARIO', NULL, 'FarmaPlus', -34.59941, -58.4395, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13083634599', 'TRANSPORTE', NULL, 'Ramírez de Velasco y Acevedo', -34.595125, -58.439737, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13083634600', 'TRANSPORTE', NULL, 'Ramírez de Velasco y Acevedo', -34.596791, -58.438185, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13119518727', 'TRANSPORTE', NULL, 'Avenida Scalabrini Ortiz', -34.598803, -58.436303, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13119522762', 'TRANSPORTE', NULL, 'Ramirez de Velasco y Acevedo', -34.59684, -58.438139, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13606903198', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.600593, -58.439637, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:209137992', 'PARQUE', NULL, 'Lugar sin nombre', -34.600981, -58.435936, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:272982123', 'ESCUELA', NULL, 'Escuela Primaria Común 17 Francisco de Vitoria', -34.601678, -58.438293, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:333661073', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.599065, -58.439716, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:333661080', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.599015, -58.439646, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:431991177', 'ESCUELA', NULL, 'Escuela Primaria Común 02 Francisco Desiderio Herrera', -34.598525, -58.441766, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477615566', 'COMPLEMENTARIO', NULL, 'Simplicity', -34.600135, -58.438763, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477617847', 'COMPLEMENTARIO', NULL, 'Supervielle', -34.600435, -58.439218, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477621649', 'COMPLEMENTARIO', NULL, 'Coto', -34.601504, -58.442437, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477621649', 'COMPETENCIA', 'ALMACEN', 'Coto', -34.601504, -58.442437, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477621651', 'COMPLEMENTARIO', NULL, 'Coto', -34.601402, -58.441824, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477621651', 'COMPETENCIA', 'ALMACEN', 'Coto', -34.601402, -58.441824, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:717444513', 'PARQUE', NULL, 'Plazoleta Baron Hirsch', -34.603174, -58.439804, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1203016772', 'PARQUE', NULL, 'Lugar sin nombre', -34.601224, -58.435552, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1264381136', 'PARQUE', NULL, 'Plazoleta Salvador Benedit', -34.602062, -58.442562, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1533932371', 'ESCUELA', NULL, 'Scholem Aleijem', -34.59815, -58.443798, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1566512936', 'ESCUELA', NULL, 'Escuela Primaria Común 01 Tomasa de la Quintana de Escalada', -34.59899, -58.44049, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:2539189', 'TRANSPORTE', NULL, 'Estación Malabia - Osvaldo Pugliese', -34.59904, -58.439681, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6972787', 'TRANSPORTE', NULL, 'Malabia y Warnes', -34.601741, -58.443192, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7272612', 'TRANSPORTE', NULL, 'Avenida Corrientes', -34.596677, -58.442933, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7272613', 'TRANSPORTE', NULL, 'Avenida Corrientes', -34.598957, -58.439741, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7272614', 'TRANSPORTE', NULL, 'Avenida Corrientes', -34.599606, -58.438441, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7748404', 'TRANSPORTE', NULL, 'Scalabrini Ortiz y Warnes', -34.602377, -58.442705, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8000302', 'TRANSPORTE', NULL, 'Camargo y Acevedo', -34.599174, -58.441477, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8000303', 'TRANSPORTE', NULL, 'Camargo y Julián Álvarez', -34.602369, -58.438293, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8421463', 'TRANSPORTE', NULL, 'Malabia y Murillo', -34.601337, -58.442707, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8421464', 'TRANSPORTE', NULL, 'Malabia y Padilla', -34.600498, -58.441644, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9957064', 'TRANSPORTE', NULL, 'Murillo y Serrano', -34.599003, -58.445538, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9957065', 'TRANSPORTE', NULL, 'Murillo y Gurruchaga', -34.599917, -58.44453, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9957066', 'TRANSPORTE', NULL, 'Warnes y Scalabrini Ortíz', -34.602321, -58.442117, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9957067', 'TRANSPORTE', NULL, 'Plazoleta Barón Hirsch', -34.603238, -58.439872, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9969611', 'TRANSPORTE', NULL, 'Ramírez de Velazco Aráoz', -34.599349, -58.435753, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9991295', 'TRANSPORTE', NULL, 'Drago y Lavalleja', -34.601992, -58.436736, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10662536', 'TRANSPORTE', NULL, 'Gurruchaga y Muñecas', -34.600383, -58.44556, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10662537', 'TRANSPORTE', NULL, 'Murillo y Acevedo', -34.600844, -58.443487, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10743587', 'TRANSPORTE', NULL, 'Lavalleja y Vera', -34.601672, -58.435573, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14130767', 'TRANSPORTE', NULL, 'Acevedo y Padilla', -34.599941, -58.442933, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:16218631', 'TRANSPORTE', NULL, 'Malabia y Ramírez de Velasco', -34.597437, -58.43739, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:17365035', 'TRANSPORTE', NULL, 'Acevedo y Acoyte', -34.601226, -58.444507, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 27, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 10, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 167, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 4, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 0, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 35, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 35, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 1, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 11, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa Crespo Sur' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:256249010', 'TRANSPORTE', NULL, 'Villa del Parque', -34.601485, -58.493975, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1228764024', 'COMPLEMENTARIO', NULL, 'Banco Provincia', -34.604279, -58.492925, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1228764028', 'COMPLEMENTARIO', NULL, 'Banco Nación', -34.602434, -58.494276, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1228764030', 'COMPLEMENTARIO', NULL, 'Comafi', -34.603465, -58.492679, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1228764039', 'COMPLEMENTARIO', NULL, 'Banco Ciudad', -34.603694, -58.493069, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1228764041', 'COMPLEMENTARIO', NULL, 'Banco Nación', -34.605884, -58.491902, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1231804487', 'COMPLEMENTARIO', NULL, 'Acuario', -34.599895, -58.492384, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1231804493', 'COMPLEMENTARIO', NULL, 'Farmacity', -34.601045, -58.49534, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1235275587', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado E (EPCjs 02/17°) Juana Manso', -34.599602, -58.494899, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1369432524', 'COMPLEMENTARIO', NULL, 'Carrefour Market', -34.602053, -58.489226, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1369432524', 'COMPETENCIA', 'ALMACEN', 'Carrefour Market', -34.602053, -58.489226, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1369432528', 'COMPLEMENTARIO', NULL, 'Daneri', -34.607632, -58.492241, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1369432529', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.601025, -58.494512, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1369432529', 'COMPETENCIA', 'KIOSCO', 'Carrefour Express', -34.601025, -58.494512, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1369432534', 'ESCUELA', NULL, 'Escuela Primaria Común 01 Doctor Antonio Dellepiane', -34.604214, -58.492338, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1492110367', 'ESCUELA', NULL, 'Instituto San José de Villa del Parque', -34.600795, -58.494686, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2132473187', 'TRANSPORTE', NULL, 'Villa del Parque', -34.601325, -58.494002, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2623920748', 'TRANSPORTE', NULL, 'Villa del Parque', -34.601358, -58.493994, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2623920749', 'TRANSPORTE', NULL, 'Villa del Parque', -34.601452, -58.493983, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3451071478', 'TRANSPORTE', NULL, 'Villa del Parque', -34.601404, -58.494074, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3593731483', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado A (EPCjc 03/17°) Alicia Moreau de Justo', -34.603503, -58.489487, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3799992152', 'COMPLEMENTARIO', NULL, 'Luna', -34.606437, -58.490926, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3799992152', 'COMPETENCIA', 'ALMACEN', 'Luna', -34.606437, -58.490926, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3799992156', 'ESCUELA', NULL, 'Instituto Santísima Virgen Niña', -34.605089, -58.492156, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3800000761', 'COMPLEMENTARIO', NULL, 'supermercado chino', -34.604547, -58.493061, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3800000761', 'COMPETENCIA', 'ALMACEN', 'supermercado chino', -34.604547, -58.493061, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3800000762', 'COMPLEMENTARIO', NULL, 'Farmacity', -34.602682, -58.49416, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3800000764', 'COMPLEMENTARIO', NULL, 'Dia', -34.604206, -58.493796, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3800000764', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.604206, -58.493796, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3808483629', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.602405, -58.49421, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3808561911', 'COMPLEMENTARIO', NULL, 'BBVA', -34.604241, -58.493975, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4517239399', 'OFICINA', NULL, 'Turismo Springfield', -34.600997, -58.496527, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4660716799', 'COMPLEMENTARIO', NULL, 'Dia', -34.602604, -58.495044, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4660716799', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.602604, -58.495044, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4661078171', 'OFICINA', NULL, 'Estudio Juridico', -34.604907, -58.490358, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4661078177', 'OFICINA', NULL, 'Rey Inmobiliaria', -34.603087, -58.491894, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4661097718', 'OFICINA', NULL, 'Susana Farina', -34.602822, -58.492109, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4675229802', 'COMPETENCIA', 'ALMACEN', 'JR', -34.603482, -58.488387, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4675229802', 'COMPETENCIA', 'KIOSCO', 'JR', -34.603482, -58.488387, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391441', 'TRANSPORTE', NULL, 'Avenida Nazca', -34.601834, -58.489998, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391442', 'TRANSPORTE', NULL, 'Nogoyá y Nazca', -34.601876, -58.489963, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391444', 'TRANSPORTE', NULL, 'Nogoyá y Cuenca', -34.603936, -58.493608, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391445', 'TRANSPORTE', NULL, 'Nogoyá y Cuenca', -34.603979, -58.493578, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391446', 'TRANSPORTE', NULL, 'Nogoyá y Llavallol', -34.605169, -58.495759, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4819391447', 'TRANSPORTE', NULL, 'Nogoyá y Llavallol', -34.605208, -58.495725, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376546', 'TRANSPORTE', NULL, 'Melincué y Concordia', -34.605196, -58.497908, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376547', 'TRANSPORTE', NULL, 'Melincué y Concordia', -34.605153, -58.497947, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376548', 'TRANSPORTE', NULL, 'Melincué y Llavallol', -34.604132, -58.496148, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376549', 'TRANSPORTE', NULL, 'Melincué y Llavallol', -34.604095, -58.496179, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376550', 'TRANSPORTE', NULL, 'Melincué y Cuenca', -34.602819, -58.493955, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376551', 'TRANSPORTE', NULL, 'Melincué y Cuenca', -34.602782, -58.493992, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376552', 'TRANSPORTE', NULL, 'Nogoyá y Helguera', -34.602921, -58.49189, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376553', 'TRANSPORTE', NULL, 'Nogoyá y Helguera', -34.602894, -58.491909, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376554', 'TRANSPORTE', NULL, 'Nogoyá y Nazca', -34.601984, -58.490261, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4826376555', 'TRANSPORTE', NULL, 'Nogoyá y Nazca', -34.601938, -58.4903, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4860969369', 'TRANSPORTE', NULL, 'Estación Villa del Parque', -34.601717, -58.493701, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4860969370', 'TRANSPORTE', NULL, 'Estación Villa del Parque', -34.601685, -58.493705, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4860969371', 'TRANSPORTE', NULL, 'Helguera y Nogoyá', -34.60319, -58.491986, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4860969372', 'TRANSPORTE', NULL, 'Helguera y Nogoyá', -34.603161, -58.491945, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4860969373', 'TRANSPORTE', NULL, 'Helguera y Sastre', -34.604992, -58.490429, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4860969374', 'TRANSPORTE', NULL, 'Helguera y Sastre', -34.604964, -58.490396, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5116071178', 'ESCUELA', NULL, 'Escuela Primaria Común 03 Policía Federal Argentina', -34.603492, -58.489497, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5116071179', 'ESCUELA', NULL, 'Escuela Primaria Común 02 Prof. Juan José Millan', -34.599638, -58.494866, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5116071180', 'ESCUELA', NULL, 'Escuela de Música 03', -34.604258, -58.49227, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117390663', 'ESCUELA', NULL, 'Jardín Andy Panda', -34.602618, -58.493597, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117401519', 'ESCUELA', NULL, 'Colegio del Trébol', -34.603679, -58.490681, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117405294', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado A (EPCjc 01/17°) Alicia Moreau de Justo', -34.604289, -58.492332, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117621868', 'ESCUELA', NULL, 'Instituto Schiller', -34.600941, -58.493543, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6625251981', 'COMPLEMENTARIO', NULL, 'FarmaPlus', -34.600783, -58.495884, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6828139562', 'COMPETENCIA', 'ALMACEN', 'Xin Yuan', -34.60505, -58.497877, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6828139562', 'COMPETENCIA', 'KIOSCO', 'Xin Yuan', -34.60505, -58.497877, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6831943026', 'TRANSPORTE', NULL, 'Nazarre y Lavallol', -34.603449, -58.496695, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6831943027', 'TRANSPORTE', NULL, 'Nazarre y Llavallol', -34.60342, -58.496722, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6831943029', 'TRANSPORTE', NULL, 'Estación Villa del Parque', -34.602154, -58.494524, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6831943030', 'TRANSPORTE', NULL, 'Estación Villa del Parque', -34.602111, -58.49456, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6913840520', 'PARQUE', NULL, 'Lugar sin nombre', -34.601484, -58.491708, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845645046', 'TRANSPORTE', NULL, 'Sastre y Nazca', -34.603602, -58.488374, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845645047', 'TRANSPORTE', NULL, 'Sastre y Nazca', -34.60364, -58.488341, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845645050', 'TRANSPORTE', NULL, 'Plaza Del Valle', -34.605709, -58.491992, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845645051', 'TRANSPORTE', NULL, 'Plaza Del Valle', -34.605751, -58.491962, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845645056', 'TRANSPORTE', NULL, 'Sastre y Vilardebo', -34.607545, -58.495151, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845645057', 'TRANSPORTE', NULL, 'Sastre y Vilardebo', -34.607581, -58.49512, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662420', 'TRANSPORTE', NULL, 'Cuenca y Nogoyá', -34.603613, -58.493683, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662421', 'TRANSPORTE', NULL, 'Cuenca y Nogoyá', -34.603578, -58.493637, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662422', 'TRANSPORTE', NULL, 'Cuenca y Nogoyá', -34.603685, -58.493555, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662423', 'TRANSPORTE', NULL, 'Cuenca y Nogoyá', -34.603709, -58.493613, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662424', 'TRANSPORTE', NULL, 'Helguera y Nogoyá', -34.603185, -58.491919, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662425', 'TRANSPORTE', NULL, 'Helguera y Nogoyá', -34.603222, -58.491957, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662426', 'TRANSPORTE', NULL, 'Helguera y Sastre', -34.604955, -58.490461, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662427', 'TRANSPORTE', NULL, 'Helguera y Sastre', -34.604933, -58.490422, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662428', 'TRANSPORTE', NULL, 'Nazca y Sastre', -34.603761, -58.487928, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662429', 'TRANSPORTE', NULL, 'Nazca y Sastre', -34.603709, -58.487842, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662431', 'TRANSPORTE', NULL, 'Sastre y Helguera', -34.604935, -58.490665, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9845662432', 'TRANSPORTE', NULL, 'Sastre y Helguera', -34.604969, -58.490636, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11178068758', 'COMPETENCIA', 'ALMACEN', 'La Estación', -34.601298, -58.495078, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11464592213', 'SALUD', NULL, 'Hospital Italiano', -34.606015, -58.491727, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11464596497', 'COMPETENCIA', 'ALMACEN', 'M24', -34.605657, -58.491668, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11464596497', 'COMPETENCIA', 'KIOSCO', 'M24', -34.605657, -58.491668, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11467773186', 'TRANSPORTE', NULL, 'Plaza Aristóbulo del Valle', -34.605401, -58.493866, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11467773187', 'TRANSPORTE', NULL, 'Plaza Aristóbulo del Valle', -34.605363, -58.493897, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11467773758', 'TRANSPORTE', NULL, 'Estación Villa del Parque', -34.601749, -58.49541, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11467773759', 'TRANSPORTE', NULL, 'Estación Villa del Parque', -34.601702, -58.495414, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11467773760', 'TRANSPORTE', NULL, 'Estación Villa del Parque', -34.601694, -58.495296, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11467773761', 'TRANSPORTE', NULL, 'Estación Villa del Parque', -34.601738, -58.495292, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11467995621', 'TRANSPORTE', NULL, 'Plaza Aristóbulo del Valle', -34.60546, -58.492006, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11467995623', 'TRANSPORTE', NULL, 'Plaza Aristóbulo del Valle', -34.605494, -58.492063, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:19207420', 'PARQUE', NULL, 'Plaza Aristóbulo Del Valle', -34.605546, -58.49294, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:71865200', 'PARQUE', NULL, 'Lugar sin nombre', -34.600822, -58.490559, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:107489035', 'PARQUE', NULL, 'Plazoleta Jorge N. Williams', -34.600834, -58.494054, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:315932735', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.601271, -58.493755, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:315932736', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.601376, -58.493663, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:315932738', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.601477, -58.493498, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:412588595', 'PARQUE', NULL, 'Lugar sin nombre', -34.60079, -58.494018, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471907120', 'SALUD', NULL, 'Centro Medico Helguera', -34.605402, -58.490461, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471907122', 'COMPETENCIA', 'ALMACEN', 'La Esquina de la Medialuna', -34.604841, -58.490784, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471916626', 'OFICINA', NULL, 'London House', -34.603003, -58.4923, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471922160', 'COMPLEMENTARIO', NULL, 'Macro', -34.602856, -58.495259, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471959406', 'ESCUELA', NULL, 'Escuela Primaria Común 10 Jose Ernesto Galloni', -34.606622, -58.488658, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471961193', 'COMPLEMENTARIO', NULL, 'Banco Santander', -34.602269, -58.495101, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:471963520', 'COMPETENCIA', 'ALMACEN', 'La Nueva Muguet', -34.602703, -58.495772, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:473405402', 'ESCUELA', NULL, 'Jardín Materno Infantil Mi Mundo y Yo', -34.604518, -58.48757, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:473405403', 'OFICINA', NULL, 'Miotto', -34.604307, -58.487716, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:473405404', 'OFICINA', NULL, 'González', -34.60369, -58.488227, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:738356027', 'PARQUE', NULL, 'Plazoleta VDP', -34.601494, -58.491737, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1059789710', 'ESCUELA', NULL, 'Instituto Platerillo', -34.600392, -58.489296, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1059821778', 'ESCUELA', NULL, 'Escuela Técnica 24 - Defensa de Buenos Aires', -34.602025, -58.495802, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1062796551', 'ESCUELA', NULL, 'Escuela para Niños, Niñas, Jóvenes y Adultos con Discapacidad Mental y Formación Integral 02 Domingo F Sarmiento', -34.600486, -58.49314, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1073239725', 'OFICINA', NULL, 'Terminal Línea 24', -34.602031, -58.493719, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:1894747', 'TRANSPORTE', NULL, 'Estación Villa del Parque', -34.60138, -58.493622, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7192480', 'TRANSPORTE', NULL, 'Avenida Nazca', -34.601855, -58.489981, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7192481', 'TRANSPORTE', NULL, 'Nogoyá y Cuenca', -34.603957, -58.493593, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7192482', 'TRANSPORTE', NULL, 'Nogoyá y Llavallol', -34.605189, -58.495742, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7204733', 'TRANSPORTE', NULL, 'Melincué y Concordia', -34.605175, -58.497927, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7204734', 'TRANSPORTE', NULL, 'Melincué y Llavallol', -34.604113, -58.496163, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7204735', 'TRANSPORTE', NULL, 'Melincué y Cuenca', -34.602801, -58.493973, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7204736', 'TRANSPORTE', NULL, 'Nogoyá y Helguera', -34.602907, -58.4919, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7204737', 'TRANSPORTE', NULL, 'Avenida Nazca', -34.601961, -58.490281, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7255443', 'TRANSPORTE', NULL, 'Estación Villa del Parque', -34.601701, -58.493703, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7255444', 'TRANSPORTE', NULL, 'Helguera y Nogoyá', -34.603175, -58.491965, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10088830', 'TRANSPORTE', NULL, 'Nazarre y Lavallol', -34.603434, -58.496708, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14292505', 'TRANSPORTE', NULL, 'Sastre y Nazca', -34.603621, -58.488358, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14292506', 'TRANSPORTE', NULL, 'Plaza Del Valle', -34.60573, -58.491977, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14292507', 'TRANSPORTE', NULL, 'Sastre y Vilardebo', -34.607563, -58.495136, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14292524', 'TRANSPORTE', NULL, 'Cuenca y Nogoyá', -34.603697, -58.493584, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14292525', 'TRANSPORTE', NULL, 'Helguera y Sastre', -34.604963, -58.490428, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14292526', 'TRANSPORTE', NULL, 'Nazca y Sastre', -34.603735, -58.487885, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14292527', 'TRANSPORTE', NULL, 'Sastre y Helguera', -34.604952, -58.490651, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:16899484', 'TRANSPORTE', NULL, 'Estación Villa del Parque', -34.601716, -58.495294, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:16899491', 'TRANSPORTE', NULL, 'Plaza Aristóbulo del Valle', -34.605382, -58.493881, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:16900118', 'TRANSPORTE', NULL, 'Plaza Aristóbulo del Valle', -34.605477, -58.492034, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 17, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 8, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 82, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 6, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 2, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 19, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 12, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 0, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 4, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Villa del Parque' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:239629296', 'TRANSPORTE', NULL, 'Flores', -34.627843, -58.466063, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:661033560', 'COMPLEMENTARIO', NULL, 'Farmacity', -34.628884, -58.462373, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1085899414', 'TRANSPORTE', NULL, 'San José de Flores', -34.629093, -58.46347, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1393092854', 'ESCUELA', NULL, 'Fundacion Educar (iforma) Colegio Para Adultos', -34.63082, -58.46167, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1393104682', 'ESCUELA', NULL, 'Abrahan Lincoln Flores', -34.628422, -58.464584, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1393104685', 'ESCUELA', NULL, 'Escuela Primaria Común 01 Museo de Bellas Artes Gral. Urquiza', -34.627572, -58.462552, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1393189672', 'ESCUELA', NULL, 'Instituto Superior Mariano Moreno', -34.628448, -58.46081, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1991078522', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.629046, -58.463068, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1991078523', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.629218, -58.46365, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1991084605', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.62885, -58.46338, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1991084606', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.628902, -58.463564, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1991084607', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.629021, -58.463945, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2303070857', 'TRANSPORTE', NULL, 'Flores', -34.627804, -58.466075, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3086927108', 'COMPLEMENTARIO', NULL, 'Credicoop', -34.63031, -58.462056, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254646377', 'TRANSPORTE', NULL, 'Rivadavia y Gavilán', -34.627745, -58.459617, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254646378', 'TRANSPORTE', NULL, 'Rivadavia y Gavilán', -34.627787, -58.459736, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254647078', 'TRANSPORTE', NULL, 'Rivadavia y Gavilán', -34.628049, -58.460428, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254647082', 'TRANSPORTE', NULL, 'Plaza de Flores', -34.628251, -58.46443, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254647177', 'TRANSPORTE', NULL, 'Rivadavia y Gavilán', -34.627666, -58.459438, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254647178', 'TRANSPORTE', NULL, 'Rivadavia y Gavilán', -34.628126, -58.460565, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254647179', 'TRANSPORTE', NULL, 'Plaza de Flores', -34.628548, -58.462902, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254647183', 'TRANSPORTE', NULL, 'Plaza de Flores', -34.628542, -58.4643, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254647184', 'TRANSPORTE', NULL, 'Plaza de Flores', -34.628371, -58.464371, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254683035', 'TRANSPORTE', NULL, 'Plaza de Flores', -34.628406, -58.464469, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254683036', 'TRANSPORTE', NULL, 'Plaza de Flores', -34.628532, -58.462839, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254683044', 'TRANSPORTE', NULL, 'Rivadavia y Gavilán', -34.627825, -58.459559, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254683045', 'TRANSPORTE', NULL, 'Rivadavia y Gavilán', -34.627752, -58.459382, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254683046', 'TRANSPORTE', NULL, 'Rivadavia y Gavilán', -34.627883, -58.4597, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3254683047', 'TRANSPORTE', NULL, 'Rivadavia y Gavilán', -34.628179, -58.460529, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3261106260', 'TRANSPORTE', NULL, 'Rivadavia y Gavilán', -34.628136, -58.460396, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3449180200', 'TRANSPORTE', NULL, 'San José de Flores', -34.629059, -58.463484, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3449220583', 'TRANSPORTE', NULL, 'San José de Flores', -34.629075, -58.463477, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3456010438', 'TRANSPORTE', NULL, 'Flores', -34.627823, -58.466069, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3632329643', 'TRANSPORTE', NULL, 'Gavilán y Avellaneda', -34.623163, -58.462323, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3632329644', 'TRANSPORTE', NULL, 'Gavilán y Avellaneda', -34.623178, -58.462368, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3687589458', 'TRANSPORTE', NULL, 'Avellaneda y Artigas', -34.624784, -58.466512, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3687589459', 'TRANSPORTE', NULL, 'Avellaneda y Artigas', -34.624863, -58.466485, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3687589460', 'TRANSPORTE', NULL, 'Avellaneda y Fray Cayetano Rodríguez', -34.624262, -58.464872, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3687589461', 'TRANSPORTE', NULL, 'Avellaneda y Fray Cayetano Rodríguez', -34.624351, -58.464832, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3687589476', 'TRANSPORTE', NULL, 'Cayetano Rodríguez y Bacacay', -34.62626, -58.463772, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3687589477', 'TRANSPORTE', NULL, 'Cayetano Rodríguez y Bacacay', -34.626273, -58.463819, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3687589480', 'TRANSPORTE', NULL, 'Plaza de Flores', -34.628284, -58.464523, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3733985996', 'TRANSPORTE', NULL, 'Rivadavia y Camacuá', -34.62772, -58.459307, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3733985997', 'TRANSPORTE', NULL, 'Rivadavia y Camacuá', -34.627797, -58.45925, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3733985998', 'TRANSPORTE', NULL, 'Rivadavia y Bonorino', -34.627804, -58.459509, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3733985999', 'TRANSPORTE', NULL, 'Rivadavia y Bonorino', -34.627889, -58.459463, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3733986000', 'TRANSPORTE', NULL, 'Rivadavia y Bonorino', -34.628038, -58.460111, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3733986002', 'TRANSPORTE', NULL, 'Rivadavia y Bonorino', -34.628124, -58.460068, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3733986003', 'TRANSPORTE', NULL, 'Rivadavia y Bonorino', -34.628214, -58.460341, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3733986007', 'TRANSPORTE', NULL, 'Rivadavia y Camacuá', -34.627651, -58.459138, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3733986008', 'TRANSPORTE', NULL, 'Rivadavia y Camacuá', -34.627727, -58.459086, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3733986018', 'TRANSPORTE', NULL, 'Rivadavia y Membrillar', -34.62855, -58.461389, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3752289568', 'COMPETENCIA', 'PAPELERIA', 'Librería Puerta del Sol', -34.629975, -58.463903, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3760991164', 'COMPLEMENTARIO', NULL, 'Dia Market', -34.630346, -58.462776, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3760991164', 'COMPETENCIA', 'ALMACEN', 'Dia Market', -34.630346, -58.462776, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3910057352', 'TRANSPORTE', NULL, 'Artigas y Aranguren', -34.623596, -58.466533, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3910057353', 'TRANSPORTE', NULL, 'Artigas y Aranguren', -34.62361, -58.46658, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3910057354', 'TRANSPORTE', NULL, 'Artigas y Bogotá', -34.625854, -58.465557, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3910057355', 'TRANSPORTE', NULL, 'Artigas y Bogotá', -34.625866, -58.465607, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3910058562', 'TRANSPORTE', NULL, 'Artigas y Avellaneda', -34.624836, -58.465998, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837508', 'COMPLEMENTARIO', NULL, 'Banco Ciudad', -34.629086, -58.462898, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837509', 'COMPLEMENTARIO', NULL, 'Comafi', -34.628409, -58.460704, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837510', 'COMPLEMENTARIO', NULL, 'Banco Piano', -34.627951, -58.459418, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837511', 'COMPLEMENTARIO', NULL, 'Banco Provincia', -34.628704, -58.461763, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837514', 'COMPLEMENTARIO', NULL, 'Santander', -34.629083, -58.462983, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837515', 'COMPETENCIA', 'PAPELERIA', 'Dali', -34.628518, -58.462705, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837517', 'ESCUELA', NULL, 'Escuela Primaria Común 02 Florencio Varela', -34.628255, -58.461445, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837518', 'COMPLEMENTARIO', NULL, 'Farmcia Azul', -34.628769, -58.462026, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837519', 'COMPLEMENTARIO', NULL, 'Galicia', -34.627841, -58.45917, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837520', 'OFICINA', NULL, 'Instituto Tecnologico Argentino', -34.628268, -58.461827, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837522', 'COMPETENCIA', 'ALMACEN', 'San Cayetano', -34.627261, -58.460093, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837524', 'COMPLEMENTARIO', NULL, 'Santander', -34.629027, -58.462798, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837526', 'COMPLEMENTARIO', NULL, 'Patagonia', -34.628307, -58.460451, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837536', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.627866, -58.459158, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837550', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.628677, -58.4626, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837554', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.628727, -58.461752, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4318837556', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.629062, -58.46292, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4337573240', 'ESCUELA', NULL, 'Jardín de Infantes La Obra', -34.625591, -58.462326, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483326955', 'TRANSPORTE', NULL, 'Alberdi y Rivera Indarte', -34.631154, -58.461599, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4483326956', 'TRANSPORTE', NULL, 'Alberdi y Rivera Indarte', -34.6312, -58.461579, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4536753813', 'TRANSPORTE', NULL, 'Fray Cayetano Rodríguez y Avellaneda', -34.623775, -58.464637, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4536753814', 'TRANSPORTE', NULL, 'Fray Cayetano Rodríguez y Avellaneda', -34.623803, -58.464678, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4538673522', 'TRANSPORTE', NULL, 'Plaza de Flores', -34.628583, -58.464392, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4538747877', 'TRANSPORTE', NULL, 'Artigas y Avellaneda', -34.624849, -58.466046, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4551126689', 'COMPETENCIA', 'PAPELERIA', 'La Chispa', -34.630108, -58.465183, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4600374196', 'OFICINA', NULL, 'Centro de atención a clientes Claro', -34.629101, -58.464329, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4682611464', 'TRANSPORTE', NULL, 'Rivadavia y Caracas', -34.628251, -58.46075, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4682611465', 'TRANSPORTE', NULL, 'Rivadavia y Caracas', -34.628177, -58.460788, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4701797412', 'COMPLEMENTARIO', NULL, 'Alberdi', -34.625199, -58.467363, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4701797415', 'COMPETENCIA', 'ALMACEN', 'San Ceferino', -34.625146, -58.467271, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4701797456', 'OFICINA', NULL, 'Asociación Agentes de Propaganda Médica', -34.623263, -58.461535, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4702383150', 'COMPETENCIA', 'ALMACEN', 'La Francesa', -34.625145, -58.468164, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4702383163', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.624898, -58.467531, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4702383163', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.624898, -58.467531, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4702383178', 'COMPETENCIA', 'ALMACEN', 'Casher', -34.624185, -58.465029, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4702383178', 'COMPETENCIA', 'KIOSCO', 'Casher', -34.624185, -58.465029, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4702522908', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.623186, -58.462162, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4860969397', 'TRANSPORTE', NULL, 'Aranguren y Artigas', -34.623534, -58.466866, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4860969398', 'TRANSPORTE', NULL, 'Aranguren y Artigas', -34.623492, -58.46689, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4860969399', 'TRANSPORTE', NULL, 'Aranguren y Artigas', -34.623431, -58.466496, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4860969400', 'TRANSPORTE', NULL, 'Aranguren y Artigas', -34.623382, -58.466523, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4899843344', 'COMPLEMENTARIO', NULL, 'Chino', -34.626882, -58.468884, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4899843344', 'COMPETENCIA', 'ALMACEN', 'Chino', -34.626882, -58.468884, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4899843345', 'COMPLEMENTARIO', NULL, 'Chino', -34.628143, -58.469021, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4899843345', 'COMPETENCIA', 'ALMACEN', 'Chino', -34.628143, -58.469021, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5000389429', 'COMPLEMENTARIO', NULL, 'Galicia', -34.629565, -58.465905, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531523', 'TRANSPORTE', NULL, 'Flores', -34.629434, -58.464025, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531524', 'TRANSPORTE', NULL, 'Flores', -34.629424, -58.463981, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531525', 'TRANSPORTE', NULL, 'Plaza de Flores', -34.628348, -58.462906, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531526', 'TRANSPORTE', NULL, 'Plaza de Flores', -34.628362, -58.46297, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531527', 'TRANSPORTE', NULL, 'Bolivia', -34.629939, -58.466651, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531528', 'TRANSPORTE', NULL, 'Bolivia', -34.629847, -58.466284, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531529', 'TRANSPORTE', NULL, 'Bolivia', -34.629803, -58.466307, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531530', 'TRANSPORTE', NULL, 'Bolivia', -34.629893, -58.466668, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531532', 'TRANSPORTE', NULL, 'Lafuente', -34.630155, -58.467563, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531533', 'TRANSPORTE', NULL, 'Lafuente', -34.6302, -58.467536, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531539', 'TRANSPORTE', NULL, 'Bolivia', -34.629755, -58.465914, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531540', 'TRANSPORTE', NULL, 'Bolivia', -34.629702, -58.465935, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531541', 'TRANSPORTE', NULL, 'General Artigas', -34.629577, -58.465253, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531542', 'TRANSPORTE', NULL, 'General Artigas', -34.629467, -58.46487, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531543', 'TRANSPORTE', NULL, 'General Artigas', -34.629528, -58.465275, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531544', 'TRANSPORTE', NULL, 'General Artigas', -34.629419, -58.46489, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531545', 'TRANSPORTE', NULL, 'General Artigas', -34.629377, -58.464538, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531547', 'TRANSPORTE', NULL, 'General Artigas', -34.629325, -58.464565, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531548', 'TRANSPORTE', NULL, 'Lafuente', -34.630064, -58.467207, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5076531549', 'TRANSPORTE', NULL, 'Lafuente', -34.630113, -58.46719, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5113999977', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado A (EPCjc 01/12°)', -34.627596, -58.462619, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5116062575', 'ESCUELA', NULL, 'Escuela Primaria p/Adultos 02 Florencio Varela', -34.628296, -58.461491, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117379920', 'ESCUELA', NULL, 'El Jardín de los Colores', -34.630018, -58.462529, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117390654', 'ESCUELA', NULL, 'Jardín de Infantes La Puerta Mágica', -34.623378, -58.46572, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117402555', 'ESCUELA', NULL, 'Escuela Para El Hombre Nuevo', -34.625749, -58.459961, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117402561', 'ESCUELA', NULL, 'Instituto ESBA - Flores', -34.631282, -58.465861, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117413157', 'ESCUELA', NULL, 'Centro de Formación Profesional 10 Raúl Scalabrini Ortiz (anexo) - SEC', -34.629932, -58.467627, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117413160', 'ESCUELA', NULL, 'Colegio 09 Justo José de Urquiza', -34.627784, -58.468042, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117413161', 'ESCUELA', NULL, 'Escuela Primaria Común 19 Leandro Niceforo Alem', -34.627777, -58.463008, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117441228', 'ESCUELA', NULL, 'UGEE 12 Bachillerato Popular de Adultos y Adolescentes Flores', -34.623125, -58.462022, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117448183', 'ESCUELA', NULL, 'El Rincón del Duende', -34.62695, -58.466099, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117448217', 'ESCUELA', NULL, 'New Sweet Palace', -34.626046, -58.462724, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117449282', 'ESCUELA', NULL, 'Centro Educativo de Nivel Primario 21 - Parroquia San José', -34.630126, -58.462941, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117565856', 'ESCUELA', NULL, 'Escuela Primaria Común 12 Provincia del Chaco', -34.624705, -58.466975, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117565857', 'ESCUELA', NULL, 'Jardín de Infantes Común 02/12° Provincia de Chaco', -34.624643, -58.466986, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117576236', 'ESCUELA', NULL, 'Jardín de Infantes Nucleado A (EPCjc 19/12°)', -34.627744, -58.462999, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117576239', 'ESCUELA', NULL, 'CENOF Centro Aranguren', -34.623197, -58.464931, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117576240', 'ESCUELA', NULL, 'Escuela Primaria Común 15 Cnel. C. T. Sourigues', -34.623165, -58.464934, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117591956', 'ESCUELA', NULL, 'Centro Educativo de Nivel Secundario 76', -34.627761, -58.463001, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117591979', 'ESCUELA', NULL, 'Escuela de Comercio 09 José Ingenieros', -34.629506, -58.459874, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5117591980', 'ESCUELA', NULL, 'Escuela Técnica 06 Fernando Fader', -34.629497, -58.467868, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6215436324', 'TRANSPORTE', NULL, 'Rivadavia y Bonorino', -34.628109, -58.460315, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6215436325', 'TRANSPORTE', NULL, 'Rivadavia y Bonorino', -34.628189, -58.460272, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6397710294', 'TRANSPORTE', NULL, 'Rivadavia y Membrillar', -34.628466, -58.461426, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6397710319', 'TRANSPORTE', NULL, 'Rivadavia y Granaderos', -34.627303, -58.458534, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6397710320', 'TRANSPORTE', NULL, 'Rivadavia y Granaderos', -34.627385, -58.458489, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6397710328', 'TRANSPORTE', NULL, 'Rivadavia y Bonorino', -34.628132, -58.460385, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6397710332', 'TRANSPORTE', NULL, 'Rivadavia y Membrillar', -34.628629, -58.461984, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6397710333', 'TRANSPORTE', NULL, 'Rivadavia y Membrillar', -34.628711, -58.46196, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6397710334', 'TRANSPORTE', NULL, 'Rivadavia y Membrillar', -34.628649, -58.462052, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6397710335', 'TRANSPORTE', NULL, 'Rivadavia y Membrillar', -34.628728, -58.462019, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6550514676', 'TRANSPORTE', NULL, 'Plaza Flores', -34.628, -58.463095, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6550514677', 'TRANSPORTE', NULL, 'Plaza Pueyrredón', -34.627984, -58.463037, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6770891033', 'TRANSPORTE', NULL, 'Rivadavia y Membrillar', -34.628387, -58.46087, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6770891034', 'TRANSPORTE', NULL, 'Rivadavia y Membrillar', -34.628304, -58.46091, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6978668361', 'TRANSPORTE', NULL, 'Avellaneda y Artigas', -34.62481, -58.466296, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6978668362', 'TRANSPORTE', NULL, 'Avellaneda y Artigas', -34.624725, -58.466326, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7156733935', 'SALUD', NULL, 'Medicina Estética Flebológica', -34.628313, -58.462792, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7170990498', 'TRANSPORTE', NULL, 'Avenida Alberdi', -34.631111, -58.461791, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7170990499', 'TRANSPORTE', NULL, 'Rivera Indarte y Alberdi', -34.631121, -58.461827, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7254082660', 'OFICINA', NULL, 'Servicio Social Zonal 7', -34.62779, -58.464125, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8207585164', 'OFICINA', NULL, 'Arquitecto Alberto Taie', -34.623725, -58.462848, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8260892582', 'COMPETENCIA', 'PAPELERIA', 'Lugar sin nombre', -34.62482, -58.465929, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8260892583', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.624788, -58.46594, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8265647912', 'TRANSPORTE', NULL, 'Avellaneda y Gavilán', -34.623541, -58.462479, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8265647913', 'TRANSPORTE', NULL, 'Avellaneda y Gavilán', -34.623498, -58.462363, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10672060994', 'COMPETENCIA', 'ALMACEN', 'Dorita', -34.628219, -58.464674, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10672150682', 'COMPETENCIA', 'ALMACEN', 'Maxikiosco Anees 5 Hermanos', -34.628251, -58.464661, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10672150682', 'COMPETENCIA', 'KIOSCO', 'Maxikiosco Anees 5 Hermanos', -34.628251, -58.464661, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10676648271', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.624568, -58.464698, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10676648271', 'COMPETENCIA', 'KIOSCO', 'Carrefour Express', -34.624568, -58.464698, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10679730330', 'TRANSPORTE', NULL, 'Cayetano Rodríguez y Bacacay', -34.626181, -58.463806, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10679730331', 'TRANSPORTE', NULL, 'Cayetano Rodríguez y Bacacay', -34.626202, -58.463853, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11013438244', 'OFICINA', NULL, 'Ledesma Aranguren Inmobiliaria', -34.630561, -58.461947, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11158246399', 'COMPETENCIA', 'ALMACEN', 'Artigas', -34.625587, -58.465618, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11374843864', 'COMPETENCIA', 'PAPELERIA', 'Liberia BBM', -34.629172, -58.461736, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11496124289', 'TRANSPORTE', NULL, 'Avellaneda y Gavilán', -34.623458, -58.462512, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11496124290', 'TRANSPORTE', NULL, 'Avellaneda y Gavilán', -34.62342, -58.462402, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12958009477', 'SALUD', NULL, 'Imagen Test', -34.631271, -58.464676, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12958123810', 'COMPLEMENTARIO', NULL, 'Yanni', -34.627672, -58.464712, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12958705830', 'TRANSPORTE', NULL, 'Alberdi y Rivera Indarte', -34.631172, -58.461488, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12958705831', 'TRANSPORTE', NULL, 'Alberdi y Rivera Indarte', -34.631126, -58.461511, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12958705841', 'TRANSPORTE', NULL, 'Varela y Falcón', -34.630575, -58.465107, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12958705842', 'TRANSPORTE', NULL, 'Varela y Falcón', -34.63059, -58.465157, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13091386456', 'COMPLEMENTARIO', NULL, 'Farmacias Dr. Ahorro', -34.629706, -58.466547, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13091732620', 'COMPLEMENTARIO', NULL, 'Soy Tu', -34.628236, -58.461151, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13372058170', 'COMPETENCIA', 'ALMACEN', 'La Palma de Flores', -34.63054, -58.463477, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13372059459', 'COMPLEMENTARIO', NULL, 'Acasiem Market', -34.628381, -58.464601, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13372059459', 'COMPETENCIA', 'ALMACEN', 'Acasiem Market', -34.628381, -58.464601, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13372091697', 'COMPETENCIA', 'ALMACEN', 'Jamón Jamón', -34.624864, -58.465941, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13778576265', 'COMPETENCIA', 'ALMACEN', 'Open 25 Hs! Market', -34.627513, -58.459193, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13778576265', 'COMPETENCIA', 'KIOSCO', 'Open 25 Hs! Market', -34.627513, -58.459193, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13807314458', 'COMPLEMENTARIO', NULL, 'Carrefour Express', -34.628612, -58.467634, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13807314458', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.628612, -58.467634, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13807349248', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.630236, -58.467118, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13807349456', 'COMPLEMENTARIO', NULL, 'Social Flores', -34.630258, -58.467193, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:14243231832', 'COMPETENCIA', 'ALMACEN', 'Saint Honoré', -34.626097, -58.465393, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:14243423646', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.626178, -58.465355, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:14243423646', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.626178, -58.465355, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:14243473290', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.627288, -58.464874, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:14243479831', 'COMPETENCIA', 'ALMACEN', 'Autoservicio Jheny', -34.624891, -58.463259, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:14243479831', 'COMPETENCIA', 'KIOSCO', 'Autoservicio Jheny', -34.624891, -58.463259, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:26682898', 'COMPLEMENTARIO', NULL, 'Banco Nación', -34.629508, -58.463648, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:129870639', 'PARQUE', NULL, 'Plaza Pueyrredón', -34.628384, -58.463646, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:129910911', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.627848, -58.465961, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:129910912', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.627781, -58.466113, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:206452751', 'PARQUE', NULL, 'Plaza Patio Porteño Fray Cayetano', -34.627204, -58.463583, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:337872573', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.629038, -58.463494, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:337872577', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.629115, -58.463462, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:448963745', 'PARQUE', NULL, 'Lugar sin nombre', -34.627979, -58.463857, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:465003823', 'COMPLEMENTARIO', NULL, 'Supermercado', -34.630116, -58.463468, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:465003823', 'COMPETENCIA', 'ALMACEN', 'Supermercado', -34.630116, -58.463468, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:465003826', 'COMPLEMENTARIO', NULL, 'Dia', -34.628866, -58.464541, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:465003826', 'COMPETENCIA', 'ALMACEN', 'Dia', -34.628866, -58.464541, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:465012646', 'COMPETENCIA', 'ALMACEN', 'Shell Select', -34.623885, -58.464837, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:465012646', 'COMPETENCIA', 'KIOSCO', 'Shell Select', -34.623885, -58.464837, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476692400', 'COMPETENCIA', 'ALMACEN', 'Del Pueblo', -34.624579, -58.464953, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476692400', 'COMPETENCIA', 'KIOSCO', 'Del Pueblo', -34.624579, -58.464953, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477522551', 'ESCUELA', NULL, 'Instituto Ana María Janer', -34.631549, -58.463967, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:520941016', 'TRANSPORTE', NULL, 'General Artigas', -34.629405, -58.464842, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:520941017', 'TRANSPORTE', NULL, 'Bolivia', -34.629775, -58.466221, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:520941018', 'TRANSPORTE', NULL, 'Lafuente', -34.630211, -58.467598, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:615884063', 'COMPLEMENTARIO', NULL, 'Disco', -34.628328, -58.45874, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:615884063', 'COMPETENCIA', 'ALMACEN', 'Disco', -34.628328, -58.45874, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1054703193', 'ESCUELA', NULL, 'Instituto Superior de Ciencias de la Salud', -34.627038, -58.460225, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1056564883', 'ESCUELA', NULL, 'Nueva Escuela de Educación La Obra', -34.625573, -58.462265, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1059456391', 'ESCUELA', NULL, 'Escuela de Educacion Media 01/12° Julio Cortazar', -34.626522, -58.469316, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1360033466', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.623386, -58.466484, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:1359825', 'TRANSPORTE', NULL, 'Estación Flores', -34.627797, -58.466083, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:2541718', 'TRANSPORTE', NULL, 'Estación San José de Flores', -34.629057, -58.463478, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6691906', 'TRANSPORTE', NULL, 'Alberdi y Rivera Indarte', -34.631177, -58.461589, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6760527', 'TRANSPORTE', NULL, 'Avenida Avellaneda', -34.623789, -58.464658, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6762798', 'TRANSPORTE', NULL, 'Plaza de Flores', -34.628563, -58.464346, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6762874', 'TRANSPORTE', NULL, 'Artigas y Avellaneda', -34.624842, -58.466022, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6972775', 'TRANSPORTE', NULL, 'Rivadavia 6700', -34.628214, -58.460769, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7255452', 'TRANSPORTE', NULL, 'Aranguren y Artigas', -34.623454, -58.466661, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7537751', 'TRANSPORTE', NULL, 'Flores', -34.629429, -58.464003, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7537753', 'TRANSPORTE', NULL, 'Bolivia', -34.629778, -58.466221, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7537754', 'TRANSPORTE', NULL, 'General Artigas', -34.629405, -58.464842, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7537755', 'TRANSPORTE', NULL, 'Lafuente', -34.630211, -58.467598, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9475619', 'TRANSPORTE', NULL, 'Membrillar - Caracas', -34.628508, -58.461407, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:9475623', 'TRANSPORTE', NULL, 'Rivadavia al 6800', -34.628688, -58.462036, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:10652549', 'TRANSPORTE', NULL, 'Avenida Alberdi', -34.631116, -58.461809, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:16951901', 'TRANSPORTE', NULL, 'Avellaneda y Gavilán', -34.623459, -58.462382, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:19297479', 'TRANSPORTE', NULL, 'Varela y Falcón', -34.630583, -58.465132, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 31, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 6, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 136, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 3, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 2, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 30, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 28, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 5, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 10, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Flores Norte' AND localidad = 'Ciudad Autónoma de Buenos Aires';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:290534208', 'TRANSPORTE', NULL, 'Rivadavia y Avenida de Mayo', -34.640897, -58.566164, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1529630806', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.640505, -58.564281, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:1529630899', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.640397, -58.564257, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2310625606', 'PARQUE', NULL, 'Lugar sin nombre', -34.639552, -58.564309, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2310625610', 'PARQUE', NULL, 'Lugar sin nombre', -34.639653, -58.564867, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2339303723', 'COMPLEMENTARIO', NULL, 'Alem', -34.640477, -58.566689, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2339303723', 'COMPETENCIA', 'ALMACEN', 'Alem', -34.640477, -58.566689, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2522732472', 'COMPLEMENTARIO', NULL, 'Rivadavia', -34.64116, -58.566799, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2647504568', 'COMPETENCIA', 'ALMACEN', 'Carrefour Express', -34.639754, -58.565346, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2647504568', 'COMPETENCIA', 'KIOSCO', 'Carrefour Express', -34.639754, -58.565346, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2647504603', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.639794, -58.565136, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2647504633', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.639809, -58.565221, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2647504667', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640195, -58.565036, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2647504685', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640208, -58.565127, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2647504812', 'TRANSPORTE', NULL, '9 de Julio y Alvarez Jonte', -34.638977, -58.565336, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2647504867', 'TRANSPORTE', NULL, '9 de Julio y Álvarez Jonte', -34.638987, -58.565415, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2647504922', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.63978, -58.563946, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2647504990', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.639774, -58.563901, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2782314303', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.640536, -58.564212, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2932121301', 'TRANSPORTE', NULL, 'Rivadavia y Alvear', -34.641552, -58.570325, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:2932121401', 'TRANSPORTE', NULL, 'Rivadavia y Alvear', -34.641486, -58.570339, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3003582621', 'TRANSPORTE', NULL, 'Rivadavia y Alvear', -34.641651, -58.570399, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3003813633', 'TRANSPORTE', NULL, 'Rivadavia y Alvear', -34.641568, -58.570427, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3019211324', 'TRANSPORTE', NULL, 'Coto Ramos', -34.640308, -58.561888, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3021186533', 'TRANSPORTE', NULL, 'Coto Ramos', -34.640236, -58.562, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3071298138', 'TRANSPORTE', NULL, 'Coto Ramos', -34.640179, -58.562149, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3071382447', 'TRANSPORTE', NULL, 'Coto Ramos', -34.640257, -58.562128, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3219588913', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640134, -58.56352, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3219599561', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640084, -58.563534, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3231408161', 'TRANSPORTE', NULL, 'Alem y Soler', -34.640734, -58.567538, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3231408162', 'TRANSPORTE', NULL, 'Alem y Soler', -34.640772, -58.567532, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3456039893', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.640455, -58.564265, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3526517386', 'COMPLEMENTARIO', NULL, 'Supervielle', -34.643107, -58.565462, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3526558779', 'ESCUELA', NULL, 'Escuela Argentina del Oeste', -34.642922, -58.567488, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3526558780', 'SALUD', NULL, 'Consultorios Médicos Privados SA', -34.641401, -58.568332, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3526558781', 'ESCUELA', NULL, 'Jardín de infantes el Gato con Botas', -34.643152, -58.567697, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3526558782', 'COMPLEMENTARIO', NULL, 'Rosales', -34.643862, -58.567519, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3526558783', 'COMPLEMENTARIO', NULL, 'Espora', -34.644774, -58.567424, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3526558783', 'COMPETENCIA', 'ALMACEN', 'Espora', -34.644774, -58.567424, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3526585182', 'COMPLEMENTARIO', NULL, 'Villa Sarmiento', -34.63805, -58.565906, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3526746694', 'COMPETENCIA', 'PAPELERIA', 'Cúspide', -34.644656, -58.565124, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3527153629', 'COMPLEMENTARIO', NULL, 'Super General', -34.644709, -58.565975, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3527153629', 'COMPETENCIA', 'ALMACEN', 'Super General', -34.644709, -58.565975, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3527153643', 'COMPETENCIA', 'PAPELERIA', 'El Aleph', -34.645582, -58.564483, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3527177948', 'COMPLEMENTARIO', NULL, 'Súper 2000', -34.643245, -58.563436, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3527177948', 'COMPETENCIA', 'ALMACEN', 'Súper 2000', -34.643245, -58.563436, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3708479384', 'TRANSPORTE', NULL, 'Avenida de Mayo y Alsina', -34.642586, -58.565503, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3708819485', 'TRANSPORTE', NULL, 'Avenida de Mayo y Alsina', -34.642571, -58.565367, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3767500946', 'COMPLEMENTARIO', NULL, 'Central Oeste', -34.640883, -58.564456, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3833679614', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.640062, -58.562597, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3844108044', 'TRANSPORTE', NULL, 'Chassaing/Blas Parera', -34.641218, -58.570423, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3844108045', 'TRANSPORTE', NULL, 'Chassaing/Blas Parera', -34.641264, -58.570412, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3849759759', 'TRANSPORTE', NULL, 'Moreno y Mitre', -34.641565, -58.562766, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3863051326', 'SALUD', NULL, 'Consultorio de radiología dento-maxilo facial', -34.644082, -58.567799, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3867477385', 'TRANSPORTE', NULL, 'Av. de Mayo y Belgrano', -34.641504, -58.56562, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3867477386', 'TRANSPORTE', NULL, 'Av. de Mayo y Belgrano', -34.641521, -58.565762, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3867477387', 'TRANSPORTE', NULL, 'Av. de Mayo y Rosales', -34.643484, -58.565164, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3867477388', 'TRANSPORTE', NULL, 'Av. de Mayo y Rosales', -34.643495, -58.565242, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3867477390', 'SALUD', NULL, 'OSECAC - Centro ambulatorio Ramos Mejía', -34.643644, -58.566089, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3867477393', 'TRANSPORTE', NULL, 'Av. de Mayo y Rivadavia', -34.641216, -58.565812, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3867477395', 'TRANSPORTE', NULL, 'Av. de Mayo y Belgrano', -34.641663, -58.56571, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4113115698', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.641398, -58.564154, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4139764920', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.641214, -58.564264, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4139764921', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.641274, -58.564251, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4139764923', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.641405, -58.564228, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329807177', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.641739, -58.56475, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329807178', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.641783, -58.565066, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329807179', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.641741, -58.564262, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329807180', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.641859, -58.565053, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329807185', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640512, -58.563668, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329807186', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.64097, -58.564319, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329807187', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.64107, -58.564297, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329807188', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.641169, -58.564272, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4330109506', 'TRANSPORTE', NULL, 'Av. de Mayo y Rosales', -34.643733, -58.564993, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4330109507', 'TRANSPORTE', NULL, 'Av. de Mayo y Rosales', -34.643751, -58.565104, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4352743499', 'COMPETENCIA', 'ALMACEN', 'Dietética', -34.645127, -58.568061, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4352743499', 'COMPETENCIA', 'KIOSCO', 'Dietética', -34.645127, -58.568061, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4368906210', 'TRANSPORTE', NULL, 'Moreno y Mitre', -34.641562, -58.562726, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4374223448', 'SALUD', NULL, 'Centro Médico San Lucas', -34.641065, -58.570136, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4412289860', 'TRANSPORTE', NULL, 'Rivadavia y Avenida de Mayo', -34.641003, -58.566355, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4462698954', 'TRANSPORTE', NULL, 'Perón y Macho Vidal', -34.638003, -58.565432, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4462698955', 'TRANSPORTE', NULL, 'Perón y Macho Vidal', -34.638056, -58.565416, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4462698958', 'TRANSPORTE', NULL, 'Perón y Solier', -34.638231, -58.56778, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4462698959', 'TRANSPORTE', NULL, 'Perón y Solier', -34.638285, -58.567771, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4598154712', 'COMPETENCIA', 'ALMACEN', 'La Esquina', -34.6414, -58.561446, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4598154712', 'COMPETENCIA', 'KIOSCO', 'La Esquina', -34.6414, -58.561446, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4598154714', 'COMPETENCIA', 'KIOSCO', 'La Plaza', -34.641197, -58.561474, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4601041769', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640583, -58.563647, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4620852380', 'COMPETENCIA', 'ALMACEN', 'Pancitos', -34.641175, -58.566893, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4620852448', 'TRANSPORTE', NULL, 'Coto Ramos', -34.640337, -58.56205, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4621619599', 'TRANSPORTE', NULL, 'Coto Ramos', -34.640248, -58.562075, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4625632234', 'ESCUELA', NULL, 'Instituto Británico de Cultura Inglesa', -34.641325, -58.562345, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4625632678', 'OFICINA', NULL, 'Swiss Medical Group', -34.641467, -58.562121, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689433925', 'COMPETENCIA', 'KIOSCO', 'Maxikiosko', -34.641329, -58.563087, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689433941', 'COMPLEMENTARIO', NULL, 'Supermercado Argenchino', -34.641076, -58.563091, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4689433941', 'COMPETENCIA', 'ALMACEN', 'Supermercado Argenchino', -34.641076, -58.563091, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4695952729', 'SALUD', NULL, 'DIM', -34.641418, -58.568448, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4695952750', 'SALUD', NULL, 'DIM', -34.641376, -58.568161, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4695998019', 'OFICINA', NULL, 'Tarraubella', -34.641099, -58.566445, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4695998047', 'COMPETENCIA', 'ALMACEN', 'Maxikiosco Arlequín', -34.64123, -58.565887, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4695998047', 'COMPETENCIA', 'KIOSCO', 'Maxikiosco Arlequín', -34.64123, -58.565887, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4695998051', 'COMPLEMENTARIO', NULL, 'Farmacia Social', -34.64134, -58.565838, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4696097130', 'COMPLEMENTARIO', NULL, 'Farmacia Moreno 90', -34.641325, -58.562709, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708581226', 'COMPETENCIA', 'ALMACEN', 'Granja San Nicolás', -34.643893, -58.561738, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708713621', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.643573, -58.56184, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708713625', 'OFICINA', NULL, 'Garcia Propiedades', -34.643515, -58.561917, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708726894', 'SALUD', NULL, 'Proteger', -34.642972, -58.562314, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708726895', 'OFICINA', NULL, 'Carlos Mendez', -34.642929, -58.562276, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708773491', 'COMPLEMENTARIO', NULL, 'Central Oeste', -34.64472, -58.565112, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708773502', 'COMPETENCIA', 'ALMACEN', 'Hausbrot', -34.645127, -58.564988, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708832585', 'SALUD', NULL, 'Del Buen Pastor', -34.645913, -58.564827, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4710121418', 'SALUD', NULL, 'Otropedía Pedicuros', -34.644834, -58.564638, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4710121437', 'COMPETENCIA', 'ALMACEN', 'Patri Ser', -34.645448, -58.564509, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4710121437', 'COMPETENCIA', 'KIOSCO', 'Patri Ser', -34.645448, -58.564509, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4710121444', 'COMPETENCIA', 'PAPELERIA', 'Patri Ser', -34.645618, -58.564473, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4714452334', 'TRANSPORTE', NULL, 'Vidal y Gaona', -34.637828, -58.565618, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4714452335', 'TRANSPORTE', NULL, 'Vidal y Gaona', -34.637833, -58.565677, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5500156856', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640633, -58.563864, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5500156857', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640551, -58.563883, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5500156858', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.639709, -58.563922, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5500156859', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.639711, -58.563963, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5500156860', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.639674, -58.563971, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5500156861', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.639666, -58.563933, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5500156862', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.639894, -58.563917, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5500156863', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.639893, -58.563875, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5599584112', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.641206, -58.564197, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5643086232', 'TRANSPORTE', NULL, 'Av. de Mayo y Belgrano', -34.641642, -58.565586, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5999506693', 'COMPETENCIA', 'ALMACEN', 'La Pequeña Nacional', -34.640327, -58.565816, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6008861676', 'ESCUELA', NULL, 'Eddis', -34.640548, -58.562867, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133597007', 'TRANSPORTE', NULL, 'Moreno 50', -34.640961, -58.562876, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133597008', 'TRANSPORTE', NULL, 'Moreno 50', -34.640973, -58.562943, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133597009', 'TRANSPORTE', NULL, 'San Martín y Sarmiento', -34.643256, -58.562282, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6133597010', 'TRANSPORTE', NULL, 'San Martín y Sarmiento', -34.643264, -58.562293, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6179963536', 'TRANSPORTE', NULL, 'Av. de Mayo y Rivadavia', -34.641204, -58.565696, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6275693027', 'TRANSPORTE', NULL, 'Coto Ramos', -34.640222, -58.561907, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6281964929', 'TRANSPORTE', NULL, 'Coto Ramos', -34.640157, -58.562023, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6296921188', 'COMPETENCIA', 'ALMACEN', '9 de Julio', -34.639636, -58.565359, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6298511589', 'TRANSPORTE', NULL, 'Gaona y Echeverría', -34.637847, -58.564223, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6363857869', 'COMPETENCIA', 'ALMACEN', 'Gulf Store', -34.637994, -58.566229, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6363857869', 'COMPETENCIA', 'KIOSCO', 'Gulf Store', -34.637994, -58.566229, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6363858183', 'COMPETENCIA', 'ALMACEN', 'La Durée', -34.637956, -58.565354, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6445527807', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.638242, -58.56384, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6445527807', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.638242, -58.56384, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6445634507', 'OFICINA', NULL, 'Nivel Propiedades', -34.638372, -58.563498, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6445634508', 'COMPETENCIA', 'PAPELERIA', 'Lugar sin nombre', -34.638437, -58.563835, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6445634510', 'COMPETENCIA', 'ALMACEN', 'Mamá Cora', -34.63889, -58.56406, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6445643056', 'SALUD', NULL, 'Lugar sin nombre', -34.638098, -58.564863, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6830716575', 'COMPLEMENTARIO', NULL, 'Galicia', -34.638298, -58.566331, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6925805674', 'TRANSPORTE', NULL, 'Plazoleta San Martín', -34.642164, -58.563619, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6925805675', 'TRANSPORTE', NULL, 'Plazoleta San Martín', -34.642283, -58.563467, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6925805676', 'TRANSPORTE', NULL, 'Plazoleta San Martín', -34.642385, -58.563358, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6925805677', 'TRANSPORTE', NULL, 'Plazoleta San Martín', -34.642362, -58.563329, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6925805678', 'TRANSPORTE', NULL, 'Plazoleta San Martín', -34.642256, -58.563443, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6925805679', 'TRANSPORTE', NULL, 'Plazoleta San Martín', -34.642134, -58.563587, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7282982917', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.639364, -58.566058, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7283035354', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.638073, -58.564241, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7283035354', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.638073, -58.564241, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7679792251', 'COMPLEMENTARIO', NULL, 'Banco Patagonia', -34.643241, -58.565435, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7679792252', 'COMPLEMENTARIO', NULL, 'ICBC', -34.643176, -58.565449, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7679792256', 'OFICINA', NULL, 'Personal Flow', -34.64597, -58.564809, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:8512033145', 'ESCUELA', NULL, 'El Principito', -34.640963, -58.569578, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9135836072', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.644115, -58.568256, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9696649493', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.641051, -58.564232, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9696649498', 'TRANSPORTE', NULL, 'Belgrano y Avenida de Mayo', -34.641849, -58.565885, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9696649499', 'TRANSPORTE', NULL, 'Belgrano y Avenida de Mayo', -34.641877, -58.565878, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9696649500', 'TRANSPORTE', NULL, 'Belgrano y Avellaneda', -34.64229, -58.568718, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9696649501', 'TRANSPORTE', NULL, 'Belgrano y Avellaneda', -34.64232, -58.568711, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9696649502', 'TRANSPORTE', NULL, 'Belgrano y Rondeau', -34.642737, -58.571662, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9696649503', 'TRANSPORTE', NULL, 'Belgrano y Rondeau', -34.642765, -58.571656, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9710551344', 'TRANSPORTE', NULL, 'Alsina y Avenida de Mayo', -34.642658, -58.565129, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9710551345', 'TRANSPORTE', NULL, 'Alsina y Avenida de Mayo', -34.642623, -58.565137, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9710551346', 'TRANSPORTE', NULL, 'Alsina y Rondeau', -34.643591, -58.571176, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9710551347', 'TRANSPORTE', NULL, 'Alsina y Rondeau', -34.643564, -58.571182, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9725774941', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640084, -58.5641, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9725774942', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640139, -58.564091, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9725774949', 'TRANSPORTE', NULL, '3 de Febrero y Perón', -34.637848, -58.565027, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9725774950', 'TRANSPORTE', NULL, '3 de Febrero y Perón', -34.63783, -58.565028, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9750866487', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640224, -58.564645, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9750866488', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640136, -58.564417, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9930674502', 'COMPETENCIA', 'ALMACEN', 'Roma', -34.640516, -58.566954, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10009522613', 'TRANSPORTE', NULL, 'Espora y Necochea', -34.644653, -58.566376, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10009522614', 'TRANSPORTE', NULL, 'Espora y Avellaneda', -34.644886, -58.567854, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10009522615', 'TRANSPORTE', NULL, 'Espora y Alvear', -34.645114, -58.569295, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10032679215', 'TRANSPORTE', NULL, 'Espora y Necochea', -34.64469, -58.566369, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10032679216', 'TRANSPORTE', NULL, 'Espora y Avellaneda', -34.64492, -58.567846, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10032717017', 'TRANSPORTE', NULL, 'Espora y Alvear', -34.645133, -58.569269, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10647899496', 'OFICINA', NULL, 'Bottiglia', -34.641475, -58.568883, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10701948673', 'TRANSPORTE', NULL, 'Rivadavia y Avenida de Mayo', -34.640933, -58.566377, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10857416743', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.641262, -58.564184, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10857416744', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640957, -58.564254, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10951215505', 'COMPETENCIA', 'PAPELERIA', 'Fénix Gráfica', -34.638025, -58.564248, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074037', 'TRANSPORTE', NULL, 'Alem y Soler', -34.64076, -58.567449, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074038', 'TRANSPORTE', NULL, 'Alem y Soler', -34.640722, -58.567455, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074039', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.64081, -58.564816, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074040', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640789, -58.564686, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074041', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.64077, -58.56456, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074042', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640737, -58.564826, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074043', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.64072, -58.564697, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074044', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640703, -58.564577, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074049', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640205, -58.564505, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074050', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.64019, -58.564769, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074051', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640243, -58.564756, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11472074052', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640187, -58.564408, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11601304721', 'TRANSPORTE', NULL, 'Av. de Mayo y Rivadavia', -34.641355, -58.565782, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11601304722', 'TRANSPORTE', NULL, 'Av. de Mayo y Rivadavia', -34.641338, -58.565662, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11601355609', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.641815, -58.564734, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11601355610', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.641673, -58.564274, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11658271845', 'COMPETENCIA', 'PAPELERIA', 'Librería Tince', -34.643052, -58.565491, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11762665552', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.64386, -58.561755, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11772742664', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.641817, -58.563834, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11772756990', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.642001, -58.56404, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11807312670', 'COMPETENCIA', 'ALMACEN', 'FrutMart', -34.64277, -58.562759, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11866562370', 'COMPETENCIA', 'ALMACEN', 'Dulce Virginia', -34.643026, -58.562695, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11866565370', 'COMPETENCIA', 'ALMACEN', 'Dulce Virginia', -34.642573, -58.564428, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11866565371', 'COMPETENCIA', 'ALMACEN', 'Dulce Virginia', -34.641893, -58.566814, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11917109753', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640173, -58.564656, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11917109758', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.640152, -58.564519, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11963016117', 'COMPETENCIA', 'PAPELERIA', 'Lugar sin nombre', -34.638738, -58.563436, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11963016118', 'COMPETENCIA', 'PAPELERIA', 'Lugar sin nombre', -34.638267, -58.563977, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12086509650', 'COMPETENCIA', 'ALMACEN', 'Top Shop', -34.641068, -58.561317, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12086509650', 'COMPETENCIA', 'KIOSCO', 'Top Shop', -34.641068, -58.561317, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12086534707', 'COMPETENCIA', 'ALMACEN', 'Ramosport', -34.641534, -58.562488, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12086534707', 'COMPETENCIA', 'KIOSCO', 'Ramosport', -34.641534, -58.562488, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12092772645', 'TRANSPORTE', NULL, 'Alsina y Avellaneda', -34.643083, -58.568183, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12101686875', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.641158, -58.564208, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12132165648', 'COMPLEMENTARIO', NULL, 'Pago 24', -34.642717, -58.563052, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12133253165', 'OFICINA', NULL, 'Westech', -34.64265, -58.562908, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12612196766', 'TRANSPORTE', NULL, 'Gaona y Echeverría', -34.637868, -58.564215, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12743708074', 'COMPLEMENTARIO', NULL, 'Banco Piano', -34.641522, -58.569111, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12854367312', 'ESCUELA', NULL, 'Colegio Del Parque', -34.640373, -58.569451, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13238101082', 'TRANSPORTE', NULL, 'Rivadavia y Avenida de Mayo', -34.640974, -58.566151, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13238101084', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.641555, -58.563469, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13295920020', 'OFICINA', NULL, 'Delegación Ramos Mejía Suterh Osperyh', -34.639445, -58.565417, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13323403796', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.641514, -58.56348, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13530602873', 'OFICINA', NULL, 'Galeno', -34.642297, -58.567172, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13723241725', 'COMPETENCIA', 'ALMACEN', 'La Europea', -34.638416, -58.570017, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13755732636', 'COMPETENCIA', 'ALMACEN', 'El Limite', -34.640127, -58.566122, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:13755732636', 'COMPETENCIA', 'KIOSCO', 'El Limite', -34.640127, -58.566122, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:27230263', 'PARQUE', NULL, 'Plaza Domingo Faustino Sarmiento', -34.639584, -58.564594, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:27233077', 'COMPLEMENTARIO', NULL, 'Coto', -34.640502, -58.561876, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:27233077', 'COMPETENCIA', 'ALMACEN', 'Coto', -34.640502, -58.561876, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:46845493', 'PARQUE', NULL, 'Plaza Bartolomé Mitre', -34.64158, -58.560555, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:139537891', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.640539, -58.564229, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:422127731', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.640041, -58.562501, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475148291', 'COMPETENCIA', 'PAPELERIA', 'Las Vegas', -34.641362, -58.563493, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475915330', 'COMPLEMENTARIO', NULL, 'Disco', -34.64198, -58.570803, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475915330', 'COMPETENCIA', 'ALMACEN', 'Disco', -34.64198, -58.570803, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475915335', 'COMPLEMENTARIO', NULL, 'Panda', -34.641714, -58.569184, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475915335', 'COMPETENCIA', 'ALMACEN', 'Panda', -34.641714, -58.569184, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475925106', 'COMPLEMENTARIO', NULL, 'Banco Provincia', -34.641916, -58.564433, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:475925114', 'COMPLEMENTARIO', NULL, 'Dodera', -34.641657, -58.563383, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477529919', 'SALUD', NULL, 'Clinica Privada Mitre', -34.641061, -58.561976, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477540161', 'OFICINA', NULL, 'Prevención Salud', -34.642066, -58.562522, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477542922', 'COMPLEMENTARIO', NULL, 'Banco Santander', -34.643915, -58.562477, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477546187', 'SALUD', NULL, 'Hospital Italiano', -34.644839, -58.565152, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477554899', 'SALUD', NULL, 'Doctores Trovato', -34.645684, -58.564926, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477554906', 'COMPLEMENTARIO', NULL, 'Hipotecario', -34.646183, -58.564916, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:569503533', 'PARQUE', NULL, 'Plazoleta Dante Alighieri', -34.637771, -58.564085, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:574669465', 'ESCUELA', NULL, 'Colegio Santo Domingo', -34.63847, -58.564545, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:574670614', 'ESCUELA', NULL, 'Colegio Parroquial Juan XXIII', -34.638362, -58.565193, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:581329182', 'OFICINA', NULL, 'La Caja', -34.63962, -58.566355, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:581331572', 'ESCUELA', NULL, 'Colegio Medalla Milagrosa de Ramos Mejía', -34.639517, -58.565773, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:582896760', 'COMPETENCIA', 'ALMACEN', 'Dall''Italia', -34.639129, -58.563835, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:585485381', 'ESCUELA', NULL, 'Instituto French', -34.639474, -58.562793, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:585486394', 'ESCUELA', NULL, 'Escuela Juan Bautista de la Salle', -34.63905, -58.562893, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:585486817', 'ESCUELA', NULL, 'Escuela Especial 501 - Doctor Carlos Cometto', -34.638682, -58.562973, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:586881132', 'SALUD', NULL, 'Casa de Auxilio de Ramos Mejia', -34.641367, -58.565348, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:586881135', 'PARQUE', NULL, 'Lugar sin nombre', -34.641617, -58.565292, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:594655057', 'COMPETENCIA', 'ALMACEN', 'Full', -34.637795, -58.564776, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:594655057', 'COMPETENCIA', 'KIOSCO', 'Full', -34.637795, -58.564776, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:599450581', 'COMPETENCIA', 'ALMACEN', 'Pancitos', -34.641689, -58.563768, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:599457836', 'COMPLEMENTARIO', NULL, 'Credicoop', -34.642031, -58.5641, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:599458543', 'ESCUELA', NULL, 'Escuela N°4 José de San Martín', -34.643085, -58.564629, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:731229261', 'COMPLEMENTARIO', NULL, 'Galicia', -34.642483, -58.565752, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1254511859', 'COMPLEMENTARIO', NULL, 'BBVA', -34.642304, -58.565756, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1310778425', 'PARQUE', NULL, 'Plazoleta San Martín', -34.64211, -58.563705, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:1877879', 'TRANSPORTE', NULL, 'Estación Ramos Mejía', -34.6403, -58.563392, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6449502', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.641799, -58.564664, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6450029', 'TRANSPORTE', NULL, 'Av. de Mayo y Rosales', -34.643742, -58.565048, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6532530', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.641564, -58.562746, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6601721', 'TRANSPORTE', NULL, 'Rivadavia y Avenida de Mayo', -34.640968, -58.566366, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6666715', 'TRANSPORTE', NULL, 'Perón y Macho Vidal', -34.638029, -58.565424, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6666717', 'TRANSPORTE', NULL, 'Perón y Solier', -34.638258, -58.567776, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6868142', 'TRANSPORTE', NULL, 'Est. Ramos Mejía', -34.640547, -58.563657, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6897193', 'TRANSPORTE', NULL, 'Coto Ramos', -34.640279, -58.561982, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:7029955', 'TRANSPORTE', NULL, 'Vidal y Gaona', -34.63783, -58.565647, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8141653', 'TRANSPORTE', NULL, 'Ramos Mejía', -34.639893, -58.563896, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14081318', 'TRANSPORTE', NULL, 'Avenida de Mayo', -34.641863, -58.565882, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14081319', 'TRANSPORTE', NULL, 'Belgrano y Avellaneda', -34.642305, -58.568714, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14081320', 'TRANSPORTE', NULL, 'Belgrano y Rondeau', -34.642751, -58.571659, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14100311', 'TRANSPORTE', NULL, 'Alsina y Avenida de Mayo', -34.642641, -58.565133, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14100312', 'TRANSPORTE', NULL, 'Alsina y Rondeau', -34.643564, -58.571182, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14119236', 'TRANSPORTE', NULL, '3 de Febrero y Perón', -34.637839, -58.565028, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14593186', 'TRANSPORTE', NULL, 'Espora y Necochea', -34.644672, -58.566372, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14593187', 'TRANSPORTE', NULL, 'Espora y Avellaneda', -34.644903, -58.56785, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:14593188', 'TRANSPORTE', NULL, 'Espora y Alvear', -34.645123, -58.569282, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:18742705', 'TRANSPORTE', NULL, 'Gaona y Echeverría', -34.637858, -58.564219, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 13, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 12, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 160, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 7, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 14, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 32, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 36, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 9, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 14, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'Ramos Mejía' AND localidad = 'Ramos Mejía';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3138515585', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.67278, -58.561803, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3138515586', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.67392, -58.562298, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3596659053', 'TRANSPORTE', NULL, 'Illia y Entre Ríos', -34.674698, -58.562758, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3596659054', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.674362, -58.562585, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3895106363', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.668656, -58.56075, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:3895106363', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.668656, -58.56075, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4328939173', 'TRANSPORTE', NULL, 'Illia y Perón', -34.673291, -58.561236, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329983537', 'TRANSPORTE', NULL, 'Florencio Varela y Av. Pres. J. D. Perón', -34.671194, -58.563277, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329983538', 'TRANSPORTE', NULL, 'Florencio Varela y Av. Pres. J. D. Perón', -34.671231, -58.563208, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329983541', 'TRANSPORTE', NULL, 'Perón e Illia', -34.672926, -58.561342, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329983542', 'TRANSPORTE', NULL, 'Perón e Illia', -34.672996, -58.561386, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329983547', 'TRANSPORTE', NULL, 'Av. de Mayo y Rincón', -34.667303, -58.560817, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329983548', 'TRANSPORTE', NULL, 'Av. de Mayo y Rincón', -34.667312, -58.560902, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329983550', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.67036, -58.562344, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329983551', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.6704, -58.562284, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329983552', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.670444, -58.562445, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4329983553', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.670492, -58.562387, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4330109514', 'TRANSPORTE', NULL, 'Illia y Entre Ríos', -34.674631, -58.562843, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4330109515', 'TRANSPORTE', NULL, 'Av. Pres. J. D. Perón 2748', -34.672556, -58.561745, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4330109516', 'TRANSPORTE', NULL, 'Av. Pres. J. D. Perón 2748', -34.672585, -58.561783, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4330109517', 'TRANSPORTE', NULL, 'Av. Pres. J. D. Perón y Florencio Varela', -34.671634, -58.562962, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4330109518', 'TRANSPORTE', NULL, 'Av. Pres. J. D. Perón y Florencio Varela', -34.67167, -58.563003, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4330109521', 'TRANSPORTE', NULL, 'Av. de Mayo y Rincón', -34.667916, -58.560651, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4330109522', 'TRANSPORTE', NULL, 'Av. de Mayo y Rincón', -34.667917, -58.560706, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4330109523', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.670155, -58.562012, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4330109524', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.670187, -58.561963, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4693013578', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.667418, -58.560943, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4693091293', 'TRANSPORTE', NULL, 'Lugar sin nombre', -34.669857, -58.557643, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708900714', 'OFICINA', NULL, 'Estudio Juridico Dr. Roberto Bruno', -34.674734, -58.563162, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708900715', 'OFICINA', NULL, 'Inmobiliaria Bruno', -34.674702, -58.56313, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708931072', 'OFICINA', NULL, 'Marmoleria Giglio S.R.L.', -34.672092, -58.562733, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708931084', 'COMPLEMENTARIO', NULL, 'Banco Provincia', -34.672811, -58.561765, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708931292', 'OFICINA', NULL, 'Gestion Laboral', -34.672977, -58.561577, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708948549', 'COMPLEMENTARIO', NULL, 'Luna', -34.672743, -58.560484, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:4708948549', 'COMPETENCIA', 'ALMACEN', 'Luna', -34.672743, -58.560484, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5406479760', 'TRANSPORTE', NULL, 'Illia y Rincón', -34.670134, -58.557759, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5406479761', 'TRANSPORTE', NULL, 'Illia y Rincón', -34.670091, -58.557818, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5406479772', 'TRANSPORTE', NULL, 'Mosconi y Rincón', -34.669856, -58.55747, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:5406479773', 'TRANSPORTE', NULL, 'Mosconi y Rincón', -34.669796, -58.557518, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6179815177', 'TRANSPORTE', NULL, 'Dr. Arturo Illia', -34.673181, -58.560888, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6179815178', 'TRANSPORTE', NULL, 'Dr. Arturo Illia', -34.673236, -58.560945, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6209410496', 'TRANSPORTE', NULL, 'Perón y Salta', -34.674, -58.55979, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6209410497', 'TRANSPORTE', NULL, 'Perón y Salta', -34.674041, -58.559842, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6377268334', 'TRANSPORTE', NULL, 'Illia y Perón', -34.673237, -58.561312, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6517626981', 'TRANSPORTE', NULL, 'Rincón y Mosconi', -34.670126, -58.557505, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:6517650585', 'TRANSPORTE', NULL, 'Rincón y Mosconi', -34.670067, -58.557428, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:7504693164', 'SALUD', NULL, 'Ambulancia Jefatura Provincial', -34.671236, -58.567397, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:9136566883', 'COMPETENCIA', 'PAPELERIA', 'Einstein', -34.671593, -58.563379, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10656619033', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.670122, -58.561701, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10656619034', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.670113, -58.561685, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10656619035', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.670238, -58.562027, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10656619036', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.670199, -58.562061, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10813082492', 'TRANSPORTE', NULL, 'Avenida de Mayo y Páez', -34.666287, -58.561062, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10820803631', 'TRANSPORTE', NULL, 'Hospital Italiano', -34.66843, -58.567435, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:10823070470', 'TRANSPORTE', NULL, 'Hospital Italiano', -34.668858, -58.566709, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11086312813', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.669464, -58.564304, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11086312814', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.669452, -58.564319, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11086312815', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.66944, -58.564335, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11086312816', 'COMPLEMENTARIO', NULL, 'Lugar sin nombre', -34.669427, -58.564351, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11087095292', 'OFICINA', NULL, 'Rectorado', -34.670009, -58.562378, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11145323544', 'TRANSPORTE', NULL, 'Av. Pres. J. D. Perón y Florencio Varela', -34.671398, -58.563486, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11145323545', 'TRANSPORTE', NULL, 'Almafuerte y Varela', -34.673451, -58.56577, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11600089429', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.670154, -58.561925, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:11714854466', 'TRANSPORTE', NULL, 'Avenida de Mayo y Páez', -34.666272, -58.560987, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'node:12513360551', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.67012, -58.561973, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:28901628', 'ESCUELA', NULL, 'Universidad Nacional de La Matanza', -34.667584, -58.566308, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:238680311', 'COMPETENCIA', 'PAPELERIA', 'Fotocopiadora del Mástil', -34.670239, -58.563317, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:238680731', 'OFICINA', NULL, 'Administración', -34.669708, -58.565085, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:257269043', 'SALUD', NULL, 'Hospital Italiano De San Justo', -34.670734, -58.567649, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:476997055', 'COMPLEMENTARIO', NULL, 'Central Oeste', -34.674705, -58.562557, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477551358', 'COMPETENCIA', 'ALMACEN', 'Shell Shop', -34.669994, -58.558156, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477551358', 'COMPETENCIA', 'KIOSCO', 'Shell Shop', -34.669994, -58.558156, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477562661', 'COMPLEMENTARIO', NULL, 'Credicoop', -34.673837, -58.562402, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477562662', 'SALUD', NULL, 'Clinica de Ojos del Oeste', -34.673902, -58.562528, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477562663', 'COMPLEMENTARIO', NULL, 'Farmacia Sorbas', -34.674132, -58.562581, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:477562664', 'SALUD', NULL, 'Clinica Modelo Los Cedros Sa', -34.674233, -58.56273, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:595214336', 'PARQUE', NULL, 'Huerta organica', -34.671788, -58.568531, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:771589235', 'SALUD', NULL, 'Diagnóstico Tesla', -34.673644, -58.561317, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:771589236', 'COMPETENCIA', 'PAPELERIA', 'Papelera Batipel', -34.671626, -58.559147, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194161038', 'OFICINA', NULL, 'Oficina de Socio Culturales', -34.670117, -58.563921, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194161045', 'COMPETENCIA', 'ALMACEN', 'Buffet', -34.670295, -58.563882, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194161045', 'COMPETENCIA', 'KIOSCO', 'Buffet', -34.670295, -58.563882, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194161057', 'OFICINA', NULL, 'Centro de Estudiantes LFU', -34.670331, -58.563922, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194168831', 'OFICINA', NULL, 'Lugar sin nombre', -34.670206, -58.563762, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194168846', 'OFICINA', NULL, 'Formación Continua', -34.669502, -58.564868, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194168863', 'OFICINA', NULL, 'Departamento de Ciencias Economicas', -34.669195, -58.565389, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194168878', 'OFICINA', NULL, 'Dirección de Alumnos', -34.669274, -58.564904, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194168879', 'OFICINA', NULL, 'Supervielle', -34.669564, -58.564195, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194168880', 'OFICINA', NULL, 'ANSES', -34.669528, -58.564225, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194168882', 'OFICINA', NULL, 'Lugar sin nombre', -34.669279, -58.564548, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194531640', 'COMPETENCIA', 'ALMACEN', 'Lugar sin nombre', -34.670569, -58.563053, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1194531640', 'COMPETENCIA', 'KIOSCO', 'Lugar sin nombre', -34.670569, -58.563053, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1204041823', 'OFICINA', NULL, 'ADUNLAM', -34.669862, -58.564522, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1204041852', 'OFICINA', NULL, 'Departamento de Ingeniería e Investigaciones Tecnológicas', -34.670545, -58.563829, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1204041853', 'OFICINA', NULL, 'Laboratorio de Idiomas', -34.669636, -58.564625, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'way:1204041862', 'OFICINA', NULL, 'Dirección de Pedagogia', -34.669643, -58.564755, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6449851', 'TRANSPORTE', NULL, 'Florencio Varela y Perón', -34.671213, -58.563242, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6449853', 'TRANSPORTE', NULL, 'Perón e Illia', -34.672961, -58.561364, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6449856', 'TRANSPORTE', NULL, 'Rincón / Colombia', -34.667307, -58.56086, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6449857', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.670426, -58.562365, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6450033', 'TRANSPORTE', NULL, 'Illia y Entre Ríos', -34.674665, -58.5628, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6450034', 'TRANSPORTE', NULL, 'Perón 2800', -34.672571, -58.561764, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6450035', 'TRANSPORTE', NULL, 'Perón y Florencio Varela', -34.671652, -58.562983, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6450036', 'TRANSPORTE', NULL, 'Rincón / Colombia', -34.667916, -58.560678, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:6450037', 'TRANSPORTE', NULL, 'Universidad de La Matanza', -34.670171, -58.561987, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:8013966', 'TRANSPORTE', NULL, 'Mosconi y Rincón', -34.669826, -58.557494, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO punto_interes_zona
    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)
SELECT id, 'relation:17331205', 'TRANSPORTE', NULL, 'Avenida de Mayo y Páez', -34.66628, -58.561025, 'OSM', '2026-11-10'
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ACTIVIDAD_ACCESO', NULL, 100, 'indice_0_100', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'ESCUELAS', NULL, 1, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'OFICINAS', NULL, 19, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'TRANSPORTE', NULL, 57, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'PARQUES', NULL, 1, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'SALUD', NULL, 5, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPLEMENTARIOS', NULL, 11, 'lugares_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'ALMACEN', 6, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'PAPELERIA', 3, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';
INSERT INTO metrica_zona
    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)
SELECT id, 'COMPETENCIA', 'KIOSCO', 4, 'comercios_500m', 'OSM', '2026-11-10', 80
FROM zona WHERE nombre = 'San Justo - UNLaM' AND localidad = 'San Justo';

