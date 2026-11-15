package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.NegocioDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.NegocioDePrueba.YERBA;
import static com.ensambla.backend.support.NegocioDePrueba.almacen;
import static org.assertj.core.api.Assertions.assertThat;

class ContextoObjetivoIaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
    private static final MetricasVentas METRICAS = new MetricasVentas(30, new BigDecimal("0.5"));

    @Test
    @DisplayName("Arma el contexto con los totales, los productos, los gastos y la zona del negocio")
    void deShouldBuildTheContextFromTheBusiness() {
        MedidorMiniObjetivo medidor = MedidorMiniObjetivo.de(List.of(almacen()), METRICAS, HOY);

        ContextoObjetivoIa result = ContextoObjetivoIa.de(medidor, zonaConBudin());

        assertThat(result.hayVentas()).isTrue();
        assertThat(result.resultado()).isEqualByComparingTo(medidor.resultado());
        assertThat(result.margen()).isEqualByComparingTo(medidor.margen().orElseThrow());
        assertThat(result.ventasSemana()).isEqualByComparingTo(medidor.ventasSemana());
        assertThat(result.productos()).extracting(ContextoObjetivoIa.ProductoIa::nombre)
                .containsExactlyInAnyOrder("Yerba 1 kg", "Fideos 500 g", "Aceite 900 ml");
        assertThat(result.productos()).filteredOn(producto -> producto.nombre().equals("Yerba 1 kg"))
                .singleElement()
                .satisfies(yerba -> {
                    assertThat(yerba.precio()).isEqualByComparingTo("4200");
                    assertThat(yerba.vendidas30Dias())
                            .isEqualByComparingTo(medidor.unidades(RegistroId.de(YERBA), 30));
                });
        assertThat(result.gastos()).extracting(ContextoObjetivoIa.GastoIa::categoria)
                .containsExactlyInAnyOrder("Servicios", "Alquiler");
        assertThat(result.zona()).singleElement().satisfies(budin -> {
            assertThat(budin.nombre()).isEqualTo("Budín");
            assertThat(budin.comercios()).isEqualTo(4);
            assertThat(budin.precioMinimo()).isEqualByComparingTo("1800");
            assertThat(budin.precioPromedio()).isEqualByComparingTo("2000");
            assertThat(budin.precioMaximo()).isEqualByComparingTo("2300");
        });
    }

    @Test
    @DisplayName("Con datos de todo, todas las fuentes figuran como que hay")
    void datosShouldSayEverythingIsAvailable() {
        ContextoObjetivoIa result = ContextoObjetivoIa.de(
                MedidorMiniObjetivo.de(List.of(almacen()), METRICAS, HOY), zonaConBudin());

        assertThat(result.datos()).containsExactly(
                new DatoUsado(FuenteDato.VENTAS, true),
                new DatoUsado(FuenteDato.MARGEN, true),
                new DatoUsado(FuenteDato.INVENTARIO, true),
                new DatoUsado(FuenteDato.GASTOS, true),
                new DatoUsado(FuenteDato.ZONA, true));
    }

    @Test
    @DisplayName("Sin zona la fuente de la zona figura como que no hay")
    void datosShouldSayThereIsNoZone() {
        ContextoObjetivoIa result = ContextoObjetivoIa.de(
                MedidorMiniObjetivo.de(List.of(almacen()), METRICAS, HOY), AnalisisZona.sinZona());

        assertThat(result.zona()).isEmpty();
        assertThat(result.datos()).contains(new DatoUsado(FuenteDato.ZONA, false));
    }

    @Test
    @DisplayName("Un negocio sin nada cargado dice que no hay ventas, margen, inventario ni gastos")
    void datosShouldSayThereIsNothing() {
        MedidorMiniObjetivo vacio = MedidorMiniObjetivo.de(
                List.of(NegocioDePrueba.cuaderno("Vacío").build()), METRICAS, HOY);

        ContextoObjetivoIa result = ContextoObjetivoIa.de(vacio, AnalisisZona.sinZona());

        assertThat(result.hayVentas()).isFalse();
        assertThat(result.margen()).isNull();
        assertThat(result.datos()).containsExactly(
                new DatoUsado(FuenteDato.VENTAS, false),
                new DatoUsado(FuenteDato.MARGEN, false),
                new DatoUsado(FuenteDato.INVENTARIO, false),
                new DatoUsado(FuenteDato.GASTOS, false),
                new DatoUsado(FuenteDato.ZONA, false));
    }

    private static AnalisisZona zonaConBudin() {
        return new AnalisisZona("San Justo", 5, List.of(new AgregadoZonaProducto(
                "Budín", 4, 4, new BigDecimal("1800"), new BigDecimal("2000"), new BigDecimal("2300"),
                new BigDecimal("6"))));
    }
}
