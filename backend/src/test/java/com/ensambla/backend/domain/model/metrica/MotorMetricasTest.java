package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.metrica.ResultadoMetrica.Fila;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.CATEGORIA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.CLIENTE;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.DIA_DE_LA_SEMANA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.NINGUNA;
import static com.ensambla.backend.domain.model.metrica.AgrupacionMetrica.PRODUCTO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.CONTEO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.PROMEDIO;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.SUMA;
import static com.ensambla.backend.domain.model.metrica.CalculoMetrica.UNIDADES;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.GASTOS;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.INVENTARIO;
import static com.ensambla.backend.domain.model.metrica.FuenteMetrica.VENTAS;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.MES;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.SEMANA;
import static com.ensambla.backend.domain.model.metrica.PeriodoMetrica.TODO;
import static com.ensambla.backend.support.MetricaTestData.HOY;
import static com.ensambla.backend.support.MetricaTestData.PAN_ID;
import static com.ensambla.backend.support.MetricaTestData.SAL_ID;
import static com.ensambla.backend.support.MetricaTestData.datos;
import static com.ensambla.backend.support.MetricaTestData.definicion;
import static com.ensambla.backend.support.MetricaTestData.gasto;
import static com.ensambla.backend.support.MetricaTestData.gastos;
import static com.ensambla.backend.support.MetricaTestData.gastosSinImporte;
import static com.ensambla.backend.support.MetricaTestData.inventario;
import static com.ensambla.backend.support.MetricaTestData.unAlmacenConDoceClientes;
import static com.ensambla.backend.support.MetricaTestData.unAlmacenConVentasYGastos;
import static com.ensambla.backend.support.MetricaTestData.unAlmacenQueVendio;
import static com.ensambla.backend.support.MetricaTestData.unCuadernoConGastos;
import static com.ensambla.backend.support.MetricaTestData.unCuadernoVacio;
import static com.ensambla.backend.support.MetricaTestData.unInventarioSinCantidades;
import static com.ensambla.backend.support.MetricaTestData.unInventarioSinCostos;
import static com.ensambla.backend.support.MetricaTestData.venta;
import static com.ensambla.backend.support.MetricaTestData.ventas;
import static org.assertj.core.api.Assertions.assertThat;

class MotorMetricasTest {

    private final MotorMetricas motor = new MotorMetricas();

    private DatosDeProyecto cuaderno;

    private ResultadoMetrica resultado;

    @Test
    @DisplayName("Suma lo vendido del mes en pesos y cuenta aparte las ventas sin precio")
    void sumaLoVendidoDelMesYCuentaAparteLasVentasSinPrecio() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(VENTAS, SUMA, NINGUNA, MES));

        entoncesDaEnPesos("1000");
        entoncesNoSeReparte();
        entoncesSaleDe(3, 1);
    }

    @Test
    @DisplayName("Mira sólo los últimos 7 días en una semana y dice desde y hasta cuándo")
    void miraSoloLosUltimosSieteDiasEnUnaSemana() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(VENTAS, SUMA, NINGUNA, SEMANA));

        entoncesDa("550");
        entoncesMiraDesdeHasta(LocalDate.of(2026, 11, 5), HOY);
    }

    @Test
    @DisplayName("Mira sólo lo de hoy cuando el período es hoy")
    void miraSoloLoDeHoyCuandoElPeriodoEsHoy() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(VENTAS, SUMA, NINGUNA, PeriodoMetrica.HOY));

        entoncesDa("300");
    }

    @Test
    @DisplayName("Mira todo lo cargado sin fechas cuando el período es todo")
    void miraTodoLoCargadoCuandoElPeriodoEsTodo() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(VENTAS, SUMA, NINGUNA, TODO));

        entoncesDa("1150");
        entoncesMiraDesdeHasta(null, null);
    }

    @Test
    @DisplayName("Promedia el importe de las ventas que tienen precio")
    void promediaElImporteDeLasVentasConPrecio() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(VENTAS, PROMEDIO, NINGUNA, MES));

        entoncesDa("333.33");
    }

    @Test
    @DisplayName("Cuenta todas las ventas, también las que no tienen precio")
    void cuentaTodasLasVentasTambienLasSinPrecio() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(VENTAS, CONTEO, NINGUNA, MES));

        entoncesDaEnCantidad("4");
        entoncesSaleDe(4, 0);
    }

    @Test
    @DisplayName("Suma las unidades vendidas")
    void sumaLasUnidadesVendidas() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(VENTAS, UNIDADES, NINGUNA, MES));

        entoncesDa("7");
    }

    @Test
    @DisplayName("Reparte por producto de mayor a menor con el nombre del inventario")
    void repartePorProductoDeMayorAMenor() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(VENTAS, SUMA, PRODUCTO, MES));

        entoncesSeReparteEn(Map.of("Pan", "750", "Leche", "250"), "Pan", "Leche");
    }

    @Test
    @DisplayName("Reparte por cliente y pone 'Sin cliente' a las ventas sin cliente")
    void repartePorClienteYNombraALasVentasSinCliente() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(VENTAS, SUMA, CLIENTE, MES));

        entoncesLasPartesSon("Sin cliente", "Ana", "Beto");
    }

    @Test
    @DisplayName("Muestra el nombre del cliente cuando la venta guarda la conexión a la tabla de clientes")
    void muestraElNombreDelClienteDeLaTablaDeClientes() {
        dadoUnaVentaAlClienteConectado("id-ana", "Ana García");

        cuandoSeCalcula(definicion(VENTAS, SUMA, CLIENTE, MES));

        entoncesLasPartesSon("Ana García");
    }

    @Test
    @DisplayName("Reparte por día de la semana en orden de lunes a domingo")
    void repartePorDiaDeLaSemanaDeLunesADomingo() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(VENTAS, SUMA, DIA_DE_LA_SEMANA, MES));

        entoncesSeReparteEn(Map.of("Martes", "250", "Miércoles", "750"), "Martes", "Miércoles");
    }

    @Test
    @DisplayName("Con 'lo que te deben' suma sólo las ventas fiadas")
    void conLoQueTeDebenSumaSoloLasVentasFiadas() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(VENTAS, SUMA, CLIENTE, TODO, true));

        entoncesDa("450");
        entoncesSeReparteEn(Map.of("Ana", "450"), "Ana");
    }

    @Test
    @DisplayName("Muestra como mucho 10 partes del reparto, las más grandes primero")
    void muestraComoMuchoDiezPartesDelReparto() {
        dadoUnAlmacenConDoceClientes();

        cuandoSeCalcula(definicion(VENTAS, SUMA, CLIENTE, MES));

        entoncesSeMuestranPartes(MotorMetricas.MAXIMO_DE_FILAS);
        entoncesLaParteMasGrandeEs("cliente 12");
    }

    @Test
    @DisplayName("Ignora las ventas que no se pueden leer")
    void ignoraLasVentasQueNoSePuedenLeer() {
        dadoUnaVentaIlegibleYUnaDeSal();

        cuandoSeCalcula(definicion(VENTAS, CONTEO, PRODUCTO, MES));

        entoncesDa("1");
        entoncesLasPartesSon("Sal");
    }

    @Test
    @DisplayName("Suma los gastos del mes, ignora los que no tienen importe y los cuenta aparte")
    void sumaLosGastosDelMesYCuentaAparteLosSinImporte() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(GASTOS, SUMA, NINGUNA, MES));

        entoncesDa("1200");
        entoncesSaleDe(2, 2);
    }

    @Test
    @DisplayName("Reparte los gastos por categoría y pone 'Sin categoría' al que no la tiene")
    void reparteLosGastosPorCategoria() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(GASTOS, SUMA, CATEGORIA, TODO));

        entoncesSeReparteEn(Map.of("Alquiler", "1000", "Sin categoría", "200", "Luz", "75"),
                "Alquiler", "Sin categoría", "Luz");
    }

    @Test
    @DisplayName("Reparte los gastos por día y deja al final los que no tienen fecha")
    void reparteLosGastosPorDiaYDejaAlFinalLosSinFecha() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(GASTOS, SUMA, DIA_DE_LA_SEMANA, TODO));

        entoncesLasPartesSon("Martes", "Miércoles", "Sin fecha");
    }

    @Test
    @DisplayName("Si hay una tabla de gastos sin importe y otra con importe usa sólo la que tiene importe")
    void usaSoloLaTablaDeGastosConImporte() {
        dadoUnaTablaDeGastosSinImporteYOtraConUnGastoDeLuz();

        cuandoSeCalcula(definicion(GASTOS, SUMA, NINGUNA, MES));

        entoncesDa("300");
    }

    @Test
    @DisplayName("Suma lo que vale el stock y deja afuera el producto sin costo")
    void sumaLoQueValeElStockSinElProductoSinCosto() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(INVENTARIO, SUMA, NINGUNA, TODO));

        entoncesDa("2000");
        entoncesSaleDe(2, 1);
    }

    @Test
    @DisplayName("Suma las unidades en stock repartidas por producto")
    void sumaLasUnidadesEnStockPorProducto() {
        dadoUnAlmacenConVentasYGastos();

        cuandoSeCalcula(definicion(INVENTARIO, UNIDADES, PRODUCTO, TODO));

        entoncesDa("19");
        entoncesLasPartesSon("Pan", "Leche", "Sal");
    }

    @Test
    @DisplayName("Si el cuaderno no tiene tabla de ventas lo dice y no inventa un cero")
    void sinTablaDeVentasLoDiceYNoInventaUnCero() {
        dadoUnCuadernoVacio();

        cuandoSeCalcula(definicion(VENTAS, SUMA, NINGUNA, MES));

        entoncesNoSePuedeCalcularPorque("No encontramos la tabla de ventas en este cuaderno.");
    }

    @Test
    @DisplayName("Si la tabla de gastos no tiene importe es como si no hubiera tabla")
    void unaTablaDeGastosSinImporteEsComoSiNoHubieraTabla() {
        dadoUnaTablaDeGastosSinImporte();

        cuandoSeCalcula(definicion(GASTOS, SUMA, NINGUNA, MES));

        entoncesNoSePuedeCalcularPorque("No encontramos la tabla de gastos en este cuaderno.");
    }

    @Test
    @DisplayName("Si no hay inventario lo dice")
    void sinInventarioLoDice() {
        dadoUnCuadernoVacio();

        cuandoSeCalcula(definicion(INVENTARIO, SUMA, NINGUNA, TODO));

        entoncesNoSePuedeCalcularPorque("No encontramos la tabla de inventario en este cuaderno.");
    }

    @Test
    @DisplayName("Si no hay filas en el período lo dice")
    void sinFilasEnElPeriodoLoDice() {
        dadaUnaVentaDeSeptiembre();

        cuandoSeCalcula(definicion(VENTAS, SUMA, NINGUNA, SEMANA));

        entoncesNoSePuedeCalcularPorque("No hay ventas en este período.");
        entoncesSaleDe(0, 0);
    }

    @Test
    @DisplayName("Si no hay ventas fiadas lo dice")
    void sinVentasFiadasLoDice() {
        dadaUnaVentaAlContado();

        cuandoSeCalcula(definicion(VENTAS, SUMA, CLIENTE, MES, true));

        entoncesNoSePuedeCalcularPorque("No hay ventas fiadas en este período.");
    }

    @Test
    @DisplayName("Si la métrica mira todo y no hay filas dice que todavía no cargó nada")
    void siMiraTodoYNoHayFilasDiceQueNoCargoNada() {
        dadoUnAlmacenSinVentas();

        cuandoSeCalcula(definicion(VENTAS, SUMA, NINGUNA, TODO));

        entoncesNoSePuedeCalcularPorque("Todavía no cargaste ventas.");
    }

    @Test
    @DisplayName("Si ninguna venta tiene precio pide cargarlo")
    void siNingunaVentaTienePrecioPideCargarlo() {
        dadaUnaVentaSinPrecio();

        cuandoSeCalcula(definicion(VENTAS, SUMA, NINGUNA, MES));

        entoncesNoSePuedeCalcularPorque("Cargá el precio de tus ventas para poder calcularlo.");
        entoncesSaleDe(0, 1);
    }

    @Test
    @DisplayName("Si ningún gasto tiene importe pide cargarlo")
    void siNingunGastoTieneImportePideCargarlo() {
        dadoUnGastoSinImporte();

        cuandoSeCalcula(definicion(GASTOS, SUMA, NINGUNA, MES));

        entoncesNoSePuedeCalcularPorque("Cargá el importe de tus gastos para poder calcularlo.");
    }

    @Test
    @DisplayName("Si ningún producto tiene costo pide cargarlo")
    void siNingunProductoTieneCostoPideCargarlo() {
        dadoUnInventarioSinCostos();

        cuandoSeCalcula(definicion(INVENTARIO, SUMA, NINGUNA, TODO));

        entoncesNoSePuedeCalcularPorque("Cargá el costo de tus productos para poder calcularlo.");
    }

    @Test
    @DisplayName("Si ningún producto tiene cantidad pide cargarla")
    void siNingunProductoTieneCantidadPideCargarla() {
        dadoUnInventarioSinCantidades();

        cuandoSeCalcula(definicion(INVENTARIO, UNIDADES, NINGUNA, TODO));

        entoncesNoSePuedeCalcularPorque("Cargá la cantidad de tus productos para poder calcularlo.");
    }

    private void dadoUnAlmacenConVentasYGastos() {
        cuaderno = unAlmacenConVentasYGastos();
    }

    private void dadoUnAlmacenConDoceClientes() {
        cuaderno = unAlmacenConDoceClientes();
    }

    private void dadoUnCuadernoVacio() {
        cuaderno = unCuadernoVacio();
    }

    private void dadoUnAlmacenSinVentas() {
        cuaderno = unAlmacenQueVendio();
    }

    private void dadoUnaVentaAlClienteConectado(String clienteId, String nombre) {
        cuaderno = datos(List.of(inventario()), List.of(ventas(venta(PAN_ID, 1, 100, "2026-11-10", clienteId, false))),
                List.of(), Map.of(clienteId, nombre));
    }

    private void dadoUnaVentaIlegibleYUnaDeSal() {
        cuaderno = unAlmacenQueVendio(venta(null, 1, 100, "2026-11-10", null, false),
                venta(SAL_ID, 1, 100, "2026-11-10", null, null));
    }

    private void dadaUnaVentaDeSeptiembre() {
        cuaderno = unAlmacenQueVendio(venta(PAN_ID, 1, 100, "2026-09-01", null, false));
    }

    private void dadaUnaVentaAlContado() {
        cuaderno = unAlmacenQueVendio(venta(PAN_ID, 1, 100, "2026-11-10", null, false));
    }

    private void dadaUnaVentaSinPrecio() {
        cuaderno = unAlmacenQueVendio(venta(PAN_ID, 1, null, "2026-11-10", null, false));
    }

    private void dadoUnaTablaDeGastosSinImporte() {
        cuaderno = unCuadernoConGastos(gastosSinImporte());
    }

    private void dadoUnaTablaDeGastosSinImporteYOtraConUnGastoDeLuz() {
        cuaderno = unCuadernoConGastos(gastosSinImporte(), gastos(gasto(300, "2026-11-10", "Luz")));
    }

    private void dadoUnGastoSinImporte() {
        cuaderno = unCuadernoConGastos(gastos(gasto(null, "2026-11-10", "Luz")));
    }

    private void dadoUnInventarioSinCostos() {
        cuaderno = unInventarioSinCostos();
    }

    private void dadoUnInventarioSinCantidades() {
        cuaderno = unInventarioSinCantidades();
    }

    private void cuandoSeCalcula(DefinicionMetrica definicion) {
        resultado = motor.calcular(definicion, cuaderno, HOY);
    }

    private void entoncesDa(String valor) {
        assertThat(resultado.calculable()).isTrue();
        assertThat(resultado.valor()).isEqualByComparingTo(valor);
    }

    private void entoncesDaEnPesos(String valor) {
        entoncesDa(valor);
        assertThat(resultado.formato()).isEqualTo(FormatoMetrica.MONEDA);
    }

    private void entoncesDaEnCantidad(String valor) {
        entoncesDa(valor);
        assertThat(resultado.formato()).isEqualTo(FormatoMetrica.NUMERO);
    }

    private void entoncesNoSePuedeCalcularPorque(String motivo) {
        assertThat(resultado.calculable()).isFalse();
        assertThat(resultado.valor()).isNull();
        assertThat(resultado.motivo()).isEqualTo(motivo);
    }

    private void entoncesNoSeReparte() {
        assertThat(resultado.filas()).isEmpty();
    }

    private void entoncesLasPartesSon(String... etiquetas) {
        assertThat(resultado.filas()).extracting(Fila::etiqueta).containsExactly(etiquetas);
    }

    private void entoncesSeReparteEn(Map<String, String> valores, String... enEsteOrden) {
        entoncesLasPartesSon(enEsteOrden);
        assertThat(resultado.filas()).allSatisfy(
                fila -> assertThat(fila.valor()).isEqualByComparingTo(valores.get(fila.etiqueta())));
    }

    private void entoncesSeMuestranPartes(int cantidad) {
        assertThat(resultado.filas()).hasSize(cantidad);
    }

    private void entoncesLaParteMasGrandeEs(String etiqueta) {
        assertThat(resultado.filas().getFirst().etiqueta()).isEqualTo(etiqueta);
    }

    private void entoncesSaleDe(long filasUsadas, long filasSinDato) {
        assertThat(resultado.deDondeSale().filasUsadas()).isEqualTo(filasUsadas);
        assertThat(resultado.deDondeSale().filasSinDato()).isEqualTo(filasSinDato);
    }

    private void entoncesMiraDesdeHasta(LocalDate desde, LocalDate hasta) {
        assertThat(resultado.deDondeSale().desde()).isEqualTo(desde);
        assertThat(resultado.deDondeSale().hasta()).isEqualTo(hasta);
    }
}
