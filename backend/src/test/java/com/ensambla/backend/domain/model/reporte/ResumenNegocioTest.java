package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ClienteTop;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Faltante;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.GastoPorCategoria;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoDesalineado;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoParado;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoVendido;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.DatosDeResumenDePrueba.AHORA;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.FIDEOS;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.MERMELADA;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.YERBA;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.aceite;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.fideos;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.gastosDe;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.gastosSinImporte;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.mermelada;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.productosCarosNumerados;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.productosNumerados;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.rangosBaratosNumerados;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unAviso;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unAvisoAplicadoYMedido;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unAvisoPendiente;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unGasto;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unProducto;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unRango;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unaVenta;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unaVentaA;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unaVentaFiadaA;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.ventasAClientesNumerados;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.ventasCrecientesDe;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.ventasDe;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.yerba;
import static org.assertj.core.api.Assertions.assertThat;

class ResumenNegocioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final PeriodoResumen PERIODO = PeriodoResumen.armar(null, null, HOY);
    private static final Integer SIN_PRECIO = null;
    private static final String ID_DE_ROSA = "a1b2c3d4-0000-4000-8000-000000000001";
    private static final String ID_DESCONOCIDO = "A1B2C3D4-0000-4000-8000-000000000002";

    private List<Registro> productos = List.of(yerba());
    private List<Registro> ventas = List.of();
    private List<BloqueGastos> gastos = List.of();
    private Map<String, String> nombresDeClientes = Map.of();
    private AnalisisZona zona = AnalisisZona.sinZona();
    private List<Aviso> avisos = List.of();
    private ResumenNegocio resumen;

    // --- ventas ---

    @Test
    @DisplayName("Suma ingresos, unidades y ventas del período, y la variación contra el período anterior")
    void sumaLasVentasDelPeriodoYLasComparaConElAnterior() {
        dadoElInventario(yerba());
        dadoQueSeVendio(
                unaVenta(YERBA, 10, 3400, "2026-10-10"),
                unaVenta(YERBA, 5, 3400, "2026-10-12"),
                unaVenta(YERBA, 2, SIN_PRECIO, "2026-10-13"),
                unaVenta(YERBA, 5, 3400, "2026-09-10"),
                unaVenta(YERBA, 100, 3400, "2026-08-01"));

        cuandoSeArmaElResumen();

        entoncesLasVentasSuman("51000", "17", 3);
        entoncesElIngresoPorDiaEs("1700");
        entoncesLaVariacionContraElPeriodoAnteriorDe("17000", "200");
        entoncesLaGananciaEstimadaEs("10500");
    }

    @Test
    @DisplayName("El resumen dice de qué negocio y sistema es, de qué período y cuándo se armó")
    void elResumenDiceDeQueNegocioYPeriodoEs() {
        dadoElInventario(yerba());

        cuandoSeArmaElResumen();

        entoncesElResumenEsDe("Almacén de Ana", "Almacén de prueba", PERIODO, HOY);
    }

    @Test
    @DisplayName("Sin ventas en el período anterior no hay variación, y sin costos no hay ganancia estimada")
    void sinBaseNoHayVariacionNiGanancia() {
        dadoElInventario(fideos());
        dadoQueSeVendio(unaVenta(FIDEOS, 3, 1700, "2026-10-10"));

        cuandoSeArmaElResumen();

        entoncesNoHayVariacionNiGananciaEstimada();
        entoncesLosIngresosSon("5100");
    }

    @Test
    @DisplayName("Sin ventas todo queda en cero")
    void sinVentasTodoQuedaEnCero() {
        dadoElInventario(yerba());

        cuandoSeArmaElResumen();

        entoncesLasVentasSuman("0", "0", 0);
        entoncesNoHayMasVendidos();
        entoncesNoHayClientes();
    }

    // --- productos ---

    @Test
    @DisplayName("Ordena los más vendidos por unidades y, a igualdad, por ingresos, con la ganancia cuando hay costo")
    void ordenaLosMasVendidosPorUnidadesYLuegoPorIngresos() {
        dadoElInventario(yerba(), fideos(), mermelada());
        dadoQueSeVendio(
                unaVenta(YERBA, 5, 3400, "2026-10-10"),
                unaVenta(FIDEOS, 9, 1700, "2026-10-10"),
                unaVenta(YERBA, 4, 3400, "2026-10-11"),
                unaVenta(MERMELADA, 9, 2600, "2026-10-11"),
                unaVenta(UUID.randomUUID(), 1, 100, "2026-10-12"));

        cuandoSeArmaElResumen();

        entoncesLosMasVendidosSon("Yerba Nobleza Gaucha", "Mermelada BC", "Fideos Luchetti", "Producto eliminado");
        entoncesElMasVendidoVendioUnidades("9");
        entoncesElSegundoMasVendidoIngresoYGano("23400", "7200");
        entoncesElTerceroMasVendidoNoTieneGanancia();
    }

    @Test
    @DisplayName("Lista a lo sumo 10 productos")
    void listaALoSumoDiezProductos() {
        List<Registro> doce = productosNumerados(12, 5, null);
        dadoElInventario(doce);
        dadoQueSeVendio(ventasCrecientesDe(doce));

        cuandoSeArmaElResumen();

        entoncesHayMasVendidos(10);
        entoncesElMasVendidoEs("Producto 11");
    }

    @Test
    @DisplayName("Los productos con stock que no se vendieron quedan parados, con la plata que tienen inmovilizada")
    void losProductosConStockSinVentasQuedanParados() {
        dadoElInventario(yerba(), mermelada(), aceite(), fideos(),
                unProducto(null, 4, 0, 0, null), unProducto("  ", 2, 100, null, null));
        dadoQueSeVendio(unaVenta(YERBA, 1, 3400, "2026-10-10"));

        cuandoSeArmaElResumen();

        entoncesLosParadosSon("Mermelada BC", "Aceite Cocinero", "Producto sin nombre", "Producto sin nombre");
        entoncesElPrimerParadoTieneInmovilizado("36000");
        entoncesElSegundoParadoNoTieneCapitalConocido();
    }

    @Test
    @DisplayName("Lista a lo sumo 10 productos parados")
    void listaALoSumoDiezProductosParados() {
        dadoElInventario(productosNumerados(12, 5, null));

        cuandoSeArmaElResumen();

        entoncesHayParados(10);
    }

    // --- stock ---

    @Test
    @DisplayName("El stock lista los agotados y los bajo el mínimo, ordenados por cantidad")
    void elStockListaLosAgotadosYLosBajoElMinimo() {
        dadoUnInventarioConFaltantes();

        cuandoSeArmaElResumen();

        entoncesElStockTieneProductosYFaltantes(8, 5);
        entoncesLosFaltantesSon("Fideos Luchetti", "Agua", "Leche", "Aceite Cocinero", "Harina");
    }

    @Test
    @DisplayName("Dice cuánto pedir para llegar al doble del mínimo, y nada si no hay mínimo")
    void diceCuantoPedirParaLlegarAlDobleDelMinimo() {
        dadoUnInventarioConFaltantes();

        cuandoSeArmaElResumen();

        entoncesHayQuePedir(new BigDecimal("12"), null, null, new BigDecimal("9"), new BigDecimal("5"));
    }

    @Test
    @DisplayName("Valúa el inventario con stock a costo y a precio")
    void valuaElInventarioConStockACostoYAPrecio() {
        dadoUnInventarioConFaltantes();

        cuandoSeArmaElResumen();

        entoncesElInventarioVale("114250", "153900");
    }

    @Test
    @DisplayName("Pide al menos una unidad aunque ya haya más del doble del mínimo")
    void pideAlMenosUnaUnidad() {
        dadoElInventario(unProducto("Yerba", 5, 100, 50, 5), unProducto("Té", 10, 100, 50, 10));

        cuandoSeArmaElResumen();

        entoncesHayQuePedir(new BigDecimal("5"), new BigDecimal("10"));
    }

    @Test
    @DisplayName("Lista 15 faltantes y dice cuántos son en total")
    void listaQuinceFaltantesYDiceCuantosSonEnTotal() {
        dadoElInventario(productosNumerados(18, 0, 5));

        cuandoSeArmaElResumen();

        entoncesSeListanFaltantesDeUnTotalDe(15, 18);
    }

    // --- clientes y fiado ---

    @Test
    @DisplayName("Los clientes se agrupan sin importar tildes ni mayúsculas")
    void losClientesSeAgrupanSinImportarTildesNiMayusculas() {
        dadoElInventario(yerba(), fideos());
        dadoVentasADonaRosaYAlBar();

        cuandoSeArmaElResumen();

        entoncesLosMejoresClientesSon("Doña Rosa", "Bar La Esquina");
        entoncesElMejorClienteComproVecesPor(2, "6800");
    }

    @Test
    @DisplayName("El fiado suma lo vendido a crédito en el período")
    void elFiadoSumaLoVendidoACreditoEnElPeriodo() {
        dadoElInventario(yerba(), fideos());
        dadoVentasADonaRosaYAlBar();

        cuandoSeArmaElResumen();

        entoncesSeFio("6800", 2);
    }

    @Test
    @DisplayName("El cliente guardado como fila de la tabla de clientes se muestra con su nombre, nunca con el id")
    void elClienteGuardadoComoFilaSeMuestraConSuNombre() {
        dadoQueRosaEstaEnLaTablaDeClientes();
        dadoQueSeVendio(
                unaVentaFiadaA(YERBA, 1, 3400, "2026-10-10", ID_DE_ROSA),
                unaVentaA(YERBA, 2, 3400, "2026-10-11", ID_DE_ROSA),
                unaVentaA(YERBA, 1, 3400, "2026-10-12", ID_DESCONOCIDO),
                unaVentaA(YERBA, 1, 3400, "2026-10-12", "Bar La Esquina"));

        cuandoSeArmaElResumen();

        entoncesLosClientesSonEnCualquierOrden("Doña Rosa", "Bar La Esquina", "Cliente sin nombre");
        entoncesElMejorClienteEs("Doña Rosa", 2);
    }

    @Test
    @DisplayName("Las ventas a fiado sin cliente cargado igual cuentan como fiado")
    void lasVentasAFiadoSinClienteCuentanComoFiado() {
        dadoElInventario(yerba());
        dadoQueSeVendio(unaVentaFiadaA(YERBA, 1, 3400, "2026-10-10", null));

        cuandoSeArmaElResumen();

        entoncesNoHayMejoresClientes();
        entoncesHayVentasFiadas(1);
    }

    @Test
    @DisplayName("Lista a lo sumo 5 clientes")
    void listaALoSumoCincoClientes() {
        dadoElInventario(yerba());
        dadoQueSeVendio(ventasAClientesNumerados(7));

        cuandoSeArmaElResumen();

        entoncesHayMejoresClientes(5);
    }

    // --- gastos ---

    @Test
    @DisplayName("Sin tabla de gastos no hay sección de gastos")
    void sinTablaDeGastosNoHaySeccionDeGastos() {
        dadoElInventario(yerba());

        cuandoSeArmaElResumen();

        entoncesNoHayGastos();
    }

    @Test
    @DisplayName("Con una tabla de gastos sin importe no hay sección de gastos")
    void conUnaTablaDeGastosSinImporteNoHaySeccionDeGastos() {
        dadoLaTablaDeGastos(gastosSinImporte());

        cuandoSeArmaElResumen();

        entoncesNoHayGastos();
    }

    @Test
    @DisplayName("Suma los gastos del período y calcula el resultado contra los ingresos")
    void sumaLosGastosDelPeriodoYCalculaElResultado() {
        dadoGastosDeVariasCategorias();
        dadoQueSeVendio(unaVenta(YERBA, 10, 3400, "2026-10-10"));

        cuandoSeArmaElResumen();

        entoncesLosGastosSuman("46500", 5);
        entoncesElResultadoEs("-12500");
    }

    @Test
    @DisplayName("Agrupa los gastos por categoría, de la que más gastó a la que menos")
    void agrupaLosGastosPorCategoria() {
        dadoGastosDeVariasCategorias();

        cuandoSeArmaElResumen();

        entoncesLasCategoriasSon("Mercadería", "Servicios", "Sin categoría");
        entoncesLaPrimeraYLaUltimaCategoriaSuman("40000", "1500");
    }

    // --- zona ---

    @Test
    @DisplayName("Compara los precios con el rango de la zona")
    void comparaLosPreciosConElRangoDeLaZona() {
        dadoUnaZonaConRangosParaElAlmacen();

        cuandoSeArmaElResumen();

        entoncesLaZonaEs("San Justo", 8);
        entoncesQuedanDebajoDentroYEncima(1, 1, 1);
    }

    @Test
    @DisplayName("Lista los productos desalineados con cuánto se alejan del rango")
    void listaLosDesalineadosConCuantoSeAlejan() {
        dadoUnaZonaConRangosParaElAlmacen();

        cuandoSeArmaElResumen();

        entoncesLosDesalineadosSon("Fideos Luchetti", "Yerba Nobleza Gaucha");
        entoncesSeAlejan(new BigDecimal("13"), new BigDecimal("-3"));
    }

    @Test
    @DisplayName("Lista a lo sumo 5 productos desalineados, los más alejados primero")
    void listaALoSumoCincoDesalineados() {
        dadoElInventario(productosCarosNumerados(7));
        dadoLaZona(new AnalisisZona("San Justo", 8, rangosBaratosNumerados(7)));

        cuandoSeArmaElResumen();

        entoncesQuedanDebajoDentroYEncima(0, 0, 7);
        entoncesHayDesalineados(5);
        entoncesElMasAlejadoEs("Producto 6");
    }

    @Test
    @DisplayName("Sin zona no hay sección de zona")
    void sinZonaNoHaySeccionDeZona() {
        dadoElInventario(yerba());

        cuandoSeArmaElResumen();

        entoncesNoHayZona();
    }

    @Test
    @DisplayName("Con menos de 3 comercios en la zona no hay sección de zona")
    void conMenosDeTresComerciosNoHaySeccionDeZona() {
        dadoElInventario(yerba());
        dadoLaZona(new AnalisisZona("San Justo", 2, List.of(unRango("Yerba Nobleza Gaucha", "3500", "4800", 8))));

        cuandoSeArmaElResumen();

        entoncesNoHayZona();
    }

    @Test
    @DisplayName("Sin productos comparables con la zona no hay sección de zona")
    void sinProductosComparablesNoHaySeccionDeZona() {
        dadoElInventario(yerba());
        dadoLaZona(new AnalisisZona("San Justo", 8, List.of(unRango("Otra cosa", "1", "2", 8))));

        cuandoSeArmaElResumen();

        entoncesNoHayZona();
    }

    // --- avisos ---

    @Test
    @DisplayName("Cuenta los avisos pendientes por tipo")
    void cuentaLosAvisosPendientesPorTipo() {
        dadoAvisosDeTodosLosEstados();

        cuandoSeArmaElResumen();

        entoncesLosPendientesSon(Map.of(TipoAviso.STOCK, 2L, TipoAviso.COMBO, 1L));
        entoncesHayPendientesEnTotal(3);
    }

    @Test
    @DisplayName("Cuenta lo que se aplicó y descartó dentro del período, sin impacto medido")
    void cuentaLoAplicadoYDescartadoDentroDelPeriodo() {
        dadoAvisosDeTodosLosEstados();

        cuandoSeArmaElResumen();

        entoncesSeAplicaronYDescartaron(1, 1);
        entoncesSeMidieronConUnCambioDe(0, "0");
    }

    @Test
    @DisplayName("Suma el cambio de ingreso semanal de los avisos aplicados que ya se midieron")
    void sumaElCambioDeIngresoDeLosAvisosMedidos() {
        dadoLosAvisos(unAvisoAplicadoYMedido(), unAviso(TipoAviso.STOCK, EstadoAviso.APLICADO, AHORA.minusDays(4)));

        cuandoSeArmaElResumen();

        entoncesSeAplicaronYDescartaron(2, 0);
        entoncesSeMidieronConUnCambioDe(1, "3500");
    }

    // --- dado ---

    private void dadoElInventario(Registro... delInventario) {
        productos = List.of(delInventario);
    }

    private void dadoElInventario(List<Registro> delInventario) {
        productos = delInventario;
    }

    private void dadoUnInventarioConFaltantes() {
        dadoElInventario(yerba(), fideos(), aceite(),
                unProducto("Harina", 5, 1100, 750, 5),
                unProducto("Sal", null, 900, 600, 5),
                unProducto("Agua", 0, 1100, null, null),
                unProducto("Pan", 5, 800, 500, 0),
                unProducto("Leche", 0, 1650, 1250, 0));
    }

    private void dadoQueSeVendio(Registro... vendidas) {
        ventas = List.of(vendidas);
    }

    private void dadoQueSeVendio(List<Registro> vendidas) {
        ventas = vendidas;
    }

    private void dadoVentasADonaRosaYAlBar() {
        dadoQueSeVendio(
                unaVentaFiadaA(YERBA, 1, 3400, "2026-10-10", "Doña Rosa"),
                unaVentaA(FIDEOS, 2, 1700, "2026-10-11", "DOÑA ROSA"),
                unaVentaFiadaA(YERBA, 1, 3400, "2026-10-12", "Bar La Esquina"),
                unaVenta(YERBA, 1, 3400, "2026-10-12"),
                unaVentaFiadaA(YERBA, 4, 3400, "2026-08-12", "Club Social"));
    }

    private void dadoQueRosaEstaEnLaTablaDeClientes() {
        nombresDeClientes = Map.of(ID_DE_ROSA, "Doña Rosa");
    }

    private void dadoLaTablaDeGastos(BloqueGastos tabla) {
        gastos = List.of(tabla);
    }

    private void dadoGastosDeVariasCategorias() {
        dadoLaTablaDeGastos(gastosDe(List.of(
                unGasto("Proveedor", "2026-10-15", 32000, "Mercadería"),
                unGasto("Otro proveedor", "2026-10-16", 8000, "Mercadería"),
                unGasto("Luz", "2026-10-10", 5000, "Servicios"),
                unGasto("Sin categoría", "2026-10-11", 1000, null),
                unGasto("En blanco", "2026-10-11", 500, "  "),
                unGasto("Viejo", "2026-08-10", 99999, "Mercadería"),
                unGasto("Sin fecha", null, 99999, "Mercadería"),
                unGasto("Fecha rota", "ayer", 99999, "Mercadería"),
                unGasto("Sin importe", "2026-10-12", null, "Mercadería"))));
    }

    private void dadoLaZona(AnalisisZona deLaZona) {
        zona = deLaZona;
    }

    private void dadoUnaZonaConRangosParaElAlmacen() {
        dadoElInventario(yerba(), fideos(), mermelada(), aceite(), unProducto("Sin precio", 1, null, null, null));
        dadoLaZona(new AnalisisZona("San Justo", 8, List.<AgregadoZonaProducto>of(
                unRango("Yerba Nobleza Gaucha", "3500", "4800", 8),
                unRango("Fideos Luchetti", "1100", "1500", 7),
                unRango("Mermelada BC", "1700", "3000", 6),
                unRango("Aceite Cocinero", "2000", "3000", 2))));
    }

    private void dadoLosAvisos(Aviso... delProyecto) {
        avisos = List.of(delProyecto);
    }

    private void dadoAvisosDeTodosLosEstados() {
        dadoLosAvisos(
                unAvisoPendiente(TipoAviso.STOCK),
                unAvisoPendiente(TipoAviso.STOCK),
                unAvisoPendiente(TipoAviso.COMBO),
                unAviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO, AHORA.minusDays(3)),
                unAviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO, AHORA.minusDays(60)),
                unAviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO, null),
                unAviso(TipoAviso.STOCK, EstadoAviso.DESCARTADO, AHORA.minusDays(2)),
                unAviso(TipoAviso.STOCK, EstadoAviso.DESCARTADO, AHORA.minusDays(90)),
                unAviso(TipoAviso.STOCK, EstadoAviso.VENCIDO, null));
    }

    // --- cuando ---

    private void cuandoSeArmaElResumen() {
        DatosDeProyecto datos = new DatosDeProyecto("Almacén de prueba",
                List.of(AvisoTestData.inventarioConPrecios(productos.toArray(Registro[]::new))),
                List.of(ventasDe(ventas)), gastos, nombresDeClientes);
        resumen = ResumenNegocio.armar("Almacén de Ana", datos, zona, avisos, PERIODO, HOY);
    }

    // --- entonces: ventas ---

    private void entoncesElResumenEsDe(String negocio, String sistema, PeriodoResumen periodo, LocalDate emitidoEl) {
        assertThat(resumen.negocio()).isEqualTo(negocio);
        assertThat(resumen.sistema()).isEqualTo(sistema);
        assertThat(resumen.periodo()).isEqualTo(periodo);
        assertThat(resumen.emitidoEl()).isEqualTo(emitidoEl);
    }

    private void entoncesLasVentasSuman(String ingresos, String unidades, long cantidad) {
        assertThat(resumen.ventas().ingresos()).isEqualByComparingTo(ingresos);
        assertThat(resumen.ventas().unidades()).isEqualByComparingTo(unidades);
        assertThat(resumen.ventas().cantidad()).isEqualTo(cantidad);
    }

    private void entoncesLosIngresosSon(String ingresos) {
        assertThat(resumen.ventas().ingresos()).isEqualByComparingTo(ingresos);
    }

    private void entoncesElIngresoPorDiaEs(String porDia) {
        assertThat(resumen.ventas().ingresoPorDia()).isEqualByComparingTo(porDia);
    }

    private void entoncesLaVariacionContraElPeriodoAnteriorDe(String anteriores, String variacion) {
        assertThat(resumen.ventas().ingresosAnteriores()).isEqualByComparingTo(anteriores);
        assertThat(resumen.ventas().variacion()).isEqualByComparingTo(variacion);
    }

    private void entoncesLaGananciaEstimadaEs(String ganancia) {
        assertThat(resumen.ventas().gananciaEstimada()).isEqualByComparingTo(ganancia);
    }

    private void entoncesNoHayVariacionNiGananciaEstimada() {
        assertThat(resumen.ventas().variacion()).isNull();
        assertThat(resumen.ventas().gananciaEstimada()).isNull();
    }

    // --- entonces: productos ---

    private void entoncesNoHayMasVendidos() {
        assertThat(resumen.masVendidos()).isEmpty();
    }

    private void entoncesHayMasVendidos(int cantidad) {
        assertThat(resumen.masVendidos()).hasSize(cantidad);
    }

    private void entoncesLosMasVendidosSon(String... nombres) {
        assertThat(resumen.masVendidos()).extracting(ProductoVendido::nombre).containsExactly(nombres);
    }

    private void entoncesElMasVendidoEs(String nombre) {
        assertThat(resumen.masVendidos().getFirst().nombre()).isEqualTo(nombre);
    }

    private void entoncesElMasVendidoVendioUnidades(String unidades) {
        assertThat(resumen.masVendidos().getFirst().unidades()).isEqualByComparingTo(unidades);
    }

    private void entoncesElSegundoMasVendidoIngresoYGano(String ingresos, String ganancia) {
        assertThat(resumen.masVendidos().get(1).ingresos()).isEqualByComparingTo(ingresos);
        assertThat(resumen.masVendidos().get(1).ganancia()).isEqualByComparingTo(ganancia);
    }

    private void entoncesElTerceroMasVendidoNoTieneGanancia() {
        assertThat(resumen.masVendidos().get(2).ganancia()).isNull();
    }

    private void entoncesLosParadosSon(String... nombres) {
        assertThat(resumen.sinMovimiento()).extracting(ProductoParado::nombre).containsExactly(nombres);
    }

    private void entoncesHayParados(int cantidad) {
        assertThat(resumen.sinMovimiento()).hasSize(cantidad);
    }

    private void entoncesElPrimerParadoTieneInmovilizado(String capital) {
        assertThat(resumen.sinMovimiento().getFirst().capital()).isEqualByComparingTo(capital);
    }

    private void entoncesElSegundoParadoNoTieneCapitalConocido() {
        assertThat(resumen.sinMovimiento().get(1).capital()).isNull();
    }

    // --- entonces: stock ---

    private void entoncesElStockTieneProductosYFaltantes(long productos, long faltantes) {
        assertThat(resumen.stock().productos()).isEqualTo(productos);
        assertThat(resumen.stock().totalFaltantes()).isEqualTo(faltantes);
    }

    private void entoncesLosFaltantesSon(String... nombres) {
        assertThat(resumen.stock().faltantes()).extracting(Faltante::nombre).containsExactly(nombres);
    }

    private void entoncesHayQuePedir(BigDecimal... cantidades) {
        assertThat(resumen.stock().faltantes()).extracting(Faltante::pedir)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(cantidades);
    }

    private void entoncesElInventarioVale(String aCosto, String aPrecio) {
        assertThat(resumen.stock().valorACosto()).isEqualByComparingTo(aCosto);
        assertThat(resumen.stock().valorAPrecio()).isEqualByComparingTo(aPrecio);
    }

    private void entoncesSeListanFaltantesDeUnTotalDe(int listados, long total) {
        assertThat(resumen.stock().faltantes()).hasSize(listados);
        assertThat(resumen.stock().totalFaltantes()).isEqualTo(total);
    }

    // --- entonces: clientes ---

    private void entoncesNoHayClientes() {
        assertThat(resumen.clientes().vacio()).isTrue();
    }

    private void entoncesNoHayMejoresClientes() {
        assertThat(resumen.clientes().top()).isEmpty();
    }

    private void entoncesHayMejoresClientes(int cantidad) {
        assertThat(resumen.clientes().top()).hasSize(cantidad);
    }

    private void entoncesLosMejoresClientesSon(String... nombres) {
        assertThat(resumen.clientes().top()).extracting(ClienteTop::nombre).containsExactly(nombres);
    }

    private void entoncesLosClientesSonEnCualquierOrden(String... nombres) {
        assertThat(resumen.clientes().top()).extracting(ClienteTop::nombre).containsExactlyInAnyOrder(nombres);
    }

    private void entoncesElMejorClienteEs(String nombre, long compras) {
        assertThat(resumen.clientes().top().getFirst().nombre()).isEqualTo(nombre);
        assertThat(resumen.clientes().top().getFirst().compras()).isEqualTo(compras);
    }

    private void entoncesElMejorClienteComproVecesPor(long compras, String ingresos) {
        assertThat(resumen.clientes().top().getFirst().compras()).isEqualTo(compras);
        assertThat(resumen.clientes().top().getFirst().ingresos()).isEqualByComparingTo(ingresos);
    }

    private void entoncesSeFio(String fiado, long ventasFiadas) {
        assertThat(resumen.clientes().fiado()).isEqualByComparingTo(fiado);
        assertThat(resumen.clientes().ventasFiadas()).isEqualTo(ventasFiadas);
        assertThat(resumen.clientes().vacio()).isFalse();
    }

    private void entoncesHayVentasFiadas(long ventasFiadas) {
        assertThat(resumen.clientes().ventasFiadas()).isEqualTo(ventasFiadas);
        assertThat(resumen.clientes().vacio()).isFalse();
    }

    // --- entonces: gastos ---

    private void entoncesNoHayGastos() {
        assertThat(resumen.gastos()).isNull();
    }

    private void entoncesLosGastosSuman(String total, long cantidad) {
        assertThat(resumen.gastos().total()).isEqualByComparingTo(total);
        assertThat(resumen.gastos().cantidad()).isEqualTo(cantidad);
    }

    private void entoncesElResultadoEs(String resultado) {
        assertThat(resumen.gastos().resultado()).isEqualByComparingTo(resultado);
    }

    private void entoncesLasCategoriasSon(String... categorias) {
        assertThat(resumen.gastos().porCategoria()).extracting(GastoPorCategoria::categoria)
                .containsExactly(categorias);
    }

    private void entoncesLaPrimeraYLaUltimaCategoriaSuman(String primera, String ultima) {
        assertThat(resumen.gastos().porCategoria().getFirst().total()).isEqualByComparingTo(primera);
        assertThat(resumen.gastos().porCategoria().getLast().total()).isEqualByComparingTo(ultima);
    }

    // --- entonces: zona ---

    private void entoncesNoHayZona() {
        assertThat(resumen.zona()).isNull();
    }

    private void entoncesLaZonaEs(String nombre, long comercios) {
        assertThat(resumen.zona().nombre()).isEqualTo(nombre);
        assertThat(resumen.zona().comercios()).isEqualTo(comercios);
    }

    private void entoncesQuedanDebajoDentroYEncima(long debajo, long dentro, long encima) {
        assertThat(resumen.zona().debajo()).isEqualTo(debajo);
        assertThat(resumen.zona().dentro()).isEqualTo(dentro);
        assertThat(resumen.zona().encima()).isEqualTo(encima);
    }

    private void entoncesLosDesalineadosSon(String... nombres) {
        assertThat(resumen.zona().desalineados()).extracting(ProductoDesalineado::nombre).containsExactly(nombres);
    }

    private void entoncesSeAlejan(BigDecimal... porcentajes) {
        assertThat(resumen.zona().desalineados()).extracting(ProductoDesalineado::porcentaje)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(porcentajes);
    }

    private void entoncesHayDesalineados(int cantidad) {
        assertThat(resumen.zona().desalineados()).hasSize(cantidad);
    }

    private void entoncesElMasAlejadoEs(String nombre) {
        assertThat(resumen.zona().desalineados().getFirst().nombre()).isEqualTo(nombre);
    }

    // --- entonces: avisos ---

    private void entoncesLosPendientesSon(Map<TipoAviso, Long> pendientes) {
        assertThat(resumen.avisos().pendientes()).containsExactlyInAnyOrderEntriesOf(pendientes);
    }

    private void entoncesHayPendientesEnTotal(long total) {
        assertThat(resumen.avisos().totalPendientes()).isEqualTo(total);
    }

    private void entoncesSeAplicaronYDescartaron(long aplicados, long descartados) {
        assertThat(resumen.avisos().aplicados()).isEqualTo(aplicados);
        assertThat(resumen.avisos().descartados()).isEqualTo(descartados);
    }

    private void entoncesSeMidieronConUnCambioDe(long medidos, String ingresoSemanal) {
        assertThat(resumen.avisos().medidos()).isEqualTo(medidos);
        assertThat(resumen.avisos().ingresoSemanalDelta()).isEqualByComparingTo(ingresoSemanal);
    }
}
