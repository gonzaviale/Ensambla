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

    @Test
    @DisplayName("Compara las unidades y el ingreso por día de los días anteriores con los posteriores")
    void medirShouldCompareDaysBeforeAndAfter() {
        List<VentaProducto> ventas = List.of(
                venta(99, "100", "2026-09-01"),
                venta(2, "100", "2026-09-20"),
                venta(3, null, "2026-09-25"),
                venta(7, "100", "2026-10-02"),
                venta(5, "100", "2026-10-15"));

        ImpactoAviso impacto = whenMedir(ventas, HOY).orElseThrow();

        assertThat(impacto.ventanaDias()).isEqualTo(VENTANA);
        assertThat(impacto.unidadesPorDiaAntes()).isEqualByComparingTo("0.36");
        assertThat(impacto.unidadesPorDiaDespues()).isEqualByComparingTo("0.50");
        assertThat(impacto.ingresoPorDiaAntes()).isEqualByComparingTo("14.29");
        assertThat(impacto.ingresoPorDiaDespues()).isEqualByComparingTo("50.00");
        assertThat(impacto.deltaIngresoSemanal()).isEqualByComparingTo("249.97");
    }

    @Test
    @DisplayName("La venta del día del aviso cuenta como posterior")
    void medirShouldCountTheSaleOfTheDayOfTheAvisoAsAfter() {
        ImpactoAviso impacto = whenMedir(List.of(venta(14, "10", "2026-10-01")), HOY).orElseThrow();

        assertThat(impacto.unidadesPorDiaAntes()).isEqualByComparingTo("0");
        assertThat(impacto.unidadesPorDiaDespues()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("Sin ventas el impacto es cero y no cambia el ingreso")
    void medirShouldBeZeroWithoutSales() {
        ImpactoAviso impacto = whenMedir(List.of(), HOY).orElseThrow();

        assertThat(impacto.deltaIngresoSemanal()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Si bajaron las ventas el cambio de ingreso es negativo")
    void deltaIngresoSemanalShouldBeNegativeWhenSalesDrop() {
        ImpactoAviso impacto = whenMedir(List.of(venta(14, "100", "2026-09-20")), HOY).orElseThrow();

        assertThat(impacto.deltaIngresoSemanal()).isEqualByComparingTo("-700.00");
    }

    @Test
    @DisplayName("No mide hasta que termina la ventana posterior: el día de hoy no cuenta")
    void medirShouldWaitUntilTheWindowEnds() {
        assertThat(whenMedir(List.of(), LocalDate.of(2026, 10, 14))).isEmpty();
        assertThat(whenMedir(List.of(), LocalDate.of(2026, 10, 15))).isPresent();
    }

    @Test
    @DisplayName("Lo que se guardó en los datos del aviso se vuelve a leer igual")
    void deShouldReadWhatAMapStored() {
        ImpactoAviso impacto = whenMedir(List.of(venta(7, "100", "2026-10-02")), HOY).orElseThrow();
        Map<String, Object> datos = new HashMap<>();
        datos.put(ImpactoAviso.CLAVE, impacto.aMapa());

        assertThat(ImpactoAviso.de(datos)).contains(impacto);
    }

    @Test
    @DisplayName("Sin impacto guardado, o sin datos, no hay nada que leer")
    void deShouldBeEmptyWithoutStoredImpact() {
        assertThat(ImpactoAviso.de(null)).isEmpty();
        assertThat(ImpactoAviso.de(Map.of("precio", 10))).isEmpty();
        assertThat(ImpactoAviso.de(Map.of(ImpactoAviso.CLAVE, "no es un mapa"))).isEmpty();
    }

    private static Optional<ImpactoAviso> whenMedir(List<VentaProducto> ventas, LocalDate hoy) {
        return ImpactoAviso.medir(ventas, APLICADO, hoy, VENTANA);
    }

    private static VentaProducto venta(int cantidad, String precio, String fecha) {
        return new VentaProducto(PRODUCTO, BigDecimal.valueOf(cantidad),
                precio == null ? null : new BigDecimal(precio), LocalDate.parse(fecha), null);
    }
}
