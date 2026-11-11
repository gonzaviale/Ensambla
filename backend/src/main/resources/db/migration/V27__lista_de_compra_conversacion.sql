-- V27: la lista de compra que Sami le armó al cliente con el stock del comercio, mientras la conversación espera que la
-- confirme. Cada renglón guarda el producto elegido (con su precio y stock al armarla) y las alternativas de otra marca.

ALTER TABLE conversacion_cliente ADD COLUMN lista jsonb NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN conversacion_cliente.lista IS 'Renglones de la lista de compra armada con el stock del comercio, aún sin confirmar.';
