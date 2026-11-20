-- V45: dos cuentas nuevas para las métricas a medida: GANANCIA (lo vendido menos lo que costó) y COSTO (lo que costó
-- lo vendido). La columna ya es texto; sólo se amplía la lista de valores permitidos.

ALTER TABLE metrica DROP CONSTRAINT metrica_calculo_check;

ALTER TABLE metrica
    ADD CONSTRAINT metrica_calculo_check CHECK (calculo IN ('SUMA', 'PROMEDIO', 'CONTEO', 'UNIDADES', 'GANANCIA', 'COSTO'));
