package com.ensambla.backend.domain.model.reposicion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoReposicionTest {

    private static final String NEGOCIO = "Almacén de Ana";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 19);
    private static final ItemReposicion ITEM = new ItemReposicion(
            YERBA_ID, "Yerba", "Molinos", new BigDecimal("3"), new BigDecimal("17"), "Quedan 3 (mínimo 10)", 1);

    @Test
    @DisplayName("Arma el pedido con el negocio, el proveedor sin espacios sobrantes y los productos")
    void armarShouldBuildTheOrder() {
        PedidoReposicion result = PedidoReposicion.armar(NEGOCIO, "  Molinos ", FECHA, List.of(ITEM));

        assertThat(result.negocio()).isEqualTo(NEGOCIO);
        assertThat(result.proveedor()).isEqualTo("Molinos");
        assertThat(result.fecha()).isEqualTo(FECHA);
        assertThat(result.items()).containsExactly(ITEM);
    }

    @Test
    @DisplayName("Un proveedor vacío o en blanco queda sin proveedor")
    void armarShouldLeaveBlankSupplierEmpty() {
        PedidoReposicion sinDato = PedidoReposicion.armar(NEGOCIO, null, FECHA, List.of(ITEM));
        PedidoReposicion enBlanco = PedidoReposicion.armar(NEGOCIO, "  ", FECHA, List.of(ITEM));

        assertThat(sinDato.proveedor()).isNull();
        assertThat(enBlanco.proveedor()).isNull();
    }

    @Test
    @DisplayName("Rechaza un pedido sin productos")
    void armarShouldRejectAnOrderWithoutProducts() {
        assertThatThrownBy(() -> PedidoReposicion.armar(NEGOCIO, "Molinos", FECHA, List.of()))
                .isInstanceOf(BusinessRuleException.class);
    }
}
