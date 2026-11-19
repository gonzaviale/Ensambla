package com.ensambla.backend.infrastructure.reporte;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;

import static com.ensambla.backend.infrastructure.reporte.MarcaEnsambla.NARANJA;

/** El filo naranja del borde izquierdo de una tarjeta: de punta a punta del fondo, sin invadir el margen blanco. */
final class FiloDeTarjeta implements PdfPCellEvent {

    private static final float ANCHO = 3;
    private static final float MARGEN = 3;

    @Override
    public void cellLayout(PdfPCell celda, Rectangle posicion, PdfContentByte[] lienzos) {
        PdfContentByte lienzo = lienzos[PdfPTable.LINECANVAS];
        lienzo.setColorFill(NARANJA);
        lienzo.rectangle(posicion.getLeft() + MARGEN, posicion.getBottom() + MARGEN, ANCHO,
                posicion.getHeight() - 2 * MARGEN);
        lienzo.fill();
    }
}
