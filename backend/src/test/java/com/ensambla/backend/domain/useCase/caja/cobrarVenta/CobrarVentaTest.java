package com.ensambla.backend.domain.useCase.caja.cobrarVenta;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CobrarVentaTest {

    @Mock
    private NegocioRepository negocioRepository;
    @Mock
    private UsoPlanRepository usoPlanRepository;
    @Mock
    private SistemaRepository sistemaRepository;
    @Mock
    private VentaCajaRepository ventaCajaRepository;
    @Mock
    private ControlPlanService controlPlanService;
    @Mock
    private MovimientoStockRepository movimientoStockRepository;
    private CobrarVenta cobrarVenta;
    private Sistema sistema;

    @BeforeEach
    void setUp() {
        cobrarVenta = new CobrarVenta(negocioRepository, usoPlanRepository, sistemaRepository,
                ventaCajaRepository, movimientoStockRepository, controlPlanService, new TransaccionDirecta(),
                Clock.fixed(CajaTestData.AHORA.toInstant(), ZoneOffset.UTC));
        sistema = CajaTestData.sistema();
    }

    @Test
    @DisplayName("Confirma la venta por peso y descuenta kilos del inventario en una sola operación")
    void executeShouldPersistSaleAndStock() {
        givenCaja();
        when(ventaCajaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        CobrarVentaResponse respuesta = cobrarVenta.execute(CajaTestData.request("0.5"));

        thenSaleAndStockAreSaved(respuesta);
    }

    @Test
    @DisplayName("Repetir la misma operación devuelve el comprobante sin volver a descontar")
    void executeShouldReturnSameReceiptWhenRetried() {
        dadoUsuario();
        CobrarVentaRequest request = CajaTestData.request("0.5");
        VentaCaja venta = ventaPrevia(request.huella());
        when(ventaCajaRepository.findByIdAndNegocioId(request.id(), CajaTestData.NEGOCIO))
                .thenReturn(Optional.of(venta));

        CobrarVentaResponse respuesta = cobrarVenta.execute(request);

        assertThat(respuesta.id()).isEqualTo(venta.id());
        verify(sistemaRepository, never()).guardar(any(Sistema.class));
    }

    @Test
    @DisplayName("Reutilizar una operación con otros datos responde conflicto")
    void executeShouldRejectChangedRetry() {
        dadoUsuario();
        when(ventaCajaRepository.findByIdAndNegocioId(CajaTestData.OPERACION, CajaTestData.NEGOCIO))
                .thenReturn(Optional.of(ventaPrevia("otra huella")));

        assertThatThrownBy(() -> cobrarVenta.execute(CajaTestData.request("0.5")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde como inexistente")
    void executeShouldHideForeignSystem() {
        dadoUsuario();
        when(sistemaRepository.findAllByNegocioId(CajaTestData.NEGOCIO)).thenReturn(List.of());

        assertThatThrownBy(() -> cobrarVenta.execute(CajaTestData.request("0.5")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rechaza ventas que no apuntan al inventario elegido")
    void executeShouldRejectWrongRelation() {
        givenVentasRelacionadasConOtroInventario();

        thenRejects(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una tabla de tipo distinto aunque el id coincida")
    void executeShouldRejectWrongTableType() {
        givenCaja();
        inventario().setTipo(TipoBloque.PEDIDOS);

        thenRejects(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rechaza un producto que desapareció del inventario")
    void executeShouldRejectDeletedProduct() {
        givenCaja();
        producto().setId(RegistroId.nuevo());

        thenRejects(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("No confirma si el precio cambió mientras se armaba la compra")
    void executeShouldRevalidatePrice() {
        givenCaja();
        CajaTestData.poner(inventario(), producto(), "price", new BigDecimal("5000"));

        thenRejects(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("No confirma si la forma de venta cambió")
    void executeShouldRevalidateSaleMode() {
        givenCaja();
        CajaTestData.poner(inventario(), producto(), "saleMode", "unidad");

        thenRejects(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Una salida sin stock rechaza la compra completa")
    void executeShouldRejectInsufficientStock() {
        givenCaja();
        CajaTestData.poner(inventario(), producto(), "quantity", BigDecimal.ZERO);

        thenRejects(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Un artículo sin control de stock registra la venta sin inventar existencias")
    void executeShouldSkipUntrackedStock() {
        givenCaja();
        CajaTestData.poner(inventario(), producto(), "stockControl", false);
        when(ventaCajaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        cobrarVenta.execute(CajaTestData.request("0.5"));

        assertThat(valor("quantity")).isEqualTo(new BigDecimal("2"));
    }

    @Test
    @DisplayName("Rechaza datos numéricos ausentes sin tratarlos como cero")
    void executeShouldRejectMissingPrice() {
        givenCaja();
        inventario().getCampos().removeIf(campo -> campo.getClave().equals("price"));

        thenRejects(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Explica qué campo necesita una tabla de ventas incompatible")
    void executeShouldRejectMissingSalesField() {
        givenCaja();
        ventas().getCampos().removeIf(campo -> campo.getClave().equals("price"));

        thenRejects(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Una venta fiada conserva al cliente y no inventa un cobro")
    void executeShouldRegisterCreditSaleWithCustomer() {
        givenCajaConCliente();

        CobrarVentaResponse respuesta = cobrarVenta.execute(solicitudConCliente(List.of()));

        thenPendingBalanceIs(respuesta, "2000", "PENDIENTE");
        assertThat(respuesta.pagos()).isEmpty();
        assertThat(respuesta.recibido()).isEqualByComparingTo("0");
        assertThat(valor("quantity")).isEqualTo(new BigDecimal("1.5"));
    }

    @Test
    @DisplayName("Una entrega parcial conserva el saldo y lo marca en la fila de ventas")
    void executeShouldRegisterPartialPayment() {
        givenCajaConCliente();

        CobrarVentaResponse respuesta = cobrarVenta.execute(solicitudConCliente(
                List.of(new CobrarVentaRequest.PagoRequest("TRANSFERENCIA", new BigDecimal("800")))));

        thenPendingBalanceIs(respuesta, "1200", "PARCIAL");
        Registro fila = sistema.registrosPorBloque().get(CajaTestData.VENTAS).getFirst();
        assertThat(fila.getValores().get(campoVentas("credit").getId().toString())).isEqualTo(true);
        assertThat(fila.getValores().get(campoVentas("customer").getId().toString()))
                .isEqualTo(CajaTestData.CLIENTE.toString());
    }

    @Test
    @DisplayName("Permite asociar un cliente a una venta pagada aunque la tabla use texto")
    void executeShouldUseCustomerNameWhenSalesFieldIsText() {
        givenClienteRegistradoComoTexto();

        CobrarVentaResponse respuesta = whenSeCobraLaVentaConCliente();

        thenSeConservaElNombreDelCliente(respuesta);
    }

    @Test
    @DisplayName("Una venta pagada permite conservar el comprador sin identificar")
    void executeShouldAllowAnonymousPaidSaleWithCustomerFields() {
        givenCajaConCliente();

        CobrarVentaResponse respuesta = cobrarVenta.execute(CajaTestData.request("0.5"));

        assertThat(respuesta.clienteId()).isNull();
        Registro fila = sistema.registrosPorBloque().get(CajaTestData.VENTAS).getFirst();
        assertThat(fila.getValores().get(campoVentas("credit").getId().toString())).isEqualTo(false);
    }

    @Test
    @DisplayName("Oculta un cliente que no pertenece al negocio sin descontar stock")
    void executeShouldHideMissingCustomer() {
        givenCaja();

        assertThatThrownBy(() -> cobrarVenta.execute(solicitudConCliente(List.of())))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(valor("quantity")).isEqualTo(new BigDecimal("2"));
    }

    @Test
    @DisplayName("No permite fiar a un cliente de ejemplo")
    void executeShouldRejectExampleCustomer() {
        givenCaja();
        CajaTestData.agregarCliente(sistema);
        sistema.registrosPorBloque().get(CajaTestData.CLIENTES).getFirst().setEsEjemplo(true);

        assertThatThrownBy(() -> cobrarVenta.execute(solicitudConCliente(List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rechaza un cliente de una tabla distinta a la relacionada con ventas")
    void executeShouldRejectCustomerFromWrongTable() {
        givenClienteRelacionadoConOtraTabla();

        thenSeRechazaElClienteDeOtraTabla();
    }

    private void givenVentasRelacionadasConOtroInventario() {
        givenCaja();
        CajaTestData.cambiarCampo(ventas(), "product", TipoCampo.RELACION, BloqueId.nuevo());
    }

    private void givenClienteRegistradoComoTexto() {
        givenCajaConCliente();
        CajaTestData.cambiarCampo(ventas(), "customer", TipoCampo.TEXTO, null);
    }

    private CobrarVentaResponse whenSeCobraLaVentaConCliente() {
        return cobrarVenta.execute(solicitudConCliente(null));
    }

    private void thenSeConservaElNombreDelCliente(CobrarVentaResponse respuesta) {
        thenPendingBalanceIs(respuesta, "0", "PAGADA");
        Registro fila = sistema.registrosPorBloque().get(CajaTestData.VENTAS).getFirst();
        assertThat(fila.getValores().get(campoVentas("customer").getId().toString())).isEqualTo("Ana López");
    }

    private void givenClienteRelacionadoConOtraTabla() {
        givenCaja();
        CajaTestData.agregarCliente(sistema);
        CajaTestData.cambiarCampo(ventas(), "customer", TipoCampo.RELACION, BloqueId.nuevo());
    }

    private void thenSeRechazaElClienteDeOtraTabla() {
        assertThatThrownBy(() -> cobrarVenta.execute(solicitudConCliente(List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("No confunde otro cliente con el solicitado")
    void executeShouldMatchExactCustomerId() {
        givenCaja();
        CajaTestData.agregarCliente(sistema);
        sistema.registrosPorBloque().get(CajaTestData.CLIENTES).getFirst().setId(RegistroId.nuevo());

        assertThatThrownBy(() -> cobrarVenta.execute(solicitudConCliente(List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenCajaConCliente() {
        givenCaja();
        CajaTestData.agregarCliente(sistema);
        when(ventaCajaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private CobrarVentaRequest solicitudConCliente(List<CobrarVentaRequest.PagoRequest> pagos) {
        CobrarVentaRequest base = CajaTestData.request("0.5");
        return new CobrarVentaRequest(base.usuarioId(), base.id(), base.proyectoId(), base.inventarioId(),
                base.ventasId(), base.lineas(), base.medio(), base.recibido(), pagos, CajaTestData.CLIENTE);
    }

    private Campo campoVentas(String clave) {
        return ventas().getCampos().stream().filter(campo -> clave.equals(campo.getClave())).findFirst().orElseThrow();
    }

    private void thenPendingBalanceIs(CobrarVentaResponse respuesta, String saldo, String estado) {
        assertThat(respuesta.saldo()).isEqualByComparingTo(saldo);
        assertThat(respuesta.cobrado()).isEqualByComparingTo(respuesta.total().subtract(new BigDecimal(saldo)));
        assertThat(respuesta.estado()).isEqualTo(estado);
        assertThat(respuesta.clienteId()).isEqualTo(CajaTestData.CLIENTE);
        assertThat(respuesta.clienteNombre()).isEqualTo("Ana López");
    }

    private void dadoUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(CajaTestData.USUARIO)).thenReturn(CajaTestData.NEGOCIO);
    }

    private void givenCaja() {
        dadoUsuario();
        when(sistemaRepository.findAllByNegocioId(CajaTestData.NEGOCIO)).thenReturn(List.of(sistema));
    }

    // --- assert ---
    private void thenRejects(Class<? extends RuntimeException> tipo) {
        assertThatThrownBy(() -> cobrarVenta.execute(CajaTestData.request("0.5"))).isInstanceOf(tipo);
        verify(ventaCajaRepository, never()).save(any());
        verify(sistemaRepository, never()).guardar(any(Sistema.class));
    }

    private void thenSaleAndStockAreSaved(CobrarVentaResponse respuesta) {
        assertThat(respuesta.total()).isEqualByComparingTo("2000");
        assertThat(valor("quantity")).isEqualTo(new BigDecimal("1.5"));
        ArgumentCaptor<Sistema> captor = ArgumentCaptor.forClass(Sistema.class);
        verify(sistemaRepository).guardar(captor.capture());
        Registro fila = captor.getValue().registrosPorBloque().get(CajaTestData.VENTAS).getFirst();
        assertThat(fila.getOrigen()).isEqualTo("Caja " + CajaTestData.OPERACION);
        assertThat(fila.getCreadoEn()).isEqualTo(CajaTestData.AHORA);
        assertThat(fila.getId()).isNotNull();
        assertThat(fila.getOrden()).isZero();
        verify(usoPlanRepository).bloquearNegocio(CajaTestData.NEGOCIO);
        verify(controlPlanService).validarAltaRegistros(CajaTestData.NEGOCIO, 1, CajaTestData.AHORA);
    }

    // --- helpers ---
    private VentaCaja ventaPrevia(String huella) {
        return new VentaCaja(CajaTestData.OPERACION, CajaTestData.NEGOCIO, CajaTestData.PROYECTO,
                huella, List.of(), MedioPagoCaja.EFECTIVO,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, CajaTestData.AHORA);
    }

    private Bloque inventario() {
        return sistema.proyecto().getBloques().getFirst();
    }

    private Bloque ventas() {
        return sistema.proyecto().getBloques().getLast();
    }

    private Registro producto() {
        return sistema.registrosPorBloque().get(CajaTestData.INVENTARIO).getFirst();
    }

    private Object valor(String clave) {
        Campo campo = inventario().getCampos().stream().filter(item -> item.getClave().equals(clave))
                .findFirst().orElseThrow();
        return producto().getValores().get(campo.getId().toString());
    }
}
