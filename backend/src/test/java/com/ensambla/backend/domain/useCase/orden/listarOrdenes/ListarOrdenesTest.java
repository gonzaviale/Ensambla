package com.ensambla.backend.domain.useCase.orden.listarOrdenes;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.LineaDeCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
import com.ensambla.backend.domain.repository.pedido.OrdenCompraRepository;
import com.ensambla.backend.domain.useCase.orden.OrdenItem;
import com.ensambla.backend.infrastructure.config.useCase.OrdenUseCaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static com.ensambla.backend.support.PedidoTestData.producto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarOrdenesTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ClienteId CLIENTE_UNO = ClienteId.de("c0000000-0000-4000-8000-000000000001");
    private static final ClienteId CLIENTE_DOS = ClienteId.de("c0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00Z");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final OrdenCompraRepository ordenRepository = mock(OrdenCompraRepository.class);
    private final ClienteWhatsappRepository clienteRepository = mock(ClienteWhatsappRepository.class);
    private final ListarOrdenes listarOrdenes = new OrdenUseCaseConfig().listarOrdenes(negocioRepository,
            ordenRepository, clienteRepository);

    @BeforeEach
    void givenNegocioDeLaSesion() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Lista las órdenes del negocio de la sesión con el WhatsApp de cada cliente y sus ítems")
    void executeShouldListTheOrdersOfTheSessionBusiness() {
        OrdenCompra primera = givenOrden(CLIENTE_UNO);
        OrdenCompra segunda = givenOrden(CLIENTE_DOS);
        when(ordenRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(primera, segunda));
        when(clienteRepository.findAllByIds(List.of(CLIENTE_UNO, CLIENTE_DOS))).thenReturn(List.of(
                givenCliente(CLIENTE_DOS, "5491100000002"), givenCliente(CLIENTE_UNO, "5491100000001")));

        List<OrdenItem> ordenes = listarOrdenes.execute(new ListarOrdenesRequest(USUARIO_ID, null));

        assertThat(ordenes).extracting(OrdenItem::clienteTelefono).containsExactly("5491100000001", "5491100000002");
        assertThat(ordenes.getFirst().estado()).isEqualTo(EstadoOrdenCompra.PENDIENTE);
        assertThat(ordenes.getFirst().total()).isEqualByComparingTo("7000");
        assertThat(ordenes.getFirst().items()).singleElement().satisfies(item -> {
            assertThat(item.nombre()).isEqualTo("Yerba Playadito 1 kg");
            assertThat(item.cantidad()).isEqualByComparingTo("2");
            assertThat(item.precioUnitario()).isEqualByComparingTo("3500");
            assertThat(item.subtotal()).isEqualByComparingTo("7000");
        });
        verify(ordenRepository, never()).findAllByNegocioIdAndEstado(any(), any());
    }

    @Test
    @DisplayName("Con un estado, trae sólo las órdenes de ese estado")
    void executeShouldFilterByState() {
        OrdenCompra pendiente = givenOrden(CLIENTE_UNO);
        when(ordenRepository.findAllByNegocioIdAndEstado(NEGOCIO_ID, EstadoOrdenCompra.PENDIENTE))
                .thenReturn(List.of(pendiente));
        when(clienteRepository.findAllByIds(List.of(CLIENTE_UNO))).thenReturn(List.of(
                givenCliente(CLIENTE_UNO, "5491100000001")));

        List<OrdenItem> ordenes = listarOrdenes.execute(
                new ListarOrdenesRequest(USUARIO_ID, EstadoOrdenCompra.PENDIENTE));

        assertThat(ordenes).hasSize(1);
        verify(ordenRepository, never()).findAllByNegocioId(any());
    }

    @Test
    @DisplayName("Un cliente con varias órdenes se consulta una sola vez")
    void executeShouldLookUpEachClientOnce() {
        when(ordenRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(
                givenOrden(CLIENTE_UNO), givenOrden(CLIENTE_UNO)));
        when(clienteRepository.findAllByIds(List.of(CLIENTE_UNO))).thenReturn(List.of(
                givenCliente(CLIENTE_UNO, "5491100000001")));

        assertThat(listarOrdenes.execute(new ListarOrdenesRequest(USUARIO_ID, null)))
                .extracting(OrdenItem::clienteTelefono).containsExactly("5491100000001", "5491100000001");
    }

    @Test
    @DisplayName("Sin órdenes devuelve una lista vacía")
    void executeShouldReturnEmptyWhenThereAreNoOrders() {
        when(ordenRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        when(clienteRepository.findAllByIds(List.of())).thenReturn(List.of());

        assertThat(listarOrdenes.execute(new ListarOrdenesRequest(USUARIO_ID, null))).isEmpty();
    }

    private static OrdenCompra givenOrden(ClienteId clienteId) {
        return OrdenCompra.pendiente(NEGOCIO_ID, clienteId, List.of(LineaDeCompra.de(
                producto("Yerba Playadito 1 kg", "3500", "10"), BigDecimal.valueOf(2), List.of())), AHORA);
    }

    private static ClienteWhatsapp givenCliente(ClienteId id, String telefono) {
        return ClienteWhatsapp.builder().id(id).telefono(telefono).creadoEn(AHORA).build();
    }
}
