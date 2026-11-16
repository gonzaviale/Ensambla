package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.useCase.caja.cobrarVenta.CobrarVentaRequest;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.support.CajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImporteLibreCajaTest {
    @Test
    @DisplayName("El importe libre guarda una descripción sin vincular un producto")
    void importeLibreShouldKeepDescriptionWithoutProduct() {
        LineaVentaCaja linea = whenSeCreaImporteLibre();

        thenSeConservaDescripcionEImporte(linea);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("Un importe libre necesita una descripción")
    void importeLibreShouldRequireDescription(String descripcion) {
        thenRechazaDescripcion(descripcion);
    }

    @Test
    @DisplayName("La descripción no supera los 160 caracteres")
    void importeLibreShouldRejectLongDescription() {
        thenRechazaDescripcion("a".repeat(161));
    }

    @Test
    @DisplayName("El importe libre tiene cantidad uno para conservar su monto original")
    void importeLibreShouldRejectOtherQuantity() {
        thenRechazaCantidadDistintaDeUno();
    }

    @Test
    @DisplayName("No permite interpretar un importe libre como una venta por peso")
    void resolverShouldRejectWeightForFreeAmount() {
        thenRechazaLinea(entrada(null, "PESO", "Servicio"));
    }

    @Test
    @DisplayName("Una línea identifica un producto o describe un importe, sin mezclar ambos")
    void resolverShouldRejectAmbiguousLine() {
        thenRechazaLinea(entrada(CajaTestData.PRODUCTO, "UNIDAD", "Servicio"));
    }

    @Test
    @DisplayName("Resuelve el importe sin consultar el inventario")
    void resolverShouldSkipInventoryForFreeAmount() {
        LineaVentaCaja linea = whenSeResuelveImporteLibre();

        thenSeConservaDescripcionEImporte(linea);
    }

    @Test
    @DisplayName("La huella histórica de líneas de inventario se mantiene compatible")
    void toStringShouldKeepLegacyFingerprint() {
        CobrarVentaRequest.LineaRequest linea = entrada(CajaTestData.PRODUCTO, "PESO", null);

        thenConservaFormatoAnterior(linea);
    }

    @Test
    @DisplayName("La descripción del importe forma parte de la huella de idempotencia")
    void toStringShouldIncludeFreeDescription() {
        CobrarVentaRequest.LineaRequest linea = entrada(null, "UNIDAD", "Servicio");

        thenIncluyeDescripcion(linea);
    }

    private LineaVentaCaja whenSeCreaImporteLibre() {
        return LineaVentaCaja.importeLibre("  Servicio  ", BigDecimal.ONE, new BigDecimal("2500"));
    }

    private LineaVentaCaja whenSeResuelveImporteLibre() {
        return entrada(null, "UNIDAD", "Servicio").resolver(linea -> {
            throw new AssertionError("El importe libre no usa inventario");
        });
    }

    private CobrarVentaRequest.LineaRequest entrada(
            RegistroId producto, String modo, String descripcion) {
        return new CobrarVentaRequest.LineaRequest(producto, BigDecimal.ONE,
                new BigDecimal("2500"), modo, descripcion);
    }

    private void thenSeConservaDescripcionEImporte(LineaVentaCaja linea) {
        assertThat(linea.productoId()).isNull();
        assertThat(linea.nombre()).isEqualTo("Servicio");
        assertThat(linea.modo()).isEqualTo(ModoVenta.UNIDAD);
        assertThat(linea.cantidad()).isEqualByComparingTo("1");
        assertThat(linea.precio()).isEqualByComparingTo("2500");
        assertThat(linea.importe()).isEqualByComparingTo("2500");
    }

    private void thenRechazaDescripcion(String descripcion) {
        assertThatThrownBy(() -> LineaVentaCaja.importeLibre(descripcion, BigDecimal.ONE, BigDecimal.TEN))
                .isInstanceOf(BusinessRuleException.class);
    }

    private void thenRechazaCantidadDistintaDeUno() {
        assertThatThrownBy(() -> LineaVentaCaja.importeLibre("Servicio", BigDecimal.TEN, BigDecimal.TEN))
                .isInstanceOf(BusinessRuleException.class);
    }

    private void thenRechazaLinea(CobrarVentaRequest.LineaRequest linea) {
        assertThatThrownBy(() -> linea.resolver(entrada -> null)).isInstanceOf(BusinessRuleException.class);
    }

    private void thenConservaFormatoAnterior(CobrarVentaRequest.LineaRequest linea) {
        assertThat(linea.toString()).isEqualTo("LineaRequest[productoId=" + CajaTestData.PRODUCTO
                + ", cantidad=1, precio=2500, modo=PESO]");
    }

    private void thenIncluyeDescripcion(CobrarVentaRequest.LineaRequest linea) {
        assertThat(linea.toString()).endsWith(", descripcion=Servicio]");
    }
}
