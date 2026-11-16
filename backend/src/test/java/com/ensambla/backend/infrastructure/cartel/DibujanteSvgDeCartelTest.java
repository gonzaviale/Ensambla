package com.ensambla.backend.infrastructure.cartel;

import com.ensambla.backend.domain.model.cartel.AlineacionDeTexto;
import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.FormatoDeCartel;
import com.ensambla.backend.domain.model.cartel.Rectangulo;
import com.ensambla.backend.domain.model.cartel.TextoDeCartel;
import com.ensambla.backend.domain.model.cartel.VarianteDeComposicion;
import com.ensambla.backend.domain.model.cartel.Zona;
import com.ensambla.backend.domain.model.marca.ColorHex;
import com.ensambla.backend.domain.model.marca.Tipografia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DibujanteSvgDeCartelTest {

    private final DibujanteSvgDeCartel dibujante = new DibujanteSvgDeCartel();

    private String svg;

    @Test
    @DisplayName("Dibuja el fondo, los rectángulos y los textos con su tipografía")
    void dibujarShouldDrawEveryPiece() {
        givenUnaComposicionCon("Oferta");

        whenSeDibuja();

        assertThat(svg).startsWith("<svg").contains("viewBox=\"0 0 1080 1080\"").contains("'Poppins'")
                .contains("fill=\"#FFFDFA\"")
                .contains("<rect x=\"10.0\" y=\"20.0\" width=\"300.0\" height=\"400.0\" fill=\"#00506A\"/>")
                .contains("text-anchor=\"middle\">Oferta</text>").endsWith("</svg>");
    }

    @Test
    @DisplayName("Escapa los caracteres que el texto de la persona podría usar para romper el dibujo")
    void dibujarShouldEscapeText() {
        givenUnaComposicionCon("<script>\"a\" & b</script>");

        whenSeDibuja();

        assertThat(svg).doesNotContain("<script>")
                .contains("&lt;script&gt;&quot;a&quot; &amp; b&lt;/script&gt;");
    }

    private void givenUnaComposicionCon(String texto) {
        composicion = new ComposicionDeCartel(FormatoDeCartel.CUADRADO, VarianteDeComposicion.BLOQUE,
                ColorHex.de("#FFFDFA"), Tipografia.POPPINS, List.of(
                new Rectangulo(new Zona(10, 20, 300, 400), ColorHex.de("#00506A")),
                new TextoDeCartel(texto, 540, 100, 60, 700, ColorHex.de("#FFFFFF"), AlineacionDeTexto.CENTRO)));
    }

    private ComposicionDeCartel composicion;

    private void whenSeDibuja() {
        svg = dibujante.dibujar(composicion);
    }
}
