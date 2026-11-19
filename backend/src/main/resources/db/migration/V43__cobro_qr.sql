ALTER TABLE negocio ADD COLUMN cobro_alias text;
ALTER TABLE negocio ADD COLUMN cobro_titular text;

COMMENT ON COLUMN negocio.cobro_alias IS 'Alias o CVU en texto libre donde el comerciante recibe las transferencias del cobro con QR.';
COMMENT ON COLUMN negocio.cobro_titular IS 'Nombre del titular de esa cuenta, tal como lo escribió el comerciante.';

CREATE TABLE cobro_qr (
    id uuid PRIMARY KEY,
    negocio_id uuid NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
    alias text NOT NULL,
    titular text,
    importe numeric(14, 2) NOT NULL,
    estado text NOT NULL,
    creado_en timestamptz NOT NULL,
    vence_en timestamptz NOT NULL,
    confirmado_en timestamptz,
    confirmacion text,
    actualizado_en timestamptz NOT NULL,
    CONSTRAINT ck_cobro_qr_importe CHECK (importe > 0),
    CONSTRAINT ck_cobro_qr_estado CHECK (estado IN ('PENDIENTE', 'COBRADO', 'CANCELADO')),
    CONSTRAINT ck_cobro_qr_confirmacion CHECK (
        (estado = 'COBRADO') = (confirmado_en IS NOT NULL AND confirmacion IS NOT NULL))
);

CREATE INDEX ix_cobro_qr_negocio ON cobro_qr (negocio_id, creado_en DESC);

COMMENT ON TABLE cobro_qr IS 'Cobro de una venta mostrando un QR: lo que el cliente iba a transferir y si el comerciante lo confirmó.';
COMMENT ON COLUMN cobro_qr.alias IS 'Copia del alias o CVU del negocio al generar el cobro; cambiarlo después no altera cobros anteriores.';
COMMENT ON COLUMN cobro_qr.estado IS 'Sólo se guarda PENDIENTE, COBRADO o CANCELADO; el vencimiento se calcula al leer con vence_en.';
COMMENT ON COLUMN cobro_qr.confirmacion IS 'Cómo se confirmó el cobro; MANUAL es el comerciante diciendo que ya le transfirieron.';
