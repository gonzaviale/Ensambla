package com.ensambla.backend.domain.useCase.caja.registrarCobro;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.caja.CobroCajaItem;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarCobroTest {

    @Mock
    private NegocioRepository negocios;
    @Mock
    private UsoPlanRepository uso;
    @Mock
    private VentaCajaRepository ventas;
    @Mock
    private CobroCajaRepository cobros;
    private final Clock clock = Clock.fixed(CajaTestData.AHORA.plusDays(1).toInstant(), ZoneOffset.UTC);
    private RegistrarCobro registrar;

    private CobroCajaItem respuesta;
    private Throwable rechazo;
    private VentaCaja segundaVenta;

    @BeforeEach
    void setUp() {
        registrar = new RegistrarCobro(negocios, uso, ventas, cobros, new TransaccionDirecta(), clock);
    }

    @Test
    @DisplayName("Un cobro reduce el saldo y se guarda con fecha propia bajo el bloqueo del negocio")
    void unCobroReduceElSaldoYSeGuardaConFechaPropiaBajoElBloqueoDelNegocio() {
        dadoUnaVentaConDeuda(CobroCajaTestData.venta());
        dadoQueGuardarUnCobroDevuelveElCobro();

        cuandoSeRegistraElCobro();

        entoncesLaVentaQuedaConSaldoDe("600");
        entoncesSeDevuelveElCobroGuardado();
        entoncesSeBloqueoElNegocio();
    }

    @Test
    @DisplayName("Reintentar el mismo cobro devuelve el movimiento guardado sin reducir otra vez la deuda")
    void reintentarElMismoCobroDevuelveElMovimientoGuardadoSinReducirOtraVezLaDeuda() {
        dadoUnCobroYaGuardado();

        cuandoSeRegistraElCobro();

        entoncesSeDevuelveElCobroOriginal();
        entoncesNoSeReduceOtraVezLaDeuda();
    }

    @Test
    @DisplayName("Reutilizar el id de cobro con otro contenido responde conflicto")
    void reutilizarElIdDeCobroConOtroContenidoRespondeConflicto() {
        dadoUnCobroGuardadoConOtraHuella();

        cuandoSeIntentaRegistrarElCobro();

        entoncesSeRechazaCon(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Una venta ajena responde como inexistente")
    void unaVentaAjenaRespondeComoInexistente() {
        dadoQueNoHayVentasConEsosIds();

        cuandoSeIntentaRegistrarElCobro();

        entoncesSeRechazaCon(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("No aplica cobros a ventas de otro proyecto")
    void noAplicaCobrosAVentasDeOtroProyecto() {
        dadoUnaVentaConDeuda(ventaCon(ProyectoId.nuevo(), CajaTestData.INVENTARIO, CajaTestData.CLIENTE));

        cuandoSeIntentaRegistrarElCobro();

        entoncesSeRechazaCon(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("No aplica cobros a ventas de otro inventario")
    void noAplicaCobrosAVentasDeOtroInventario() {
        dadoUnaVentaConDeuda(ventaCon(CajaTestData.PROYECTO, BloqueId.nuevo(), CajaTestData.CLIENTE));

        cuandoSeIntentaRegistrarElCobro();

        entoncesSeRechazaCon(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("No aplica una entrega de deuda a una venta anónima")
    void noAplicaUnaEntregaDeDeudaAUnaVentaAnonima() {
        dadoUnaVentaConDeuda(ventaCon(CajaTestData.PROYECTO, CajaTestData.INVENTARIO, null));

        cuandoSeIntentaRegistrarElCobro();

        entoncesSeRechazaCon(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una solicitud sin deudas seleccionadas")
    void rechazaUnaSolicitudSinDeudasSeleccionadas() {
        dadoUnUsuarioDelNegocio();

        cuandoSeIntentaRegistrarUnCobroSinDeudas();

        entoncesSeRechazaCon(BusinessRuleException.class);
    }

    @Test
    @DisplayName("No permite mezclar deudas de clientes distintos en un mismo cobro")
    void noPermiteMezclarDeudasDeClientesDistintosEnUnMismoCobro() {
        dadoDosDeudasDeClientesDistintos();

        cuandoSeIntentaRegistrarUnCobroDeLasDosDeudas();

        entoncesSeRechazaCon(BusinessRuleException.class);
        entoncesNoSeGuardaNingunCobro();
    }

    @Test
    @DisplayName("Un mismo pago puede reducir dos deudas del mismo cliente")
    void unMismoPagoPuedeReducirDosDeudasDelMismoCliente() {
        dadoDosDeudasDelMismoCliente();

        cuandoSeRegistraUnCobroDeLasDosDeudas();

        entoncesElCobroSumaDoscientosEnDosAplicaciones();
    }

    // --- dado ---

    private void dadoUnUsuarioDelNegocio() {
        when(negocios.obtenerIdDelUsuario(CajaTestData.USUARIO)).thenReturn(CajaTestData.NEGOCIO);
    }

    private void dadoUnaVentaConDeuda(VentaCaja venta) {
        dadoUnUsuarioDelNegocio();
        when(ventas.listarPorIds(CajaTestData.NEGOCIO, List.of(CajaTestData.OPERACION))).thenReturn(List.of(venta));
    }

    private void dadoQueGuardarUnCobroDevuelveElCobro() {
        when(cobros.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void dadoUnCobroYaGuardado() {
        dadoUnUsuarioDelNegocio();
        when(cobros.findByIdAndNegocioId(CobroCajaTestData.COBRO, CajaTestData.NEGOCIO))
                .thenReturn(Optional.of(CobroCajaTestData.cobro()));
    }

    private void dadoUnCobroGuardadoConOtraHuella() {
        dadoUnUsuarioDelNegocio();
        CobroCaja base = CobroCajaTestData.cobro();
        CobroCaja conOtraHuella = new CobroCaja(base.id(), base.negocioId(), base.proyectoId(), base.inventarioId(),
                base.clienteId(), base.clienteNombre(), "otra huella", base.aplicaciones(), base.creadoEn());
        when(cobros.findByIdAndNegocioId(base.id(), base.negocioId())).thenReturn(Optional.of(conOtraHuella));
    }

    private void dadoQueNoHayVentasConEsosIds() {
        dadoUnUsuarioDelNegocio();
        when(ventas.listarPorIds(any(), any())).thenReturn(List.of());
    }

    private void dadoDosDeudasDeClientesDistintos() {
        dadoUnUsuarioDelNegocio();
        segundaVenta = unaSegundaVenta(RegistroId.nuevo());
        when(ventas.listarPorIds(any(), any())).thenReturn(List.of(CobroCajaTestData.venta(), segundaVenta));
    }

    private void dadoDosDeudasDelMismoCliente() {
        dadoUnUsuarioDelNegocio();
        segundaVenta = unaSegundaVenta(CajaTestData.CLIENTE);
        when(ventas.listarPorIds(any(), any())).thenReturn(List.of(CobroCajaTestData.venta(), segundaVenta));
        dadoQueGuardarUnCobroDevuelveElCobro();
    }

    // --- cuando ---

    private void cuandoSeRegistraElCobro() {
        respuesta = registrar.execute(CobroCajaTestData.request());
    }

    private void cuandoSeIntentaRegistrarElCobro() {
        rechazo = catchThrowable(() -> registrar.execute(CobroCajaTestData.request()));
    }

    private void cuandoSeIntentaRegistrarUnCobroSinDeudas() {
        RegistrarCobroRequest sinDeudas = new RegistrarCobroRequest(CajaTestData.USUARIO, CobroCajaTestData.COBRO,
                CajaTestData.PROYECTO, CajaTestData.INVENTARIO, List.of());
        rechazo = catchThrowable(() -> registrar.execute(sinDeudas));
    }

    private void cuandoSeIntentaRegistrarUnCobroDeLasDosDeudas() {
        rechazo = catchThrowable(() -> registrar.execute(unCobroDeLasDosDeudas()));
    }

    private void cuandoSeRegistraUnCobroDeLasDosDeudas() {
        respuesta = registrar.execute(unCobroDeLasDosDeudas());
    }

    // --- entonces ---

    private void entoncesLaVentaQuedaConSaldoDe(String saldo) {
        ArgumentCaptor<VentaCaja> actualizada = ArgumentCaptor.forClass(VentaCaja.class);
        verify(ventas).actualizarCobros(actualizada.capture(), eq(CajaTestData.AHORA.plusDays(1)));
        assertThat(actualizada.getValue().saldo()).isEqualByComparingTo(saldo);
    }

    private void entoncesSeDevuelveElCobroGuardado() {
        assertThat(respuesta).isEqualTo(CobroCajaItem.de(CobroCajaTestData.cobro()));
    }

    private void entoncesSeBloqueoElNegocio() {
        verify(uso).bloquearNegocio(CajaTestData.NEGOCIO);
    }

    private void entoncesSeDevuelveElCobroOriginal() {
        assertThat(respuesta.id()).isEqualTo(CobroCajaTestData.COBRO);
    }

    private void entoncesNoSeReduceOtraVezLaDeuda() {
        verify(ventas, never()).actualizarCobros(any(), any());
    }

    private void entoncesSeRechazaCon(Class<? extends Throwable> tipo) {
        assertThat(rechazo).isInstanceOf(tipo);
    }

    private void entoncesNoSeGuardaNingunCobro() {
        verify(cobros, never()).save(any());
    }

    private void entoncesElCobroSumaDoscientosEnDosAplicaciones() {
        assertThat(respuesta.importe()).isEqualByComparingTo("200");
        assertThat(respuesta.aplicaciones()).hasSize(2);
    }

    // --- datos ---

    private RegistrarCobroRequest unCobroDeLasDosDeudas() {
        return solicitudDoble(segundaVenta.id());
    }

    private VentaCaja unaSegundaVenta(RegistroId clienteId) {
        VentaCaja base = ventaCon(CajaTestData.PROYECTO, CajaTestData.INVENTARIO, clienteId);
        return new VentaCaja(VentaCajaId.nuevo(), base.negocioId(), base.proyectoId(), base.huella(), base.lineas(),
                base.medio(), base.total(), base.recibido(), base.vuelto(), base.creadoEn(), base.pagos(),
                base.inventarioId(), clienteId, base.clienteNombre());
    }

    private RegistrarCobroRequest solicitudDoble(VentaCajaId segundaId) {
        var pago = List.of(new RegistrarCobroRequest.PagoRequest("EFECTIVO", new BigDecimal("100")));
        return new RegistrarCobroRequest(CajaTestData.USUARIO, CobroCajaTestData.COBRO, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, List.of(new RegistrarCobroRequest.AplicacionRequest(CajaTestData.OPERACION, pago),
                        new RegistrarCobroRequest.AplicacionRequest(segundaId, pago)));
    }

    private VentaCaja ventaCon(ProyectoId proyectoId, BloqueId inventarioId, RegistroId clienteId) {
        VentaCaja base = CobroCajaTestData.venta();
        return new VentaCaja(base.id(), base.negocioId(), proyectoId, base.huella(), base.lineas(), base.medio(),
                base.total(), base.recibido(), base.vuelto(), base.creadoEn(), base.pagos(), inventarioId, clienteId,
                base.clienteNombre());
    }
}
