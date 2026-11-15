package com.ensambla.backend.infrastructure.repository;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.caja.CobroCajaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reporte.PanelNegocio;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.useCase.caja.registrarCobro.RegistrarCobro;
import com.ensambla.backend.domain.useCase.caja.registrarCobro.RegistrarCobroRequest;
import com.ensambla.backend.domain.useCase.caja.listarVentas.ListarVentasCaja;
import com.ensambla.backend.domain.useCase.caja.listarVentas.ListarVentasCajaRequest;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCaja;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCajaRequest;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCaja;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCajaRequest;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVenta;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaRequest;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaResponse;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class CajaPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private UsuarioId usuarioId;
    private NegocioId negocioId;

    @Autowired private CobrarVenta cobrarVenta;
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

    @Test
    @DisplayName("PostgreSQL conserva venta, pago, stock y comprobante idempotente después de recargar")
    void checkoutShouldRoundTripWithPostgres() {
        givenCuentaConInventario(new BigDecimal("2"));

        CobrarVentaResponse respuesta = cobrarVenta.execute(solicitud());
        entityManager.flush();
        entityManager.clear();
        CobrarVentaResponse repetida = cobrarVenta.execute(solicitud());

        assertThat(repetida).isEqualTo(respuesta);
        assertThat(ventaCajaRepository.findByIdAndNegocioId(respuesta.id(), NegocioId.nuevo())).isEmpty();
        thenStockIs("1.5");
        assertThat(ventaCajaRepository.findByIdAndNegocioId(respuesta.id(), negocioId))
                .hasValueSatisfying(venta -> {
                    assertThat(venta.total()).isEqualByComparingTo("2000");
                    assertThat(venta.lineas().getFirst().nombre()).isEqualTo("Pan francés");
                });
        assertThat(cajaUseCaseConfig).isNotNull();
    }

    @Test
    @DisplayName("La ruta real exige sesión antes de intentar registrar un cobro")
    void checkoutShouldRequireSession() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build()
                .perform(post("/api/caja/ventas").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El cierre conserva pagos mixtos y separa las ventas posteriores aunque se recargue la base")
    void closureShouldPersistSplitAndStartNextPeriod() {
        givenCuentaConInventario(new BigDecimal("2"));
        CobrarVentaRequest base = solicitud();
        CobrarVentaRequest mixta = new CobrarVentaRequest(base.usuarioId(), base.id(), base.proyectoId(),
                base.inventarioId(), base.ventasId(), base.lineas(), "MIXTO", new BigDecimal("1500"),
                List.of(new CobrarVentaRequest.PagoRequest("EFECTIVO", new BigDecimal("1000")),
                        new CobrarVentaRequest.PagoRequest("TRANSFERENCIA", new BigDecimal("1000"))));
        cobrarVenta.execute(mixta);
        ResumenCaja resumen = ResumenCaja.de(ventaCajaRepository.listarAbiertas(
                negocioId, base.proyectoId(), base.inventarioId()));
        CerrarCajaRequest cierre = new CerrarCajaRequest(usuarioId, CierreCajaId.nuevo(), base.proyectoId(),
                base.inventarioId(), resumen.revision(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000"), "");

        var guardado = cerrarCaja.execute(cierre);
        entityManager.flush();
        entityManager.clear();
        cobrarVenta.execute(new CobrarVentaRequest(base.usuarioId(), VentaCajaId.nuevo(), base.proyectoId(),
                base.inventarioId(), base.ventasId(), base.lineas(), "TARJETA", null));

        assertThat(cerrarCaja.execute(cierre)).isEqualTo(guardado);
        assertThat(guardado.resumen().efectivo()).isEqualByComparingTo("1000");
        assertThat(guardado.resumen().transferencia()).isEqualByComparingTo("1000");
        assertThat(guardado.resumen().ventas().getFirst().vuelto()).isEqualByComparingTo("500");
        assertThat(ventaCajaRepository.listarAbiertas(negocioId, base.proyectoId(), base.inventarioId())).hasSize(1);
        assertThat(cierreCajaRepository.listar(negocioId, base.proyectoId(), base.inventarioId(), 0)).hasSize(1);
        assertThat(cierreCajaRepository.findByIdAndNegocioId(cierre.id(), NegocioId.nuevo())).isEmpty();
        var consulta = consultarCaja.execute(new ConsultarCajaRequest(usuarioId, base.proyectoId(),
                base.inventarioId(), ZonaNegocio.dia(guardado.cerradoEn()), 0));
        assertThat(consulta.abierta().ventas()).hasSize(1);
        assertThat(consulta.dia().ventas()).hasSize(2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Dos cajas no pueden vender ambas el último medio kilo")
    void checkoutShouldSerializeLastStock() {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        transaccion.executeWithoutResult(estado -> givenCuentaConInventario(new BigDecimal("0.5")));
        CobrarVentaRequest primera = solicitud();
        CobrarVentaRequest segunda = new CobrarVentaRequest(primera.usuarioId(), VentaCajaId.nuevo(),
                primera.proyectoId(), primera.inventarioId(), primera.ventasId(), primera.lineas(),
                primera.medio(), primera.recibido());

        try {
            CompletableFuture<Boolean> a = CompletableFuture.supplyAsync(() -> intentarCobro(primera));
            CompletableFuture<Boolean> b = CompletableFuture.supplyAsync(() -> intentarCobro(segunda));

            assertThat(a.join()).isNotEqualTo(b.join());
            transaccion.executeWithoutResult(estado -> thenStockIs("0.0"));
        } finally {
            transaccion.executeWithoutResult(estado -> {
                negocios.deleteById(negocioId.valor());
                usuarios.deleteById(usuarioId.valor());
            });
        }
    }

    @Test
    @DisplayName("La venta fiada conserva su cliente y saldo después de recargar PostgreSQL")
    void creditSaleShouldKeepCustomerAndBalanceAfterReload() {
        givenCuentaConCliente();
        CobrarVentaRequest base = solicitud();
        CobrarVentaRequest fiada = new CobrarVentaRequest(base.usuarioId(), base.id(), base.proyectoId(),
                base.inventarioId(), base.ventasId(), base.lineas(), "EFECTIVO", null, List.of(), CajaTestData.CLIENTE);

        cobrarVenta.execute(fiada);
        entityManager.flush();
        entityManager.clear();
        CobrarVentaResponse respuesta = cobrarVenta.execute(fiada);

        assertThat(respuesta.estado()).isEqualTo("PENDIENTE");
        assertThat(respuesta.saldo()).isEqualByComparingTo("2000");
        assertThat(respuesta.cobrado()).isEqualByComparingTo("0");
        assertThat(respuesta.clienteNombre()).isEqualTo("Ana López");
        thenStockIs("1.5");
    }

    @Test
    @DisplayName("La caja del día cuenta sólo la entrega recibida de una venta parcial")
    void partialSaleShouldOnlyIncludeCollectedMoneyInDaySummary() {
        givenCuentaConCliente();
        CobrarVentaRequest base = solicitud();
        CobrarVentaRequest parcial = new CobrarVentaRequest(base.usuarioId(), base.id(), base.proyectoId(),
                base.inventarioId(), base.ventasId(), base.lineas(), "TRANSFERENCIA", null,
                List.of(new CobrarVentaRequest.PagoRequest("TRANSFERENCIA", new BigDecimal("800"))), CajaTestData.CLIENTE);

        cobrarVenta.execute(parcial);
        entityManager.flush();
        entityManager.clear();
        var resumen = consultarCaja.execute(new ConsultarCajaRequest(usuarioId, base.proyectoId(),
                base.inventarioId(), ZonaNegocio.dia(OffsetDateTime.now()), 0)).dia();

        assertThat(resumen.total()).isEqualByComparingTo("2000");
        assertThat(resumen.transferencia()).isEqualByComparingTo("800");
        assertThat(resumen.efectivo()).isEqualByComparingTo("0");
        assertThat(resumen.ventas().getFirst().saldo()).isEqualByComparingTo("1200");
    }

    @Test
    @DisplayName("Cobrar una deuda de un período cerrado no modifica el cierre y permite cerrar sólo cobros")
    void debtCollectionShouldKeepOldClosureAndCloseNewMoney() {
        givenCuentaConCliente();
        var venta = cobrarVenta.execute(solicitudParcial(CajaTestData.OPERACION, "800"));
        var resumen = consultarCaja.execute(consultaActual()).abierta();
        var primerCierre = cerrarCaja.execute(cierrePara(resumen.revision()));
        RegistrarCobroRequest request = solicitudCobro(venta.id(), "1200", "EFECTIVO");

        registrarCobro.execute(request);
        entityManager.flush();
        entityManager.clear();
        var abierta = consultarCaja.execute(consultaActual()).abierta();
        var segundoCierre = cerrarCaja.execute(cierrePara(abierta.revision()));

        assertThat(abierta.ventas()).isEmpty();
        assertThat(abierta.cobros()).hasSize(1);
        assertThat(abierta.efectivo()).isEqualByComparingTo("1200");
        assertThat(segundoCierre.resumen().total()).isEqualByComparingTo("0");
        assertThat(segundoCierre.resumen().efectivo()).isEqualByComparingTo("1200");
        assertThat(cierreCajaRepository.findByIdAndNegocioId(primerCierre.id(), negocioId)).hasValueSatisfying(c -> {
            assertThat(c.resumen().efectivo()).isEqualByComparingTo("800");
            assertThat(c.resumen().ventas().getFirst().saldo()).isEqualByComparingTo("1200");
        });
        assertThat(listarVentasCaja.execute(new ListarVentasCajaRequest(usuarioId, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO)).ventas().getFirst().estado()).isEqualTo("PAGADA");
        assertThat(registrarCobro.execute(request).id()).isEqualTo(request.id());
        assertThat(cobroCajaRepository.listarAbiertos(negocioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO)).isEmpty();
        thenStockIs("1.5");
    }

    @Test
    @DisplayName("Un pago recibido otro día entra en ese día sin volver a contar la venta")
    void debtCollectionShouldUseCollectionDateForCash() {
        givenCuentaConCliente();
        var venta = cobrarVenta.execute(solicitudParcial(CajaTestData.OPERACION, "800"));
        var manana = venta.creadoEn().plusDays(1);
        RegistrarCobro otroDia = new RegistrarCobro(negocioRepository, usoPlanRepository, ventaCajaRepository,
                cobroCajaRepository, transaccionPort, Clock.fixed(manana.toInstant(), ZoneOffset.UTC));

        otroDia.execute(solicitudCobro(venta.id(), "600", "TRANSFERENCIA"));
        entityManager.flush();
        entityManager.clear();
        var hoy = consultarCaja.execute(new ConsultarCajaRequest(usuarioId, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, ZonaNegocio.dia(venta.creadoEn()), 0)).dia();
        var siguiente = consultarCaja.execute(new ConsultarCajaRequest(usuarioId, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, ZonaNegocio.dia(manana), 0)).dia();

        assertThat(hoy.total()).isEqualByComparingTo("2000");
        assertThat(hoy.efectivo()).isEqualByComparingTo("800");
        assertThat(hoy.transferencia()).isEqualByComparingTo("0");
        assertThat(hoy.ventas().getFirst().saldo()).isEqualByComparingTo("600");
        assertThat(siguiente.ventas()).isEmpty();
        assertThat(siguiente.cobros()).hasSize(1);
        assertThat(siguiente.transferencia()).isEqualByComparingTo("600");
    }

    @Test
    @DisplayName("Inicio lee el saldo y el flujo reales después de recargar ventas y cobros de PostgreSQL")
    void reportsShouldUseCurrentDebtAndRealCash() {
        givenCuentaConCliente();
        var venta = cobrarVenta.execute(solicitudParcial(CajaTestData.OPERACION, "800"));
        registrarCobro.execute(solicitudCobro(venta.id(), "600", "TRANSFERENCIA"));
        entityManager.flush();
        entityManager.clear();
        Sistema sistema = sistemaRepository.findAllByNegocioId(negocioId).getFirst();
        var dia = ZonaNegocio.dia(venta.creadoEn());

        var datos = datosDeProyectoService.cargar(sistema);
        PanelNegocio panel = PanelNegocio.armar(datos, new PeriodoResumen(dia, dia), dia);

        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("2000");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("600");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("1400");
        assertThat(ventaCajaRepository.listarPorProyecto(NegocioId.nuevo(), CajaTestData.PROYECTO)).isEmpty();
        assertThat(ventaCajaRepository.listarPorProyecto(negocioId, ProyectoId.nuevo())).isEmpty();
        assertThat(cobroCajaRepository.listarPorProyecto(NegocioId.nuevo(), CajaTestData.PROYECTO)).isEmpty();
        assertThat(cobroCajaRepository.listarPorProyecto(negocioId, ProyectoId.nuevo())).isEmpty();
    }

    @Test
    @DisplayName("Un pago puede completar varias deudas y conservar sus medios en un movimiento")
    void debtCollectionShouldApplyToMultipleSales() {
        givenCuentaConCliente();
        var primera = cobrarVenta.execute(solicitudParcial(CajaTestData.OPERACION, "0"));
        var segunda = cobrarVenta.execute(solicitudParcial(VentaCajaId.nuevo(), "0"));
        var request = new RegistrarCobroRequest(usuarioId, CobroCajaId.nuevo(), CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, List.of(aplicacion(primera.id(), "600", "EFECTIVO"),
                        aplicacion(segunda.id(), "700", "TARJETA")));

        var cobro = registrarCobro.execute(request);
        entityManager.flush();
        entityManager.clear();
        var cuenta = listarVentasCaja.execute(new ListarVentasCajaRequest(usuarioId,
                CajaTestData.PROYECTO, CajaTestData.INVENTARIO));

        assertThat(cobro.importe()).isEqualByComparingTo("1300");
        assertThat(cuenta.cobros()).hasSize(1);
        assertThat(cuenta.ventas()).extracting(v -> v.saldo().intValue()).containsExactlyInAnyOrder(1400, 1300);
        assertThat(cobroCajaRepository.findByIdAndNegocioId(cobro.id(), NegocioId.nuevo())).isEmpty();
        thenStockIs("1.0");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Dos cobros simultáneos no pueden cancelar dos veces el mismo saldo")
    void debtCollectionShouldSerializeRemainingBalance() {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        transaccion.executeWithoutResult(estado -> {
            givenCuentaConCliente();
            cobrarVenta.execute(solicitudParcial(CajaTestData.OPERACION, "800"));
        });
        var a = solicitudCobro(CajaTestData.OPERACION, "1200", "EFECTIVO");
        var b = solicitudCobro(CajaTestData.OPERACION, "1200", "TRANSFERENCIA");

        try {
            var primerCobro = CompletableFuture.supplyAsync(() -> intentarPago(a));
            var segundoCobro = CompletableFuture.supplyAsync(() -> intentarPago(b));

            assertThat(primerCobro.join()).isNotEqualTo(segundoCobro.join());
            transaccion.executeWithoutResult(estado -> {
                assertThat(ventaCajaRepository.findByIdAndNegocioId(CajaTestData.OPERACION, negocioId))
                        .hasValueSatisfying(venta -> assertThat(venta.saldo()).isEqualByComparingTo("0"));
                assertThat(cobroCajaRepository.listarTodos(negocioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO)).hasSize(1);
                thenStockIs("1.5");
            });
        } finally {
            transaccion.executeWithoutResult(estado -> {
                negocios.deleteById(negocioId.valor());
                usuarios.deleteById(usuarioId.valor());
            });
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Si una aplicación del cobro es inválida se revierte el pago de todas las ventas")
    void debtCollectionShouldRollbackAllApplications() {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        VentaCajaId segunda = VentaCajaId.nuevo();
        transaccion.executeWithoutResult(estado -> {
            givenCuentaConCliente();
            cobrarVenta.execute(solicitudParcial(CajaTestData.OPERACION, "0"));
            cobrarVenta.execute(solicitudParcial(segunda, "0"));
        });
        var request = new RegistrarCobroRequest(usuarioId, CobroCajaId.nuevo(), CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, List.of(aplicacion(CajaTestData.OPERACION, "500", "EFECTIVO"),
                        aplicacion(segunda, "2001", "EFECTIVO")));

        try {
            assertThatThrownBy(() -> registrarCobro.execute(request)).isInstanceOf(BusinessRuleException.class);

            transaccion.executeWithoutResult(estado -> {
                assertThat(ventaCajaRepository.listarTodas(negocioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                        .allSatisfy(venta -> assertThat(venta.saldo()).isEqualByComparingTo("2000"));
                assertThat(cobroCajaRepository.listarTodos(negocioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO)).isEmpty();
            });
        } finally {
            transaccion.executeWithoutResult(estado -> {
                negocios.deleteById(negocioId.valor());
                usuarios.deleteById(usuarioId.valor());
            });
        }
    }

    @Test
    @DisplayName("Los comprobantes y cierres anteriores a los cobros de deudas se siguen leyendo")
    void legacyReceiptShouldLoadWithoutNewJsonFields() {
        givenCuentaConInventario(new BigDecimal("2"));
        cobrarVenta.execute(solicitud());
        entityManager.flush();
        jdbcTemplate.update("update venta_caja set datos = datos - 'pagosIniciales' where id = ?", CajaTestData.OPERACION.valor());
        entityManager.clear();
        var abierta = consultarCaja.execute(consultaActual()).abierta();
        var cierre = cerrarCaja.execute(cierrePara(abierta.revision()));
        entityManager.flush();
        jdbcTemplate.update("update cierre_caja set datos = jsonb_set(datos, '{resumen}', (datos->'resumen') - 'cobros') where id = ?",
                cierre.id().valor());
        entityManager.clear();

        var guardado = cierreCajaRepository.findByIdAndNegocioId(cierre.id(), negocioId).orElseThrow();

        assertThat(guardado.resumen().cobros()).isEmpty();
        assertThat(guardado.resumen().ventas().getFirst().pagosIniciales()).hasSize(1);
        assertThat(guardado.resumen().efectivo()).isEqualByComparingTo("2000");
    }

    // --- arrange ---
    private CobrarVentaRequest solicitudParcial(VentaCajaId id, String importe) {
        CobrarVentaRequest base = solicitud();
        List<CobrarVentaRequest.PagoRequest> pagos = "0".equals(importe) ? List.of()
                : List.of(new CobrarVentaRequest.PagoRequest("EFECTIVO", new BigDecimal(importe)));
        return new CobrarVentaRequest(usuarioId, id, base.proyectoId(), base.inventarioId(), base.ventasId(),
                base.lineas(), "EFECTIVO", null, pagos, CajaTestData.CLIENTE);
    }

    private RegistrarCobroRequest solicitudCobro(VentaCajaId ventaId, String importe, String medio) {
        return new RegistrarCobroRequest(usuarioId, CobroCajaId.nuevo(), CajaTestData.PROYECTO, CajaTestData.INVENTARIO,
                List.of(aplicacion(ventaId, importe, medio)));
    }

    private RegistrarCobroRequest.AplicacionRequest aplicacion(VentaCajaId ventaId, String importe, String medio) {
        return new RegistrarCobroRequest.AplicacionRequest(ventaId,
                List.of(new RegistrarCobroRequest.PagoRequest(medio, new BigDecimal(importe))));
    }

    private ConsultarCajaRequest consultaActual() {
        return new ConsultarCajaRequest(usuarioId, CajaTestData.PROYECTO, CajaTestData.INVENTARIO, null, 0);
    }

    private CerrarCajaRequest cierrePara(String revision) {
        return new CerrarCajaRequest(usuarioId, CierreCajaId.nuevo(), CajaTestData.PROYECTO, CajaTestData.INVENTARIO,
                revision, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "Recuento de prueba");
    }

    private boolean intentarPago(RegistrarCobroRequest request) {
        try {
            registrarCobro.execute(request);
            return true;
        } catch (BusinessRuleException e) {
            return false;
        }
    }

    private void givenCuentaConCliente() {
        givenCuentaConInventario(new BigDecimal("2"));
        Sistema sistema = sistemaRepository.findAllByNegocioId(negocioId).getFirst();
        CajaTestData.agregarCliente(sistema);
        var tabla = sistema.proyecto().getBloques().stream()
                .filter(bloque -> bloque.getId().equals(CajaTestData.CLIENTES)).findFirst().orElseThrow();
        tabla.setNegocioId(negocioId);
        tabla.getCampos().forEach(campo -> campo.setNegocioId(negocioId));
        sistema.proyecto().getBloques().stream().filter(bloque -> bloque.getId().equals(CajaTestData.VENTAS))
                .flatMap(bloque -> bloque.getCampos().stream()).forEach(campo -> campo.setNegocioId(negocioId));
        sistema.registrosPorBloque().get(CajaTestData.CLIENTES).forEach(fila -> fila.setNegocioId(negocioId));
        sistemaRepository.guardar(sistema);
        entityManager.flush();
        entityManager.clear();
    }

    private void givenCuentaConInventario(BigDecimal stock) {
        usuarioId = UsuarioId.de(usuarios.saveAndFlush(UsuarioEntity.builder().nombre("Caja")
                .email(UsuarioId.nuevo() + "@example.com").estado(EstadoUsuario.ACTIVO)
                .creadoEn(CajaTestData.AHORA).actualizadoEn(CajaTestData.AHORA).build()).getId());
        negocioId = NegocioId.de(negocios.saveAndFlush(NegocioEntity.builder()
                .duenioId(usuarioId.valor()).nombre("Mi negocio")
                .creadoEn(CajaTestData.AHORA).actualizadoEn(CajaTestData.AHORA).build()).getId());
        suscripciones.asignarGratuito(negocioId, OffsetDateTime.now().minusDays(1));
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
        entityManager.flush();
        entityManager.clear();
    }

    // --- act ---
    private boolean intentarCobro(CobrarVentaRequest request) {
        try {
            cobrarVenta.execute(request);
            return true;
        } catch (DuplicateResourceException e) {
            return false;
        }
    }

    // --- assert ---
    private void thenStockIs(String esperado) {
        Sistema recargado = sistemaRepository.findAllByNegocioId(negocioId).getFirst();
        var campo = recargado.proyecto().getBloques().stream().filter(b -> b.getId().equals(CajaTestData.INVENTARIO))
                .findFirst().orElseThrow().getCampos().stream().filter(c -> c.getClave().equals("quantity"))
                .findFirst().orElseThrow();
        Object stock = recargado.registrosPorBloque().get(CajaTestData.INVENTARIO).getFirst()
                .getValores().get(campo.getId().toString());
        assertThat(new BigDecimal(stock.toString())).isEqualByComparingTo(esperado);
    }

    private CobrarVentaRequest solicitud() {
        CobrarVentaRequest base = CajaTestData.request("0.5");
        return new CobrarVentaRequest(usuarioId, base.id(), base.proyectoId(), base.inventarioId(),
                base.ventasId(), base.lineas(), base.medio(), base.recibido());
    }
}
