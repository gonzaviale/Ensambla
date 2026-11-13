package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.negocio.EstadoDelNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObtenerResumenDeLaCuentaResponseTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 14);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 12);
    private static final NegocioResumido ALMACEN = negocio("a0000000-0000-4000-8000-000000000001", 1250000, 310000, 3);
    private static final NegocioResumido KIOSCO = negocio("a0000000-0000-4000-8000-000000000002", 280000, -72000, 2);

    @Test
    @DisplayName("Copia el período y los negocios en el mismo orden")
    void deShouldCopyThePeriodAndKeepTheOrderOfTheBusinesses() {
        ObtenerResumenDeLaCuentaResponse result = whenArmar(List.of(ALMACEN, KIOSCO));

        assertThat(result.periodo().desde()).isEqualTo(DESDE);
        assertThat(result.periodo().hasta()).isEqualTo(HASTA);
        assertThat(result.negocios()).containsExactly(ALMACEN, KIOSCO);
    }

    @Test
    @DisplayName("Suma las ventas, los márgenes y los avisos pendientes de todos los negocios")
    void deShouldAddUpTheTotalsOfEveryBusiness() {
        ObtenerResumenDeLaCuentaResponse result = whenArmar(List.of(ALMACEN, KIOSCO));

        assertThat(result.totales().ventas()).isEqualTo(1530000);
        assertThat(result.totales().margen()).isEqualTo(238000);
        assertThat(result.totales().avisosPendientes()).isEqualTo(5);
    }

    @Test
    @DisplayName("Sin negocios los totales quedan en cero")
    void deShouldReturnZeroTotalsWhenThereAreNoBusinesses() {
        ObtenerResumenDeLaCuentaResponse result = whenArmar(List.of());

        assertThat(result.negocios()).isEmpty();
        assertThat(result.totales()).isEqualTo(new ObtenerResumenDeLaCuentaResponse.Totales(0, 0, 0));
    }

    // --- act ---
    private static ObtenerResumenDeLaCuentaResponse whenArmar(List<NegocioResumido> negocios) {
        return ObtenerResumenDeLaCuentaResponse.de(new PeriodoResumen(DESDE, HASTA), negocios);
    }

    // --- helpers ---
    private static NegocioResumido negocio(String id, long ventas, long margen, long avisosPendientes) {
        return new NegocioResumido(NegocioId.de(id), "Negocio " + id, RubroNegocio.OTRO, ventas, margen,
                avisosPendientes, EstadoDelNegocio.BIEN);
    }
}
