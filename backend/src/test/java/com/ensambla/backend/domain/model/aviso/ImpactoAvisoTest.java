package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactoAvisoTest {

    private static final RegistroId PRODUCTO = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final LocalDate APLICADO = LocalDate.of(2026, 10, 1);
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final int VENTANA = 14;

    private List<VentaProducto> ventas = List.of();
    private Map<String, Object> datosDelAviso;
    private Optional<ImpactoAviso> impacto;

    @Test
    @DisplayName("Compara las unidades y el ingreso por día de los días anteriores con los posteriores")
    void comparaLasUnidadesYElIngresoPorDiaDeAntesYDespues() {
        dadoQueSeVendio(
                unaVenta(99, "100", "2026-09-01"),
                unaVenta(2, "100", "2026-09-20"),
                unaVentaSinPrecio(3, "2026-09-25"),
                unaVenta(7, "100", "2026-10-02"),
                unaVenta(5, "100", "2026-10-15"));

        cuandoSeMideEl(HOY);

        entoncesLaVentanaEsDe(VENTANA);
        entoncesLasUnidadesPorDiaPasaronDe("0.36", "0.50");
        entoncesElIngresoPorDiaPasoDe("14.29", "50.00");
        entoncesElIngresoSemanalCambio("249.97");
    }

    @Test
    @DisplayName("La venta del día del aviso cuenta como posterior")
    void laVentaDelDiaDelAvisoCuentaComoPosterior() {
        dadoQueSeVendio(unaVenta(14, "10", "2026-10-01"));

        cuandoSeMideEl(HOY);

        entoncesLasUnidadesPorDiaPasaronDe("0", "1");
    }

    @Test
    @DisplayName("Sin ventas el impacto es cero y no cambia el ingreso")
    void sinVentasElImpactoEsCeroYNoCambiaElIngreso() {
        dadoQueNoSeVendioNada();

        cuandoSeMideEl(HOY);

        entoncesElIngresoSemanalCambio("0");
    }

    @Test
    @DisplayName("Si bajaron las ventas el cambio de ingreso es negativo")
    void siBajaronLasVentasElCambioDeIngresoEsNegativo() {
        dadoQueSeVendio(unaVenta(14, "100", "2026-09-20"));

        cuandoSeMideEl(HOY);

        entoncesElIngresoSemanalCambio("-700.00");
    }

    @Test
    @DisplayName("No mide mientras la ventana posterior no terminó: el día de hoy no cuenta")
    void noMideMientrasLaVentanaPosteriorNoTermino() {
        dadoQueNoSeVendioNada();

        cuandoSeMideEl(LocalDate.of(2026, 10, 14));

        entoncesNoHayImpacto();
    }

    @Test
    @DisplayName("Mide apenas termina la ventana posterior")
    void mideApenasTerminaLaVentanaPosterior() {
        dadoQueNoSeVendioNada();

        cuandoSeMideEl(LocalDate.of(2026, 10, 15));

        entoncesHayImpacto();
    }

    @Test
    @DisplayName("Lo que se guardó en los datos del aviso se vuelve a leer igual")
    void loQueSeGuardoEnLosDatosDelAvisoSeVuelveALeerIgual() {
        ImpactoAviso medido = dadoUnImpactoGuardadoEnLosDatosDelAviso();

        cuandoSeLeenLosDatosDelAviso();

        entoncesSeLeyo(medido);
    }

    @Test
    @DisplayName("Sin datos no hay impacto que leer")
    void sinDatosNoHayImpactoQueLeer() {
        dadoUnAvisoConDatos(null);

        cuandoSeLeenLosDatosDelAviso();

        entoncesNoHayImpacto();
    }

    @Test
    @DisplayName("Sin impacto guardado no hay nada que leer")
    void sinImpactoGuardadoNoHayNadaQueLeer() {
        dadoUnAvisoConDatos(Map.of("precio", 10));

        cuandoSeLeenLosDatosDelAviso();

        entoncesNoHayImpacto();
    }

    @Test
    @DisplayName("Un impacto guardado con otra forma no se lee")
    void unImpactoGuardadoConOtraFormaNoSeLee() {
        dadoUnAvisoConDatos(Map.of(ImpactoAviso.CLAVE, "no es un mapa"));

        cuandoSeLeenLosDatosDelAviso();

        entoncesNoHayImpacto();
    }

    private static VentaProducto unaVenta(int cantidad, String precio, String fecha) {
        return new VentaProducto(PRODUCTO, BigDecimal.valueOf(cantidad), new BigDecimal(precio), LocalDate.parse(fecha), null);
    }

    private static VentaProducto unaVentaSinPrecio(int cantidad, String fecha) {
        return new VentaProducto(PRODUCTO, BigDecimal.valueOf(cantidad), null, LocalDate.parse(fecha), null);
    }

    private void dadoQueSeVendio(VentaProducto... vendidas) {
        ventas = List.of(vendidas);
    }

    private void dadoQueNoSeVendioNada() {
        ventas = List.of();
    }

    private ImpactoAviso dadoUnImpactoGuardadoEnLosDatosDelAviso() {
        ImpactoAviso medido = ImpactoAviso.medir(List.of(unaVenta(7, "100", "2026-10-02")), APLICADO, HOY, VENTANA)
                .orElseThrow();
        Map<String, Object> datos = new HashMap<>();
        datos.put(ImpactoAviso.CLAVE, medido.aMapa());
        dadoUnAvisoConDatos(datos);
        return medido;
    }

    private void dadoUnAvisoConDatos(Map<String, Object> datos) {
        datosDelAviso = datos;
    }

    private void cuandoSeMideEl(LocalDate hoy) {
        impacto = ImpactoAviso.medir(ventas, APLICADO, hoy, VENTANA);
    }

    private void cuandoSeLeenLosDatosDelAviso() {
        impacto = ImpactoAviso.de(datosDelAviso);
    }

    private void entoncesHayImpacto() {
        assertThat(impacto).isPresent();
    }

    private void entoncesNoHayImpacto() {
        assertThat(impacto).isEmpty();
    }

    private void entoncesSeLeyo(ImpactoAviso medido) {
        assertThat(impacto).contains(medido);
    }

    private void entoncesLaVentanaEsDe(int dias) {
        assertThat(impacto.orElseThrow().ventanaDias()).isEqualTo(dias);
    }

    private void entoncesLasUnidadesPorDiaPasaronDe(String antes, String despues) {
        assertThat(impacto.orElseThrow().unidadesPorDiaAntes()).isEqualByComparingTo(antes);
        assertThat(impacto.orElseThrow().unidadesPorDiaDespues()).isEqualByComparingTo(despues);
    }

    private void entoncesElIngresoPorDiaPasoDe(String antes, String despues) {
        assertThat(impacto.orElseThrow().ingresoPorDiaAntes()).isEqualByComparingTo(antes);
        assertThat(impacto.orElseThrow().ingresoPorDiaDespues()).isEqualByComparingTo(despues);
    }

    private void entoncesElIngresoSemanalCambio(String delta) {
        assertThat(impacto.orElseThrow().deltaIngresoSemanal()).isEqualByComparingTo(delta);
    }
}
