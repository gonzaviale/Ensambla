package com.ensambla.backend.domain.model.promocion;

import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.PromocionDePrueba.ACEITE_ID;
import static com.ensambla.backend.support.PromocionDePrueba.FIDEOS_ID;
import static com.ensambla.backend.support.PromocionDePrueba.YERBA_ID;
import static com.ensambla.backend.support.PromocionDePrueba.contexto;
import static com.ensambla.backend.support.PromocionDePrueba.fila;
import static com.ensambla.backend.support.PromocionDePrueba.inventarioCon;
import static com.ensambla.backend.support.PromocionDePrueba.producto;
import static com.ensambla.backend.support.PromocionDePrueba.ventas;
import static org.assertj.core.api.Assertions.assertThat;

class ContextoPromocionTest {

    @Test
    @DisplayName("Toma de los inventarios solo los productos que se pueden promocionar")
    void deShouldKeepOnlyPromotableProducts() {
        DatosDeProyecto datos = new DatosDeProyecto("Almacén", List.of(
                inventarioCon(fila(YERBA_ID, "Yerba", 3600, 10), fila(FIDEOS_ID, "Fideos", null, 30)),
                inventarioCon(fila(ACEITE_ID, "Aceite", 2000, 5))), List.of(), List.of());

        ContextoPromocion result = ContextoPromocion.de(datos, Map.of());

        assertThat(result.productos()).extracting(ProductoDeInventario::nombre).containsExactly("Yerba", "Aceite");
    }

    @Test
    @DisplayName("Dice cuántas unidades vendió un producto, y cero si no tiene ventas")
    void unidadesVendidasShouldBeZeroWithoutSales() {
        ProductoDeInventario yerba = producto(YERBA_ID, "Yerba", "3600", "10");
        ProductoDeInventario fideos = producto(FIDEOS_ID, "Fideos", "1100", "30");
        ContextoPromocion contexto = contexto(List.of(yerba, fideos), ventas(YERBA_ID, "8"));

        assertThat(contexto.unidadesVendidas(yerba)).isEqualByComparingTo(new BigDecimal("8"));
        assertThat(contexto.unidadesVendidas(fideos)).isZero();
    }

    @Test
    @DisplayName("Hay ventas anotadas cuando algún producto tiene métricas")
    void hayVentasAnotadasShouldDependOnTheMetrics() {
        ProductoDeInventario yerba = producto(YERBA_ID, "Yerba", "3600", "10");

        assertThat(contexto(List.of(yerba)).hayVentasAnotadas()).isFalse();
        assertThat(contexto(List.of(yerba), ventas(YERBA_ID, "8")).hayVentasAnotadas()).isTrue();
    }
}
