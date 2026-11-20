package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo.Gasto;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo.Producto;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.support.NegocioDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.NegocioDePrueba.ACEITE;
import static com.ensambla.backend.support.NegocioDePrueba.FIDEOS;
import static com.ensambla.backend.support.NegocioDePrueba.YERBA;
import static com.ensambla.backend.support.NegocioDePrueba.almacen;
import static org.assertj.core.api.Assertions.assertThat;

class MedidorMiniObjetivoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
    private static final UUID GALLETITAS = UUID.fromString("d0000000-0000-4000-8000-000000000004");
    private static final UUID ALFAJOR = UUID.fromString("d0000000-0000-4000-8000-000000000005");
    private static final UUID REGALO = UUID.fromString("d0000000-0000-4000-8000-000000000006");
    private static final RegistroId YERBA_ID = RegistroId.de(YERBA);
    private static final RegistroId FIDEOS_ID = RegistroId.de(FIDEOS);
    private static final RegistroId ACEITE_ID = RegistroId.de(ACEITE);
    private static final RegistroId INEXISTENTE_ID = RegistroId.de("d0000000-0000-4000-8000-0000000000ff");

    private MedidorMiniObjetivo medidor;
    private boolean respuesta;
    private BigDecimal numero;
    private Optional<BigDecimal> medida;
    private List<Gasto> gastos;
    private List<Producto> productos;
    private Map<LocalDate, BigDecimal> resultadoPorSemana;

    @Test
    @DisplayName("Hay ventas cuando se vendió algo en los últimos 30 días")
    void hayVentasCuandoSeVendioAlgoEnLosUltimos30Dias() {
        dadoUnNegocio(almacen());

        cuandoSePreguntaSiHayVentas();

        entoncesLaRespuestaEs(true);
    }

    @Test
    @DisplayName("Sin ventas en los últimos 30 días no hay ventas")
    void sinVentasEnLosUltimos30DiasNoHayVentas() {
        dadoUnNegocio(conVentasViejas());

        cuandoSePreguntaSiHayVentas();

        entoncesLaRespuestaEs(false);
    }

    @Test
    @DisplayName("Sin ventas en los últimos 30 días el resultado es cero")
    void sinVentasEnLosUltimos30DiasElResultadoEsCero() {
        dadoUnNegocio(conVentasViejas());

        cuandoSePideElResultado();

        entoncesElNumeroEs("0");
    }

    @Test
    @DisplayName("Sin ventas en los últimos 30 días no hay margen")
    void sinVentasEnLosUltimos30DiasNoHayMargen() {
        dadoUnNegocio(conVentasViejas());

        cuandoSePideElMargen();

        entoncesNoSePuedeMedir();
    }

    @Test
    @DisplayName("Lo vendido son las ventas de 30 días")
    void loVendidoSonLasVentasDe30Dias() {
        dadoUnNegocio(almacen());

        cuandoSePideLoVendido();

        entoncesElNumeroEs("309000");
    }

    @Test
    @DisplayName("El resultado es lo vendido menos la mercadería y los gastos sin la categoría Mercadería, de 30 días")
    void elResultadoEsLoVendidoMenosLaMercaderiaYLosGastosSinLaCategoriaMercaderiaDe() {
        dadoUnNegocio(almacen());

        cuandoSePideElResultado();

        entoncesElNumeroEs("-27000");
    }

    @Test
    @DisplayName("El margen es lo que queda de cada $100 vendidos después de pagar la mercadería")
    void elMargenEsLoQueQuedaDeCada100VendidosDespuesDePagarLaMercaderia() {
        dadoUnNegocio(almacen());

        cuandoSePideElMargen();

        entoncesLaMedidaEs("23.62");
    }

    @Test
    @DisplayName("Sin costos cargados no hay margen con qué calcularlo")
    void sinCostosCargadosNoHayMargenConQueCalcularlo() {
        dadoUnNegocio(sinCostosCargados());

        cuandoSePideElMargen();

        entoncesNoSePuedeMedir();
    }

    @Test
    @DisplayName("Las ventas de la semana son lo vendido con precio en los últimos 7 días")
    void lasVentasDeLaSemanaSonLoVendidoConPrecioEnLosUltimos7Dias() {
        dadoUnNegocio(almacen());

        cuandoSePidenLasVentasDeLaSemana();

        entoncesElNumeroEs("73050");
    }

    @Test
    @DisplayName("Las ventas de la semana no cuentan las que no tienen precio")
    void lasVentasDeLaSemanaNoCuentanLasQueNoTienenPrecio() {
        dadoUnNegocio(conUnaVentaSinPrecio());

        cuandoSePidenLasVentasDeLaSemana();

        entoncesElNumeroEs("0");
    }

    @Test
    @DisplayName("Las unidades de la yerba en la semana cuentan la ventana de 7 días, hoy incluido")
    void lasUnidadesDeLaYerbaEnLaSemanaCuentanLaVentanaDe7DiasHoyIncluido() {
        dadoUnNegocio(almacen());

        cuandoSePidenLasUnidades(YERBA_ID, MedidorMiniObjetivo.DIAS_SEMANA);

        entoncesElNumeroEs("14");
    }

    @Test
    @DisplayName("Las unidades de la yerba en el mes cuentan la ventana de 30 días")
    void lasUnidadesDeLaYerbaEnElMesCuentanLaVentanaDe30Dias() {
        dadoUnNegocio(almacen());

        cuandoSePidenLasUnidades(YERBA_ID, MedidorMiniObjetivo.DIAS_MES);

        entoncesElNumeroEs("60");
    }

    @Test
    @DisplayName("Las unidades de un producto que no se vendió son cero")
    void lasUnidadesDeUnProductoQueNoSeVendioSonCero() {
        dadoUnNegocio(almacen());

        cuandoSePidenLasUnidades(FIDEOS_ID, MedidorMiniObjetivo.DIAS_MES);

        entoncesElNumeroEs("0");
    }

    @Test
    @DisplayName("La ganancia de la semana es lo vendido al precio de cada venta menos el costo actual")
    void laGananciaDeLaSemanaEsLoVendidoAlPrecioDeCadaVentaMenosElCostoActual() {
        dadoUnNegocio(almacen());

        cuandoSePideLaGananciaSemanalDeLaYerba();

        entoncesElNumeroEs("12600");
    }

    @Test
    @DisplayName("La ganancia de una venta sin precio usa el precio actual del producto")
    void laGananciaDeUnaVentaSinPrecioUsaElPrecioActualDelProducto() {
        dadoUnNegocio(conUnaVentaSinPrecio());

        cuandoSePideLaGananciaSemanalDeLaYerba();

        entoncesElNumeroEs("1800");
    }

    @Test
    @DisplayName("Los gastos son los de cada categoría en 30 días, sin la mercadería y sin lo anterior")
    void losGastosSonLosDeCadaCategoriaEn30DiasSinLaMercaderiaYSinLoAnterior() {
        dadoUnNegocio(almacen());

        cuandoSePidenLosGastos();

        entoncesLosGastosSonAlquilerYServicios();
    }

    @Test
    @DisplayName("Suma la misma categoría de distintos cuadernos aunque esté escrita distinto, y salta lo que no gastó")
    void sumaLaMismaCategoriaDeDistintosCuadernosAunqueEsteEscritaDistintoYSaltaLo() {
        dadoVariosCuadernosConLaMismaCategoria();

        cuandoSePidenLosGastos();

        entoncesSeSumanLosServiciosDeLosCuadernos();
    }

    @Test
    @DisplayName("Sólo sirven los productos con nombre, precio y costo; sin stock cargado cuenta cero")
    void soloSirvenLosProductosConNombrePrecioYCostoSinStockCargadoCuentaCero() {
        dadoUnNegocio(conProductosIncompletos());

        cuandoSePidenLosProductos();

        entoncesSoloQuedaLaYerbaConStockCero();
    }

    @Test
    @DisplayName("Subir un precio se mide con la ganancia semanal del producto")
    void subirUnPrecioSeMideConLaGananciaSemanalDelProducto() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.SUBIR_PRECIO, YERBA_ID, "Yerba 1 kg");

        entoncesLaMedidaEs("12600");
    }

    @Test
    @DisplayName("Mover un parado se mide con las unidades de la semana del producto")
    void moverUnParadoSeMideConLasUnidadesDeLaSemanaDelProducto() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.MOVER_PARADO, FIDEOS_ID, "Fideos 500 g");

        entoncesLaMedidaEs("0");
    }

    @Test
    @DisplayName("Poner a la vista se mide con las unidades de la semana del producto")
    void ponerALaVistaSeMideConLasUnidadesDeLaSemanaDelProducto() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.PONER_A_LA_VISTA, ACEITE_ID, "Aceite 900 ml");

        entoncesLaMedidaEs("5");
    }

    @Test
    @DisplayName("Un producto que ya no está en el cuaderno no se puede medir")
    void unProductoQueYaNoEstaEnElCuadernoNoSePuedeMedir() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.SUBIR_PRECIO, INEXISTENTE_ID, "Algo");

        entoncesNoSePuedeMedir();
    }

    @Test
    @DisplayName("Un mini-objetivo de producto sin producto no se puede medir")
    void unMiniObjetivoDeProductoSinProductoNoSePuedeMedir() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.MOVER_PARADO, null, "Algo");

        entoncesNoSePuedeMedir();
    }

    @Test
    @DisplayName("Recortar un gasto se mide con lo gastado en la categoría, sin importar tildes ni mayúsculas")
    void recortarUnGastoSeMideConLoGastadoEnLaCategoriaSinImportarTildesNiMayusculas() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.RECORTAR_GASTO, null, "SERVICIOS");

        entoncesLaMedidaEs("40000");
    }

    @Test
    @DisplayName("Una categoría sin gastos en 30 días se mide como cero")
    void unaCategoriaSinGastosEn30DiasSeMideComoCero() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.RECORTAR_GASTO, null, "Impuestos");

        entoncesLaMedidaEs("0");
    }

    @Test
    @DisplayName("Mejorar el margen se mide con el margen del negocio")
    void mejorarElMargenSeMideConElMargenDelNegocio() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.MEJORAR_MARGEN, null, null);

        entoncesLaMedidaEs("23.62");
    }

    @Test
    @DisplayName("Vender más por semana se mide con las ventas de la semana")
    void venderMasPorSemanaSeMideConLasVentasDeLaSemana() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.VENTAS_SEMANALES, null, null);

        entoncesLaMedidaEs("73050");
    }

    @Test
    @DisplayName("El resultado por semana trae el de los 30 días que terminan cada semana, por su lunes, hasta hoy")
    void elResultadoPorSemanaTraeElDeLos30DiasQueTerminanCadaSemanaPorSuLunesHasta() {
        dadoUnNegocio(almacen());

        cuandoSePideElResultadoPorSemana();

        entoncesElResultadoPorSemanaVaDesdeElPrimerLunesHastaHoy();
    }

    @Test
    @DisplayName("Sin ventas no hay resultado por semana")
    void sinVentasNoHayResultadoPorSemana() {
        dadoUnNegocio(sinVentas());

        cuandoSePideElResultadoPorSemana();

        entoncesNoHayResultadoPorSemana();
    }

    @Test
    @DisplayName("Productos bajo el mínimo cuenta los que están en el mínimo o por debajo, y sólo los que tienen uno")
    void productosBajoElMinimoCuentaLosQueEstanEnElMinimoOPorDebajoYSoloLosQueTienen() {
        dadoUnNegocio(conAlgunosProductosBajoElMinimo());

        cuandoSeMide(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO, null, null);

        entoncesLaMedidaEs("2");
    }

    @Test
    @DisplayName("Sin ningún mínimo definido no hay datos de productos bajo el mínimo (no es cero)")
    void sinNingunMinimoDefinidoNoHayDatosDeProductosBajoElMinimoNoEsCero() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO, null, null);

        entoncesNoSePuedeMedir();
    }

    @Test
    @DisplayName("Un mínimo en cero o negativo se toma como que no hay mínimo")
    void unMinimoEnCeroONegativoSeTomaComoQueNoHayMinimo() {
        dadoUnNegocio(conMinimoEnCero());

        cuandoSeMide(TipoMiniObjetivo.PRODUCTOS_BAJO_EL_MINIMO, null, null);

        entoncesNoSePuedeMedir();
    }

    @Test
    @DisplayName("Un mínimo en cero o negativo deja al producto sin mínimo")
    void unMinimoEnCeroONegativoDejaAlProductoSinMinimo() {
        dadoUnNegocio(conMinimoEnCero());

        cuandoSePidenLosProductos();

        entoncesElProductoQuedaSinMinimo();
    }

    @Test
    @DisplayName("El capital parado es el costo del stock de lo que no se vendió en 30 días")
    void elCapitalParadoEsElCostoDelStockDeLoQueNoSeVendioEn30Dias() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.CAPITAL_PARADO, null, null);

        entoncesLaMedidaEs("32000");
    }

    @Test
    @DisplayName("Un producto sin stock no suma capital parado aunque no se venda")
    void unProductoSinStockNoSumaCapitalParadoAunqueNoSeVenda() {
        dadoUnNegocio(conUnProductoSinStockQueNoSeVende());

        cuandoSeMide(TipoMiniObjetivo.CAPITAL_PARADO, null, null);

        entoncesLaMedidaEs("0");
    }

    @Test
    @DisplayName("Sin ventas no hay datos de capital parado")
    void sinVentasNoHayDatosDeCapitalParado() {
        dadoUnNegocio(sinVentas());

        cuandoSeMide(TipoMiniObjetivo.CAPITAL_PARADO, null, null);

        entoncesNoSePuedeMedir();
    }

    @Test
    @DisplayName("Sin productos con precio y costo no hay datos de capital parado")
    void sinProductosConPrecioYCostoNoHayDatosDeCapitalParado() {
        dadoUnNegocio(sinProductos());

        cuandoSeMide(TipoMiniObjetivo.CAPITAL_PARADO, null, null);

        entoncesNoSePuedeMedir();
    }

    @Test
    @DisplayName("Sin ventas no hay datos de variedad")
    void sinVentasNoHayDatosDeVariedad() {
        dadoUnNegocio(sinVentas());

        cuandoSeMide(TipoMiniObjetivo.VARIEDAD_VENDIDA, null, null);

        entoncesNoSePuedeMedir();
    }

    @Test
    @DisplayName("Sin productos con precio y costo no hay datos de variedad")
    void sinProductosConPrecioYCostoNoHayDatosDeVariedad() {
        dadoUnNegocio(sinProductos());

        cuandoSeMide(TipoMiniObjetivo.VARIEDAD_VENDIDA, null, null);

        entoncesNoSePuedeMedir();
    }

    @Test
    @DisplayName("La variedad cuenta los productos distintos que se vendieron en los últimos 7 días")
    void laVariedadCuentaLosProductosDistintosQueSeVendieronEnLosUltimos7Dias() {
        dadoUnNegocio(almacen());

        cuandoSeMide(TipoMiniObjetivo.VARIEDAD_VENDIDA, null, null);

        entoncesLaMedidaEs("2");
    }

    // --- arrange ---
    private void dadoUnNegocio(DatosDeProyecto datos) {
        medidor = MedidorMiniObjetivo.de(List.of(datos), metricas(), HOY);
    }

    private void dadoVariosCuadernosConLaMismaCategoria() {
        DatosDeProyecto uno = conVentaYGasto("Almacén", "Servicios", 30000);
        DatosDeProyecto otro = conVentaYGasto("Kiosco", " servicios ", 12000);
        DatosDeProyecto sinTabla = NegocioDePrueba.cuaderno("Feria").sinTablaDeGastos().build();
        DatosDeProyecto sinGasto = conVentaYGasto("Puesto", "Limpieza", 0);
        medidor = MedidorMiniObjetivo.de(List.of(uno, otro, sinTabla, sinGasto), metricas(), HOY);
    }

    // --- act ---
    private void cuandoSePreguntaSiHayVentas() {
        respuesta = medidor.hayVentas();
    }

    private void cuandoSePideElResultado() {
        numero = medidor.resultado();
    }

    private void cuandoSePideElMargen() {
        medida = medidor.margen();
    }

    private void cuandoSePideLoVendido() {
        numero = BigDecimal.valueOf(medidor.vendiste());
    }

    private void cuandoSePidenLasVentasDeLaSemana() {
        numero = medidor.ventasSemana();
    }

    private void cuandoSePidenLasUnidades(RegistroId producto, int dias) {
        numero = medidor.unidades(producto, dias);
    }

    private void cuandoSePideLaGananciaSemanalDeLaYerba() {
        numero = medidor.gananciaSemanal(productoDe(medidor, YERBA_ID));
    }

    private void cuandoSePidenLosGastos() {
        gastos = medidor.gastos();
    }

    private void cuandoSePidenLosProductos() {
        productos = medidor.productos();
    }

    private void cuandoSeMide(TipoMiniObjetivo tipo, RegistroId producto, String referencia) {
        medida = medidor.valorDe(tipo, producto, referencia);
    }

    private void cuandoSePideElResultadoPorSemana() {
        resultadoPorSemana = medidor.resultadoPorSemana();
    }

    // --- assert ---
    private void entoncesLaRespuestaEs(boolean esperada) {
        assertThat(respuesta).isEqualTo(esperada);
    }

    private void entoncesElNumeroEs(String esperado) {
        assertThat(numero).isEqualByComparingTo(esperado);
    }

    private void entoncesLaMedidaEs(String esperada) {
        assertThat(medida).hasValueSatisfying(valor -> assertThat(valor).isEqualByComparingTo(esperada));
    }

    private void entoncesNoSePuedeMedir() {
        assertThat(medida).isEmpty();
    }

    private void entoncesLosGastosSonAlquilerYServicios() {
        assertThat(gastos).extracting(Gasto::categoria).containsExactly("Alquiler", "Servicios");
        assertThat(gastos).extracting(Gasto::total).satisfiesExactly(
                total -> assertThat(total).isEqualByComparingTo("60000"),
                total -> assertThat(total).isEqualByComparingTo("40000"));
    }

    private void entoncesSeSumanLosServiciosDeLosCuadernos() {
        assertThat(gastos).singleElement().satisfies(gasto -> {
            assertThat(gasto.categoria()).isEqualTo("Servicios");
            assertThat(gasto.total()).isEqualByComparingTo("42000");
        });
    }

    private void entoncesSoloQuedaLaYerbaConStockCero() {
        assertThat(productos).singleElement().satisfies(producto -> {
            assertThat(producto.id()).isEqualTo(YERBA_ID);
            assertThat(producto.nombre()).isEqualTo("Yerba 1 kg");
            assertThat(producto.stock()).isEqualByComparingTo("0");
            assertThat(producto.gananciaPorUnidad()).isEqualByComparingTo("900");
        });
    }

    private void entoncesElResultadoPorSemanaVaDesdeElPrimerLunesHastaHoy() {
        assertThat(resultadoPorSemana).hasSize(ComoVieneElNegocio.SEMANAS_DE_HISTORIAL);
        assertThat(resultadoPorSemana.keySet()).first().isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(resultadoPorSemana.keySet()).last().isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(resultadoPorSemana.get(LocalDate.of(2026, 11, 9))).isEqualByComparingTo("-27000");
    }

    private void entoncesNoHayResultadoPorSemana() {
        assertThat(resultadoPorSemana).isEmpty();
    }

    private void entoncesElProductoQuedaSinMinimo() {
        assertThat(productos).singleElement().satisfies(producto -> assertThat(producto.minimo()).isNull());
    }

    // --- helpers ---
    private static DatosDeProyecto conVentasViejas() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .venta(YERBA, 10, 4200, "2026-08-01")
                .build();
    }

    private static DatosDeProyecto sinCostosCargados() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, null)
                .venta(YERBA, 10, 4200, "2026-11-08")
                .build();
    }

    private static DatosDeProyecto conUnaVentaSinPrecio() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .venta(YERBA, 2, null, "2026-11-10")
                .build();
    }

    private static DatosDeProyecto conProductosIncompletos() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", null, 4200, 3300)
                .producto(FIDEOS, "  ", 10, 1100, 800)
                .producto(ACEITE, null, 10, 2850, 1900)
                .producto(GALLETITAS, "Galletitas", 10, null, 600)
                .producto(ALFAJOR, "Alfajor", 10, 900, null)
                .producto(REGALO, "Regalo", 10, 0, 0)
                .build();
    }

    private static DatosDeProyecto sinVentas() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .build();
    }

    private static DatosDeProyecto sinProductos() {
        return NegocioDePrueba.cuaderno("Almacén")
                .venta(YERBA, 1, 4200, "2026-11-08")
                .build();
    }

    private static DatosDeProyecto conAlgunosProductosBajoElMinimo() {
        return NegocioDePrueba.cuaderno("Almacén")
                .productoConMinimo(YERBA, "Yerba 1 kg", 3, 5, 4200, 3300)
                .productoConMinimo(FIDEOS, "Fideos 500 g", 10, 10, 1100, 800)
                .productoConMinimo(ACEITE, "Aceite 900 ml", 20, 10, 2850, 1900)
                .producto(GALLETITAS, "Galletitas", 0, 900, 600)
                .venta(YERBA, 1, 4200, "2026-11-08")
                .build();
    }

    private static DatosDeProyecto conMinimoEnCero() {
        return NegocioDePrueba.cuaderno("Almacén")
                .productoConMinimo(YERBA, "Yerba 1 kg", 0, 0, 4200, 3300)
                .venta(YERBA, 1, 4200, "2026-11-08")
                .build();
    }

    private static DatosDeProyecto conUnProductoSinStockQueNoSeVende() {
        return NegocioDePrueba.cuaderno("Almacén")
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .producto(GALLETITAS, "Galletitas", 0, 900, 600)
                .venta(YERBA, 1, 4200, "2026-11-08")
                .build();
    }

    private static DatosDeProyecto conVentaYGasto(String cuaderno, String categoria, int total) {
        return NegocioDePrueba.cuaderno(cuaderno)
                .producto(YERBA, "Yerba 1 kg", 40, 4200, 3300)
                .venta(YERBA, 1, 4200, "2026-11-08")
                .gasto("2026-11-01", total, categoria)
                .build();
    }

    private static MetricasVentas metricas() {
        return new MetricasVentas(30, new BigDecimal("0.5"));
    }

    private static Producto productoDe(MedidorMiniObjetivo medidor, RegistroId id) {
        return medidor.productos().stream().filter(producto -> producto.id().equals(id)).findFirst().orElseThrow();
    }
}
