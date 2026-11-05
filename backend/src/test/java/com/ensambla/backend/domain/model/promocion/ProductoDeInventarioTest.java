package com.ensambla.backend.domain.model.promocion;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.registro.Registro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static com.ensambla.backend.support.PromocionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.PromocionDePrueba.fila;
import static com.ensambla.backend.support.PromocionDePrueba.inventarioCon;
import static org.assertj.core.api.Assertions.assertThat;

class ProductoDeInventarioTest {

    @Test
    @DisplayName("Arma el producto con su nombre, su precio y su cantidad")
    void deShouldBuildTheProduct() {
        Registro yerba = fila(YERBA_ID, "  Yerba Playadito  ", 3600, 10);

        Optional<ProductoDeInventario> result = ProductoDeInventario.de(inventarioCon(yerba), yerba);

        assertThat(result).contains(new ProductoDeInventario(
                YERBA_ID, "Yerba Playadito", new BigDecimal("3600"), new BigDecimal("10")));
    }

    @Test
    @DisplayName("Sin cantidad cargada cuenta como cero")
    void deShouldCountAMissingQuantityAsZero() {
        Registro yerba = fila(YERBA_ID, "Yerba", 3600, null);

        Optional<ProductoDeInventario> result = ProductoDeInventario.de(inventarioCon(yerba), yerba);

        assertThat(result).hasValueSatisfying(producto -> assertThat(producto.cantidad()).isZero());
    }

    @Test
    @DisplayName("Sin nombre o con el nombre en blanco no se puede promocionar")
    void deShouldBeEmptyWithoutAName() {
        Registro sinNombre = fila(YERBA_ID, null, 3600, 10);
        Registro enBlanco = fila(YERBA_ID, "  ", 3600, 10);
        Registro numerico = fila(YERBA_ID, 123, 3600, 10);
        BloqueInventario inventario = inventarioCon(sinNombre);

        assertThat(ProductoDeInventario.de(inventario, sinNombre)).isEmpty();
        assertThat(ProductoDeInventario.de(inventario, enBlanco)).isEmpty();
        assertThat(ProductoDeInventario.de(inventario, numerico)).isEmpty();
    }

    @Test
    @DisplayName("Sin precio, o con un precio en cero o negativo, no se puede promocionar")
    void deShouldBeEmptyWithoutAPositivePrice() {
        Registro sinPrecio = fila(YERBA_ID, "Yerba", null, 10);
        Registro enCero = fila(YERBA_ID, "Yerba", 0, 10);
        Registro negativo = fila(YERBA_ID, "Yerba", -5, 10);
        BloqueInventario inventario = inventarioCon(sinPrecio);

        assertThat(ProductoDeInventario.de(inventario, sinPrecio)).isEmpty();
        assertThat(ProductoDeInventario.de(inventario, enCero)).isEmpty();
        assertThat(ProductoDeInventario.de(inventario, negativo)).isEmpty();
    }

    @Test
    @DisplayName("Se pasa a producto promocionado con el mismo nombre y precio")
    void promocionadoShouldKeepNameAndPrice() {
        ProductoDeInventario producto = new ProductoDeInventario(
                YERBA_ID, "Yerba", new BigDecimal("3600"), BigDecimal.TEN);

        ProductoPromocionado result = producto.promocionado();

        assertThat(result).isEqualTo(new ProductoPromocionado("Yerba", new BigDecimal("3600")));
    }
}
