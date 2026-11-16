package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextosDeCartelModeloTest {

    private static final DatosDePromocion ACEITE = new DatosDePromocion(
            List.of(TipoDeCartel.PROMO_PRECIO), List.of("Aceite de girasol 900 ml"),
            List.of(new BigDecimal("2500"), new BigDecimal("2000.00"), new BigDecimal("20"), new BigDecimal("22"),
                    new BigDecimal("11")));

    private Set<BigDecimal> numeros;

    private TextosDeCartel textos;

    @Test
    @DisplayName("Lee los números como se escriben en Argentina: miles con punto, decimales con coma, fechas y porcentajes")
    void numerosShouldReadArgentineFormat() {
        whenSeLeen("Antes $2.500, ahora $1.999,90 (20,5 % menos) hasta el 22/11 · 1.5 kg · 12.50");

        assertThat(numeros).containsExactlyInAnyOrder(
                numero("2500"), numero("1999.9"), numero("20.5"), numero("22"), numero("11"), numero("1.5"),
                numero("12.5"));
    }

    @Test
    @DisplayName("Un texto sin números no lee ninguno")
    void numerosShouldBeEmptyWithoutDigits() {
        whenSeLeen("Llegó el invierno");

        assertThat(numeros).isEmpty();
    }

    @Test
    @DisplayName("Los números permitidos son los calculados y los del nombre de los productos, sin importar los ceros")
    void datosShouldAllowCalculatedNumbersAndProductNumbers() {
        assertThat(ACEITE.numerosPermitidos()).contains(numero("2000"), numero("2500"), numero("900"), numero("20"));
        assertThat(ACEITE.primerProducto()).isEqualTo("Aceite de girasol 900 ml");
    }

    @Test
    @DisplayName("Sin productos ni números los datos quedan vacíos y no falla")
    void datosShouldDefaultMissingLists() {
        DatosDePromocion datos = new DatosDePromocion(List.of(TipoDeCartel.HORARIOS), null, null);

        assertThat(datos.primerProducto()).isEmpty();
        assertThat(datos.numerosPermitidos()).isEmpty();
    }

    @Test
    @DisplayName("Los datos exigen al menos un tipo de cartel posible")
    void datosShouldRequireSomeType() {
        assertThatThrownBy(() -> new DatosDePromocion(List.of(), List.of(), List.of()))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new DatosDePromocion(null, List.of(), List.of()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Un texto con los números reales es coherente con los datos")
    void textosShouldAcceptRealNumbers() {
        givenTextos(TipoDeCartel.PROMO_PRECIO, "Aceite a precio de vecino", "20 % menos hasta el 22/11",
                "El aceite de 900 ml pasa de $2.500 a $2.000. ¡Pasá por Don Rufino!");

        textos.exigirCoherentesCon(ACEITE);
    }

    @Test
    @DisplayName("Un número que no coincide con los datos descarta el texto")
    void textosShouldRejectAContradictingNumber() {
        givenTextos(TipoDeCartel.PROMO_PRECIO, "Aceite barato", "30 % menos", "Ahora $2.000");

        assertThatThrownBy(() -> textos.exigirCoherentesCon(ACEITE))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("número");
    }

    @Test
    @DisplayName("Una plantilla que no está entre las posibles descarta el texto")
    void textosShouldRejectAnImpossibleTemplate() {
        givenTextos(TipoDeCartel.COMBO, "Aceite", "Bajada", "Posteo");

        assertThatThrownBy(() -> textos.exigirCoherentesCon(ACEITE))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("plantilla");
    }

    @Test
    @DisplayName("Un texto con marcado, arrobas o enlaces se descarta")
    void textosShouldRejectMarkup() {
        givenTextos(TipoDeCartel.PROMO_PRECIO, "Aceite", "Bajada", "Mirá https://ejemplo.com");

        assertThatThrownBy(() -> textos.exigirCoherentesCon(ACEITE))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("marcado");
    }

    @Test
    @DisplayName("Los textos rechazan lo vacío y lo que pasa el largo máximo")
    void textosShouldRejectEmptyAndTooLong() {
        assertThatThrownBy(() -> new TextosDeCartel(TipoDeCartel.COMBO, " ", "b", "p"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new TextosDeCartel(TipoDeCartel.COMBO, "t", "b", "x".repeat(281)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new TextosDeCartel(TipoDeCartel.COMBO, "t", null, "p"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Los textos de reserva usan la primera plantilla posible y el primer producto")
    void deReservaShouldUseTheFirstTemplate() {
        textos = TextosDeCartel.deReserva(ACEITE, "Don Rufino");

        assertThat(textos.plantilla()).isEqualTo(TipoDeCartel.PROMO_PRECIO);
        assertThat(textos.titular()).isEqualTo("¡Oferta en Aceite de girasol 900 ml!");
        assertThat(textos.textoPosteo()).endsWith("¡Te esperamos en Don Rufino!");
        textos.exigirCoherentesCon(ACEITE);
    }

    @Test
    @DisplayName("Un producto corto no se abrevia y un tipo sin producto usa su titular fijo")
    void tipoShouldBuildItsReserveTitular() {
        assertThat(TipoDeCartel.DOS_POR_UNO.titularDeReservaPara("Medialunas")).isEqualTo("2x1 en Medialunas");
        assertThat(TipoDeCartel.HORARIOS.titularDeReservaPara("")).isEqualTo("Nuestros horarios");
        assertThat(TipoDeCartel.NOVEDAD.bajadaDeReserva()).isEqualTo("Ya podés pasar a verlo");
    }

    @Test
    @DisplayName("Un nombre de producto largo se abrevia en el titular de reserva para que entre en el máximo")
    void tipoShouldAbbreviateALongProduct() {
        String titular = TipoDeCartel.PROMO_PRECIO.titularDeReservaPara("Aceite de girasol alto oleico 900 ml");

        assertThat(titular).isEqualTo("¡Oferta en Aceite de girasol alto…!");
        assertThat(titular.length()).isLessThanOrEqualTo(ContenidoDeCartel.MAXIMO_TITULAR);
    }

    private static BigDecimal numero(String texto) {
        return NumerosDeTexto.normalizado(new BigDecimal(texto));
    }

    private void givenTextos(TipoDeCartel plantilla, String titular, String bajada, String posteo) {
        textos = new TextosDeCartel(plantilla, titular, bajada, posteo);
    }

    private void whenSeLeen(String texto) {
        numeros = NumerosDeTexto.en(texto);
    }
}
