package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static com.ensambla.backend.support.PedidoTestData.producto;
import static org.assertj.core.api.Assertions.assertThat;

class OrdenCompraTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00Z");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ClienteId CLIENTE_ID = ClienteId.de("c0000000-0000-4000-8000-000000000001");

    @Test
    @DisplayName("La orden nueva nace pendiente, con los ítems, el precio de ese momento y el total de la lista")
    void pendienteShouldCopyTheConfirmedList() {
        ProductoEnStock yerba = producto("Yerba Playadito 1 kg", "3500", "10");
        ProductoEnStock aceite = producto("Aceite Cocinero 900 ml", "2000", "3");

        OrdenCompra orden = OrdenCompra.pendiente(NEGOCIO_ID, CLIENTE_ID, List.of(
                LineaDeCompra.de(yerba, BigDecimal.valueOf(2), List.of()),
                LineaDeCompra.de(aceite, BigDecimal.ONE, List.of())), AHORA);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.PENDIENTE);
        assertThat(orden.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(orden.getClienteId()).isEqualTo(CLIENTE_ID);
        assertThat(orden.getTotal()).isEqualByComparingTo("9000");
        assertThat(orden.getMotivoRechazo()).isNull();
        assertThat(orden.getCreadoEn()).isEqualTo(AHORA);
        assertThat(orden.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(orden.getItems()).containsExactly(
                new ItemDeOrden(yerba.id(), "Yerba Playadito 1 kg", BigDecimal.valueOf(2), yerba.precio()),
                new ItemDeOrden(aceite.id(), "Aceite Cocinero 900 ml", BigDecimal.ONE, aceite.precio()));
        assertThat(orden.getItems().getFirst().subtotal()).isEqualByComparingTo("7000");
    }
}
