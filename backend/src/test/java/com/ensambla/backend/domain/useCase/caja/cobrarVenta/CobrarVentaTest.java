package com.ensambla.backend.domain.useCase.caja.cobrarVenta;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
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
import static org.assertj.core.api.Assertions.catchThrowable;
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

    private CobrarVentaResponse respuesta;
    private Throwable rechazo;
    private VentaCaja ventaPrevia;

    @BeforeEach
    void setUp() {
        cobrarVenta = new CobrarVenta(negocioRepository, usoPlanRepository, sistemaRepository,
                ventaCajaRepository, movimientoStockRepository, controlPlanService, new TransaccionDirecta(),
                Clock.fixed(CajaTestData.AHORA.toInstant(), ZoneOffset.UTC));
        sistema = CajaTestData.sistema();
    }

    @Test
    @DisplayName("Confirma la venta por peso y descuenta kilos del inventario en una sola operación")
    void confirmaLaVentaPorPesoYDescuentaKilosDelInventarioEnUnaSolaOperacion() {
        dadoUnaCajaQueGuardaLasVentas();

        cuandoSeCobraMedioKilo();

        entoncesSeGuardanLaVentaYElDescuentoDeStock();
    }

    @Test
    @DisplayName("Repetir la misma operación devuelve el comprobante sin volver a descontar")
    void repetirLaMismaOperacionDevuelveElComprobanteSinVolverADescontar() {
        dadoUnaVentaPreviaConLaMismaHuella();

        cuandoSeCobraMedioKilo();

        entoncesSeDevuelveElComprobanteDeLaVentaPrevia();
        entoncesNoSeVuelveADescontar();
    }

    @Test
    @DisplayName("Reutilizar una operación con otros datos responde conflicto")
    void reutilizarUnaOperacionConOtrosDatosRespondeConflicto() {
        dadoUnaVentaPreviaConOtraHuella();

        cuandoSeIntentaCobrarMedioKilo();

        entoncesSeRechazaCon(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde como inexistente")
    void unSistemaDeOtroNegocioRespondeComoInexistente() {
        dadoUnNegocioSinSistemas();

        cuandoSeIntentaCobrarMedioKilo();

        entoncesSeRechazaCon(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rechaza ventas que no apuntan al inventario elegido")
    void rechazaVentasQueNoApuntanAlInventarioElegido() {
        dadoUnaTablaDeVentasRelacionadaConOtroInventario();

        cuandoSeIntentaCobrarMedioKilo();

        entoncesSeRechazaSinGuardarNada(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una tabla de tipo distinto aunque el id coincida")
    void rechazaUnaTablaDeTipoDistintoAunqueElIdCoincida() {
        dadoUnInventarioQueEnRealidadEsDePedidos();

        cuandoSeIntentaCobrarMedioKilo();

        entoncesSeRechazaSinGuardarNada(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rechaza un producto que desapareció del inventario")
    void rechazaUnProductoQueDesaparecioDelInventario() {
        dadoUnProductoQueDesaparecioDelInventario();

        cuandoSeIntentaCobrarMedioKilo();

        entoncesSeRechazaSinGuardarNada(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("No confirma si el precio cambió mientras se armaba la compra")
    void noConfirmaSiElPrecioCambioMientrasSeArmabaLaCompra() {
        dadoUnProductoConElPrecioCambiado();

        cuandoSeIntentaCobrarMedioKilo();

        entoncesSeRechazaSinGuardarNada(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("No confirma si la forma de venta cambió")
    void noConfirmaSiLaFormaDeVentaCambio() {
        dadoUnProductoConLaFormaDeVentaCambiada();

        cuandoSeIntentaCobrarMedioKilo();

        entoncesSeRechazaSinGuardarNada(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Una salida sin stock rechaza la compra completa")
    void unaSalidaSinStockRechazaLaCompraCompleta() {
        dadoUnProductoSinStock();

        cuandoSeIntentaCobrarMedioKilo();

        entoncesSeRechazaSinGuardarNada(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Un artículo sin control de stock registra la venta sin inventar existencias")
    void unArticuloSinControlDeStockRegistraLaVentaSinInventarExistencias() {
        dadoUnProductoSinControlDeStock();

        cuandoSeCobraMedioKilo();

        entoncesElStockDelProductoQuedaEn("2");
    }

    @Test
    @DisplayName("Rechaza datos numéricos ausentes sin tratarlos como cero")
    void rechazaDatosNumericosAusentesSinTratarlosComoCero() {
        dadoUnInventarioSinCampoDePrecio();

        cuandoSeIntentaCobrarMedioKilo();

        entoncesSeRechazaSinGuardarNada(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Explica qué campo necesita una tabla de ventas incompatible")
    void explicaQueCampoNecesitaUnaTablaDeVentasIncompatible() {
        dadoUnaTablaDeVentasSinCampoDePrecio();

        cuandoSeIntentaCobrarMedioKilo();

        entoncesSeRechazaSinGuardarNada(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Una venta fiada conserva al cliente y no inventa un cobro")
    void unaVentaFiadaConservaAlClienteYNoInventaUnCobro() {
        dadoUnaCajaConUnCliente();

        cuandoSeCobraAlClienteEntregando(List.of());

        entoncesLaDeudaQuedaPendienteEn("2000", "PENDIENTE");
        entoncesNoSeRegistraNingunPago();
        entoncesElStockDelProductoQuedaEn("1.5");
    }

    @Test
    @DisplayName("Una entrega parcial conserva el saldo y lo marca en la fila de ventas")
    void unaEntregaParcialConservaElSaldoYLoMarcaEnLaFilaDeVentas() {
        dadoUnaCajaConUnCliente();

        cuandoSeCobraAlClienteEntregando(List.of(new CobrarVentaRequest.PagoRequest("TRANSFERENCIA", new BigDecimal("800"))));

        entoncesLaDeudaQuedaPendienteEn("1200", "PARCIAL");
        entoncesLaFilaDeVentasQuedaFiadaAlCliente();
    }

    @Test
    @DisplayName("Permite asociar un cliente a una venta pagada aunque la tabla use texto")
    void permiteAsociarUnClienteAUnaVentaPagadaAunqueLaTablaUseTexto() {
        dadoUnaTablaDeVentasQueGuardaElClienteComoTexto();

        cuandoSeCobraAlClienteSinEntregaExplicita();

        entoncesLaDeudaQuedaPendienteEn("0", "PAGADA");
        entoncesLaFilaDeVentasConservaElNombreDelCliente();
    }

    @Test
    @DisplayName("Una venta pagada permite conservar el comprador sin identificar")
    void unaVentaPagadaPermiteConservarElCompradorSinIdentificar() {
        dadoUnaCajaConUnCliente();

        cuandoSeCobraMedioKilo();

        entoncesLaVentaNoTieneCliente();
        entoncesLaFilaDeVentasNoQuedaFiada();
    }

    @Test
    @DisplayName("Oculta un cliente que no pertenece al negocio sin descontar stock")
    void ocultaUnClienteQueNoPerteneceAlNegocioSinDescontarStock() {
        dadoUnaCajaSinClientes();

        cuandoSeIntentaCobrarAlClienteEntregando(List.of());

        entoncesSeRechazaCon(ResourceNotFoundException.class);
        entoncesElStockDelProductoQuedaEn("2");
    }

    @Test
    @DisplayName("No permite fiar a un cliente de ejemplo")
    void noPermiteFiarAUnClienteDeEjemplo() {
        dadoUnClienteDeEjemplo();

        cuandoSeIntentaCobrarAlClienteEntregando(List.of());

        entoncesSeRechazaCon(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rechaza un cliente de una tabla distinta a la relacionada con ventas")
    void rechazaUnClienteDeUnaTablaDistintaALaRelacionadaConVentas() {
        dadoUnClienteRelacionadoConOtraTabla();

        cuandoSeIntentaCobrarAlClienteEntregando(List.of());

        entoncesSeRechazaCon(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("No confunde otro cliente con el solicitado")
    void noConfundeOtroClienteConElSolicitado() {
        dadoUnClienteConOtroId();

        cuandoSeIntentaCobrarAlClienteEntregando(List.of());

        entoncesSeRechazaCon(ResourceNotFoundException.class);
    }

    // --- dado ---

    private void dadoUnUsuarioDelNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(CajaTestData.USUARIO)).thenReturn(CajaTestData.NEGOCIO);
    }

    private void dadoUnaCaja() {
        dadoUnUsuarioDelNegocio();
        when(sistemaRepository.findAllByNegocioId(CajaTestData.NEGOCIO)).thenReturn(List.of(sistema));
    }

    private void dadoQueGuardarUnaVentaLaDevuelve() {
        when(ventaCajaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void dadoUnaCajaQueGuardaLasVentas() {
        dadoUnaCaja();
        dadoQueGuardarUnaVentaLaDevuelve();
    }

    private void dadoUnaCajaConUnCliente() {
        dadoUnaCajaQueGuardaLasVentas();
        CajaTestData.agregarCliente(sistema);
    }

    private void dadoUnaCajaSinClientes() {
        dadoUnaCaja();
    }

    private void dadoUnaVentaPreviaConLaMismaHuella() {
        dadoUnUsuarioDelNegocio();
        ventaPrevia = unaVentaPreviaConHuella(CajaTestData.request("0.5").huella());
        when(ventaCajaRepository.findByIdAndNegocioId(CajaTestData.OPERACION, CajaTestData.NEGOCIO))
                .thenReturn(Optional.of(ventaPrevia));
    }

    private void dadoUnaVentaPreviaConOtraHuella() {
        dadoUnUsuarioDelNegocio();
        when(ventaCajaRepository.findByIdAndNegocioId(CajaTestData.OPERACION, CajaTestData.NEGOCIO))
                .thenReturn(Optional.of(unaVentaPreviaConHuella("otra huella")));
    }

    private void dadoUnNegocioSinSistemas() {
        dadoUnUsuarioDelNegocio();
        when(sistemaRepository.findAllByNegocioId(CajaTestData.NEGOCIO)).thenReturn(List.of());
    }

    private void dadoUnaTablaDeVentasRelacionadaConOtroInventario() {
        dadoUnaCaja();
        CajaTestData.cambiarCampo(ventas(), "product", TipoCampo.RELACION, BloqueId.nuevo());
    }

    private void dadoUnInventarioQueEnRealidadEsDePedidos() {
        dadoUnaCaja();
        inventario().setTipo(TipoBloque.PEDIDOS);
    }

    private void dadoUnProductoQueDesaparecioDelInventario() {
        dadoUnaCaja();
        producto().setId(RegistroId.nuevo());
    }

    private void dadoUnProductoConElPrecioCambiado() {
        dadoUnaCaja();
        CajaTestData.poner(inventario(), producto(), "price", new BigDecimal("5000"));
    }

    private void dadoUnProductoConLaFormaDeVentaCambiada() {
        dadoUnaCaja();
        CajaTestData.poner(inventario(), producto(), "saleMode", "unidad");
    }

    private void dadoUnProductoSinStock() {
        dadoUnaCaja();
        CajaTestData.poner(inventario(), producto(), "quantity", BigDecimal.ZERO);
    }

    private void dadoUnProductoSinControlDeStock() {
        dadoUnaCajaQueGuardaLasVentas();
        CajaTestData.poner(inventario(), producto(), "stockControl", false);
    }

    private void dadoUnInventarioSinCampoDePrecio() {
        dadoUnaCaja();
        inventario().getCampos().removeIf(campo -> campo.getClave().equals("price"));
    }

    private void dadoUnaTablaDeVentasSinCampoDePrecio() {
        dadoUnaCaja();
        ventas().getCampos().removeIf(campo -> campo.getClave().equals("price"));
    }

    private void dadoUnaTablaDeVentasQueGuardaElClienteComoTexto() {
        dadoUnaCajaConUnCliente();
        CajaTestData.cambiarCampo(ventas(), "customer", TipoCampo.TEXTO, null);
    }

    private void dadoUnClienteDeEjemplo() {
        dadoUnaCaja();
        CajaTestData.agregarCliente(sistema);
        elCliente().setEsEjemplo(true);
    }

    private void dadoUnClienteRelacionadoConOtraTabla() {
        dadoUnaCaja();
        CajaTestData.agregarCliente(sistema);
        CajaTestData.cambiarCampo(ventas(), "customer", TipoCampo.RELACION, BloqueId.nuevo());
    }

    private void dadoUnClienteConOtroId() {
        dadoUnaCaja();
        CajaTestData.agregarCliente(sistema);
        elCliente().setId(RegistroId.nuevo());
    }

    // --- cuando ---

    private void cuandoSeCobraMedioKilo() {
        respuesta = cobrarVenta.execute(CajaTestData.request("0.5"));
    }

    private void cuandoSeIntentaCobrarMedioKilo() {
        rechazo = catchThrowable(() -> cobrarVenta.execute(CajaTestData.request("0.5")));
    }

    private void cuandoSeCobraAlClienteEntregando(List<CobrarVentaRequest.PagoRequest> pagos) {
        respuesta = cobrarVenta.execute(solicitudConCliente(pagos));
    }

    private void cuandoSeCobraAlClienteSinEntregaExplicita() {
        respuesta = cobrarVenta.execute(solicitudConCliente(null));
    }

    private void cuandoSeIntentaCobrarAlClienteEntregando(List<CobrarVentaRequest.PagoRequest> pagos) {
        rechazo = catchThrowable(() -> cobrarVenta.execute(solicitudConCliente(pagos)));
    }

    // --- entonces ---

    private void entoncesSeGuardanLaVentaYElDescuentoDeStock() {
        assertThat(respuesta.total()).isEqualByComparingTo("2000");
        entoncesElStockDelProductoQuedaEn("1.5");
        entoncesSeGuardoLaFilaDeVentasDeLaOperacion();
        verify(usoPlanRepository).bloquearNegocio(CajaTestData.NEGOCIO);
        verify(controlPlanService).validarAltaRegistros(CajaTestData.NEGOCIO, 1, CajaTestData.AHORA);
    }

    private void entoncesSeGuardoLaFilaDeVentasDeLaOperacion() {
        ArgumentCaptor<Sistema> guardado = ArgumentCaptor.forClass(Sistema.class);
        verify(sistemaRepository).guardar(guardado.capture());
        Registro fila = guardado.getValue().registrosPorBloque().get(CajaTestData.VENTAS).getFirst();
        assertThat(fila.getOrigen()).isEqualTo("Caja " + CajaTestData.OPERACION);
        assertThat(fila.getCreadoEn()).isEqualTo(CajaTestData.AHORA);
        assertThat(fila.getId()).isNotNull();
        assertThat(fila.getOrden()).isZero();
    }

    private void entoncesSeDevuelveElComprobanteDeLaVentaPrevia() {
        assertThat(respuesta.id()).isEqualTo(ventaPrevia.id());
    }

    private void entoncesNoSeVuelveADescontar() {
        verify(sistemaRepository, never()).guardar(any(Sistema.class));
    }

    private void entoncesSeRechazaCon(Class<? extends Throwable> tipo) {
        assertThat(rechazo).isInstanceOf(tipo);
    }

    private void entoncesSeRechazaSinGuardarNada(Class<? extends Throwable> tipo) {
        entoncesSeRechazaCon(tipo);
        verify(ventaCajaRepository, never()).save(any());
        verify(sistemaRepository, never()).guardar(any(Sistema.class));
    }

    private void entoncesElStockDelProductoQuedaEn(String cantidad) {
        assertThat(valorDelProducto("quantity")).isEqualTo(new BigDecimal(cantidad));
    }

    private void entoncesLaDeudaQuedaPendienteEn(String saldo, String estado) {
        assertThat(respuesta.saldo()).isEqualByComparingTo(saldo);
        assertThat(respuesta.cobrado()).isEqualByComparingTo(respuesta.total().subtract(new BigDecimal(saldo)));
        assertThat(respuesta.estado()).isEqualTo(estado);
        assertThat(respuesta.clienteId()).isEqualTo(CajaTestData.CLIENTE);
        assertThat(respuesta.clienteNombre()).isEqualTo("Ana López");
    }

    private void entoncesNoSeRegistraNingunPago() {
        assertThat(respuesta.pagos()).isEmpty();
        assertThat(respuesta.recibido()).isEqualByComparingTo("0");
    }

    private void entoncesLaFilaDeVentasQuedaFiadaAlCliente() {
        assertThat(valorDeLaFilaDeVentas("credit")).isEqualTo(true);
        assertThat(valorDeLaFilaDeVentas("customer")).isEqualTo(CajaTestData.CLIENTE.toString());
    }

    private void entoncesLaFilaDeVentasConservaElNombreDelCliente() {
        assertThat(valorDeLaFilaDeVentas("customer")).isEqualTo("Ana López");
    }

    private void entoncesLaVentaNoTieneCliente() {
        assertThat(respuesta.clienteId()).isNull();
    }

    private void entoncesLaFilaDeVentasNoQuedaFiada() {
        assertThat(valorDeLaFilaDeVentas("credit")).isEqualTo(false);
    }

    // --- datos ---

    private CobrarVentaRequest solicitudConCliente(List<CobrarVentaRequest.PagoRequest> pagos) {
        CobrarVentaRequest base = CajaTestData.request("0.5");
        return new CobrarVentaRequest(base.usuarioId(), base.id(), base.proyectoId(), base.inventarioId(),
                base.ventasId(), base.lineas(), base.medio(), base.recibido(), pagos, CajaTestData.CLIENTE);
    }

    private VentaCaja unaVentaPreviaConHuella(String huella) {
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

    private Registro elCliente() {
        return sistema.registrosPorBloque().get(CajaTestData.CLIENTES).getFirst();
    }

    private Object valorDelProducto(String clave) {
        return producto().getValores().get(campoDe(inventario(), clave).getId().toString());
    }

    private Object valorDeLaFilaDeVentas(String clave) {
        Registro fila = sistema.registrosPorBloque().get(CajaTestData.VENTAS).getFirst();
        return fila.getValores().get(campoDe(ventas(), clave).getId().toString());
    }

    private Campo campoDe(Bloque bloque, String clave) {
        return bloque.getCampos().stream().filter(campo -> clave.equals(campo.getClave())).findFirst().orElseThrow();
    }
}
