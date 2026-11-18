package com.ensambla.backend.infrastructure.reporte;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;

import java.awt.Color;
import java.util.List;

/** Los colores de la marca y la banda de cinco colores que corre al tope de cada página, igual a la de la barra. */
final class MarcaEnsambla {

    static final Color NARANJA = new Color(0xee, 0x80, 0x00);
    static final Color NARANJA_TEXTO = new Color(0xa9, 0x57, 0x00);
    static final Color PETROLEO = new Color(0x00, 0x50, 0x6a);
    static final Color CELESTE = new Color(0x4e, 0xb2, 0xd4);
    static final Color AMARILLO = new Color(0xeb, 0xce, 0x32);
    static final Color CORAL = new Color(0xf8, 0x5f, 0x5a);
    static final Color CREMA = new Color(0xff, 0xf8, 0xf1);
    static final Color TINTA = new Color(0x26, 0x34, 0x43);
    static final Color GRIS = new Color(0x5c, 0x66, 0x6b);
    static final Color LINEA = new Color(0xdd, 0xe3, 0xe1);

    private static final List<Color> COLORES_DE_LA_BANDA = List.of(NARANJA, AMARILLO, CELESTE, PETROLEO, CORAL);
    private static final float ALTO_DE_LA_BANDA = 5;

    private MarcaEnsambla() {
    }

    static void banda(PdfContentByte lienzo, Rectangle pagina) {
        float tramo = pagina.getWidth() / COLORES_DE_LA_BANDA.size();
        for (int posicion = 0; posicion < COLORES_DE_LA_BANDA.size(); posicion++) {
            tramo(lienzo, COLORES_DE_LA_BANDA.get(posicion), pagina.getLeft() + posicion * tramo,
                    pagina.getTop() - ALTO_DE_LA_BANDA, tramo);
        }
    }

    private static void tramo(PdfContentByte lienzo, Color color, float izquierda, float abajo, float ancho) {
        lienzo.setColorFill(color);
        lienzo.rectangle(izquierda, abajo, ancho, ALTO_DE_LA_BANDA);
        lienzo.fill();
    }
}
