-- Ensambla · modelo del frontend nuevo y motor de monitoreo (propuesta de tema v1.3)
-- Suma: zona y ubicación del negocio, secciones y atributos de bloque/campo/registro del front, preferencias de
-- la cuenta, avisos y agregados de zona. V1–V8 permanecen intactas; todo cambio sobre tablas existentes tiene
-- DEFAULT o es nullable, así lo que ya existe sigue funcionando.

-- ===== Zona (global): el barrio donde se agrupan los comercios =====
CREATE TABLE zona (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre          text NOT NULL CHECK (length(btrim(nombre)) BETWEEN 1 AND 120),
    localidad       text NOT NULL,
    partido         text NOT NULL,
    provincia       text NOT NULL,
    latitud         numeric(9, 6) NOT NULL CHECK (latitud BETWEEN -90 AND 90),
    longitud        numeric(9, 6) NOT NULL CHECK (longitud BETWEEN -180 AND 180),
    radio_metros    integer NOT NULL CHECK (radio_metros > 0),
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_zona_nombre_localidad UNIQUE (nombre, localidad)
);

-- ===== Negocio: rubro, ubicación y pertenencia a una zona =====
ALTER TABLE negocio
    ADD COLUMN rubro text NOT NULL DEFAULT 'OTRO',
    ADD COLUMN direccion text,
    ADD COLUMN localidad text,
    ADD COLUMN latitud numeric(9, 6),
    ADD COLUMN longitud numeric(9, 6),
    ADD COLUMN zona_id uuid REFERENCES zona (id) ON DELETE SET NULL,
    -- Si es false, el negocio no aporta a los agregados de zona (privacidad).
    ADD COLUMN comparte_datos_zona boolean NOT NULL DEFAULT true,
    ADD CONSTRAINT ck_negocio_rubro CHECK (rubro IN ('ALMACEN', 'KIOSCO', 'MINIMERCADO', 'DIETETICA', 'OTRO')),
    ADD CONSTRAINT ck_negocio_coordenadas CHECK (
        (latitud IS NULL) = (longitud IS NULL)
        AND (latitud IS NULL OR (latitud BETWEEN -90 AND 90 AND longitud BETWEEN -180 AND 180)));

-- ===== Sistema (proyecto): plantilla de origen, distribución y opciones del front =====
ALTER TABLE proyecto
    ADD COLUMN plantilla_clave text,
    ADD COLUMN distribucion text NOT NULL DEFAULT 'MOSAICO',
    ADD COLUMN con_movimientos boolean NOT NULL DEFAULT false,
    ADD COLUMN con_gastos boolean NOT NULL DEFAULT false,
    ADD COLUMN es_demo boolean NOT NULL DEFAULT false,
    ADD CONSTRAINT ck_proyecto_plantilla_clave
        CHECK (plantilla_clave IS NULL OR plantilla_clave IN ('ALMACEN', 'DEPOSITO', 'PEDIDOS_Y_CLIENTES')),
    ADD CONSTRAINT ck_proyecto_distribucion CHECK (distribucion IN ('MOSAICO', 'UNA', 'DOS', 'TRES'));

-- ===== Secciones: agrupan bloques dentro de un sistema =====
CREATE TABLE seccion (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id      uuid NOT NULL,
    proyecto_id     uuid NOT NULL,
    nombre          text NOT NULL CHECK (length(btrim(nombre)) BETWEEN 1 AND 120),
    posicion        smallint NOT NULL DEFAULT 0 CHECK (posicion >= 0),
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_seccion_negocio_id UNIQUE (negocio_id, id),
    CONSTRAINT fk_seccion_proyecto FOREIGN KEY (negocio_id, proyecto_id)
        REFERENCES proyecto (negocio_id, id) ON DELETE CASCADE
);

-- ===== Bloque: tipo, color, ancho y contenido; pertenece a una sección =====
ALTER TABLE bloque
    ADD COLUMN seccion_id uuid,
    ADD COLUMN tipo text NOT NULL DEFAULT 'FORMULARIO',
    ADD COLUMN descripcion text NOT NULL DEFAULT '',
    ADD COLUMN color text NOT NULL DEFAULT 'CELESTE',
    ADD COLUMN ancho text NOT NULL DEFAULT 'MEDIO',
    -- Texto de los bloques de tipo NOTA.
    ADD COLUMN contenido text NOT NULL DEFAULT '',
    -- El bloque siguiente se une a éste en la misma fila.
    ADD COLUMN empieza_fila boolean NOT NULL DEFAULT false,
    -- +1 suma stock (entradas), -1 lo descuenta (salidas y ventas).
    ADD COLUMN direccion_stock smallint,
    ADD CONSTRAINT ck_bloque_tipo CHECK (tipo IN
        ('INVENTARIO', 'PEDIDOS', 'CLIENTES', 'VENTAS', 'GASTOS', 'RESUMEN', 'FORMULARIO', 'NOTA', 'ENTRADAS', 'SALIDAS')),
    ADD CONSTRAINT ck_bloque_color CHECK (color IN ('NARANJA', 'CORAL', 'AMARILLO', 'CELESTE', 'PETROLEO')),
    ADD CONSTRAINT ck_bloque_ancho CHECK (ancho IN ('TERCIO', 'MEDIO', 'ANCHO', 'COMPLETO')),
    ADD CONSTRAINT ck_bloque_direccion_stock CHECK (direccion_stock IS NULL OR direccion_stock IN (-1, 1)),
    ADD CONSTRAINT fk_bloque_seccion FOREIGN KEY (negocio_id, seccion_id)
        REFERENCES seccion (negocio_id, id) ON DELETE CASCADE;

-- Los proyectos que ya tienen bloques reciben una sección "Vista general" que los contiene.
INSERT INTO seccion (negocio_id, proyecto_id, nombre, posicion)
SELECT DISTINCT b.negocio_id, b.proyecto_id, 'Vista general', 0
FROM bloque b;

UPDATE bloque b
SET seccion_id = s.id
FROM seccion s
WHERE s.negocio_id = b.negocio_id AND s.proyecto_id = b.proyecto_id;

-- ===== Campo: tipos nuevos, clave semántica y mínimo =====
ALTER TABLE campo DROP CONSTRAINT ck_campo_tipo;

ALTER TABLE campo
    ADD CONSTRAINT ck_campo_tipo CHECK (tipo IN
        ('TEXTO', 'NUMERO', 'FECHA', 'OPCION', 'SI_NO', 'RELACION', 'FORMULA',
         'TEXTO_LARGO', 'MONEDA', 'EMAIL', 'TELEFONO', 'HORA')),
    -- Clave que usan los avisos y la zona para reconocer el dato (name, price, cost, quantity, minimum, product…).
    ADD COLUMN clave text CHECK (clave IS NULL OR clave ~ '^[a-z][a-zA-Z0-9_]{0,39}$'),
    ADD COLUMN minimo numeric(14, 3),
    ADD CONSTRAINT ux_campo_bloque_clave UNIQUE (bloque_id, clave);

-- ===== Registro: marca de ejemplo y origen =====
ALTER TABLE registro
    ADD COLUMN es_ejemplo boolean NOT NULL DEFAULT false,
    -- Quién lo creó cuando no fue una carga manual ("Aviso de Sami", "Lectura de foto"…).
    ADD COLUMN origen text CHECK (origen IS NULL OR length(origen) <= 120);

-- ===== Preferencias de la cuenta =====
CREATE TABLE preferencia_usuario (
    usuario_id          uuid PRIMARY KEY REFERENCES usuario (id) ON DELETE CASCADE,
    avisos_descartados  jsonb NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(avisos_descartados) = 'array'),
    grupos_avisos       jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(grupos_avisos) = 'object'),
    bienvenida_vista    boolean NOT NULL DEFAULT false,
    nombre_preferido    text CHECK (nombre_preferido IS NULL OR length(btrim(nombre_preferido)) BETWEEN 1 AND 40),
    creado_en           timestamptz NOT NULL DEFAULT now(),
    actualizado_en      timestamptz NOT NULL DEFAULT now()
);

-- ===== Avisos del motor de monitoreo =====
CREATE TABLE aviso (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    negocio_id      uuid NOT NULL REFERENCES negocio (id) ON DELETE CASCADE,
    proyecto_id     uuid,
    tipo            text NOT NULL
                    CHECK (tipo IN ('STOCK', 'PRECIO_ZONA', 'ESTANCADO', 'MUY_VENDIDO', 'COMBO', 'CONSUMO_ZONA')),
    grupo           text NOT NULL CHECK (grupo IN ('STOCK', 'VENTAS', 'ZONA')),
    -- 3 urgente, 2 para hoy, 1 para mirar.
    prioridad       smallint NOT NULL CHECK (prioridad BETWEEN 1 AND 3),
    -- Identificador estable del aviso: depende del estado de los datos que lo dispararon.
    huella          text NOT NULL CHECK (length(btrim(huella)) > 0),
    titulo          text NOT NULL,
    detalle         text NOT NULL,
    motivos         jsonb NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(motivos) = 'array'),
    acciones        jsonb NOT NULL DEFAULT '[]'::jsonb CHECK (jsonb_typeof(acciones) = 'array'),
    estado          text NOT NULL DEFAULT 'PENDIENTE'
                    CHECK (estado IN ('PENDIENTE', 'APLICADO', 'DESCARTADO', 'VENCIDO')),
    resuelto_en     timestamptz,
    creado_en       timestamptz NOT NULL DEFAULT now(),
    actualizado_en  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_aviso_negocio_huella UNIQUE (negocio_id, huella),
    CONSTRAINT fk_aviso_proyecto FOREIGN KEY (negocio_id, proyecto_id)
        REFERENCES proyecto (negocio_id, id) ON DELETE SET NULL (proyecto_id),
    CONSTRAINT ck_aviso_resuelto CHECK ((resuelto_en IS NOT NULL) = (estado IN ('APLICADO', 'DESCARTADO')))
);

-- ===== Agregados de zona: datos anónimos por producto, nunca de un comercio puntual =====
CREATE TABLE agregado_zona_producto (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    zona_id               uuid NOT NULL REFERENCES zona (id) ON DELETE CASCADE,
    producto_normalizado  text NOT NULL CHECK (length(btrim(producto_normalizado)) > 0),
    nombre                text NOT NULL,
    -- Mínimo de 3 comercios: la base no guarda un dato que permita identificar a uno.
    comercios             integer NOT NULL CHECK (comercios >= 3),
    comercios_venden      integer NOT NULL CHECK (comercios_venden >= 3 AND comercios_venden <= comercios),
    precio_minimo         numeric(14, 2) NOT NULL CHECK (precio_minimo >= 0),
    precio_promedio       numeric(14, 2) NOT NULL,
    precio_maximo         numeric(14, 2) NOT NULL,
    -- Unidades por semana de un comercio promedio.
    unidades_semana       numeric(10, 2) NOT NULL CHECK (unidades_semana >= 0),
    periodo_desde         date NOT NULL,
    periodo_hasta         date NOT NULL,
    calculado_en          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_agregado_zona_producto UNIQUE (zona_id, producto_normalizado),
    CONSTRAINT ck_agregado_precios CHECK (precio_minimo <= precio_promedio AND precio_promedio <= precio_maximo),
    CONSTRAINT ck_agregado_periodo CHECK (periodo_desde <= periodo_hasta)
);

-- ===== Índices =====
CREATE INDEX ix_negocio_zona ON negocio (zona_id);
CREATE INDEX ix_seccion_proyecto ON seccion (negocio_id, proyecto_id, posicion);
CREATE INDEX ix_bloque_seccion ON bloque (negocio_id, seccion_id, posicion);
CREATE INDEX ix_aviso_negocio_estado ON aviso (negocio_id, estado, prioridad DESC);
CREATE INDEX ix_agregado_zona ON agregado_zona_producto (zona_id);
