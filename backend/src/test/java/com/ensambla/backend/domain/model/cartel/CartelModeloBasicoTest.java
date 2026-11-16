package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.marca.ColorHex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartelModeloBasicoTest {

    private static final ColorHex BLANCO = ColorHex.de("#FFFFFF");
    private static final ColorHex NEGRO = ColorHex.de("#000000");
    private static final Zona HOJA = new Zona(0, 0, 1000, 2000);

    private ContenidoDeCartel contenido;

    private TextoDeCartel texto;

    @Test
    @DisplayName("Cada formato conoce su medida, su margen y su tamaño mínimo de texto")
    void formatoShouldKnowItsMeasures() {
        assertThat(FormatoDeCartel.A4.ancho()).isEqualTo(2100);
        assertThat(FormatoDeCartel.A4.alto()).isEqualTo(2970);
        assertThat(FormatoDeCartel.A4.margen()).isEqualTo(100);
        assertThat(FormatoDeCartel.CUADRADO.tamanioMinimoDeTexto()).isEqualTo(28);
    }

    @Test
    @DisplayName("Un tipo de cartel admite hasta su máximo de líneas y rechaza más")
    void tipoShouldLimitTheLines() {
        TipoDeCartel.LISTA_PRECIOS.exigirLineasAdmitidas(10);

        assertThatThrownBy(() -> TipoDeCartel.PROMO_PRECIO.exigirLineasAdmitidas(2))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("hasta 1");
    }

    @Test
    @DisplayName("El contenido recorta los espacios y toma los textos ausentes como vacíos")
    void contenidoShouldTrimAndDefaultMissingTexts() {
        whenSeArmaElContenido(null, "  Titular  ", null, null, null, null);

        assertThat(contenido.titular()).isEqualTo("Titular");
        assertThat(contenido.negocio()).isEmpty();
        assertThat(contenido.lineas()).isEmpty();
    }

    @Test
    @DisplayName("El contenido rechaza un titular, una bajada o una línea que pasan su máximo")
    void contenidoShouldRejectTooLongTexts() {
        String largo = "x".repeat(91);

        assertThatThrownBy(() -> whenSeArmaElContenido("N", largo, "", "", "", List.of()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("titular");
        assertThatThrownBy(() -> whenSeArmaElContenido("N", "T", "", "", "", List.of(largo)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("línea");
    }

    @Test
    @DisplayName("La luminancia va de 0 en el negro a 1 en el blanco y el contraste de 1 a 21")
    void colorShouldMeasureLuminanceAndContrast() {
        assertThat(NEGRO.luminancia()).isZero();
        assertThat(BLANCO.luminancia()).isEqualTo(1.0);
        assertThat(BLANCO.contrasteCon(NEGRO)).isEqualTo(21.0);
        assertThat(ColorHex.de("#FF0000").luminancia()).isBetween(0.2, 0.22);
    }

    @Test
    @DisplayName("Mezclar un color con otro lo acerca en la proporción pedida")
    void colorShouldMixTowardsAnother() {
        assertThat(NEGRO.mezcladoCon(BLANCO, 0.5).valor()).isEqualTo("#808080");
        assertThat(NEGRO.mezcladoCon(BLANCO, 0).valor()).isEqualTo("#000000");
    }

    @Test
    @DisplayName("Una zona se achica por los lados y se corta en franjas")
    void zonaShouldShrinkAndSlice() {
        assertThat(HOJA.interior(100)).isEqualTo(new Zona(100, 100, 800, 1800));
        assertThat(HOJA.franja(0.25, 0.5)).isEqualTo(new Zona(0, 500, 1000, 1000));
    }

    @Test
    @DisplayName("El texto centrado se escribe en el medio de la zona y el alineado a la izquierda, en su borde")
    void alineacionShouldPickTheHorizontalPoint() {
        assertThat(AlineacionDeTexto.CENTRO.puntoDe(HOJA)).isEqualTo(500);
        assertThat(AlineacionDeTexto.IZQUIERDA.puntoDe(HOJA)).isZero();
        assertThat(AlineacionDeTexto.CENTRO.ancla()).isEqualTo("middle");
    }

    @Test
    @DisplayName("Un texto que entra queda igual")
    void textoShouldStayWhenItFits() {
        givenUnTexto("Hola", 50);

        whenSeAjustaAlAncho(1000);

        assertThat(texto.texto()).isEqualTo("Hola");
        assertThat(texto.tamanio()).isEqualTo(50);
    }

    @Test
    @DisplayName("Un texto largo se achica hasta que entra")
    void textoShouldShrinkWhenTooWide() {
        givenUnTexto("Una frase bastante larga", 100);

        whenSeAjustaAlAncho(600);

        assertThat(texto.tamanio()).isBetween(40.0, 50.0);
        assertThat(texto.texto()).isEqualTo("Una frase bastante larga");
    }

    @Test
    @DisplayName("Un texto que no entra ni en el tamaño mínimo se corta con puntos suspensivos")
    void textoShouldBeCutAtTheMinimumSize() {
        givenUnTexto("Una frase bastante larga", 100);

        whenSeAjustaAlAncho(150);

        assertThat(texto.tamanio()).isEqualTo(28);
        assertThat(texto.texto()).endsWith("…").hasSizeLessThan(24);
    }

    private void whenSeArmaElContenido(String negocio, String titular, String bajada, String protagonista,
                                       String detalle, List<String> lineas) {
        contenido = new ContenidoDeCartel(negocio, titular, bajada, protagonista, detalle, lineas);
    }

    private void givenUnTexto(String palabras, double tamanio) {
        texto = new TextoDeCartel(palabras, 0, 0, tamanio, 700, NEGRO, AlineacionDeTexto.IZQUIERDA);
    }

    private void whenSeAjustaAlAncho(double ancho) {
        texto = texto.ajustadoA(ancho, 28);
    }
}
