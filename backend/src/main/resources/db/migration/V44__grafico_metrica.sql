-- V44: cómo se dibuja cada métrica a medida en su solapa del inicio. Vacío mientras la persona no eligió: el dominio usa
-- el de siempre para su reparto (sin repartir, el número; repartida, las barras).

ALTER TABLE metrica
    ADD COLUMN grafico text CHECK (grafico IN ('NUMERO', 'BARRAS', 'DONA', 'LINEA'));

COMMENT ON COLUMN metrica.grafico IS 'Gráfico elegido (NUMERO, BARRAS, DONA, LINEA); NULL = el de siempre según agrupar.';
