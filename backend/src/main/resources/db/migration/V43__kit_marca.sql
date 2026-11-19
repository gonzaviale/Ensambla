-- V34: el kit de marca del negocio (ENS-362 / T-263): logo, dos colores, tipografía y tono. Un kit por negocio; de él
-- salen los carteles y promociones (H-62). El logo se guarda en la base (hasta 2 MB, PNG o JPG).

CREATE TABLE kit_marca (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id       uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    color_principal  text NOT NULL CHECK (color_principal ~ '^#[0-9A-F]{6}$'),
    color_secundario text NOT NULL CHECK (color_secundario ~ '^#[0-9A-F]{6}$'),
    tipografia       text NOT NULL CHECK (tipografia IN ('PLUS_JAKARTA_SANS', 'MONTSERRAT', 'POPPINS',
                                                         'PLAYFAIR_DISPLAY', 'ROBOTO_SLAB', 'NUNITO')),
    tono             text NOT NULL CHECK (tono IN ('CERCANO', 'PROFESIONAL', 'DIVERTIDO')),
    logo             bytea CHECK (logo IS NULL OR octet_length(logo) <= 2097152),
    logo_tipo_mime   text CHECK (logo_tipo_mime IN ('image/png', 'image/jpeg')),
    creado_en        timestamptz NOT NULL DEFAULT now(),
    actualizado_en   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_kit_marca_colores_distintos CHECK (color_principal <> color_secundario),
    CONSTRAINT ck_kit_marca_logo_con_tipo CHECK ((logo IS NULL) = (logo_tipo_mime IS NULL))
);

CREATE UNIQUE INDEX ux_kit_marca_negocio ON kit_marca (negocio_id);
