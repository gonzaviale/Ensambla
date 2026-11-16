package com.ensambla.backend.infrastructure.cartel;

import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.ElementoDeCartel;
import com.ensambla.backend.domain.model.cartel.Rectangulo;
import com.ensambla.backend.domain.model.cartel.TextoDeCartel;
import com.ensambla.backend.domain.model.cartel.VisitanteDeElemento;
import com.ensambla.backend.domain.port.DibujanteDeCartelPort;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.stream.Collectors;

/** Dibuja la composición como SVG: un rectángulo o un texto por cada pieza, con los textos escapados. */
@Component
public class DibujanteSvgDeCartel implements DibujanteDeCartelPort, VisitanteDeElemento<String> {

    @Override
    public String dibujar(ComposicionDeCartel composicion) {
        int ancho = composicion.formato().ancho();
        int alto = composicion.formato().alto();
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + ancho + " " + alto + "\" "
                + "font-family=\"'" + composicion.tipografia().familia() + "', sans-serif\">"
                + "<rect width=\"" + ancho + "\" height=\"" + alto + "\" fill=\"" + composicion.fondo() + "\"/>"
                + composicion.elementos().stream().map(this::pieza).collect(Collectors.joining())
                + "</svg>";
    }

    private String pieza(ElementoDeCartel elemento) {
        return elemento.aceptar(this);
    }

    @Override
    public String visitar(Rectangulo rectangulo) {
        return String.format(Locale.ROOT, "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"%s\"/>",
                rectangulo.zona().x(), rectangulo.zona().y(), rectangulo.zona().ancho(), rectangulo.zona().alto(),
                rectangulo.color());
    }

    @Override
    public String visitar(TextoDeCartel texto) {
        return String.format(Locale.ROOT,
                "<text x=\"%.1f\" y=\"%.1f\" font-size=\"%.1f\" font-weight=\"%d\" fill=\"%s\" text-anchor=\"%s\">%s</text>",
                texto.x(), texto.y(), texto.tamanio(), texto.peso(), texto.color(), texto.alineacion().ancla(),
                escapado(texto.texto()));
    }

    private String escapado(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
