package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static com.ensambla.backend.support.PedidoTestData.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("Pasar la orden a la etapa siguiente cambia el estado y la hora, y descarta cualquier motivo")
    void pasarAShouldMoveToTheNextStage() {
        OrdenCompra orden = givenOrdenPendiente();

        orden.pasarA(EstadoOrdenCompra.ACEPTADA, "no corresponde", AHORA.plusMinutes(5));
        orden.pasarA(EstadoOrdenCompra.EN_PREPARACION, null, AHORA.plusMinutes(10));
        orden.pasarA(EstadoOrdenCompra.LISTA, null, AHORA.plusMinutes(20));

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.LISTA);
        assertThat(orden.getMotivoRechazo()).isNull();
        assertThat(orden.getActualizadoEn()).isEqualTo(AHORA.plusMinutes(20));
        assertThat(orden.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Un rechazo guarda el motivo sin espacios de más")
    void pasarAShouldKeepTheRejectionReason() {
        OrdenCompra orden = givenOrdenPendiente();

        orden.pasarA(EstadoOrdenCompra.RECHAZADA, "  No me queda stock ", AHORA.plusMinutes(1));

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.RECHAZADA);
        assertThat(orden.getMotivoRechazo()).isEqualTo("No me queda stock");
    }

    @Test
    @DisplayName("Un rechazo sin motivo, o con uno en blanco, queda sin motivo")
    void pasarAShouldAllowRejectingWithoutReason() {
        OrdenCompra sinMotivo = givenOrdenPendiente();
        OrdenCompra enBlanco = givenOrdenPendiente();

        sinMotivo.pasarA(EstadoOrdenCompra.RECHAZADA, null, AHORA);
        enBlanco.pasarA(EstadoOrdenCompra.RECHAZADA, "   ", AHORA);

        assertThat(sinMotivo.getMotivoRechazo()).isNull();
        assertThat(enBlanco.getMotivoRechazo()).isNull();
    }

    @Test
    @DisplayName("Un paso que no está permitido lanza el motivo y deja la orden como estaba")
    void pasarAShouldRejectAnInvalidStep() {
        OrdenCompra orden = givenOrdenPendiente();

        assertThatThrownBy(() -> orden.pasarA(EstadoOrdenCompra.LISTA, null, AHORA.plusMinutes(1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede pasar una orden pendiente a lista para retirar");

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.PENDIENTE);
        assertThat(orden.getActualizadoEn()).isEqualTo(AHORA);
    }

    private static OrdenCompra givenOrdenPendiente() {
        return OrdenCompra.pendiente(NEGOCIO_ID, CLIENTE_ID, List.of(
                LineaDeCompra.de(producto("Yerba Playadito 1 kg", "3500", "10"), BigDecimal.ONE, List.of())), AHORA);
    }
}
