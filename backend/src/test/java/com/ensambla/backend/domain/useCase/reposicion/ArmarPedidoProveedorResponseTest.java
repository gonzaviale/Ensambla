package com.ensambla.backend.domain.useCase.reposicion;

import com.ensambla.backend.domain.model.reposicion.ItemReposicion;
import com.ensambla.backend.domain.model.reposicion.PedidoReposicion;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.ReposicionDePrueba.YERBA_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ArmarPedidoProveedorResponseTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 19);
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};
    private static final ItemReposicion ITEM = new ItemReposicion(
            YERBA_ID, "Yerba", null, BigDecimal.ONE, BigDecimal.TEN, "Agotado", 2);

    @Test
    @DisplayName("El nombre lleva al proveedor sin tildes ni espacios y la fecha")
    void deShouldNameTheFileAfterTheSupplier() {
        PedidoReposicion pedido = PedidoReposicion.armar("Almacén de Ana", "Distribuidora Ñandú", FECHA, List.of(ITEM));

        ArmarPedidoProveedorResponse result = ArmarPedidoProveedorResponse.de(PDF, pedido);

        assertThat(result.contenido()).isEqualTo(PDF);
        assertThat(result.nombreArchivo()).isEqualTo("pedido-distribuidora-nandu-20261019.pdf");
    }

    @Test
    @DisplayName("Sin proveedor el nombre lleva al negocio")
    void deShouldNameTheFileAfterTheBusinessWhenThereIsNoSupplier() {
        PedidoReposicion pedido = PedidoReposicion.armar("Almacén de Ana", null, FECHA, List.of(ITEM));

        ArmarPedidoProveedorResponse result = ArmarPedidoProveedorResponse.de(PDF, pedido);

        assertThat(result.nombreArchivo()).isEqualTo("pedido-almacen-de-ana-20261019.pdf");
    }

    @Test
    @DisplayName("Si el nombre no deja ningún carácter válido queda solo el prefijo y la fecha")
    void deShouldFallBackToThePrefixWhenTheNameHasNoValidCharacters() {
        PedidoReposicion pedido = PedidoReposicion.armar("???", null, FECHA, List.of(ITEM));

        ArmarPedidoProveedorResponse result = ArmarPedidoProveedorResponse.de(PDF, pedido);

        assertThat(result.nombreArchivo()).isEqualTo("pedido-20261019.pdf");
    }
}
