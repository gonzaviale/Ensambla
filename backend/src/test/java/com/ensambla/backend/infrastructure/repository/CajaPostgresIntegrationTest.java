package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.caja.CierreCaja;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.caja.CobroCajaId;
import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reporte.PanelNegocio;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.caja.CierreCajaItem;
import com.ensambla.backend.domain.useCase.caja.CobroCajaItem;
import com.ensambla.backend.domain.useCase.caja.ResumenCajaItem;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCaja;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCajaRequest;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVenta;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaRequest;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaResponse;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCaja;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCajaRequest;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCajaResponse;
import com.ensambla.backend.domain.useCase.caja.listarVentas.ListarVentasCaja;
import com.ensambla.backend.domain.useCase.caja.listarVentas.ListarVentasCajaRequest;
import com.ensambla.backend.domain.useCase.caja.listarVentas.ListarVentasCajaResponse;
import com.ensambla.backend.domain.useCase.caja.registrarCobro.RegistrarCobro;
import com.ensambla.backend.domain.useCase.caja.registrarCobro.RegistrarCobroRequest;
import com.ensambla.backend.infrastructure.config.useCase.CajaUseCaseConfig;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class CajaPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    @Autowired private CobrarVenta cobrarVenta;
    @Autowired private MovimientoStockRepository movimientosStock;
    @Autowired private RegistrarCobro registrarCobro;
    @Autowired private ListarVentasCaja listarVentasCaja;
    @Autowired private CobroCajaRepository cobroCajaRepository;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private DatosDeProyectoService datosDeProyectoService;
    @Autowired private UsoPlanRepository usoPlanRepository;
    @Autowired private TransaccionPort transaccionPort;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private CerrarCaja cerrarCaja;
    @Autowired private ConsultarCaja consultarCaja;
    @Autowired private CierreCajaRepository cierreCajaRepository;
    @Autowired private CajaUseCaseConfig cajaUseCaseConfig;
    @Autowired private SistemaRepository sistemaRepository;
    @Autowired private VentaCajaRepository ventaCajaRepository;
    @Autowired private IUsuarioJpaRepository usuarios;
    @Autowired private INegocioJpaRepository negocios;
    @Autowired private SuscripcionesDePrueba suscripciones;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter securityFilter;

    private UsuarioId usuarioId;
    private NegocioId negocioId;
    private boolean cuentaFueraDeTransaccion;
    private CobrarVentaResponse respuesta;
    private CobrarVentaResponse repetida;
    private CobrarVentaResponse venta;
    private VentaCajaId segundaVenta;
    private CerrarCajaRequest cierre;
    private CierreCajaItem guardado;
    private CierreCajaItem primerCierre;
    private CierreCajaItem segundoCierre;
    private CierreCajaItem guardadoRepetido;
    private CierreCaja guardadoRecargado;
    private RegistrarCobroRequest cobro;
    private CobroCajaItem cobroRegistrado;
    private ConsultarCajaResponse consulta;
    private ResumenCajaItem abierta;
    private ResumenCajaItem resumenDelDia;
    private ResumenCajaItem resumenDelDiaSiguiente;
    private ListarVentasCajaResponse cuenta;
    private PanelNegocio panel;
    private ResultActions respuestaHttp;
    private List<Boolean> resultados;
    private Throwable rechazo;

    @AfterEach
    void tearDown() {
        if (cuentaFueraDeTransaccion) {
            unaTransaccion().executeWithoutResult(estado -> eliminarLaCuenta());
        }
    }

    @Test
    @DisplayName("PostgreSQL conserva venta, pago y stock después de recargar")
    void postgresConservaVentaPagoYStockDespuesDeRecargar() {
        dadoUnaCuentaConInventario("2");

        cuandoSeCobraYSeRecarga();

        entoncesLaVentaQuedaGuardadaConSuTotalYSuLinea();
        entoncesElStockEs("1.5");
    }

    @Test
    @DisplayName("Repetir el cobro devuelve el mismo comprobante")
    void repetirElCobroDevuelveElMismoComprobante() {
        dadoUnaCuentaConInventario("2");

        cuandoSeCobraYSeRepiteElCobroTrasRecargar();

        entoncesSeDevuelveElMismoComprobante();
    }

    @Test
    @DisplayName("Un negocio ajeno no ve la venta de otro negocio")
    void unNegocioAjenoNoVeLaVentaDeOtroNegocio() {
        dadoUnaCuentaConInventario("2");

        cuandoSeCobraYSeRecarga();

        entoncesUnNegocioAjenoNoVeLaVenta();
    }

    @Test
    @DisplayName("La configuración de caja registra sus casos de uso")
    void laConfiguracionDeCajaRegistraSusCasosDeUso() {
        entoncesLaConfiguracionDeCajaEstaCargada();
    }

    @Test
    @DisplayName("La ruta real exige sesión antes de intentar registrar un cobro")
    void laRutaRealExigeSesionAntesDeIntentarRegistrarUnCobro() throws Exception {
        cuandoSeIntentaCobrarSinSesion();

        entoncesSeRechazaPorFaltaDeSesion();
    }

    @Test
    @DisplayName("El cierre conserva los pagos mixtos con su vuelto")
    void elCierreConservaLosPagosMixtosConSuVuelto() {
        dadoUnaVentaMixtaConVuelto();

        cuandoSeCierraLaCajaMixta();

        entoncesElCierreGuardaEfectivoTransferenciaYVuelto();
    }

    @Test
    @DisplayName("Repetir un cierre ya hecho devuelve el mismo cierre aunque haya ventas posteriores")
    void repetirUnCierreYaHechoDevuelveElMismoCierreAunqueHayaVentasPosteriores() {
        dadoUnaVentaMixtaConVuelto();
        dadoUnCierreDeLaCajaMixta();
        dadoUnaVentaPosteriorAlCierre();

        cuandoSeRepiteElCierre();

        entoncesSeDevuelveElMismoCierre();
    }

    @Test
    @DisplayName("Las ventas posteriores al cierre quedan en la caja abierta y el día suma todas")
    void lasVentasPosterioresAlCierreQuedanEnLaCajaAbiertaYElDiaSumaTodas() {
        dadoUnaVentaMixtaConVuelto();
        dadoUnCierreDeLaCajaMixta();
        dadoUnaVentaPosteriorAlCierre();

        cuandoSeConsultaLaCajaDelDiaDelCierre();

        entoncesLaCajaAbiertaTieneUnaVentaYElDiaTieneDos();
        entoncesSoloHayUnCierreListadoYUnaVentaAbierta();
    }

    @Test
    @DisplayName("Un negocio ajeno no ve el cierre de otro negocio")
    void unNegocioAjenoNoVeElCierreDeOtroNegocio() {
        dadoUnaVentaMixtaConVuelto();

        cuandoSeCierraLaCajaMixta();

        entoncesUnNegocioAjenoNoVeElCierre();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Dos cajas no pueden vender ambas el último medio kilo")
    void dosCajasNoPuedenVenderAmbasElUltimoMedioKilo() {
        dadoUnaCuentaConInventarioFueraDeTransaccion("0.5");

        cuandoDosCajasCobranElUltimoMedioKiloAlaVez();

        entoncesSoloUnaCajaCobra();
        entoncesElStockQuedaEnCero();
    }

    @Test
    @DisplayName("La venta fiada conserva su cliente y su deuda después de recargar PostgreSQL")
    void laVentaFiadaConservaSuClienteYSuDeudaDespuesDeRecargarPostgresql() {
        dadoUnaCuentaConCliente();

        cuandoSeCobraUnaVentaFiadaYSeRecarga();

        entoncesLaVentaQuedaPendienteConTodaLaDeuda();
        entoncesElStockEs("1.5");
    }

    @Test
    @DisplayName("La caja del día cuenta sólo la entrega recibida de una venta parcial")
    void laCajaDelDiaCuentaSoloLaEntregaRecibidaDeUnaVentaParcial() {
        dadoUnaCuentaConCliente();

        cuandoSeCobraUnaVentaParcialPorTransferenciaYSeConsultaElDia();

        entoncesElDiaCuentaSoloLaEntregaRecibida();
    }

    @Test
    @DisplayName("Cobrar una deuda de un período cerrado abre un cobro sin volver a contar la venta")
    void cobrarUnaDeudaDeUnPeriodoCerradoAbreUnCobroSinVolverAContarLaVenta() {
        dadoUnaVentaParcialConSuCajaCerrada();

        cuandoSeCobraLaDeudaRestante();

        entoncesLaCajaAbiertaSoloTieneElCobroDeLaDeuda();
    }

    @Test
    @DisplayName("El cierre siguiente cuenta sólo los cobros nuevos")
    void elCierreSiguienteCuentaSoloLosCobrosNuevos() {
        dadoUnaVentaParcialConSuCajaCerrada();

        cuandoSeCobraLaDeudaRestante();
        cuandoSeCierraLaCajaAbierta();

        entoncesElSegundoCierreSoloCuentaElCobro();
    }

    @Test
    @DisplayName("Cobrar la deuda no modifica el cierre anterior")
    void cobrarLaDeudaNoModificaElCierreAnterior() {
        dadoUnaVentaParcialConSuCajaCerrada();

        cuandoSeCobraLaDeudaRestante();

        entoncesElPrimerCierreConservaSuEfectivoYSuSaldo();
    }

    @Test
    @DisplayName("La venta cobrada por completo queda pagada y mantiene el stock")
    void laVentaCobradaPorCompletoQuedaPagadaYMantieneElStock() {
        dadoUnaVentaParcialConSuCajaCerrada();

        cuandoSeCobraLaDeudaRestante();

        entoncesLaVentaListadaQuedaPagada();
        entoncesElStockEs("1.5");
    }

    @Test
    @DisplayName("Repetir el cobro de la deuda devuelve el mismo cobro y no deja cobros abiertos")
    void repetirElCobroDeLaDeudaDevuelveElMismoCobroYNoDejaCobrosAbiertos() {
        dadoUnaVentaParcialConSuCajaCerrada();
        dadoQueSeCobroLaDeudaYSeCerroLaCaja();

        cuandoSeRepiteElCobroDeLaDeuda();

        entoncesSeDevuelveElMismoCobro();
        entoncesNoQuedanCobrosAbiertos();
    }

    @Test
    @DisplayName("Un pago recibido otro día entra en ese día sin volver a contar la venta")
    void unPagoRecibidoOtroDiaEntraEnEseDiaSinVolverAContarLaVenta() {
        dadoUnaVentaParcialConCliente();

        cuandoSeCobraElSaldoAlDiaSiguiente();

        entoncesElDiaDeLaVentaNoCuentaElCobroPosterior();
        entoncesElDiaSiguienteSoloCuentaElCobro();
    }

    @Test
    @DisplayName("Inicio lee el saldo y el flujo reales después de recargar ventas y cobros")
    void inicioLeeElSaldoYElFlujoRealesDespuesDeRecargarVentasYCobros() {
        dadoUnaVentaParcialConUnCobroPosterior();

        cuandoSeArmaElPanelDelDiaDeLaVenta();

        entoncesElPanelMuestraLoVendidoLoQueDebenYLoQueQueda();
    }

    @Test
    @DisplayName("Otro negocio u otro proyecto no ven las ventas ni los cobros")
    void otroNegocioUOtroProyectoNoVenLasVentasNiLosCobros() {
        dadoUnaVentaParcialConUnCobroPosterior();

        entoncesOtroNegocioYOtroProyectoNoVenVentasNiCobros();
    }

    @Test
    @DisplayName("Un pago puede completar varias deudas y conservar sus medios en un movimiento")
    void unPagoPuedeCompletarVariasDeudasYConservarSusMediosEnUnMovimiento() {
        dadoDosVentasFiadas();

        cuandoSeRegistraUnCobroParaLasDosVentas();

        entoncesElCobroSumaLasDosAplicacionesYDejaSusSaldos();
        entoncesElStockEs("1.0");
    }

    @Test
    @DisplayName("Un negocio ajeno no ve el cobro de otro negocio")
    void unNegocioAjenoNoVeElCobroDeOtroNegocio() {
        dadoDosVentasFiadas();

        cuandoSeRegistraUnCobroParaLasDosVentas();

        entoncesUnNegocioAjenoNoVeElCobro();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Dos cobros simultáneos no pueden cancelar dos veces el mismo saldo")
    void dosCobrosSimultaneosNoPuedenCancelarDosVecesElMismoSaldo() {
        dadoUnaVentaParcialFueraDeTransaccion();

        cuandoSeCobraElMismoSaldoDesdeDosCajasAlaVez();

        entoncesSoloUnCobroSeAplica();
        entoncesLaVentaQuedaSinSaldoConUnSoloCobro();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Si una aplicación del cobro es inválida se revierte el pago de todas las ventas")
    void siUnaAplicacionDelCobroEsInvalidaSeRevierteElPagoDeTodasLasVentas() {
        dadoDosVentasFiadasFueraDeTransaccion();

        cuandoSeIntentaCobrarConUnaAplicacionInvalida();

        entoncesSeRechazaElCobroPorReglaDeNegocio();
        entoncesNingunaVentaCambiaSuSaldoYNoHayCobros();
    }

    @Test
    @DisplayName("Los comprobantes y cierres anteriores a los cobros de deudas se siguen leyendo")
    void losComprobantesYCierresAnterioresALosCobrosDeDeudasSeSiguenLeyendo() {
        dadoUnCierreConFormatoAnteriorSinCobrosNiPagosIniciales();

        cuandoSeRecargaElCierre();

        entoncesElCierreAnteriorSeLeeSinLosCamposNuevos();
    }

    @Test
    @DisplayName("El importe libre parcial mixto persiste sin crear productos ni mover stock")
    void elImporteLibreParcialMixtoPersisteSinCrearProductosNiMoverStock() {
        dadoUnaCuentaConCliente();

        cuandoSeCobraUnImporteLibreParcialMixto();

        entoncesElImporteLibrePersisteSinStockComo("PARCIAL", "1500");
    }

    @Test
    @DisplayName("El importe libre fiado conserva toda la deuda y su descripción después de recargar")
    void elImporteLibreFiadoConservaTodaLaDeudaYSuDescripcionDespuesDeRecargar() {
        dadoUnaCuentaConCliente();

        cuandoSeCobraUnImporteLibreFiado();

        entoncesElImporteLibrePersisteSinStockComo("PENDIENTE", "2500");
    }

    @Test
    @DisplayName("Una venta repetida deja una sola salida y el saldo de movimientos coincide con inventario")
    void unaVentaRepetidaDejaUnaSolaSalidaYElSaldoDeMovimientosCoincideConInventario() {
        dadoUnaCuentaConInventario("2");

        cuandoSeCobraDosVecesLaMismaVentaYSeRecarga();

        entoncesElSaldoYLosMovimientosCoinciden("1.5", 2);
    }

    @Test
    @DisplayName("Dos líneas del mismo producto no duplican el ajuste de apertura")
    void dosLineasDelMismoProductoNoDuplicanElAjusteDeApertura() {
        dadoUnaCuentaConInventario("2");

        cuandoSeCobraUnaVentaConDosLineasDelMismoProducto();

        entoncesElSaldoYLosMovimientosCoinciden("1", 3);
    }

    @Test
    @DisplayName("Un ajuste manual del inventario queda asentado antes de la siguiente salida de caja")
    void unAjusteManualDelInventarioQuedaAsentadoAntesDeLaSiguienteSalidaDeCaja() {
        dadoUnaCuentaConInventario("2");
        dadoUnaVentaYElStockCorregidoAMano();

        cuandoSeCobraUnaSegundaVenta();

        entoncesElSaldoYLosMovimientosCoinciden("0.25", 4);
    }

    // --- dado ---

    private void dadoUnaCuentaConInventario(String stock) {
        usuarioId = UsuarioId.de(usuarios.saveAndFlush(UsuarioEntity.builder().nombre("Caja")
                .email(UsuarioId.nuevo() + "@example.com").estado(EstadoUsuario.ACTIVO)
                .creadoEn(CajaTestData.AHORA).actualizadoEn(CajaTestData.AHORA).build()).getId());
        negocioId = NegocioId.de(negocios.saveAndFlush(NegocioEntity.builder()
                .duenioId(usuarioId.valor()).nombre("Mi negocio")
                .creadoEn(CajaTestData.AHORA).actualizadoEn(CajaTestData.AHORA).build()).getId());
        suscripciones.asignarGratuito(negocioId, OffsetDateTime.now().minusDays(1));
        guardarElSistemaConStock(new BigDecimal(stock));
        recargar();
    }

    private void dadoUnaCuentaConCliente() {
        dadoUnaCuentaConInventario("2");
        Sistema sistema = sistemaRepository.findAllByNegocioId(negocioId).getFirst();
        CajaTestData.agregarCliente(sistema);
        asignarElNegocioALaTablaDeClientes(sistema);
        sistemaRepository.guardar(sistema);
        recargar();
    }

    private void dadoUnaCuentaConInventarioFueraDeTransaccion(String stock) {
        unaTransaccion().executeWithoutResult(estado -> dadoUnaCuentaConInventario(stock));
        cuentaFueraDeTransaccion = true;
    }

    private void dadoUnaVentaMixtaConVuelto() {
        dadoUnaCuentaConInventario("2");
        CobrarVentaRequest base = unaSolicitud();
        cobrarVenta.execute(new CobrarVentaRequest(base.usuarioId(), base.id(), base.proyectoId(),
                base.inventarioId(), base.ventasId(), base.lineas(), "MIXTO", new BigDecimal("1500"),
                List.of(new CobrarVentaRequest.PagoRequest("EFECTIVO", new BigDecimal("1000")),
                        new CobrarVentaRequest.PagoRequest("TRANSFERENCIA", new BigDecimal("1000")))));
    }

    private void dadoUnCierreDeLaCajaMixta() {
        cerrarLaCajaMixta();
        recargar();
    }

    private void dadoUnaVentaPosteriorAlCierre() {
        CobrarVentaRequest base = unaSolicitud();
        cobrarVenta.execute(new CobrarVentaRequest(base.usuarioId(), VentaCajaId.nuevo(), base.proyectoId(),
                base.inventarioId(), base.ventasId(), base.lineas(), "TARJETA", null));
    }

    private void dadoUnaVentaParcialConCliente() {
        dadoUnaCuentaConCliente();
        venta = cobrarVenta.execute(solicitudParcial(CajaTestData.OPERACION, "800"));
    }

    private void dadoUnaVentaParcialConSuCajaCerrada() {
        dadoUnaVentaParcialConCliente();
        primerCierre = cerrarCaja.execute(cierrePara(cajaAbierta().revision()));
        cobro = solicitudCobro(venta.id(), "1200", "EFECTIVO");
    }

    private void dadoQueSeCobroLaDeudaYSeCerroLaCaja() {
        cuandoSeCobraLaDeudaRestante();
        cuandoSeCierraLaCajaAbierta();
    }

    private void dadoUnaVentaParcialConUnCobroPosterior() {
        dadoUnaVentaParcialConCliente();
        registrarCobro.execute(solicitudCobro(venta.id(), "600", "TRANSFERENCIA"));
        recargar();
    }

    private void dadoDosVentasFiadas() {
        dadoUnaCuentaConCliente();
        segundaVenta = VentaCajaId.nuevo();
        cobrarVenta.execute(solicitudParcial(CajaTestData.OPERACION, "0"));
        cobrarVenta.execute(solicitudParcial(segundaVenta, "0"));
    }

    private void dadoUnaVentaParcialFueraDeTransaccion() {
        unaTransaccion().executeWithoutResult(estado -> dadoUnaVentaParcialConCliente());
        cuentaFueraDeTransaccion = true;
    }

    private void dadoDosVentasFiadasFueraDeTransaccion() {
        unaTransaccion().executeWithoutResult(estado -> dadoDosVentasFiadas());
        cuentaFueraDeTransaccion = true;
    }

    private void dadoUnCierreConFormatoAnteriorSinCobrosNiPagosIniciales() {
        dadoUnaCuentaConInventario("2");
        cobrarVenta.execute(unaSolicitud());
        entityManager.flush();
        jdbcTemplate.update("update venta_caja set datos = datos - 'pagosIniciales' where id = ?",
                CajaTestData.OPERACION.valor());
        entityManager.clear();
        cierre = cierrePara(cajaAbierta().revision());
        guardado = cerrarCaja.execute(cierre);
        entityManager.flush();
        jdbcTemplate.update("update cierre_caja set datos = jsonb_set(datos, '{resumen}', "
                + "(datos->'resumen') - 'cobros') where id = ?", guardado.id().valor());
        entityManager.clear();
    }

    private void dadoUnaVentaYElStockCorregidoAMano() {
        cobrarVenta.execute(unaSolicitud());
        Sistema sistema = sistemaRepository.findAllByNegocioId(negocioId).getFirst();
        var inventario = sistema.proyecto().getBloques().stream()
                .filter(bloque -> bloque.getId().equals(CajaTestData.INVENTARIO)).findFirst().orElseThrow();
        CajaTestData.poner(inventario, sistema.registrosPorBloque().get(CajaTestData.INVENTARIO).getFirst(),
                "quantity", new BigDecimal("0.75"));
        sistemaRepository.guardar(sistema);
    }

    // --- cuando ---

    private void cuandoSeCobraYSeRecarga() {
        respuesta = cobrarVenta.execute(unaSolicitud());
        recargar();
    }

    private void cuandoSeCobraYSeRepiteElCobroTrasRecargar() {
        cuandoSeCobraYSeRecarga();
        repetida = cobrarVenta.execute(unaSolicitud());
    }

    private void cuandoSeIntentaCobrarSinSesion() throws Exception {
        respuestaHttp = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build()
                .perform(post("/api/caja/ventas").contentType("application/json").content("{}"));
    }

    private void cuandoSeCierraLaCajaMixta() {
        cerrarLaCajaMixta();
    }

    private void cuandoSeRepiteElCierre() {
        guardadoRepetido = cerrarCaja.execute(cierre);
    }

    private void cuandoSeConsultaLaCajaDelDiaDelCierre() {
        consulta = consultarCaja.execute(new ConsultarCajaRequest(usuarioId, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, ZonaNegocio.dia(guardado.cerradoEn()), 0));
    }

    private void cuandoDosCajasCobranElUltimoMedioKiloAlaVez() {
        CobrarVentaRequest primera = unaSolicitud();
        CobrarVentaRequest segunda = new CobrarVentaRequest(primera.usuarioId(), VentaCajaId.nuevo(),
                primera.proyectoId(), primera.inventarioId(), primera.ventasId(), primera.lineas(),
                primera.medio(), primera.recibido());
        resultados = enParalelo(() -> intentarCobro(primera), () -> intentarCobro(segunda));
    }

    private void cuandoSeCobraUnaVentaFiadaYSeRecarga() {
        CobrarVentaRequest fiada = unaVentaFiada();
        cobrarVenta.execute(fiada);
        recargar();
        respuesta = cobrarVenta.execute(fiada);
    }

    private void cuandoSeCobraUnaVentaParcialPorTransferenciaYSeConsultaElDia() {
        CobrarVentaRequest base = unaSolicitud();
        cobrarVenta.execute(new CobrarVentaRequest(base.usuarioId(), base.id(), base.proyectoId(),
                base.inventarioId(), base.ventasId(), base.lineas(), "TRANSFERENCIA", null,
                List.of(new CobrarVentaRequest.PagoRequest("TRANSFERENCIA", new BigDecimal("800"))),
                CajaTestData.CLIENTE));
        recargar();
        resumenDelDia = consultarCaja.execute(new ConsultarCajaRequest(usuarioId, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, ZonaNegocio.dia(OffsetDateTime.now()), 0)).dia();
    }

    private void cuandoSeCobraLaDeudaRestante() {
        registrarCobro.execute(cobro);
        recargar();
        abierta = cajaAbierta();
    }

    private void cuandoSeCierraLaCajaAbierta() {
        segundoCierre = cerrarCaja.execute(cierrePara(abierta.revision()));
    }

    private void cuandoSeRepiteElCobroDeLaDeuda() {
        cobroRegistrado = registrarCobro.execute(cobro);
    }

    private void cuandoSeCobraElSaldoAlDiaSiguiente() {
        OffsetDateTime manana = venta.creadoEn().plusDays(1);
        new RegistrarCobro(negocioRepository, usoPlanRepository, ventaCajaRepository, cobroCajaRepository,
                transaccionPort, Clock.fixed(manana.toInstant(), ZoneOffset.UTC))
                .execute(solicitudCobro(venta.id(), "600", "TRANSFERENCIA"));
        recargar();
        resumenDelDia = resumenDelDia(venta.creadoEn());
        resumenDelDiaSiguiente = resumenDelDia(manana);
    }

    private void cuandoSeArmaElPanelDelDiaDeLaVenta() {
        Sistema sistema = sistemaRepository.findAllByNegocioId(negocioId).getFirst();
        var dia = ZonaNegocio.dia(venta.creadoEn());
        panel = PanelNegocio.armar(datosDeProyectoService.cargar(sistema), new PeriodoResumen(dia, dia), dia);
    }

    private void cuandoSeRegistraUnCobroParaLasDosVentas() {
        cobro = new RegistrarCobroRequest(usuarioId, CobroCajaId.nuevo(), CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, List.of(aplicacion(CajaTestData.OPERACION, "600", "EFECTIVO"),
                        aplicacion(segundaVenta, "700", "TARJETA")));
        cobroRegistrado = registrarCobro.execute(cobro);
        recargar();
        cuenta = listarVentasCaja.execute(new ListarVentasCajaRequest(usuarioId, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO));
    }

    private void cuandoSeCobraElMismoSaldoDesdeDosCajasAlaVez() {
        RegistrarCobroRequest efectivo = solicitudCobro(CajaTestData.OPERACION, "1200", "EFECTIVO");
        RegistrarCobroRequest transferencia = solicitudCobro(CajaTestData.OPERACION, "1200", "TRANSFERENCIA");
        resultados = enParalelo(() -> intentarPago(efectivo), () -> intentarPago(transferencia));
    }

    private void cuandoSeIntentaCobrarConUnaAplicacionInvalida() {
        cobro = new RegistrarCobroRequest(usuarioId, CobroCajaId.nuevo(), CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, List.of(aplicacion(CajaTestData.OPERACION, "500", "EFECTIVO"),
                        aplicacion(segundaVenta, "2001", "EFECTIVO")));
        rechazo = catchThrowable(() -> registrarCobro.execute(cobro));
    }

    private void cuandoSeRecargaElCierre() {
        guardadoRecargado = cierreCajaRepository.findByIdAndNegocioId(guardado.id(), negocioId).orElseThrow();
    }

    private void cuandoSeCobraUnImporteLibreParcialMixto() {
        cobrarImporteLibre(List.of(new CobrarVentaRequest.PagoRequest("EFECTIVO", new BigDecimal("500")),
                new CobrarVentaRequest.PagoRequest("TRANSFERENCIA", new BigDecimal("500"))));
    }

    private void cuandoSeCobraUnImporteLibreFiado() {
        cobrarImporteLibre(List.of());
    }

    private void cuandoSeCobraDosVecesLaMismaVentaYSeRecarga() {
        cobrarVenta.execute(unaSolicitud());
        cobrarVenta.execute(unaSolicitud());
        recargar();
    }

    private void cuandoSeCobraUnaVentaConDosLineasDelMismoProducto() {
        CobrarVentaRequest base = unaSolicitud();
        cobrarVenta.execute(new CobrarVentaRequest(usuarioId, base.id(), base.proyectoId(), base.inventarioId(),
                base.ventasId(), List.of(base.lineas().getFirst(), base.lineas().getFirst()), "EFECTIVO", null));
        recargar();
    }

    private void cuandoSeCobraUnaSegundaVenta() {
        CobrarVentaRequest base = unaSolicitud();
        cobrarVenta.execute(new CobrarVentaRequest(usuarioId, VentaCajaId.nuevo(), base.proyectoId(),
                base.inventarioId(), base.ventasId(), base.lineas(), "EFECTIVO", null));
    }

    // --- entonces ---

    private void entoncesLaVentaQuedaGuardadaConSuTotalYSuLinea() {
        assertThat(ventaCajaRepository.findByIdAndNegocioId(respuesta.id(), negocioId))
                .hasValueSatisfying(guardada -> {
                    assertThat(guardada.total()).isEqualByComparingTo("2000");
                    assertThat(guardada.lineas().getFirst().nombre()).isEqualTo("Pan francés");
                });
    }

    private void entoncesSeDevuelveElMismoComprobante() {
        assertThat(repetida).isEqualTo(respuesta);
    }

    private void entoncesUnNegocioAjenoNoVeLaVenta() {
        assertThat(ventaCajaRepository.findByIdAndNegocioId(respuesta.id(), NegocioId.nuevo())).isEmpty();
    }

    private void entoncesLaConfiguracionDeCajaEstaCargada() {
        assertThat(cajaUseCaseConfig).isNotNull();
    }

    private void entoncesSeRechazaPorFaltaDeSesion() throws Exception {
        respuestaHttp.andExpect(status().isUnauthorized());
    }

    private void entoncesElCierreGuardaEfectivoTransferenciaYVuelto() {
        assertThat(guardado.resumen().efectivo()).isEqualByComparingTo("1000");
        assertThat(guardado.resumen().transferencia()).isEqualByComparingTo("1000");
        assertThat(guardado.resumen().ventas().getFirst().vuelto()).isEqualByComparingTo("500");
    }

    private void entoncesSeDevuelveElMismoCierre() {
        assertThat(guardadoRepetido).isEqualTo(guardado);
    }

    private void entoncesLaCajaAbiertaTieneUnaVentaYElDiaTieneDos() {
        assertThat(consulta.abierta().ventas()).hasSize(1);
        assertThat(consulta.dia().ventas()).hasSize(2);
    }

    private void entoncesSoloHayUnCierreListadoYUnaVentaAbierta() {
        assertThat(ventaCajaRepository.listarAbiertas(negocioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                .hasSize(1);
        assertThat(cierreCajaRepository.listar(negocioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO, 0))
                .hasSize(1);
    }

    private void entoncesUnNegocioAjenoNoVeElCierre() {
        assertThat(cierreCajaRepository.findByIdAndNegocioId(cierre.id(), NegocioId.nuevo())).isEmpty();
    }

    private void entoncesSoloUnaCajaCobra() {
        assertThat(resultados).containsExactlyInAnyOrder(true, false);
    }

    private void entoncesElStockQuedaEnCero() {
        unaTransaccion().executeWithoutResult(estado -> entoncesElStockEs("0.0"));
    }

    private void entoncesLaVentaQuedaPendienteConTodaLaDeuda() {
        assertThat(respuesta.estado()).isEqualTo("PENDIENTE");
        assertThat(respuesta.saldo()).isEqualByComparingTo("2000");
        assertThat(respuesta.cobrado()).isEqualByComparingTo("0");
        assertThat(respuesta.clienteNombre()).isEqualTo("Ana López");
    }

    private void entoncesElDiaCuentaSoloLaEntregaRecibida() {
        assertThat(resumenDelDia.total()).isEqualByComparingTo("2000");
        assertThat(resumenDelDia.transferencia()).isEqualByComparingTo("800");
        assertThat(resumenDelDia.efectivo()).isEqualByComparingTo("0");
        assertThat(resumenDelDia.ventas().getFirst().saldo()).isEqualByComparingTo("1200");
    }

    private void entoncesLaCajaAbiertaSoloTieneElCobroDeLaDeuda() {
        assertThat(abierta.ventas()).isEmpty();
        assertThat(abierta.cobros()).hasSize(1);
        assertThat(abierta.efectivo()).isEqualByComparingTo("1200");
    }

    private void entoncesElSegundoCierreSoloCuentaElCobro() {
        assertThat(segundoCierre.resumen().total()).isEqualByComparingTo("0");
        assertThat(segundoCierre.resumen().efectivo()).isEqualByComparingTo("1200");
    }

    private void entoncesElPrimerCierreConservaSuEfectivoYSuSaldo() {
        assertThat(cierreCajaRepository.findByIdAndNegocioId(primerCierre.id(), negocioId)).hasValueSatisfying(c -> {
            assertThat(c.resumen().efectivo()).isEqualByComparingTo("800");
            assertThat(c.resumen().ventas().getFirst().saldo()).isEqualByComparingTo("1200");
        });
    }

    private void entoncesLaVentaListadaQuedaPagada() {
        assertThat(listarVentasCaja.execute(new ListarVentasCajaRequest(usuarioId, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO)).ventas().getFirst().estado()).isEqualTo("PAGADA");
    }

    private void entoncesSeDevuelveElMismoCobro() {
        assertThat(cobroRegistrado.id()).isEqualTo(cobro.id());
    }

    private void entoncesNoQuedanCobrosAbiertos() {
        assertThat(cobroCajaRepository.listarAbiertos(negocioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                .isEmpty();
    }

    private void entoncesElDiaDeLaVentaNoCuentaElCobroPosterior() {
        assertThat(resumenDelDia.total()).isEqualByComparingTo("2000");
        assertThat(resumenDelDia.efectivo()).isEqualByComparingTo("800");
        assertThat(resumenDelDia.transferencia()).isEqualByComparingTo("0");
        assertThat(resumenDelDia.ventas().getFirst().saldo()).isEqualByComparingTo("600");
    }

    private void entoncesElDiaSiguienteSoloCuentaElCobro() {
        assertThat(resumenDelDiaSiguiente.ventas()).isEmpty();
        assertThat(resumenDelDiaSiguiente.cobros()).hasSize(1);
        assertThat(resumenDelDiaSiguiente.transferencia()).isEqualByComparingTo("600");
    }

    private void entoncesElPanelMuestraLoVendidoLoQueDebenYLoQueQueda() {
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("2000");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("600");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("1400");
    }

    private void entoncesOtroNegocioYOtroProyectoNoVenVentasNiCobros() {
        assertThat(ventaCajaRepository.listarPorProyecto(NegocioId.nuevo(), CajaTestData.PROYECTO)).isEmpty();
        assertThat(ventaCajaRepository.listarPorProyecto(negocioId, ProyectoId.nuevo())).isEmpty();
        assertThat(cobroCajaRepository.listarPorProyecto(NegocioId.nuevo(), CajaTestData.PROYECTO)).isEmpty();
        assertThat(cobroCajaRepository.listarPorProyecto(negocioId, ProyectoId.nuevo())).isEmpty();
    }

    private void entoncesElCobroSumaLasDosAplicacionesYDejaSusSaldos() {
        assertThat(cobroRegistrado.importe()).isEqualByComparingTo("1300");
        assertThat(cuenta.cobros()).hasSize(1);
        assertThat(cuenta.ventas()).extracting(v -> v.saldo().intValue()).containsExactlyInAnyOrder(1400, 1300);
    }

    private void entoncesUnNegocioAjenoNoVeElCobro() {
        assertThat(cobroCajaRepository.findByIdAndNegocioId(cobroRegistrado.id(), NegocioId.nuevo())).isEmpty();
    }

    private void entoncesSoloUnCobroSeAplica() {
        assertThat(resultados).containsExactlyInAnyOrder(true, false);
    }

    private void entoncesLaVentaQuedaSinSaldoConUnSoloCobro() {
        unaTransaccion().executeWithoutResult(estado -> {
            assertThat(ventaCajaRepository.findByIdAndNegocioId(CajaTestData.OPERACION, negocioId))
                    .hasValueSatisfying(guardada -> assertThat(guardada.saldo()).isEqualByComparingTo("0"));
            assertThat(cobroCajaRepository.listarTodos(negocioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                    .hasSize(1);
            entoncesElStockEs("1.5");
        });
    }

    private void entoncesSeRechazaElCobroPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesNingunaVentaCambiaSuSaldoYNoHayCobros() {
        unaTransaccion().executeWithoutResult(estado -> {
            assertThat(ventaCajaRepository.listarTodas(negocioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                    .allSatisfy(guardada -> assertThat(guardada.saldo()).isEqualByComparingTo("2000"));
            assertThat(cobroCajaRepository.listarTodos(negocioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                    .isEmpty();
        });
    }

    private void entoncesElCierreAnteriorSeLeeSinLosCamposNuevos() {
        assertThat(guardadoRecargado.resumen().cobros()).isEmpty();
        assertThat(guardadoRecargado.resumen().ventas().getFirst().pagosIniciales()).hasSize(1);
        assertThat(guardadoRecargado.resumen().efectivo()).isEqualByComparingTo("2000");
    }

    private void entoncesElImporteLibrePersisteSinStockComo(String estado, String saldo) {
        VentaCaja guardada = ventaCajaRepository.findByIdAndNegocioId(respuesta.id(), negocioId).orElseThrow();
        assertThat(guardada.lineas().getFirst().productoId()).isNull();
        assertThat(guardada.lineas().getFirst().nombre()).isEqualTo("Servicio");
        assertThat(guardada.estado().name()).isEqualTo(estado);
        assertThat(guardada.saldo()).isEqualByComparingTo(saldo);
        entoncesNoSeCreoNingunProductoNiSeMovioElStock();
        entoncesLaVentaDeVentasSeOrigino();
    }

    private void entoncesNoSeCreoNingunProductoNiSeMovioElStock() {
        Sistema sistema = sistemaRepository.findAllByNegocioId(negocioId).getFirst();
        assertThat(sistema.registrosPorBloque().get(CajaTestData.INVENTARIO)).hasSize(1);
        entoncesElStockEs("2");
    }

    private void entoncesLaVentaDeVentasSeOrigino() {
        Sistema sistema = sistemaRepository.findAllByNegocioId(negocioId).getFirst();
        assertThat(sistema.registrosPorBloque().get(CajaTestData.VENTAS).getFirst().getOrigen())
                .isEqualTo("Caja " + respuesta.id());
    }

    private void entoncesElSaldoYLosMovimientosCoinciden(String saldo, int cantidadMovimientos) {
        assertThat(movimientosStock.stockCompletoDe(List.of(CajaTestData.PRODUCTO), negocioId)
                .get(CajaTestData.PRODUCTO)).isEqualByComparingTo(saldo);
        assertThat(movimientosStock.findRecientes(CajaTestData.INVENTARIO, negocioId, CajaTestData.PRODUCTO, 100))
                .hasSize(cantidadMovimientos);
        entoncesElStockEs(saldo);
    }

    private void entoncesElStockEs(String esperado) {
        Sistema recargado = sistemaRepository.findAllByNegocioId(negocioId).getFirst();
        var campo = recargado.proyecto().getBloques().stream()
                .filter(bloque -> bloque.getId().equals(CajaTestData.INVENTARIO))
                .findFirst().orElseThrow().getCampos().stream()
                .filter(c -> c.getClave().equals("quantity")).findFirst().orElseThrow();
        Object stock = recargado.registrosPorBloque().get(CajaTestData.INVENTARIO).getFirst()
                .getValores().get(campo.getId().toString());
        assertThat(new BigDecimal(stock.toString())).isEqualByComparingTo(esperado);
    }

    // --- datos ---

    private CobrarVentaRequest unaSolicitud() {
        CobrarVentaRequest base = CajaTestData.request("0.5");
        return new CobrarVentaRequest(usuarioId, base.id(), base.proyectoId(), base.inventarioId(),
                base.ventasId(), base.lineas(), base.medio(), base.recibido());
    }

    private CobrarVentaRequest unaVentaFiada() {
        CobrarVentaRequest base = unaSolicitud();
        return new CobrarVentaRequest(base.usuarioId(), base.id(), base.proyectoId(), base.inventarioId(),
                base.ventasId(), base.lineas(), "EFECTIVO", null, List.of(), CajaTestData.CLIENTE);
    }

    private CobrarVentaRequest solicitudParcial(VentaCajaId id, String importe) {
        CobrarVentaRequest base = unaSolicitud();
        List<CobrarVentaRequest.PagoRequest> pagos = pagosIniciales(importe);
        return new CobrarVentaRequest(usuarioId, id, base.proyectoId(), base.inventarioId(), base.ventasId(),
                base.lineas(), "EFECTIVO", null, pagos, CajaTestData.CLIENTE);
    }

    private List<CobrarVentaRequest.PagoRequest> pagosIniciales(String importe) {
        BigDecimal entrega = new BigDecimal(importe);
        return entrega.signum() == 0 ? List.of()
                : List.of(new CobrarVentaRequest.PagoRequest("EFECTIVO", entrega));
    }

    private RegistrarCobroRequest solicitudCobro(VentaCajaId ventaId, String importe, String medio) {
        return new RegistrarCobroRequest(usuarioId, CobroCajaId.nuevo(), CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, List.of(aplicacion(ventaId, importe, medio)));
    }

    private RegistrarCobroRequest.AplicacionRequest aplicacion(VentaCajaId ventaId, String importe, String medio) {
        return new RegistrarCobroRequest.AplicacionRequest(ventaId,
                List.of(new RegistrarCobroRequest.PagoRequest(medio, new BigDecimal(importe))));
    }

    private void cerrarLaCajaMixta() {
        cierre = cierreDeLaCajaMixta();
        guardado = cerrarCaja.execute(cierre);
    }

    private CerrarCajaRequest cierreDeLaCajaMixta() {
        ResumenCaja resumen = ResumenCaja.de(ventaCajaRepository.listarAbiertas(
                negocioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO));
        return new CerrarCajaRequest(usuarioId, CierreCajaId.nuevo(), CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, resumen.revision(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1000"), "");
    }

    private CerrarCajaRequest cierrePara(String revision) {
        return new CerrarCajaRequest(usuarioId, CierreCajaId.nuevo(), CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, revision, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, "Recuento de prueba");
    }

    private ConsultarCajaRequest consultaActual() {
        return new ConsultarCajaRequest(usuarioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO, null, 0);
    }

    private ResumenCajaItem cajaAbierta() {
        return consultarCaja.execute(consultaActual()).abierta();
    }

    private ResumenCajaItem resumenDelDia(OffsetDateTime instante) {
        return consultarCaja.execute(new ConsultarCajaRequest(usuarioId, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, ZonaNegocio.dia(instante), 0)).dia();
    }

    private void cobrarImporteLibre(List<CobrarVentaRequest.PagoRequest> pagos) {
        respuesta = cobrarVenta.execute(new CobrarVentaRequest(usuarioId, VentaCajaId.nuevo(),
                CajaTestData.PROYECTO, CajaTestData.INVENTARIO, CajaTestData.VENTAS,
                List.of(new CobrarVentaRequest.LineaRequest(null, BigDecimal.ONE, new BigDecimal("2500"),
                        "UNIDAD", "Servicio")),
                "MIXTO", null, pagos, CajaTestData.CLIENTE));
        recargar();
    }

    private void guardarElSistemaConStock(BigDecimal stock) {
        Sistema sistema = CajaTestData.sistema();
        sistema.proyecto().setNegocioId(negocioId);
        sistema.proyecto().getBloques().forEach(bloque -> {
            bloque.setNegocioId(negocioId);
            bloque.getCampos().forEach(campo -> campo.setNegocioId(negocioId));
        });
        sistema.registrosPorBloque().values().forEach(filas -> filas.forEach(fila -> fila.setNegocioId(negocioId)));
        CajaTestData.poner(sistema.proyecto().getBloques().getFirst(),
                sistema.registrosPorBloque().get(CajaTestData.INVENTARIO).getFirst(), "quantity", stock);
        sistemaRepository.guardar(sistema);
    }

    private void asignarElNegocioALaTablaDeClientes(Sistema sistema) {
        var tabla = sistema.proyecto().getBloques().stream()
                .filter(bloque -> bloque.getId().equals(CajaTestData.CLIENTES)).findFirst().orElseThrow();
        tabla.setNegocioId(negocioId);
        tabla.getCampos().forEach(campo -> campo.setNegocioId(negocioId));
        sistema.proyecto().getBloques().stream().filter(bloque -> bloque.getId().equals(CajaTestData.VENTAS))
                .flatMap(bloque -> bloque.getCampos().stream()).forEach(campo -> campo.setNegocioId(negocioId));
        sistema.registrosPorBloque().get(CajaTestData.CLIENTES).forEach(fila -> fila.setNegocioId(negocioId));
    }

    private void recargar() {
        entityManager.flush();
        entityManager.clear();
    }

    private TransactionTemplate unaTransaccion() {
        return new TransactionTemplate(transactionManager);
    }

    private void eliminarLaCuenta() {
        negocios.deleteById(negocioId.valor());
        usuarios.deleteById(usuarioId.valor());
    }

    private List<Boolean> enParalelo(Supplier<Boolean> primera, Supplier<Boolean> segunda) {
        CompletableFuture<Boolean> a = CompletableFuture.supplyAsync(primera);
        CompletableFuture<Boolean> b = CompletableFuture.supplyAsync(segunda);
        return List.of(a.join(), b.join());
    }

    private boolean intentarCobro(CobrarVentaRequest request) {
        try {
            cobrarVenta.execute(request);
            return true;
        } catch (DuplicateResourceException e) {
            return false;
        }
    }

    private boolean intentarPago(RegistrarCobroRequest request) {
        try {
            registrarCobro.execute(request);
            return true;
        } catch (BusinessRuleException e) {
            return false;
        }
    }
}
