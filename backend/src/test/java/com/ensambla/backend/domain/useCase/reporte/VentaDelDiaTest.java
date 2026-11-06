package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VentaDelDiaTest {

    private static final RegistroId YERBA = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final LocalDate DESDE = LocalDate.of(2026, 10, 17);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 19);

    @Test
    @DisplayName("Suma lo que entró cada día con precio y deja en cero los días sin ventas")
    void serieShouldSumEachDayAndFillDaysWithoutSales() {
        List<VentaDelDia> serie = VentaDelDia.serie(List.of(
                venta(2, "3400", "2026-10-17"),
                venta(1, "3400", "2026-10-17"),
                venta(5, null, "2026-10-19"),
                venta(1, "1000", "2026-10-19"),
                venta(9, "3400", "2026-10-10")), DESDE, HASTA);

        assertThat(serie).extracting(VentaDelDia::fecha).containsExactly(
                LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 19));
        assertThat(serie.get(0).ingresos()).isEqualByComparingTo("10200");
        assertThat(serie.get(1).ingresos()).isEqualByComparingTo("0");
        assertThat(serie.get(2).ingresos()).isEqualByComparingTo("1000");
    }

    // --- helpers ---
    private static VentaProducto venta(int cantidad, String precio, String fecha) {
        return new VentaProducto(YERBA, BigDecimal.valueOf(cantidad), precio == null ? null : new BigDecimal(precio),
                LocalDate.parse(fecha), null);
    }
}
