package com.ensambla.backend.domain.model.promocion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromocionTest {

    private static final ProductoPromocionado YERBA = new ProductoPromocionado("Yerba", new BigDecimal("3600"));

    @Test
    @DisplayName("Arma la promoción con su clave, su título, su gancho y sus productos")
    void armarShouldBuildThePromotion() {
        Promocion result = Promocion.armar("estrella", "Tu producto estrella", "Lo más vendido", List.of(YERBA));

        assertThat(result).isEqualTo(
                new Promocion("estrella", "Tu producto estrella", "Lo más vendido", List.of(YERBA)));
    }

    @Test
    @DisplayName("Rechaza una promoción sin productos")
    void armarShouldRejectAPromotionWithoutProducts() {
        assertThatThrownBy(() -> Promocion.armar("estrella", "Título", "Gancho", List.of()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("al menos un producto");
    }

    @Test
    @DisplayName("Acepta hasta doce productos y rechaza más")
    void armarShouldAcceptUpToTwelveProducts() {
        List<ProductoPromocionado> doce = productos(12);
        List<ProductoPromocionado> trece = productos(13);

        assertThat(Promocion.armar("lista", "Título", "Gancho", doce).productos()).hasSize(12);
        assertThatThrownBy(() -> Promocion.armar("lista", "Título", "Gancho", trece))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hasta 12 productos");
    }

    // --- arrange ---
    private List<ProductoPromocionado> productos(int cantidad) {
        return IntStream.rangeClosed(1, cantidad)
                .mapToObj(numero -> new ProductoPromocionado("Producto " + numero, BigDecimal.TEN))
                .toList();
    }
}
