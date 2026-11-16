package com.ensambla.backend.infrastructure.cartel;

import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.ElementoDeCartel;
import com.ensambla.backend.domain.model.cartel.Rectangulo;
import com.ensambla.backend.domain.model.cartel.SalidaDeCartel;
import com.ensambla.backend.domain.model.cartel.TextoDeCartel;
import com.ensambla.backend.domain.model.cartel.VisitanteDeElemento;
import com.ensambla.backend.domain.model.marca.ColorHex;
import com.ensambla.backend.domain.port.ExportadorDeCartelPort;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/** Dibuja el cartel en una imagen del tamaño del formato (un píxel por unidad) y la guarda como PNG. */
@Component
public class ExportadorPngDeCartel implements ExportadorDeCartelPort {

    private static final int PESO_NEGRITA = 600;

    @Override
    public SalidaDeCartel salida() {
        return SalidaDeCartel.PNG;
    }

    @Override
    public byte[] exportar(ComposicionDeCartel composicion) {
        BufferedImage imagen = new BufferedImage(
                composicion.formato().ancho(), composicion.formato().alto(), BufferedImage.TYPE_INT_RGB);
        Graphics2D lienzo = imagen.createGraphics();
        try {
            dibujar(composicion, lienzo);
        } finally {
            lienzo.dispose();
        }
        return comoPng(imagen);
    }

    private void dibujar(ComposicionDeCartel composicion, Graphics2D lienzo) {
        lienzo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        lienzo.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        lienzo.setColor(colorDe(composicion.fondo()));
        lienzo.fillRect(0, 0, composicion.formato().ancho(), composicion.formato().alto());
        PincelDeLienzo pincel = new PincelDeLienzo(lienzo, composicion.tipografia().familia());
        composicion.elementos().forEach(elemento -> dibujar(elemento, pincel));
    }

    private void dibujar(ElementoDeCartel elemento, PincelDeLienzo pincel) {
        elemento.aceptar(pincel);
    }

    private byte[] comoPng(BufferedImage imagen) {
        return EnMemoria.sinFallar(() -> {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        });
    }

    private static Color colorDe(ColorHex color) {
        return new Color(color.rgb());
    }

    /** Pinta cada pieza sobre el lienzo. */
    private record PincelDeLienzo(Graphics2D lienzo, String familia) implements VisitanteDeElemento<Void> {

        @Override
        public Void visitar(Rectangulo rectangulo) {
            lienzo.setColor(colorDe(rectangulo.color()));
            lienzo.fillRect((int) Math.round(rectangulo.zona().x()), (int) Math.round(rectangulo.zona().y()),
                    (int) Math.round(rectangulo.zona().ancho()), (int) Math.round(rectangulo.zona().alto()));
            return null;
        }

        @Override
        public Void visitar(TextoDeCartel texto) {
            lienzo.setFont(new Font(familia, texto.peso() >= PESO_NEGRITA ? Font.BOLD : Font.PLAIN, 1)
                    .deriveFont((float) texto.tamanio()));
            lienzo.setColor(colorDe(texto.color()));
            double ancho = lienzo.getFontMetrics().stringWidth(texto.texto());
            lienzo.drawString(texto.texto(), (float) texto.alineacion().inicioDe(texto.x(), ancho), (float) texto.y());
            return null;
        }
    }
}
