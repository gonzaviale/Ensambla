-- ENS-98: el importe aprobado es final en ARS, por ciclos de treinta días.
UPDATE plan SET precio_mensual = 14900, moneda = 'ARS', configurado = true, actualizado_en = now()
WHERE codigo = 'PREMIUM' AND NOT configurado;

CREATE TABLE renovacion_premium (
    id uuid PRIMARY KEY,
    negocio_id uuid NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
    proveedor_id text UNIQUE,
    enlace text,
    estado text NOT NULL CHECK (estado IN ('PREPARANDO','PENDIENTE','AUTORIZADA','PAUSADA','CANCELADA')),
    importe numeric(14,2) NOT NULL CHECK (importe > 0),
    moneda text NOT NULL CHECK (moneda = 'ARS'),
    primer_cobro_en timestamptz NOT NULL,
    cancelacion_solicitada boolean NOT NULL DEFAULT false,
    proximo_cobro_en timestamptz,
    modificado_proveedor_en timestamptz,
    sincronizado_en timestamptz,
    creado_en timestamptz NOT NULL,
    actualizado_en timestamptz NOT NULL,
    CONSTRAINT ux_renovacion_negocio_id UNIQUE (negocio_id, id)
);

CREATE UNIQUE INDEX ux_renovacion_abierta ON renovacion_premium(negocio_id) WHERE estado <> 'CANCELADA';
CREATE INDEX ix_renovacion_negocio_fecha ON renovacion_premium(negocio_id, creado_en DESC, id);
CREATE INDEX ix_renovacion_sincronizacion ON renovacion_premium(sincronizado_en NULLS FIRST, id);

CREATE TABLE pago_premium (
    factura_id text PRIMARY KEY,
    negocio_id uuid NOT NULL,
    renovacion_id uuid NOT NULL,
    pago_id text NOT NULL UNIQUE,
    estado text NOT NULL,
    inicio_en timestamptz NOT NULL,
    fin_en timestamptz NOT NULL,
    modificado_proveedor_en timestamptz NOT NULL,
    actualizado_en timestamptz NOT NULL,
    CONSTRAINT fk_pago_renovacion FOREIGN KEY (negocio_id, renovacion_id)
        REFERENCES renovacion_premium(negocio_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_pago_periodo CHECK (fin_en = inicio_en + interval '720 hours')
);

CREATE INDEX ix_pago_vigencia ON pago_premium(negocio_id, fin_en DESC) WHERE estado = 'approved';
CREATE INDEX ix_pago_renovacion ON pago_premium(renovacion_id);
