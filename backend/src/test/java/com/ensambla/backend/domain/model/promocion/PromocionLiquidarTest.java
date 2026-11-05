package com.ensambla.backend.domain.model.promocion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PromocionDePrueba.ACEITE_ID;
import static com.ensambla.backend.support.PromocionDePrueba.ARROZ_ID;
import static com.ensambla.backend.support.PromocionDePrueba.FIDEOS_ID;
import static com.ensambla.backend.support.PromocionDePrueba.SAL_ID;
import static com.ensambla.backend.support.PromocionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.PromocionDePrueba.contexto;
import static com.ensambla.backend.support.PromocionDePrueba.producto;
import static com.ensambla.backend.support.PromocionDePrueba.ventas;
import static org.assertj.core.api.Assertions.assertThat;

class PromocionLiquidarTest extends ReglaPromocionContratoTest {

    @Override
    protected ReglaPromocion crear() {
        return new PromocionLiquidar();
    }

    @Override
    protected ContextoPromocion contextoQueAplica() {
        return contexto(
                List.of(producto(YERBA_ID, "Yerba", "3600", "10"), producto(FIDEOS_ID, "Fideos", "1100", "30")),
                ventas(YERBA_ID, "8"));
    }

    @Test
    @DisplayName("Propone los productos con stock que no se vendieron, los de más stock primero")
    void evaluarShouldProposeProductsWithStockThatDidNotSell() {
        ContextoPromocion contexto = contexto(
                List.of(producto(YERBA_ID, "Yerba", "3600", "10"), producto(FIDEOS_ID, "Fideos", "1100", "30"),
                        producto(ACEITE_ID, "Aceite", "2000", "50")),
                ventas(YERBA_ID, "8"));

        Optional<Promocion> result = crear().evaluar(contexto);

        assertThat(result).hasValueSatisfying(promocion -> {
            assertThat(promocion.clave()).isEqualTo("liquidar");
            assertThat(promocion.productos()).extracting(ProductoPromocionado::nombre)
                    .containsExactly("Aceite", "Fideos");
        });
    }

    @Test
    @DisplayName("Con el mismo stock se ordenan por nombre y se proponen hasta tres")
    void evaluarShouldLimitTheListToThreeProducts() {
        ContextoPromocion contexto = contexto(
                List.of(producto(FIDEOS_ID, "Fideos", "1100", "30"), producto(ACEITE_ID, "Aceite", "2000", "30"),
                        producto(SAL_ID, "Sal", "500", "30"), producto(YERBA_ID, "Yerba", "3600", "30"),
                        producto(ARROZ_ID, "Arroz", "900", "30")),
                ventas(YERBA_ID, "5"));

        Optional<Promocion> result = crear().evaluar(contexto);

        assertThat(result).hasValueSatisfying(promocion -> assertThat(promocion.productos())
                .extracting(ProductoPromocionado::nombre).containsExactly("Aceite", "Arroz", "Fideos"));
    }

    @Test
    @DisplayName("Un producto sin stock no se liquida")
    void evaluarShouldIgnoreProductsWithoutStock() {
        ContextoPromocion contexto = contexto(
                List.of(producto(YERBA_ID, "Yerba", "3600", "10"), producto(FIDEOS_ID, "Fideos", "1100", "0")),
                ventas(YERBA_ID, "8"));

        Optional<Promocion> result = crear().evaluar(contexto);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Sin ventas anotadas no se sabe qué está parado y no propone nada")
    void evaluarShouldBeEmptyWhenThereAreNoSalesToCompareWith() {
        ContextoPromocion contexto = contexto(List.of(producto(FIDEOS_ID, "Fideos", "1100", "30")));

        Optional<Promocion> result = crear().evaluar(contexto);

        assertThat(result).isEmpty();
    }
}
