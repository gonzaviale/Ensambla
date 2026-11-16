package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PromocionesSugeridasTest {

    private final PromocionPorProductoEstancado estancado = new PromocionPorProductoEstancado();
    private final PromocionPorCombo combo = new PromocionPorCombo();

    private Optional<PromocionSugerida> promocion;

    @Test
    @DisplayName("Una promoción calcula el ahorro y el descuento en porcentaje entero, redondeado")
    void promocionShouldCalculateSavingsAndPercentage() {
        PromocionSugerida sugerida = new PromocionSugerida(TipoDeCartel.PROMO_PRECIO, List.of("Aceite"),
                new BigDecimal("2500"), new BigDecimal("2010"));

        assertThat(sugerida.ahorro()).isEqualByComparingTo("490");
        assertThat(sugerida.descuentoPorcentaje()).isEqualByComparingTo("20");
        assertThat(sugerida.datos().numerosPermitidos()).contains(
                NumerosDeTexto.normalizado(new BigDecimal("2500")), NumerosDeTexto.normalizado(new BigDecimal("490")),
                NumerosDeTexto.normalizado(new BigDecimal("20")));
    }

    @Test
    @DisplayName("Un producto caro que no se mueve se promociona con el precio de antes y el sugerido")
    void estancadoShouldBuildAPricePromotion() {
        whenSeLeeElEstancado(avisoDeEstancado(Map.of("precio", 2500, "sugerido", new BigDecimal("2000.00"))));

        assertThat(promocion).get().satisfies(sugerida -> {
            assertThat(sugerida.tipo()).isEqualTo(TipoDeCartel.PROMO_PRECIO);
            assertThat(sugerida.productos()).containsExactly("Aceite de girasol 900 ml");
            assertThat(sugerida.precioAnterior()).isEqualByComparingTo("2500");
            assertThat(sugerida.descuentoPorcentaje()).isEqualByComparingTo("20");
        });
        assertThat(estancado.tipoDeAviso()).isEqualTo(TipoAviso.ESTANCADO);
    }

    @Test
    @DisplayName("Un aviso de estancado sin precio, sin sugerido o con un sugerido que no baja no sugiere nada")
    void estancadoShouldSuggestNothingWithoutUsefulNumbers() {
        whenSeLeeElEstancado(avisoDeEstancado(Map.of("sugerido", 2000)));
        assertThat(promocion).isEmpty();

        whenSeLeeElEstancado(avisoDeEstancado(Map.of("precio", 2500)));
        assertThat(promocion).isEmpty();

        whenSeLeeElEstancado(avisoDeEstancado(Map.of("precio", 2500, "sugerido", 2500)));
        assertThat(promocion).isEmpty();
    }

    @Test
    @DisplayName("Un combo de baja rotación se promociona contra la suma de los precios sueltos")
    void comboShouldBuildAComboPromotion() {
        whenSeLeeElCombo(avisoDeCombo(Map.of("precioA", 900, "precioB", 1600, "sugerido", 2250)));

        assertThat(promocion).get().satisfies(sugerida -> {
            assertThat(sugerida.tipo()).isEqualTo(TipoDeCartel.COMBO);
            assertThat(sugerida.productos()).containsExactly("Alfajor", "Gaseosa 500 ml");
            assertThat(sugerida.precioAnterior()).isEqualByComparingTo("2500");
            assertThat(sugerida.ahorro()).isEqualByComparingTo("250");
        });
        assertThat(combo.tipoDeAviso()).isEqualTo(TipoAviso.COMBO);
    }

    @Test
    @DisplayName("Un aviso de combo sin algún precio o con un combo que no ahorra no sugiere nada")
    void comboShouldSuggestNothingWithoutUsefulNumbers() {
        whenSeLeeElCombo(avisoDeCombo(Map.of("precioB", 1600, "sugerido", 2250)));
        assertThat(promocion).isEmpty();

        whenSeLeeElCombo(avisoDeCombo(Map.of("precioA", 900, "sugerido", 2250)));
        assertThat(promocion).isEmpty();

        whenSeLeeElCombo(avisoDeCombo(Map.of("precioA", 900, "precioB", 1600)));
        assertThat(promocion).isEmpty();

        whenSeLeeElCombo(avisoDeCombo(Map.of("precioA", 900, "precioB", 1600, "sugerido", 2500)));
        assertThat(promocion).isEmpty();
    }

    @Test
    @DisplayName("Las fuentes de promoción son las de productos estancados y combos")
    void fuentesShouldBeEstancadoAndCombo() {
        assertThat(FuentesDePromocion.todas()).extracting(FuenteDePromocion::tipoDeAviso)
                .containsExactly(TipoAviso.ESTANCADO, TipoAviso.COMBO);
    }

    private Aviso avisoDeEstancado(Map<String, Object> datos) {
        return Aviso.builder().tipo(TipoAviso.ESTANCADO).titulo("Aceite de girasol 900 ml está caro y no se mueve")
                .datos(datos).build();
    }

    private Aviso avisoDeCombo(Map<String, Object> datos) {
        return Aviso.builder().tipo(TipoAviso.COMBO).titulo("Alfajor y Gaseosa 500 ml se venden juntos")
                .datos(datos).build();
    }

    private void whenSeLeeElEstancado(Aviso aviso) {
        promocion = estancado.de(aviso);
    }

    private void whenSeLeeElCombo(Aviso aviso) {
        promocion = combo.de(aviso);
    }
}
