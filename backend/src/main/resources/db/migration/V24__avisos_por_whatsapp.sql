-- La persona puede pedir que Sami le mande por WhatsApp, una vez por día, el resumen de lo que encontró.
-- Se guarda en sus preferencias: si lo pidió, y el último día (argentino) en que se lo mandamos para no repetirlo.
ALTER TABLE preferencia_usuario
    ADD COLUMN avisos_por_whatsapp boolean NOT NULL DEFAULT false,
    ADD COLUMN resumen_whatsapp_enviado_el date;
