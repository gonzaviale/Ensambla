ALTER TABLE cobro_qr ALTER COLUMN alias DROP NOT NULL;

ALTER TABLE cobro_qr
    ADD COLUMN proveedor_orden_id text,
    ADD COLUMN proveedor_pago_id text,
    ADD COLUMN qr_dinamico text,
    ADD COLUMN qr_estatico text,
    ADD COLUMN estado_detalle text;

ALTER TABLE cobro_qr DROP CONSTRAINT ck_cobro_qr_estado;
ALTER TABLE cobro_qr ADD CONSTRAINT ck_cobro_qr_estado
    CHECK (estado IN ('PENDIENTE', 'VENCIDO', 'COBRADO', 'CANCELADO', 'REEMBOLSADO'));

ALTER TABLE cobro_qr DROP CONSTRAINT ck_cobro_qr_confirmacion;
ALTER TABLE cobro_qr ADD CONSTRAINT ck_cobro_qr_confirmacion CHECK (
    (estado IN ('COBRADO', 'REEMBOLSADO')) = (confirmado_en IS NOT NULL AND confirmacion IS NOT NULL));

CREATE UNIQUE INDEX ux_cobro_qr_proveedor_orden
    ON cobro_qr (proveedor_orden_id) WHERE proveedor_orden_id IS NOT NULL;

COMMENT ON COLUMN cobro_qr.proveedor_orden_id IS 'Id de la order QR creada en Mercado Pago.';
COMMENT ON COLUMN cobro_qr.proveedor_pago_id IS 'Id de la transacción de pago dentro de la order.';
COMMENT ON COLUMN cobro_qr.qr_dinamico IS 'Trama qr_data dinámica y de un solo uso devuelta por Mercado Pago.';
COMMENT ON COLUMN cobro_qr.qr_estatico IS 'Trama fija de la caja de Mercado Pago que atiende la misma order híbrida.';
COMMENT ON COLUMN cobro_qr.estado_detalle IS 'Detalle de estado informado por Mercado Pago para conciliación.';
COMMENT ON COLUMN cobro_qr.estado IS 'Estado conciliado: los cobros manuales calculan el vencimiento al leer; Mercado Pago persiste vencido y reembolsado.';
COMMENT ON COLUMN cobro_qr.confirmacion IS 'Origen de la acreditación: MANUAL para el flujo anterior o MERCADO_PAGO para una order verificada.';
