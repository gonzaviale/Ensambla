package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaRequest;
import com.ensambla.backend.support.CajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ImporteLibreCajaTest {

    private LineaVentaCaja linea;
    private CobrarVentaRequest.LineaRequest entrada;
    private Throwable rechazo;

    @Test
    @DisplayName("El importe libre guarda una descripción sin vincular un producto")
    void elImporteLibreGuardaUnaDescripcionSinVincularUnProducto() {
        cuandoSeCreaUnImporteLibre();

        entoncesSeConservaLaDescripcionYElImporte();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("Un importe libre necesita una descripción")
    void unImporteLibreNecesitaUnaDescripcion(String descripcion) {
        cuandoSeIntentaCrearConLaDescripcion(descripcion);

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("La descripción no supera los 160 caracteres")
    void laDescripcionNoSuperaLos160Caracteres() {
        cuandoSeIntentaCrearConLaDescripcion("a".repeat(161));

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("El importe libre tiene cantidad uno para conservar su monto original")
    void elImporteLibreTieneCantidadUnoParaConservarSuMontoOriginal() {
        cuandoSeIntentaCrearConCantidadDiez();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("No permite interpretar un importe libre como una venta por peso")
    void noPermiteInterpretarUnImporteLibreComoUnaVentaPorPeso() {
        dadoUnaLineaDeEntrada(null, "PESO", "Servicio");

        cuandoSeIntentaResolverLaLinea();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Una línea identifica un producto o describe un importe, sin mezclar ambos")
    void unaLineaIdentificaUnProductoODescribeUnImporteSinMezclarAmbos() {
        dadoUnaLineaDeEntrada(CajaTestData.PRODUCTO, "UNIDAD", "Servicio");

        cuandoSeIntentaResolverLaLinea();

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Resuelve el importe sin consultar el inventario")
    void resuelveElImporteSinConsultarElInventario() {
        dadoUnaLineaDeEntrada(null, "UNIDAD", "Servicio");

        cuandoSeResuelveElImporteLibre();

        entoncesSeConservaLaDescripcionYElImporte();
    }

    @Test
    @DisplayName("La huella histórica de líneas de inventario se mantiene compatible")
    void laHuellaHistoricaDeLineasDeInventarioSeMantieneCompatible() {
        dadoUnaLineaDeEntrada(CajaTestData.PRODUCTO, "PESO", null);

        entoncesConservaElFormatoAnterior();
    }

    @Test
    @DisplayName("La descripción del importe forma parte de la huella de idempotencia")
    void laDescripcionDelImporteFormaParteDeLaHuellaDeIdempotencia() {
        dadoUnaLineaDeEntrada(null, "UNIDAD", "Servicio");

        entoncesIncluyeLaDescripcion();
    }

    // --- dado ---

    private void dadoUnaLineaDeEntrada(RegistroId producto, String modo, String descripcion) {
        entrada = new CobrarVentaRequest.LineaRequest(producto, BigDecimal.ONE,
                new BigDecimal("2500"), modo, descripcion);
    }

    // --- cuando ---

    private void cuandoSeCreaUnImporteLibre() {
        linea = LineaVentaCaja.importeLibre("  Servicio  ", BigDecimal.ONE, new BigDecimal("2500"));
    }

    private void cuandoSeIntentaCrearConLaDescripcion(String descripcion) {
        rechazo = catchThrowable(() -> LineaVentaCaja.importeLibre(descripcion, BigDecimal.ONE, BigDecimal.TEN));
    }

    private void cuandoSeIntentaCrearConCantidadDiez() {
        rechazo = catchThrowable(() -> LineaVentaCaja.importeLibre("Servicio", BigDecimal.TEN, BigDecimal.TEN));
    }

    private void cuandoSeIntentaResolverLaLinea() {
        rechazo = catchThrowable(() -> entrada.resolver(producto -> null));
    }

    private void cuandoSeResuelveElImporteLibre() {
        linea = entrada.resolver(producto -> {
            throw new AssertionError("El importe libre no usa inventario");
        });
    }

    // --- entonces ---

    private void entoncesSeConservaLaDescripcionYElImporte() {
        assertThat(linea.productoId()).isNull();
        assertThat(linea.nombre()).isEqualTo("Servicio");
        assertThat(linea.modo()).isEqualTo(ModoVenta.UNIDAD);
        assertThat(linea.cantidad()).isEqualByComparingTo("1");
        assertThat(linea.precio()).isEqualByComparingTo("2500");
        assertThat(linea.importe()).isEqualByComparingTo("2500");
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesConservaElFormatoAnterior() {
        assertThat(entrada.toString()).isEqualTo("LineaRequest[productoId=" + CajaTestData.PRODUCTO
                + ", cantidad=1, precio=2500, modo=PESO]");
    }

    private void entoncesIncluyeLaDescripcion() {
        assertThat(entrada.toString()).endsWith(", descripcion=Servicio]");
    }
}
