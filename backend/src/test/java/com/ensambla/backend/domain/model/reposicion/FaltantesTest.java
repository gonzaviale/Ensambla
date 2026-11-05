package com.ensambla.backend.domain.model.reposicion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.ReposicionDePrueba.ACEITE_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.FIDEOS_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.GASEOSA_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.ReposicionDePrueba.datosCon;
import static com.ensambla.backend.support.ReposicionDePrueba.inventarioCon;
import static com.ensambla.backend.support.ReposicionDePrueba.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaltantesTest {

    @Test
    @DisplayName("Lista solo lo que hay que reponer, los agotados primero y cada grupo por nombre")
    void deShouldListOnlyMissingProductsMostUrgentFirst() {
        Faltantes result = faltantesDeUnAlmacen();

        assertThat(result.items()).extracting(ItemReposicion::nombre)
                .containsExactly("Aceite", "Gaseosa", "Yerba");
    }

    @Test
    @DisplayName("Junta los faltantes de todos los inventarios del sistema")
    void deShouldJoinTheMissingProductsOfEveryInventory() {
        Faltantes result = Faltantes.de(datosCon(
                inventarioCon(producto(YERBA_ID, "Yerba", 0, 10, null)),
                inventarioCon(producto(ACEITE_ID, "Aceite", 1, 10, null))));

        assertThat(result.items()).extracting(ItemReposicion::nombre).containsExactly("Yerba", "Aceite");
    }

    @Test
    @DisplayName("Agrupa por proveedor en orden alfabético y deja al final los productos sin proveedor")
    void porProveedorShouldGroupBySupplierWithTheUnassignedLast() {
        Faltantes faltantes = faltantesDeUnAlmacen();

        List<GrupoDeProveedor> result = faltantes.porProveedor();

        assertThat(result).extracting(GrupoDeProveedor::proveedor).containsExactly("arcor", "Molinos", null);
        assertThat(result.get(1).items()).extracting(ItemReposicion::nombre).containsExactly("Yerba");
        assertThat(result.get(2).items()).extracting(ItemReposicion::nombre).containsExactly("Aceite");
    }

    @Test
    @DisplayName("Sin faltantes no hay grupos")
    void porProveedorShouldBeEmptyWhenNothingIsMissing() {
        Faltantes faltantes = Faltantes.de(datosCon(inventarioCon(producto(FIDEOS_ID, "Fideos", 30, 10, "Molinos"))));

        List<GrupoDeProveedor> result = faltantes.porProveedor();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Elige los productos con las cantidades que decidió la persona")
    void elegirShouldApplyTheChosenQuantities() {
        Faltantes faltantes = faltantesDeUnAlmacen();

        List<ItemReposicion> result = faltantes.elegir(Map.of(YERBA_ID, new BigDecimal("12")));

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.productoId()).isEqualTo(YERBA_ID);
            assertThat(item.cantidadAPedir()).isEqualByComparingTo("12");
        });
    }

    @Test
    @DisplayName("Rechaza un producto que ya no hace falta reponer")
    void elegirShouldRejectAProductThatIsNoLongerMissing() {
        Faltantes faltantes = faltantesDeUnAlmacen();
        Map<RegistroId, BigDecimal> elegidos = Map.of(FIDEOS_ID, BigDecimal.ONE);

        assertThatThrownBy(() -> faltantes.elegir(elegidos)).isInstanceOf(BusinessRuleException.class);
    }

    // --- arrange ---
    private Faltantes faltantesDeUnAlmacen() {
        return Faltantes.de(datosCon(inventarioCon(
                producto(YERBA_ID, "Yerba", 3, 10, "Molinos"),
                producto(FIDEOS_ID, "Fideos", 30, 10, "Molinos"),
                producto(ACEITE_ID, "Aceite", 0, 10, null),
                producto(GASEOSA_ID, "Gaseosa", 0, 10, "arcor"))));
    }
}
