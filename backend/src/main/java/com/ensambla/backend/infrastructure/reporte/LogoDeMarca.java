package com.ensambla.backend.infrastructure.reporte;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;
import java.util.List;

import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.AMARILLO;
import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.CELESTE;
import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.CORAL;
import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.NARANJA;
import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.PETROLEO;

/** Dibuja los cinco bloques del logo de Ensambla dentro de la celda donde se lo ponga, a la medida de su alto. */
final class LogoDeMarca implements PdfPCellEvent {

    private static final float ANCHO = 320;
    private static final float ALTO = 340;
    private static final float REDONDEO = 20;

    private static final List<Bloque> BLOQUES = List.of(
            new Bloque(0, 0, 150, 100, PETROLEO),
            new Bloque(170, 120, 150, 100, CELESTE),
            new Bloque(0, 120, 150, 220, NARANJA),
            new Bloque(170, 0, 150, 100, CORAL),
            new Bloque(170, 240, 150, 100, AMARILLO));

    @Override
    public void cellLayout(PdfPCell celda, Rectangle posicion, PdfContentByte[] lienzos) {
        PdfContentByte lienzo = lienzos[PdfPTable.BACKGROUNDCANVAS];
        float escala = Math.min(posicion.getWidth() / ANCHO, posicion.getHeight() / ALTO);
        BLOQUES.forEach(bloque -> bloque.dibujarEn(lienzo, posicion.getLeft(), posicion.getBottom(), escala));
    }

    private record Bloque(float x, float y, float ancho, float alto, Color color) {

        void dibujarEn(PdfContentByte lienzo, float izquierda, float abajo, float escala) {
            lienzo.setColorFill(color);
            lienzo.roundRectangle(izquierda + x * escala, abajo + y * escala, ancho * escala, alto * escala,
                    REDONDEO * escala);
            lienzo.fill();
        }
    }
}
