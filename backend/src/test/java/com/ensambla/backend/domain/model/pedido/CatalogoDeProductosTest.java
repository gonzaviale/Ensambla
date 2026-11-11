package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.registro.Registro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.PedidoTestData.inventario;
import static com.ensambla.backend.support.PedidoTestData.registro;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogoDeProductosTest {

    @Test
    @DisplayName("Un producto con nombre, precio y stock se puede pedir, con los valores de su fila")
    void productoShouldComeFromTheInventoryRow() {
        Registro yerba = registro("  Yerba Playadito 1 kg ", 3500, 12, false);

        ProductoEnStock producto = ProductoEnStock.de(inventario(yerba), yerba).orElseThrow();

        assertThat(producto.id()).isEqualTo(yerba.getId());
        assertThat(producto.nombre()).isEqualTo("Yerba Playadito 1 kg");
        assertThat(producto.precio()).isEqualByComparingTo("3500");
        assertThat(producto.stock()).isEqualByComparingTo(BigDecimal.valueOf(12));
    }

    @Test
    @DisplayName("Lee los decimales tal como están cargados")
    void productoShouldKeepDecimals() {
        Registro queso = registro("Queso", 2500.5, 1.5, false);

        ProductoEnStock producto = ProductoEnStock.de(inventario(queso), queso).orElseThrow();

        assertThat(producto.precio()).isEqualByComparingTo("2500.5");
        assertThat(producto.stock()).isEqualByComparingTo("1.5");
    }

    @Test
    @DisplayName("No se ofrece lo que no tiene nombre, precio o stock, ni lo que no está cargado como número")
    void productoShouldBeAbsentWhenSomethingIsMissing() {
        List<Registro> incompletos = List.of(
                registro(null, 100, 5, false), registro("  ", 100, 5, false), registro(7, 100, 5, false),
                registro("Sin precio", null, 5, false), registro("Precio cero", 0, 5, false),
                registro("Precio texto", "caro", 5, false), registro("Sin stock cargado", 100, null, false),
                registro("Agotado", 100, 0, false), registro("Stock negativo", 100, -2, false));

        incompletos.forEach(registro ->
                assertThat(ProductoEnStock.de(inventario(registro), registro)).isEmpty());
    }

    @Test
    @DisplayName("El catálogo deja afuera las filas de ejemplo y los productos incompletos, y ordena por nombre")
    void catalogoShouldKeepOnlyRealOrderableProductsSortedByName() {
        Registro yerba = registro("yerba", 3500, 12, false);
        Registro aceite = registro("Aceite", 2000, 4, false);

        List<ProductoEnStock> catalogo = CatalogoDeProductos.de(List.of(
                inventario(yerba, registro("Ejemplo", 1, 1, true), registro("Agotado", 100, 0, false)),
                inventario(aceite)));

        assertThat(catalogo).extracting(ProductoEnStock::nombre).containsExactly("Aceite", "yerba");
    }

    @Test
    @DisplayName("Sin inventarios el catálogo está vacío")
    void catalogoShouldBeEmptyWithoutInventories() {
        assertThat(CatalogoDeProductos.de(List.of())).isEmpty();
    }
}
