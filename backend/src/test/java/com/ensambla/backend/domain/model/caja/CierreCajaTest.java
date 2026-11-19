package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CierreCajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CierreCajaTest {

    private CierreCaja cierre;
    private ResumenCaja resumen;
    private Throwable rechazo;

    @Test
    @DisplayName("Guarda el resumen histórico y el arqueo sin sumar transferencias ni tarjetas al cajón")
    void guardaElResumenHistoricoYElArqueoSinSumarTransferenciasNiTarjetasAlCajon() {
        cuandoSeArmaElCierreDeEjemplo();

        entoncesConservaLaIdentidadDelCierre();
        entoncesConservaElResumenHistorico();
        entoncesConservaElArqueoDelEfectivo();
    }

    @Test
    @DisplayName("Exige contar el efectivo antes de guardar un cierre nuevo")
    void exigeContarElEfectivoAntesDeGuardarUnCierreNuevo() {
        cuandoSeIntentaCerrarContando(null);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Exige explicar un faltante")
    void exigeExplicarUnFaltante() {
        cuandoSeIntentaCerrarContando("0");

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Exige explicar un sobrante")
    void exigeExplicarUnSobrante() {
        cuandoSeIntentaCerrarContando("1001");

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Acepta un recuento cero con motivo y conserva la diferencia")
    void aceptaUnRecuentoCeroConMotivoYConservaLaDiferencia() {
        cuandoSeCuentaCeroConMotivo();

        entoncesConservaElFaltanteExplicado();
    }

    @Test
    @DisplayName("No exige nota cuando el efectivo coincide")
    void noExigeNotaCuandoElEfectivoCoincide() {
        cuandoSeCierraConElEfectivoCoincidente();

        entoncesCoincideSinNota();
    }

    @Test
    @DisplayName("Un período vacío no genera un cierre")
    void unPeriodoVacioNoGeneraUnCierre() {
        cuandoSeIntentaCerrarUnPeriodoVacio();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Una venta nueva obliga a revisar el resumen antes de cerrar")
    void unaVentaNuevaObligaARevisarElResumenAntesDeCerrar() {
        cuandoSeIntentaCerrarConUnaRevisionVieja();

        entoncesSeRechazaPorRevisionDesactualizada();
    }

    @ParameterizedTest
    @CsvSource({"-1,0,0,0", "0.001,0,0,0", "0,-1,0,0", "0,0,-1,0", "0,0,0,-1",
            "0,0,1001,0", "0,0,0,0.001"})
    @DisplayName("Rechaza movimientos o recuentos inválidos")
    void rechazaMovimientosORecuentosInvalidos(String fondo, String entradas, String retiros, String contado) {
        cuandoSeIntentaCerrarConMovimientos(fondo, entradas, retiros, contado);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("El resumen vacío conserva importes cero y no inventa fecha de apertura")
    void elResumenVacioConservaImportesCeroYNoInventaFechaDeApertura() {
        cuandoSeResumeUnPeriodoVacio();

        entoncesElResumenQuedaEnCeroSinFechaDeApertura();
    }

    // --- cuando ---

    private void cuandoSeArmaElCierreDeEjemplo() {
        cierre = CierreCajaTestData.cierre();
    }

    private void cuandoSeIntentaCerrarContando(String contado) {
        rechazo = catchThrowable(() -> cerrar(resumenConUnaVenta(), resumenConUnaVenta().revision(),
                "0", "0", "0", contado));
    }

    private void cuandoSeCuentaCeroConMotivo() {
        cierre = CierreCaja.nuevo(CierreCajaTestData.CIERRE, CajaTestData.NEGOCIO, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, "huella", resumenConUnaVenta(), resumenConUnaVenta().revision(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "  Retiro sin registrar  ", CajaTestData.AHORA);
    }

    private void cuandoSeCierraConElEfectivoCoincidente() {
        cierre = cerrar(resumenConUnaVenta(), resumenConUnaVenta().revision(), "0", "0", "0", "1000");
    }

    private void cuandoSeIntentaCerrarUnPeriodoVacio() {
        rechazo = catchThrowable(() -> cerrar(ResumenCaja.de(List.of()), "", "0", "0", "0", null));
    }

    private void cuandoSeIntentaCerrarConUnaRevisionVieja() {
        rechazo = catchThrowable(() -> cerrar(resumenConUnaVenta(), "anterior", "0", "0", "0", null));
    }

    private void cuandoSeIntentaCerrarConMovimientos(String fondo, String entradas, String retiros,
            String contado) {
        rechazo = catchThrowable(() -> cerrar(resumenConUnaVenta(), resumenConUnaVenta().revision(),
                fondo, entradas, retiros, contado));
    }

    private void cuandoSeResumeUnPeriodoVacio() {
        resumen = ResumenCaja.de(List.of());
    }

    // --- entonces ---

    private void entoncesConservaLaIdentidadDelCierre() {
        assertThat(cierre.id()).isEqualTo(CierreCajaTestData.CIERRE);
        assertThat(cierre.negocioId()).isEqualTo(CajaTestData.NEGOCIO);
        assertThat(cierre.proyectoId()).isEqualTo(CajaTestData.PROYECTO);
        assertThat(cierre.inventarioId()).isEqualTo(CajaTestData.INVENTARIO);
        assertThat(cierre.huella()).isEqualTo(CierreCajaTestData.request().huella());
        assertThat(cierre.cerradoEn()).isEqualTo(CajaTestData.AHORA);
    }

    private void entoncesConservaElResumenHistorico() {
        assertThat(cierre.resumen().total()).isEqualByComparingTo("2000");
        assertThat(cierre.resumen().efectivo()).isEqualByComparingTo("1000");
        assertThat(cierre.resumen().transferencia()).isEqualByComparingTo("700");
        assertThat(cierre.resumen().tarjeta()).isEqualByComparingTo("300");
        assertThat(cierre.resumen().desde()).isEqualTo(CajaTestData.AHORA);
    }

    private void entoncesConservaElArqueoDelEfectivo() {
        assertThat(cierre.fondoInicial()).isEqualByComparingTo("500");
        assertThat(cierre.entradas()).isEqualByComparingTo("100");
        assertThat(cierre.retiros()).isEqualByComparingTo("200");
        assertThat(cierre.esperado()).isEqualByComparingTo("1400");
        assertThat(cierre.contado()).isEqualByComparingTo("1400");
        assertThat(cierre.diferencia()).isEqualByComparingTo("0");
        assertThat(cierre.nota()).isEqualTo("Revisado");
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesSeRechazaPorRevisionDesactualizada() {
        assertThat(rechazo).isInstanceOf(DuplicateResourceException.class);
    }

    private void entoncesConservaElFaltanteExplicado() {
        assertThat(cierre.contado()).isEqualByComparingTo("0");
        assertThat(cierre.diferencia()).isEqualByComparingTo("-1000");
        assertThat(cierre.nota()).isEqualTo("Retiro sin registrar");
    }

    private void entoncesCoincideSinNota() {
        assertThat(cierre.diferencia()).isEqualByComparingTo("0");
        assertThat(cierre.nota()).isEmpty();
    }

    private void entoncesElResumenQuedaEnCeroSinFechaDeApertura() {
        assertThat(resumen.total()).isEqualByComparingTo("0");
        assertThat(resumen.desde()).isNull();
        assertThat(resumen.ventas()).isEmpty();
    }

    // --- datos ---

    private ResumenCaja resumenConUnaVenta() {
        return ResumenCaja.de(List.of(CierreCajaTestData.venta()));
    }

    private CierreCaja cerrar(ResumenCaja resumenACerrar, String revision, String fondo, String entradas,
            String retiros, String contado) {
        return CierreCaja.nuevo(CierreCajaTestData.CIERRE, CajaTestData.NEGOCIO, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, "huella", resumenACerrar, revision, new BigDecimal(fondo),
                new BigDecimal(entradas), new BigDecimal(retiros),
                contado == null ? null : new BigDecimal(contado), "", CajaTestData.AHORA);
    }
}
