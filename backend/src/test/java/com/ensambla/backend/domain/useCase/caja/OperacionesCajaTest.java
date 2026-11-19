package com.ensambla.backend.domain.useCase.caja;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.caja.ResumenCaja;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.caja.CierreCajaRepository;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCaja;
import com.ensambla.backend.domain.useCase.caja.cerrarCaja.CerrarCajaRequest;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCaja;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCajaRequest;
import com.ensambla.backend.domain.useCase.caja.consultarCaja.ConsultarCajaResponse;
import com.ensambla.backend.domain.useCase.caja.obtenerCierre.ObtenerCierre;
import com.ensambla.backend.domain.useCase.caja.obtenerCierre.ObtenerCierreRequest;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CierreCajaTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperacionesCajaTest {

    private final NegocioRepository negocios = mock(NegocioRepository.class);
    private final SistemaRepository sistemas = mock(SistemaRepository.class);
    private final VentaCajaRepository ventas = mock(VentaCajaRepository.class);
    private final CierreCajaRepository cierres = mock(CierreCajaRepository.class);
    private final CobroCajaRepository cobros = mock(CobroCajaRepository.class);
    private final UsoPlanRepository uso = mock(UsoPlanRepository.class);
    private final TransaccionPort transaccion = new TransaccionDirecta();
    private final Clock clock = Clock.fixed(CajaTestData.AHORA.toInstant(), ZoneOffset.UTC);
    private final CerrarCaja cerrar = new CerrarCaja(negocios, sistemas, ventas, cierres, cobros, uso, transaccion, clock);
    private final ConsultarCaja consultar = new ConsultarCaja(negocios, sistemas, ventas, cierres, cobros, uso, transaccion, clock);
    private final ObtenerCierre obtener = new ObtenerCierre(negocios, cierres);

    private CierreCajaItem cierre;
    private ConsultarCajaResponse consulta;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        when(negocios.obtenerIdDelUsuario(CajaTestData.USUARIO)).thenReturn(CajaTestData.NEGOCIO);
    }

    @Test
    @DisplayName("Cierra y vincula exactamente las ventas revisadas bajo el mismo bloqueo que los cobros")
    void cierraYVinculaExactamenteLasVentasRevisadasBajoElMismoBloqueoQueLosCobros() {
        dadoUnaVentaAbiertaEnLaCaja();

        cuandoSeCierraLaCaja();

        entoncesElCierreTieneLaCopiaCompleta();
        entoncesSeCerroBajoElBloqueoDelNegocioVinculandoLaVentaRevisada();
    }

    @Test
    @DisplayName("Reintentar el mismo cierre devuelve la copia original sin consumir ventas posteriores")
    void reintentarElMismoCierreDevuelveLaCopiaOriginalSinConsumirVentasPosteriores() {
        dadoUnCierreYaGuardado();

        cuandoSeCierraLaCaja();

        entoncesElCierreTieneLaCopiaCompleta();
        entoncesNoSeConsumieronVentasNiSeGuardoOtroCierre();
    }

    @Test
    @DisplayName("El id de cierre no puede reutilizarse con datos distintos")
    void elIdDeCierreNoPuedeReutilizarseConDatosDistintos() {
        dadoUnCierreYaGuardado();

        cuandoSeIntentaCerrarConElMismoIdYDatosDistintos();

        entoncesSeRechazaPorDuplicado();
        entoncesNoSeGuardaOtroCierre();
    }

    @Test
    @DisplayName("El día argentino se consulta aparte del período abierto")
    void elDiaArgentinoSeConsultaAparteDelPeriodoAbierto() {
        dadoUnaVentaAbiertaYUnCierreAnterior();

        cuandoSeConsultaLaCajaDeHoy();

        entoncesElDiaSeSeparaDelPeriodoAbierto();
        entoncesElCierreAnteriorSeListaCompleto();
        entoncesSeConsultaronLasVentasDelDiaArgentino();
    }

    @Test
    @DisplayName("Permite consultar otra fecha y paginar los cierres")
    void permiteConsultarOtraFechaYPaginarLosCierres() {
        dadoUnaPaginaCompletaDeCierres();

        cuandoSeConsultaLaCajaDelPrimeroDeOctubreEnLaPaginaUno();

        entoncesSeMuestraEsaFechaYHayMasPaginas();
    }

    @Test
    @DisplayName("El detalle de un cierre sólo se obtiene dentro del negocio autenticado")
    void elDetalleDeUnCierreSoloSeObtieneDentroDelNegocioAutenticado() {
        dadoUnCierreYaGuardado();

        cuandoSeObtieneElCierreGuardado();

        entoncesElCierreTieneLaCopiaCompleta();
    }

    @Test
    @DisplayName("Un cierre ajeno responde como inexistente")
    void unCierreAjenoRespondeComoInexistente() {
        cuandoSeObtieneUnCierreAjeno();

        entoncesElCierreNoExiste();
    }

    // --- dado ---

    private void dadoUnaVentaAbiertaEnLaCaja() {
        when(ventas.listarAbiertas(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                .thenReturn(List.of(CierreCajaTestData.venta()));
    }

    private void dadoUnCierreYaGuardado() {
        when(cierres.findByIdAndNegocioId(CierreCajaTestData.CIERRE, CajaTestData.NEGOCIO))
                .thenReturn(Optional.of(CierreCajaTestData.cierre()));
    }

    private void dadoUnaVentaAbiertaYUnCierreAnterior() {
        dadoUnaVentaAbiertaEnLaCaja();
        when(cierres.listar(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO, 0))
                .thenReturn(List.of(CierreCajaTestData.cierre()));
    }

    private void dadoUnaPaginaCompletaDeCierres() {
        when(cierres.listar(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO, 1))
                .thenReturn(Collections.nCopies(20, CierreCajaTestData.cierre()));
    }

    // --- cuando ---

    private void cuandoSeCierraLaCaja() {
        cierre = cerrar.execute(CierreCajaTestData.request());
    }

    private void cuandoSeIntentaCerrarConElMismoIdYDatosDistintos() {
        CerrarCajaRequest base = CierreCajaTestData.request();
        CerrarCajaRequest cambiado = new CerrarCajaRequest(base.usuarioId(), base.id(), base.proyectoId(),
                base.inventarioId(), base.revision(), base.fondoInicial(), base.entradas(), base.retiros(), null, "");
        rechazo = catchThrowable(() -> cerrar.execute(cambiado));
    }

    private void cuandoSeConsultaLaCajaDeHoy() {
        consulta = consultar.execute(new ConsultarCajaRequest(CajaTestData.USUARIO,
                CajaTestData.PROYECTO, CajaTestData.INVENTARIO, null, 0));
    }

    private void cuandoSeConsultaLaCajaDelPrimeroDeOctubreEnLaPaginaUno() {
        consulta = consultar.execute(new ConsultarCajaRequest(CajaTestData.USUARIO,
                CajaTestData.PROYECTO, CajaTestData.INVENTARIO, LocalDate.of(2026, 10, 1), 1));
    }

    private void cuandoSeObtieneElCierreGuardado() {
        cierre = obtener.execute(new ObtenerCierreRequest(CajaTestData.USUARIO, CierreCajaTestData.CIERRE));
    }

    private void cuandoSeObtieneUnCierreAjeno() {
        rechazo = catchThrowable(
                () -> obtener.execute(new ObtenerCierreRequest(CajaTestData.USUARIO, CierreCajaId.nuevo())));
    }

    // --- entonces ---

    private void entoncesElCierreTieneLaCopiaCompleta() {
        assertThat(cierre.id()).isEqualTo(CierreCajaTestData.CIERRE);
        assertThat(cierre.resumen().ventas()).hasSize(1);
        assertThat(cierre.resumen().total()).isEqualByComparingTo("2000");
        assertThat(cierre.resumen().efectivo()).isEqualByComparingTo("1000");
        assertThat(cierre.resumen().transferencia()).isEqualByComparingTo("700");
        assertThat(cierre.resumen().tarjeta()).isEqualByComparingTo("300");
        assertThat(cierre.resumen().desde()).isEqualTo(CajaTestData.AHORA);
        assertThat(cierre.resumen().revision())
                .isEqualTo(ResumenCaja.de(List.of(CierreCajaTestData.venta())).revision());
        assertThat(cierre.fondoInicial()).isEqualByComparingTo("500");
        assertThat(cierre.entradas()).isEqualByComparingTo("100");
        assertThat(cierre.retiros()).isEqualByComparingTo("200");
        assertThat(cierre.esperado()).isEqualByComparingTo("1400");
        assertThat(cierre.contado()).isEqualByComparingTo("1400");
        assertThat(cierre.diferencia()).isEqualByComparingTo("0");
        assertThat(cierre.nota()).isEqualTo("Revisado");
        assertThat(cierre.cerradoEn()).isEqualTo(CajaTestData.AHORA);
    }

    private void entoncesSeCerroBajoElBloqueoDelNegocioVinculandoLaVentaRevisada() {
        verify(uso).bloquearNegocio(CajaTestData.NEGOCIO);
        verify(sistemas).obtenerInventarioCaja(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO);
        verify(cierres).save(CierreCajaTestData.cierre());
        verify(ventas).marcarCerradas(CajaTestData.NEGOCIO, CierreCajaTestData.CIERRE, List.of(CajaTestData.OPERACION));
    }

    private void entoncesNoSeConsumieronVentasNiSeGuardoOtroCierre() {
        verify(ventas, never()).listarAbiertas(any(), any(), any());
        entoncesNoSeGuardaOtroCierre();
    }

    private void entoncesNoSeGuardaOtroCierre() {
        verify(cierres, never()).save(any());
    }

    private void entoncesSeRechazaPorDuplicado() {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class);
    }

    private void entoncesElDiaSeSeparaDelPeriodoAbierto() {
        assertThat(consulta.fecha()).isEqualTo(ZonaNegocio.dia(CajaTestData.AHORA));
        assertThat(consulta.dia().ventas()).isEmpty();
        assertThat(consulta.abierta().total()).isEqualByComparingTo("2000");
        assertThat(consulta.pagina()).isZero();
        assertThat(consulta.hayMas()).isFalse();
    }

    private void entoncesElCierreAnteriorSeListaCompleto() {
        ConsultarCajaResponse.CierreListaItem primero = consulta.cierres().getFirst();
        assertThat(primero.id()).isEqualTo(CierreCajaTestData.CIERRE);
        assertThat(primero.desde()).isEqualTo(CajaTestData.AHORA);
        assertThat(primero.cerradoEn()).isEqualTo(CajaTestData.AHORA);
        assertThat(primero.total()).isEqualByComparingTo("2000");
        assertThat(primero.cantidadVentas()).isEqualTo(1);
        assertThat(primero.diferencia()).isEqualByComparingTo("0");
    }

    private void entoncesSeConsultaronLasVentasDelDiaArgentino() {
        LocalDate fecha = ZonaNegocio.dia(CajaTestData.AHORA);
        verify(ventas).listarEntre(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO,
                ZonaNegocio.inicioDelDia(fecha), ZonaNegocio.inicioDelDia(fecha.plusDays(1)));
    }

    private void entoncesSeMuestraEsaFechaYHayMasPaginas() {
        assertThat(consulta.fecha()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(consulta.pagina()).isEqualTo(1);
        assertThat(consulta.hayMas()).isTrue();
    }

    private void entoncesElCierreNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }
}
