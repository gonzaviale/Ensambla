-- Ensambla · esquema inicial (MVP2 · T-004)
-- Convenciones: ver docs/modelo-datos.md.
--   * ids uuid; textos text; estados text + CHECK en MAYÚSCULAS
--   * creado_en / actualizado_en en toda tabla (los completa la aplicación; el DEFAULT sirve para seeds)
--   * aislamiento por negocio: negocio_id denormalizado + FKs compuestas (negocio_id, x_id),
--     así es imposible que un hijo apunte a un padre de otro negocio.

-- ===== Cuentas y negocios =====
CREATE TABLE usuario (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email             text NOT NULL UNIQUE
                      CHECK (email = lower(btrim(email)) AND length(email) BETWEEN 3 AND 254),
    nombre            text NOT NULL CHECK (length(btrim(nombre)) BETWEEN 1 AND 120),
    hash_contrasenia  text,
    estado            text NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'SUSPENDIDO')),
    creado_en         timestamptz NOT NULL DEFAULT now(),
    actualizado_en    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE negocio (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre          text NOT NULL CHECK (length(btrim(nombre)) BETWEEN 1 AND 120),
    duenio_id       uuid NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now()
);

-- ===== Plantillas (globales: no pertenecen a ningún negocio) =====
CREATE TABLE plantilla (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre          text NOT NULL UNIQUE CHECK (length(btrim(nombre)) BETWEEN 1 AND 120),
    descripcion     text NOT NULL,
    icono           text,
    -- { "bloques": [ { "nombre": ..., "campos": [ { "nombre", "tipo", "obligatorio", "valorInicial", "opciones" } ] } ] }
    estructura      jsonb NOT NULL DEFAULT '{"bloques": []}'::jsonb
                    CHECK (jsonb_typeof(estructura) = 'object'),
    activa          boolean NOT NULL DEFAULT true,
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now()
);

-- ===== Lo que construye cada negocio =====
CREATE TABLE proyecto (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id      uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    nombre          text NOT NULL CHECK (length(btrim(nombre)) BETWEEN 1 AND 120),
    descripcion     text,
    plantilla_id    uuid REFERENCES plantilla (id) ON DELETE SET NULL,
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_proyecto_negocio_id UNIQUE (negocio_id, id)
);

CREATE TABLE bloque (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id      uuid NOT NULL,
    proyecto_id     uuid NOT NULL,
    nombre          text NOT NULL CHECK (length(btrim(nombre)) BETWEEN 1 AND 120),
    posicion        smallint NOT NULL DEFAULT 0 CHECK (posicion >= 0),
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_bloque_negocio_id UNIQUE (negocio_id, id),
    CONSTRAINT ux_bloque_proyecto_nombre UNIQUE (proyecto_id, nombre),
    CONSTRAINT fk_bloque_proyecto FOREIGN KEY (negocio_id, proyecto_id)
        REFERENCES proyecto (negocio_id, id) ON DELETE CASCADE
);

CREATE TABLE campo (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id      uuid NOT NULL,
    bloque_id       uuid NOT NULL,
    nombre          text NOT NULL CHECK (length(btrim(nombre)) BETWEEN 1 AND 120),
    tipo            text NOT NULL CHECK (tipo IN ('TEXTO', 'NUMERO', 'FECHA', 'OPCION', 'SI_NO')),
    obligatorio     boolean NOT NULL DEFAULT false,
    -- Valor inicial serializado como texto ("12", "true", "2026-10-09"); se interpreta según el tipo.
    valor_inicial   text,
    opciones        jsonb NOT NULL DEFAULT '[]'::jsonb,
    posicion        smallint NOT NULL DEFAULT 0 CHECK (posicion >= 0),
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_campo_negocio_id UNIQUE (negocio_id, id),
    CONSTRAINT ux_campo_bloque_nombre UNIQUE (bloque_id, nombre),
    CONSTRAINT fk_campo_bloque FOREIGN KEY (negocio_id, bloque_id)
        REFERENCES bloque (negocio_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_campo_opciones CHECK (
        jsonb_typeof(opciones) = 'array'
        AND (tipo <> 'OPCION' OR jsonb_array_length(opciones) > 0))
);

CREATE TABLE registro (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id      uuid NOT NULL,
    bloque_id       uuid NOT NULL,
    -- Valores indexados por el id del campo: renombrar un campo no rompe los registros.
    valores         jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(valores) = 'object'),
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_registro_bloque FOREIGN KEY (negocio_id, bloque_id)
        REFERENCES bloque (negocio_id, id) ON DELETE CASCADE
);

-- ===== Sesiones (Spring Session JDBC 4.0, PostgreSQL). Flyway es el único dueño del DDL. =====
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);

-- ===== Índices =====
CREATE INDEX ix_negocio_duenio ON negocio (duenio_id);
CREATE INDEX ix_proyecto_negocio ON proyecto (negocio_id, creado_en DESC);
CREATE INDEX ix_proyecto_plantilla ON proyecto (plantilla_id);
CREATE INDEX ix_bloque_proyecto ON bloque (proyecto_id, posicion);
CREATE INDEX ix_campo_bloque ON campo (bloque_id, posicion);
CREATE INDEX ix_registro_bloque ON registro (negocio_id, bloque_id, creado_en DESC);
-- Búsqueda de texto y filtros sobre los valores (T-026).
CREATE INDEX ix_registro_valores ON registro USING gin (valores jsonb_path_ops);
