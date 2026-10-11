-- Ensambla · digitalización del cuaderno con IA (MVP2 · T-109, H-28/H-29/H-30)
-- Flujo: el usuario sube un lote de fotos → un worker las manda a leer a la IA (lectura_ia) →
-- el usuario revisa y confirma → se crean los registros, cada uno vinculado a su foto de origen.
-- Mismas convenciones que V1: negocio_id denormalizado + FKs compuestas para el aislamiento.

-- ===== Lote de fotos subidas en una misma tanda =====
CREATE TABLE lote_subida (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id         uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    -- Bloque al que el usuario eligió sumar las fotos; NULL = la IA propone uno nuevo o busca uno parecido.
    bloque_destino_id  uuid,
    estado             text NOT NULL DEFAULT 'PENDIENTE'
                       CHECK (estado IN ('PENDIENTE', 'PROCESANDO', 'LISTO', 'ERROR', 'CONFIRMADO', 'DESCARTADO')),
    motivo_error       text,
    intentos           smallint NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    procesado_en       timestamptz,
    confirmado_en      timestamptz,
    creado_en          timestamptz NOT NULL DEFAULT now(),
    actualizado_en     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_lote_subida_negocio_id UNIQUE (negocio_id, id),
    CONSTRAINT fk_lote_subida_bloque FOREIGN KEY (negocio_id, bloque_destino_id)
        REFERENCES bloque (negocio_id, id) ON DELETE SET NULL (bloque_destino_id),
    CONSTRAINT ck_lote_subida_error CHECK (estado <> 'ERROR' OR motivo_error IS NOT NULL)
);

-- ===== Cada página fotografiada (el archivo vive en el bucket privado, acá sólo la clave) =====
CREATE TABLE foto_cuaderno (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id            uuid NOT NULL,
    lote_id               uuid NOT NULL,
    clave_almacenamiento  text NOT NULL UNIQUE CHECK (length(btrim(clave_almacenamiento)) > 0),
    tipo_contenido        text NOT NULL
                          CHECK (tipo_contenido IN ('image/jpeg', 'image/png', 'image/webp', 'image/heic', 'application/pdf')),
    tamanio_bytes         bigint NOT NULL CHECK (tamanio_bytes > 0),
    -- Rotación que eligió el usuario antes de enviar (grados horarios).
    rotacion              smallint NOT NULL DEFAULT 0 CHECK (rotacion IN (0, 90, 180, 270)),
    posicion              smallint NOT NULL DEFAULT 0 CHECK (posicion >= 0),
    creado_en             timestamptz NOT NULL DEFAULT now(),
    actualizado_en        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_foto_cuaderno_negocio_id UNIQUE (negocio_id, id),
    CONSTRAINT ux_foto_cuaderno_lote_posicion UNIQUE (lote_id, posicion),
    CONSTRAINT fk_foto_cuaderno_lote FOREIGN KEY (negocio_id, lote_id)
        REFERENCES lote_subida (negocio_id, id) ON DELETE CASCADE
);

-- ===== Cada intento de lectura con IA de un lote (un reintento suma una fila) =====
CREATE TABLE lectura_ia (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id          uuid NOT NULL,
    lote_id             uuid NOT NULL,
    proveedor           text NOT NULL CHECK (length(btrim(proveedor)) > 0),
    modelo              text NOT NULL CHECK (length(btrim(modelo)) > 0),
    version_prompt      text NOT NULL,
    -- Salida estructurada ya validada: { tipoRegistro, bloque { nombre, bloqueExistenteId, campos }, filas, advertencias }.
    -- NULL si el intento falló.
    resultado           jsonb CHECK (resultado IS NULL OR jsonb_typeof(resultado) = 'object'),
    motivo_error        text,
    paginas             smallint NOT NULL CHECK (paginas > 0),
    tokens_entrada      integer NOT NULL DEFAULT 0 CHECK (tokens_entrada >= 0),
    tokens_salida       integer NOT NULL DEFAULT 0 CHECK (tokens_salida >= 0),
    costo_estimado_usd  numeric(12, 6) CHECK (costo_estimado_usd IS NULL OR costo_estimado_usd >= 0),
    duracion_ms         integer NOT NULL DEFAULT 0 CHECK (duracion_ms >= 0),
    creado_en           timestamptz NOT NULL DEFAULT now(),
    actualizado_en      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_lectura_ia_lote FOREIGN KEY (negocio_id, lote_id)
        REFERENCES lote_subida (negocio_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_lectura_ia_resultado_o_error CHECK ((resultado IS NULL) <> (motivo_error IS NULL))
);

-- ===== Trazabilidad: de qué foto salió cada registro (NULL = carga manual) =====
ALTER TABLE registro ADD COLUMN foto_origen_id uuid;
ALTER TABLE registro ADD CONSTRAINT fk_registro_foto_origen FOREIGN KEY (negocio_id, foto_origen_id)
    REFERENCES foto_cuaderno (negocio_id, id) ON DELETE SET NULL (foto_origen_id);

-- ===== Índices =====
-- Cola del worker: lotes pendientes en orden de llegada (T-096).
CREATE INDEX ix_lote_subida_pendiente ON lote_subida (creado_en) WHERE estado = 'PENDIENTE';
CREATE INDEX ix_lote_subida_negocio ON lote_subida (negocio_id, creado_en DESC);
CREATE INDEX ix_lote_subida_bloque ON lote_subida (bloque_destino_id);
CREATE INDEX ix_foto_cuaderno_lote ON foto_cuaderno (negocio_id, lote_id, posicion);
-- Consumo de IA por negocio y período (T-098).
CREATE INDEX ix_lectura_ia_negocio ON lectura_ia (negocio_id, creado_en DESC);
CREATE INDEX ix_lectura_ia_lote ON lectura_ia (lote_id, creado_en DESC);
CREATE INDEX ix_registro_foto_origen ON registro (foto_origen_id);
