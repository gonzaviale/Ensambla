package com.ensambla.backend.domain.model.metrica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FuenteMetricaTest {

    @Test
    @DisplayName("Las ventas tienen fecha, unidades y pueden ser fiadas")
    void lasVentasTienenFechaUnidadesYFiado() {
        entoncesLaTabla(FuenteMetrica.VENTAS, true, true, true);
    }

    @Test
    @DisplayName("Los gastos tienen fecha pero no unidades ni fiado")
    void losGastosTienenFechaPeroNoUnidadesNiFiado() {
        entoncesLaTabla(FuenteMetrica.GASTOS, true, false, false);
    }

    @Test
    @DisplayName("El inventario tiene unidades pero no fecha ni fiado")
    void elInventarioTieneUnidadesPeroNoFechaNiFiado() {
        entoncesLaTabla(FuenteMetrica.INVENTARIO, false, true, false);
    }

    private static void entoncesLaTabla(FuenteMetrica fuente, boolean tieneFecha, boolean tieneUnidades,
                                        boolean admiteFiado) {
        assertThat(fuente.tieneFecha()).isEqualTo(tieneFecha);
        assertThat(fuente.tieneUnidades()).isEqualTo(tieneUnidades);
        assertThat(fuente.admiteFiado()).isEqualTo(admiteFiado);
    }
}
