-- Datos híbridos para «Dónde abrir»: snapshots trazables de fuentes públicas y métricas agregadas de Ensambla.
-- Las búsquedas de usuarios leen únicamente PostgreSQL; las fuentes externas se actualizan de forma controlada.

CREATE TABLE fuente_dato_zona (
    codigo          text PRIMARY KEY,
    nombre          text NOT NULL CHECK (length(btrim(nombre)) BETWEEN 1 AND 160),
    tipo            text NOT NULL CHECK (tipo IN ('ENSAMBLA', 'OFICIAL', 'COMUNITARIA', 'USUARIO', 'DEMOSTRACION')),
    url             text,
    licencia        text,
    consultado_en   date NOT NULL,
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_fuente_dato_zona_codigo CHECK (codigo ~ '^[A-Z][A-Z0-9_]{1,39}$'),
    CONSTRAINT ck_fuente_dato_zona_url CHECK (url IS NULL OR url ~ '^https://')
);

CREATE TABLE metrica_zona (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    zona_id         uuid NOT NULL REFERENCES zona (id) ON DELETE CASCADE,
    tipo            text NOT NULL CHECK (tipo IN (
        'ACTIVIDAD_ACCESO', 'DENSIDAD', 'ESCUELAS', 'OFICINAS', 'TRANSPORTE', 'PARQUES', 'SALUD',
        'COMPLEMENTARIOS', 'COMPETENCIA', 'PERFIL_SOCIOECONOMICO')),
    rubro           text CHECK (rubro IS NULL OR rubro IN ('ALMACEN', 'PAPELERIA', 'KIOSCO')),
    valor           numeric(14, 3) NOT NULL CHECK (valor >= 0),
    unidad          text NOT NULL CHECK (length(btrim(unidad)) BETWEEN 1 AND 60),
    fuente_codigo   text NOT NULL REFERENCES fuente_dato_zona (codigo),
    observada_en    date NOT NULL,
    confianza       smallint NOT NULL DEFAULT 100 CHECK (confianza BETWEEN 0 AND 100),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_metrica_zona_rubro CHECK ((tipo = 'COMPETENCIA') = (rubro IS NOT NULL))
);

CREATE UNIQUE INDEX ux_metrica_zona_fuente
    ON metrica_zona (zona_id, tipo, COALESCE(rubro, ''), fuente_codigo);
CREATE INDEX ix_metrica_zona_consulta
    ON metrica_zona (zona_id, tipo, rubro);
CREATE INDEX ix_metrica_zona_fuente
    ON metrica_zona (fuente_codigo);

CREATE TABLE punto_interes_zona (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    zona_id         uuid NOT NULL REFERENCES zona (id) ON DELETE CASCADE,
    clave_externa   text NOT NULL CHECK (length(btrim(clave_externa)) BETWEEN 1 AND 160),
    tipo            text NOT NULL CHECK (tipo IN (
        'COMPETENCIA', 'ESCUELA', 'OFICINA', 'TRANSPORTE', 'PARQUE', 'SALUD', 'COMPLEMENTARIO')),
    rubro           text CHECK (rubro IS NULL OR rubro IN ('ALMACEN', 'PAPELERIA', 'KIOSCO')),
    nombre          text NOT NULL CHECK (length(btrim(nombre)) BETWEEN 1 AND 240),
    latitud         numeric(9, 6) NOT NULL CHECK (latitud BETWEEN -90 AND 90),
    longitud        numeric(9, 6) NOT NULL CHECK (longitud BETWEEN -180 AND 180),
    fuente_codigo   text NOT NULL REFERENCES fuente_dato_zona (codigo),
    observada_en    date NOT NULL,
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_punto_interes_zona_rubro CHECK ((tipo = 'COMPETENCIA') = (rubro IS NOT NULL))
);

CREATE UNIQUE INDEX ux_punto_interes_zona_fuente
    ON punto_interes_zona (zona_id, fuente_codigo, clave_externa, tipo, COALESCE(rubro, ''));
CREATE INDEX ix_punto_interes_zona_consulta
    ON punto_interes_zona (zona_id, tipo, rubro);
CREATE INDEX ix_punto_interes_zona_fuente
    ON punto_interes_zona (fuente_codigo);

INSERT INTO fuente_dato_zona (codigo, nombre, tipo, url, licencia, consultado_en)
VALUES
    ('ENSAMBLA', 'Lecturas agregadas y anónimas de Ensambla', 'ENSAMBLA', NULL, NULL, current_date),
    ('OSM', 'OpenStreetMap', 'COMUNITARIA', 'https://www.openstreetmap.org/', 'ODbL 1.0', current_date),
    ('CABA_DATOS', 'Buenos Aires Data', 'OFICIAL', 'https://data.buenosaires.gob.ar/', 'Consultar por conjunto', current_date),
    ('PBA_DATOS', 'Datos Abiertos de la Provincia de Buenos Aires', 'OFICIAL',
     'https://catalogo.datos.gba.gob.ar/', 'Consultar por conjunto', current_date),
    ('INDEC_CENSO_2022', 'INDEC · Censo Nacional 2022', 'OFICIAL',
     'https://www.indec.gob.ar/indec/web/Nivel4-Tema-2-41-165', NULL, current_date),
    ('USUARIO', 'Dato informado por la persona', 'USUARIO', NULL, NULL, current_date);

-- Papelería es uno de los rubros de «Dónde abrir» y también puede configurarse como rubro del negocio.
ALTER TABLE negocio DROP CONSTRAINT ck_negocio_rubro;
ALTER TABLE negocio ADD CONSTRAINT ck_negocio_rubro
    CHECK (rubro IN ('ALMACEN', 'KIOSCO', 'MINIMERCADO', 'DIETETICA', 'PAPELERIA', 'OTRO'));
