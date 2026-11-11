package com.ensambla.backend.domain.useCase.orden.cambiarEstadoOrden;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.LineaDeCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;
import com.ensambla.backend.domain.model.pedido.ProductoEnStock;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
import com.ensambla.backend.domain.repository.pedido.OrdenCompraRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.useCase.orden.OrdenItem;
import com.ensambla.backend.infrastructure.config.useCase.OrdenUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CambiarEstadoOrdenTest {

    private static final Instant INSTANTE = Instant.parse("2026-11-11T15:00:00Z");
    private static final OffsetDateTime CREADA = OffsetDateTime.parse("2026-11-11T12:00:00Z");
    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ClienteId CLIENTE_ID = ClienteId.de("c0000000-0000-4000-8000-000000000001");
    private static final OrdenCompraId ORDEN_ID = OrdenCompraId.de("f0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CampoId CAMPO_STOCK_ID = CampoId.de("e0000000-0000-4000-8000-000000000001");
    private static final String TELEFONO = "5491100000001";

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final OrdenCompraRepository ordenRepository = mock(OrdenCompraRepository.class, CALLS_REAL_METHODS);
    private final ClienteWhatsappRepository clienteRepository = mock(ClienteWhatsappRepository.class);
    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final CambiarEstadoOrden cambiarEstadoOrden = new OrdenUseCaseConfig().cambiarEstadoOrden(
            negocioRepository, ordenRepository, clienteRepository, registroRepository, campoRepository,
            new TransaccionDirecta(), Clock.fixed(INSTANTE, ZoneOffset.UTC));

    @BeforeEach
    void givenNegocioYCliente() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(clienteRepository.findAllByIds(List.of(CLIENTE_ID))).thenReturn(List.of(
                ClienteWhatsapp.builder().id(CLIENTE_ID).telefono(TELEFONO).creadoEn(CREADA).build()));
        when(ordenRepository.save(any(OrdenCompra.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    @DisplayName("Aceptar la orden descuenta del inventario lo que pidió el cliente y la deja aceptada")
    void executeShouldAcceptTheOrderAndDeductTheStock() {
        givenOrdenPendiente(3);
        Registro yerba = givenProductoConStock(10);

        OrdenItem resultado = whenCambia(EstadoOrdenCompra.ACEPTADA, null);

        assertThat(resultado.estado()).isEqualTo(EstadoOrdenCompra.ACEPTADA);
        assertThat(resultado.clienteTelefono()).isEqualTo(TELEFONO);
        assertThat(resultado.actualizadoEn().toInstant()).isEqualTo(INSTANTE);
        assertThat((BigDecimal) yerba.getValores().get(CAMPO_STOCK_ID.toString())).isEqualByComparingTo("7");
        assertThat(yerba.getValores()).containsKey("otro");
        verify(registroRepository).saveAll(List.of(yerba));
        ArgumentCaptor<OrdenCompra> orden = ArgumentCaptor.forClass(OrdenCompra.class);
        verify(ordenRepository).save(orden.capture());
        assertThat(orden.getValue().getEstado()).isEqualTo(EstadoOrdenCompra.ACEPTADA);
    }

    @Test
    @DisplayName("Aceptar una orden que se lleva todo el stock lo deja en cero")
    void executeShouldAllowTakingAllTheStock() {
        givenOrdenPendiente(3);
        Registro yerba = givenProductoConStock(3);

        whenCambia(EstadoOrdenCompra.ACEPTADA, null);

        assertThat((BigDecimal) yerba.getValores().get(CAMPO_STOCK_ID.toString())).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Si el stock ya no alcanza no se acepta la orden y no se toca el inventario")
    void executeShouldNotAcceptWhenThereIsNotEnoughStock() {
        givenOrdenPendiente(3);
        Registro yerba = givenProductoConStock(2);

        assertThatThrownBy(() -> whenCambia(EstadoOrdenCompra.ACEPTADA, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No alcanza el stock de Yerba Playadito 1 kg: hay 2 y la orden pide 3");

        assertThat((BigDecimal) yerba.getValores().get(CAMPO_STOCK_ID.toString())).isEqualByComparingTo("2");
        verify(registroRepository, never()).saveAll(any());
        verify(ordenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un producto sin stock cargado cuenta como sin stock")
    void executeShouldTreatMissingStockAsZero() {
        givenOrdenPendiente(1);
        Registro yerba = givenProducto(Map.of());
        givenCampoDeStock(yerba);

        assertThatThrownBy(() -> whenCambia(EstadoOrdenCompra.ACEPTADA, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No alcanza el stock de Yerba Playadito 1 kg: hay 0 y la orden pide 1");
    }

    @Test
    @DisplayName("Si el producto ya no está en el inventario no se acepta la orden")
    void executeShouldNotAcceptWhenTheProductIsGone() {
        givenOrdenPendiente(1);
        when(registroRepository.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> whenCambia(EstadoOrdenCompra.ACEPTADA, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Yerba Playadito 1 kg ya no está en tu inventario");

        verify(ordenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si la tabla del producto ya no tiene la columna de stock no se acepta la orden")
    void executeShouldNotAcceptWhenTheStockColumnIsGone() {
        givenOrdenPendiente(1);
        Registro yerba = givenProducto(Map.of());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(
                Campo.builder().id(CampoId.nuevo()).clave("name").build(),
                Campo.builder().id(CampoId.nuevo()).build()));
        when(registroRepository.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID)).thenReturn(Optional.of(yerba));

        assertThatThrownBy(() -> whenCambia(EstadoOrdenCompra.ACEPTADA, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Yerba Playadito 1 kg no tiene el stock cargado en tu inventario");
    }

    @Test
    @DisplayName("Rechazar guarda el motivo sin espacios de más y no toca el inventario")
    void executeShouldRejectWithAReason() {
        givenOrdenPendiente(3);

        OrdenItem resultado = whenCambia(EstadoOrdenCompra.RECHAZADA, " No me queda stock ");

        assertThat(resultado.estado()).isEqualTo(EstadoOrdenCompra.RECHAZADA);
        assertThat(resultado.motivoRechazo()).isEqualTo("No me queda stock");
        verify(registroRepository, never()).saveAll(any());
        verify(registroRepository, never()).findByIdAndNegocioId(any(), any());
    }

    @Test
    @DisplayName("Preparar y dejar lista una orden aceptada no vuelve a descontar el stock")
    void executeShouldMoveForwardWithoutTouchingTheStock() {
        OrdenCompra aceptada = givenOrdenPendiente(3);
        aceptada.pasarA(EstadoOrdenCompra.ACEPTADA, null, CREADA);

        assertThat(whenCambia(EstadoOrdenCompra.EN_PREPARACION, null).estado())
                .isEqualTo(EstadoOrdenCompra.EN_PREPARACION);
        assertThat(whenCambia(EstadoOrdenCompra.LISTA, null).estado()).isEqualTo(EstadoOrdenCompra.LISTA);

        verify(registroRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Un paso que no está permitido responde el motivo y no guarda nada")
    void executeShouldRejectAnInvalidStep() {
        givenOrdenPendiente(3);

        assertThatThrownBy(() -> whenCambia(EstadoOrdenCompra.LISTA, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede pasar una orden pendiente a lista para retirar");

        verify(ordenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una orden ajena o inexistente responde 404 y no se toca nada")
    void executeShouldNotFindForeignOrders() {
        when(ordenRepository.findByIdAndNegocioId(ORDEN_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> whenCambia(EstadoOrdenCompra.ACEPTADA, null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ordenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si el cliente ya no existe la orden igual cambia, sin teléfono")
    void executeShouldWorkWithoutTheClientPhone() {
        givenOrdenPendiente(3);
        when(clienteRepository.findAllByIds(List.of(CLIENTE_ID))).thenReturn(List.of());

        assertThat(whenCambia(EstadoOrdenCompra.RECHAZADA, null).clienteTelefono()).isNull();
    }

    // --- arrange ---

    private OrdenCompra givenOrdenPendiente(int cantidad) {
        ProductoEnStock yerba = new ProductoEnStock(REGISTRO_ID, "Yerba Playadito 1 kg", new BigDecimal("3500"),
                new BigDecimal("10"));
        OrdenCompra orden = OrdenCompra.pendiente(NEGOCIO_ID, CLIENTE_ID, List.of(
                LineaDeCompra.de(yerba, BigDecimal.valueOf(cantidad), List.of())), CREADA).toBuilder()
                .id(ORDEN_ID).build();
        when(ordenRepository.findByIdAndNegocioId(ORDEN_ID, NEGOCIO_ID)).thenReturn(Optional.of(orden));
        return orden;
    }

    private Registro givenProductoConStock(int stock) {
        Registro yerba = givenProducto(Map.of("otro", "dato", CAMPO_STOCK_ID.toString(), BigDecimal.valueOf(stock)));
        givenCampoDeStock(yerba);
        return yerba;
    }

    private Registro givenProducto(Map<String, Object> valores) {
        return Registro.builder().id(REGISTRO_ID).negocioId(NEGOCIO_ID).bloqueId(BLOQUE_ID)
                .valores(new LinkedHashMap<>(valores)).build();
    }

    private void givenCampoDeStock(Registro producto) {
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of(
                Campo.builder().id(CampoId.nuevo()).clave("name").build(),
                Campo.builder().id(CAMPO_STOCK_ID).clave("quantity").build()));
        when(registroRepository.findByIdAndNegocioId(REGISTRO_ID, NEGOCIO_ID)).thenReturn(Optional.of(producto));
    }

    private OrdenItem whenCambia(EstadoOrdenCompra estado, String motivo) {
        return cambiarEstadoOrden.execute(new CambiarEstadoOrdenRequest(USUARIO_ID, ORDEN_ID, estado, motivo));
    }
}
