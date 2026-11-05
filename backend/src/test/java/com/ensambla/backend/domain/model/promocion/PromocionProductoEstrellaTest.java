package com.ensambla.backend.domain.model.promocion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PromocionDePrueba.ACEITE_ID;
import static com.ensambla.backend.support.PromocionDePrueba.FIDEOS_ID;
import static com.ensambla.backend.support.PromocionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.PromocionDePrueba.contexto;
import static com.ensambla.backend.support.PromocionDePrueba.producto;
import static com.ensambla.backend.support.PromocionDePrueba.ventas;
import static org.assertj.core.api.Assertions.assertThat;

class PromocionProductoEstrellaTest extends ReglaPromocionContratoTest {

    @Override
    protected ReglaPromocion crear() {
        return new PromocionProductoEstrella();
    }

    @Override
    protected ContextoPromocion contextoQueAplica() {
        return contexto(List.of(producto(YERBA_ID, "Yerba", "3600", "10")), ventas(YERBA_ID, "8"));
    }

    @Test
    @DisplayName("Propone el producto que más unidades vendió, con su precio")
    void evaluarShouldProposeTheBestSeller() {
        ContextoPromocion contexto = contexto(
                List.of(producto(YERBA_ID, "Yerba", "3600", "10"), producto(FIDEOS_ID, "Fideos", "1100", "30")),
                ventas(YERBA_ID, "8"), ventas(FIDEOS_ID, "25"));

        Optional<Promocion> result = crear().evaluar(contexto);

        assertThat(result).hasValueSatisfying(promocion -> {
            assertThat(promocion.clave()).isEqualTo("estrella");
            assertThat(promocion.productos()).containsExactly(
                    new ProductoPromocionado("Fideos", new BigDecimal("1100")));
        });
    }

    @Test
    @DisplayName("Con dos productos igual de vendidos gana el primero por orden alfabético")
    void evaluarShouldBreakTiesAlphabetically() {
        ContextoPromocion contexto = contexto(
                List.of(producto(FIDEOS_ID, "Fideos", "1100", "30"), producto(YERBA_ID, "Yerba", "3600", "10"),
                        producto(ACEITE_ID, "Aceite", "2000", "5")),
                ventas(YERBA_ID, "8"), ventas(FIDEOS_ID, "8"));

        Optional<Promocion> result = crear().evaluar(contexto);

        assertThat(result).hasValueSatisfying(promocion ->
                assertThat(promocion.productos()).extracting(ProductoPromocionado::nombre).containsExactly("Fideos"));
    }

    @Test
    @DisplayName("Sin ventas anotadas no hay un producto estrella")
    void evaluarShouldBeEmptyWhenNothingWasSold() {
        ContextoPromocion contexto = contexto(List.of(producto(YERBA_ID, "Yerba", "3600", "10")));

        Optional<Promocion> result = crear().evaluar(contexto);

        assertThat(result).isEmpty();
    }
}
