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

    private MedidorMiniObjetivo medidor;
    private ContextoObjetivoIa contexto;

    @Test
    @DisplayName("Arma el contexto con los totales, los productos, los gastos y la zona del negocio")
    void armaElContextoConLosTotalesLosProductosLosGastosYLaZonaDelNegocio() {
        dadoUnAlmacen();

        cuandoSeArmaElContexto(zonaConBudin());

        entoncesElContextoTieneLosTotalesDelNegocio();
        entoncesElContextoTieneLosProductosYLosGastos();
        entoncesElContextoTieneLaZonaConBudin();
    }

    @Test
    @DisplayName("Con datos de todo, todas las fuentes figuran como que hay")
    void conDatosDeTodoTodasLasFuentesFiguranComoQueHay() {
        dadoUnAlmacen();

        cuandoSeArmaElContexto(zonaConBudin());

        entoncesLasFuentesSon(true, true, true, true, true);
    }

    @Test
    @DisplayName("Sin zona la fuente de la zona figura como que no hay")
    void sinZonaLaFuenteDeLaZonaFiguraComoQueNoHay() {
        dadoUnAlmacen();

        cuandoSeArmaElContexto(AnalisisZona.sinZona());

        entoncesNoHayZona();
        entoncesLaFuenteDeZonaFiguraSinDatos();
    }

    @Test
    @DisplayName("Un negocio sin nada cargado dice que no hay ventas, margen, inventario ni gastos")
    void unNegocioSinNadaCargadoDiceQueNoHayVentasMargenInventarioNiGastos() {
        dadoUnNegocioVacio();

        cuandoSeArmaElContexto(AnalisisZona.sinZona());

        entoncesNoHayVentasNiMargen();
        entoncesLasFuentesSon(false, false, false, false, false);
    }

    // --- arrange ---
    private void dadoUnAlmacen() {
        medidor = MedidorMiniObjetivo.de(List.of(almacen()), METRICAS, HOY);
    }

    private void dadoUnNegocioVacio() {
        medidor = MedidorMiniObjetivo.de(List.of(NegocioDePrueba.cuaderno("Vacío").build()), METRICAS, HOY);
    }

    // --- act ---
    private void cuandoSeArmaElContexto(AnalisisZona zona) {
        contexto = ContextoObjetivoIa.de(medidor, zona);
    }

    // --- assert ---
    private void entoncesElContextoTieneLosTotalesDelNegocio() {
        assertThat(contexto.hayVentas()).isTrue();
        assertThat(contexto.resultado()).isEqualByComparingTo(medidor.resultado());
        assertThat(contexto.margen()).isEqualByComparingTo(medidor.margen().orElseThrow());
        assertThat(contexto.ventasSemana()).isEqualByComparingTo(medidor.ventasSemana());
    }

    private void entoncesElContextoTieneLosProductosYLosGastos() {
        assertThat(contexto.productos()).extracting(ContextoObjetivoIa.ProductoIa::nombre)
                .containsExactlyInAnyOrder("Yerba 1 kg", "Fideos 500 g", "Aceite 900 ml");
        assertThat(contexto.productos()).filteredOn(producto -> producto.nombre().equals("Yerba 1 kg"))
                .singleElement()
                .satisfies(yerba -> {
                    assertThat(yerba.precio()).isEqualByComparingTo("4200");
                    assertThat(yerba.vendidas30Dias())
                            .isEqualByComparingTo(medidor.unidades(RegistroId.de(YERBA), 30));
                });
        assertThat(contexto.gastos()).extracting(ContextoObjetivoIa.GastoIa::categoria)
                .containsExactlyInAnyOrder("Servicios", "Alquiler");
    }

    private void entoncesElContextoTieneLaZonaConBudin() {
        assertThat(contexto.zona()).singleElement().satisfies(budin -> {
            assertThat(budin.nombre()).isEqualTo("Budín");
            assertThat(budin.comercios()).isEqualTo(4);
            assertThat(budin.precioMinimo()).isEqualByComparingTo("1800");
            assertThat(budin.precioPromedio()).isEqualByComparingTo("2000");
            assertThat(budin.precioMaximo()).isEqualByComparingTo("2300");
        });
    }

    private void entoncesLasFuentesSon(boolean ventas, boolean margen, boolean inventario, boolean gastos,
                                   boolean zona) {
        assertThat(contexto.datos()).containsExactly(
                new DatoUsado(FuenteDato.VENTAS, ventas),
                new DatoUsado(FuenteDato.MARGEN, margen),
                new DatoUsado(FuenteDato.INVENTARIO, inventario),
                new DatoUsado(FuenteDato.GASTOS, gastos),
                new DatoUsado(FuenteDato.ZONA, zona));
    }

    private void entoncesNoHayZona() {
        assertThat(contexto.zona()).isEmpty();
    }

    private void entoncesLaFuenteDeZonaFiguraSinDatos() {
        assertThat(contexto.datos()).contains(new DatoUsado(FuenteDato.ZONA, false));
    }

    private void entoncesNoHayVentasNiMargen() {
        assertThat(contexto.hayVentas()).isFalse();
        assertThat(contexto.margen()).isNull();
    }

    // --- helpers ---
    private static AnalisisZona zonaConBudin() {
        return new AnalisisZona("San Justo", 5, List.of(new AgregadoZonaProducto(
                "Budín", 4, 4, new BigDecimal("1800"), new BigDecimal("2000"), new BigDecimal("2300"),
                new BigDecimal("6"))));
    }
}
