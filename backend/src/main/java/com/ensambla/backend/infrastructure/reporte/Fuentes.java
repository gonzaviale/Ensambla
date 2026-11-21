package com.ensambla.backend.infrastructure.reporte;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;

import java.awt.Color;

import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.GRIS;
import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.NARANJA_TEXTO;
import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.PETROLEO;
import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.TINTA;

/** Los estilos de texto del resumen, todos en Plus Jakarta Sans, la tipografía de Ensambla. */
record Fuentes(Font titulo, Font palabra, Font quien, Font seccion, Font texto, Font negrita, Font encabezado,
               Font nota, Font valor, Font notaClara, Font valorClaro, Font pie) {

    static Fuentes de(RecursosDeMarca recursos) {
        BaseFont normal = recursos.fuente("plus-jakarta-sans-regular.ttf");
        BaseFont fuerte = recursos.fuente("plus-jakarta-sans-bold.ttf");
        BaseFont extra = recursos.fuente("plus-jakarta-sans-extrabold.ttf");
        return new Fuentes(
                fuente(extra, 24, PETROLEO), fuente(extra, 17, PETROLEO), fuente(fuerte, 11, NARANJA_TEXTO),
                fuente(extra, 13, PETROLEO), fuente(normal, 9, TINTA), fuente(fuerte, 9, TINTA),
                fuente(fuerte, 8.5f, Color.WHITE), fuente(normal, 8, GRIS), fuente(extra, 14, PETROLEO),
                fuente(normal, 8, Color.WHITE), fuente(extra, 14, Color.WHITE), fuente(fuerte, 8, PETROLEO));
    }

    private static Font fuente(BaseFont base, float puntos, Color color) {
        return new Font(base, puntos, Font.NORMAL, color);
    }
}
