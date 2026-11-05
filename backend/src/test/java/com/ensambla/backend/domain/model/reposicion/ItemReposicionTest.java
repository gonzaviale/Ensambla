package com.ensambla.backend.domain.model.reposicion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.registro.Registro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.inventarioCon;
import static com.ensambla.backend.support.ReposicionDePrueba.inventarioSinProveedorCon;
import static com.ensambla.backend.support.ReposicionDePrueba.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemReposicionTest {

    private static final String NOMBRE = "Yerba Playadito 1 kg";
    private static final String PROVEEDOR = "Molinos";

    @Test
    @DisplayName("Arma el item de un producto bajo el mínimo con lo que hay que pedir")
    void deShouldBuildTheItemWhenTheProductIsBelowTheMinimum() {
        Registro yerba = producto(YERBA_ID, NOMBRE, 3, 10, PROVEEDOR);

        Optional<ItemReposicion> result = ItemReposicion.de(inventarioCon(yerba), yerba);

        assertThat(result).hasValueSatisfying(item -> {
            assertThat(item.productoId()).isEqualTo(YERBA_ID);
            assertThat(item.nombre()).isEqualTo(NOMBRE);
            assertThat(item.proveedor()).isEqualTo(PROVEEDOR);
            assertThat(item.cantidadActual()).isEqualByComparingTo("3");
            assertThat(item.cantidadAPedir()).isEqualByComparingTo("17");
            assertThat(item.situacion()).isEqualTo("Quedan 3 (mínimo 10)");
            assertThat(item.urgencia()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("Lee las cantidades con decimales")
    void deShouldReadDecimalQuantities() {
        Registro yerba = producto(YERBA_ID, NOMBRE, 2.5, 10.0, PROVEEDOR);

        Optional<ItemReposicion> result = ItemReposicion.de(inventarioCon(yerba), yerba);

        assertThat(result).hasValueSatisfying(item -> assertThat(item.cantidadActual()).isEqualByComparingTo("2.5"));
    }

    @Test
    @DisplayName("No hay item cuando el stock alcanza")
    void deShouldReturnEmptyWhenThereIsEnoughStock() {
        Registro yerba = producto(YERBA_ID, NOMBRE, 30, 10, PROVEEDOR);

        Optional<ItemReposicion> result = ItemReposicion.de(inventarioCon(yerba), yerba);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("No hay item cuando el producto no tiene cantidad cargada")
    void deShouldReturnEmptyWhenThereIsNoQuantity() {
        Registro yerba = producto(YERBA_ID, NOMBRE, null, 10, PROVEEDOR);

        Optional<ItemReposicion> result = ItemReposicion.de(inventarioCon(yerba), yerba);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Un proveedor en blanco o que no es un texto es lo mismo que no tener proveedor")
    void deShouldTreatBlankOrNonTextSupplierAsNone() {
        Registro enBlanco = producto(YERBA_ID, NOMBRE, 0, 10, "   ");
        Registro numerico = producto(YERBA_ID, NOMBRE, 0, 10, 42);

        Optional<ItemReposicion> blanco = ItemReposicion.de(inventarioCon(enBlanco), enBlanco);
        Optional<ItemReposicion> numero = ItemReposicion.de(inventarioCon(numerico), numerico);

        assertThat(blanco).hasValueSatisfying(item -> assertThat(item.proveedor()).isNull());
        assertThat(numero).hasValueSatisfying(item -> assertThat(item.proveedor()).isNull());
    }

    @Test
    @DisplayName("Sin la columna de proveedor el item queda sin proveedor")
    void deShouldLeaveTheSupplierEmptyWhenTheColumnDoesNotExist() {
        Registro yerba = producto(YERBA_ID, NOMBRE, 0, 10, PROVEEDOR);
        BloqueInventario sinColumna = inventarioSinProveedorCon(yerba);

        Optional<ItemReposicion> result = ItemReposicion.de(sinColumna, yerba);

        assertThat(result).hasValueSatisfying(item -> assertThat(item.proveedor()).isNull());
    }

    @Test
    @DisplayName("Un producto sin nombre se muestra con un nombre genérico")
    void deShouldNameAProductWithoutName() {
        Registro sinNombre = producto(YERBA_ID, " ", 0, 10, PROVEEDOR);

        Optional<ItemReposicion> result = ItemReposicion.de(inventarioCon(sinNombre), sinNombre);

        assertThat(result).hasValueSatisfying(item -> assertThat(item.nombre()).isEqualTo("Producto sin nombre"));
    }

    @Test
    @DisplayName("Cambia solo la cantidad a pedir")
    void conCantidadShouldChangeOnlyTheQuantityToOrder() {
        ItemReposicion item = itemBajoMinimo();

        ItemReposicion result = item.conCantidad(new BigDecimal("25"));

        assertThat(result).isEqualTo(new ItemReposicion(item.productoId(), item.nombre(), item.proveedor(),
                item.cantidadActual(), new BigDecimal("25"), item.situacion(), item.urgencia()));
    }

    @Test
    @DisplayName("Rechaza una cantidad en cero o negativa y nombra el producto")
    void conCantidadShouldRejectNonPositiveQuantities() {
        ItemReposicion item = itemBajoMinimo();

        assertThatThrownBy(() -> item.conCantidad(BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(NOMBRE);
        assertThatThrownBy(() -> item.conCantidad(new BigDecimal("-1")))
                .isInstanceOf(BusinessRuleException.class);
    }

    // --- arrange ---
    private ItemReposicion itemBajoMinimo() {
        Registro yerba = producto(YERBA_ID, NOMBRE, 3, 10, PROVEEDOR);
        return ItemReposicion.de(inventarioCon(yerba), yerba).orElseThrow();
    }
}
