package com.ensambla.backend.infrastructure.cartel;

import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.Rectangulo;
import com.ensambla.backend.domain.model.cartel.SalidaDeCartel;
import com.ensambla.backend.domain.model.cartel.TextoDeCartel;
import com.ensambla.backend.domain.model.cartel.VisitanteDeElemento;
import com.ensambla.backend.domain.model.marca.ColorHex;
import com.ensambla.backend.domain.port.ExportadorDeCartelPort;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;

/**
 * Dibuja el cartel en una página del tamaño del formato (las hojas se miden en décimas de milímetro) y la guarda como
 * PDF. Los márgenes de impresión ya vienen en la composición: nada importante cae fuera de la zona segura.
 */
@Component
public class ExportadorPdfDeCartel implements ExportadorDeCartelPort {

    private static final double PUNTOS_POR_UNIDAD = 72.0 / 254.0;
    private static final int PESO_NEGRITA = 600;

    @Override
    public SalidaDeCartel salida() {
        return SalidaDeCartel.PDF;
    }

    @Override
    public byte[] exportar(ComposicionDeCartel composicion) {
        return EnMemoria.sinFallar(() -> escribir(composicion));
    }

    private byte[] escribir(ComposicionDeCartel composicion) throws DocumentException {
        Rectangle pagina = new Rectangle((float) (composicion.formato().ancho() * PUNTOS_POR_UNIDAD),
                (float) (composicion.formato().alto() * PUNTOS_POR_UNIDAD));
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(pagina, 0, 0, 0, 0);
        PdfWriter escritor = PdfWriter.getInstance(documento, salida);
        documento.open();
        pintar(escritor.getDirectContent(), composicion.fondo(), 0, 0, pagina.getWidth(), pagina.getHeight());
        PincelDePdf pincel = new PincelDePdf(escritor.getDirectContent(), pagina.getHeight());
        composicion.elementos().forEach(elemento -> elemento.aceptar(pincel));
        documento.close();
        return salida.toByteArray();
    }

    private static void pintar(PdfContentByte lienzo, ColorHex color, double x, double y, double ancho, double alto) {
        lienzo.setColorFill(new Color(color.rgb()));
        lienzo.rectangle((float) x, (float) y, (float) ancho, (float) alto);
        lienzo.fill();
    }

    /** Pinta cada pieza en la página; el eje vertical del PDF va de abajo hacia arriba. */
    private record PincelDePdf(PdfContentByte lienzo, double altoDePagina) implements VisitanteDeElemento<Void> {

        @Override
        public Void visitar(Rectangulo rectangulo) {
            double alto = rectangulo.zona().alto() * PUNTOS_POR_UNIDAD;
            pintar(lienzo, rectangulo.color(), rectangulo.zona().x() * PUNTOS_POR_UNIDAD,
                    altoDePagina - rectangulo.zona().y() * PUNTOS_POR_UNIDAD - alto,
                    rectangulo.zona().ancho() * PUNTOS_POR_UNIDAD, alto);
            return null;
        }

        @Override
        public Void visitar(TextoDeCartel texto) {
            BaseFont letra = letraDe(texto);
            float tamanio = (float) (texto.tamanio() * PUNTOS_POR_UNIDAD);
            double ancho = letra.getWidthPoint(texto.texto(), tamanio);
            lienzo.beginText();
            lienzo.setFontAndSize(letra, tamanio);
            lienzo.setColorFill(new Color(texto.color().rgb()));
            lienzo.setTextMatrix((float) texto.alineacion().inicioDe(texto.x() * PUNTOS_POR_UNIDAD, ancho),
                    (float) (altoDePagina - texto.y() * PUNTOS_POR_UNIDAD));
            lienzo.showText(texto.texto());
            lienzo.endText();
            return null;
        }

        private BaseFont letraDe(TextoDeCartel texto) {
            String nombre = texto.peso() >= PESO_NEGRITA ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA;
            return EnMemoria.sinFallar(() -> BaseFont.createFont(nombre, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED));
        }
    }
}
