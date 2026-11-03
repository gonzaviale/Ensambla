package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoVendido;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResumenNegocioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final PeriodoResumen PERIODO = PeriodoResumen.armar(null, null, HOY);
    private static final UUID YERBA = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID FIDEOS = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID MERMELADA = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    private static final UUID ACEITE = UUID.fromString("d0000000-0000-4000-8000-000000000004");
    private static final CampoId CAMPO_FIADO_ID = CampoId.de("f0000000-0000-4000-8000-000000000016");
    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000004");
    private static final CampoId GASTO_NOMBRE_ID = CampoId.de("f0000000-0000-4000-8000-000000000021");
    private static final CampoId GASTO_FECHA_ID = CampoId.de("f0000000-0000-4000-8000-000000000022");
    private static final CampoId GASTO_TOTAL_ID = CampoId.de("f0000000-0000-4000-8000-000000000023");
    private static final CampoId GASTO_CATEGORIA_ID = CampoId.de("f0000000-0000-4000-8000-000000000024");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T15:00:00Z");

    // --- ventas ---

    @Test
    @DisplayName("Suma ingresos, unidades y ventas del período, y la variación contra el período anterior")
    void armarShouldSummarizeSalesAndCompareWithPreviousPeriod() {
        List<Registro> ventas = List.of(
                venta(YERBA, 10, 3400, "2026-10-10", null, false),
                venta(YERBA, 5, 3400, "2026-10-12", null, false),
                venta(YERBA, 2, null, "2026-10-13", null, false),
                venta(YERBA, 5, 3400, "2026-09-10", null, false),
                venta(YERBA, 100, 3400, "2026-08-01", null, false));

        ResumenNegocio resumen = armar(ventas, yerba());

        assertThat(resumen.ventas().ingresos()).isEqualByComparingTo("51000");
        assertThat(resumen.ventas().unidades()).isEqualByComparingTo("17");
        assertThat(resumen.ventas().cantidad()).isEqualTo(3);
        assertThat(resumen.ventas().ingresoPorDia()).isEqualByComparingTo("1700");
        assertThat(resumen.ventas().ingresosAnteriores()).isEqualByComparingTo("17000");
        assertThat(resumen.ventas().variacion()).isEqualByComparingTo("200");
        assertThat(resumen.ventas().gananciaEstimada()).isEqualByComparingTo("10500");
        assertThat(resumen.negocio()).isEqualTo("Almacén de Ana");
        assertThat(resumen.sistema()).isEqualTo("Almacén de prueba");
        assertThat(resumen.periodo()).isEqualTo(PERIODO);
        assertThat(resumen.emitidoEl()).isEqualTo(HOY);
    }

    @Test
    @DisplayName("Sin ventas en el período anterior no hay variación, y sin costos no hay ganancia estimada")
    void armarShouldLeaveVariationAndProfitEmptyWhenThereIsNoBase() {
        ResumenNegocio resumen = armar(List.of(venta(FIDEOS, 3, 1700, "2026-10-10", null, false)), fideos());

        assertThat(resumen.ventas().variacion()).isNull();
        assertThat(resumen.ventas().gananciaEstimada()).isNull();
        assertThat(resumen.ventas().ingresos()).isEqualByComparingTo("5100");
    }

    @Test
    @DisplayName("Sin ventas todo queda en cero")
    void armarShouldReturnZerosWithoutSales() {
        ResumenNegocio resumen = armar(List.of(), yerba());

        assertThat(resumen.ventas().ingresos()).isEqualByComparingTo("0");
        assertThat(resumen.ventas().cantidad()).isZero();
        assertThat(resumen.masVendidos()).isEmpty();
        assertThat(resumen.clientes().vacio()).isTrue();
    }

    // --- productos ---

    @Test
    @DisplayName("Ordena los más vendidos por unidades y, a igualdad, por ingresos, con la ganancia y ganancia cuando hay costo")
    void armarShouldRankBestSellers() {
        List<Registro> ventas = List.of(
                venta(YERBA, 5, 3400, "2026-10-10", null, false),
                venta(FIDEOS, 9, 1700, "2026-10-10", null, false),
                venta(YERBA, 4, 3400, "2026-10-11", null, false),
                venta(MERMELADA, 9, 2600, "2026-10-11", null, false),
                venta(UUID.randomUUID(), 1, 100, "2026-10-12", null, false));

        List<ProductoVendido> masVendidos = armar(ventas, yerba(), fideos(), mermelada()).masVendidos();

        assertThat(masVendidos).extracting(ProductoVendido::nombre).containsExactly(
                "Yerba Nobleza Gaucha", "Mermelada BC", "Fideos Luchetti", "Producto eliminado");
        assertThat(masVendidos.get(0).unidades()).isEqualByComparingTo("9");
        assertThat(masVendidos.get(1).ingresos()).isEqualByComparingTo("23400");
        assertThat(masVendidos.get(1).ganancia()).isEqualByComparingTo("7200");
        assertThat(masVendidos.get(2).ganancia()).isNull();
    }

    @Test
    @DisplayName("Lista a lo sumo 10 productos")
    void armarShouldLimitBestSellersToTen() {
        List<Registro> productos = new ArrayList<>();
        List<Registro> ventas = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            UUID id = UUID.randomUUID();
            productos.add(producto(id, "Producto " + i, 5, 100, 50, null));
            ventas.add(venta(id, i + 1, 100, "2026-10-10", null, false));
        }

        ResumenNegocio resumen = armar(ventas, productos.toArray(new Registro[0]));

        assertThat(resumen.masVendidos()).hasSize(10);
        assertThat(resumen.masVendidos().getFirst().nombre()).isEqualTo("Producto 11");
    }

    @Test
    @DisplayName("Los productos con stock que no se vendieron quedan parados, con la plata que tienen inmovilizada")
    void armarShouldListProductsWithStockAndNoSales() {
        List<Registro> ventas = List.of(venta(YERBA, 1, 3400, "2026-10-10", null, false));

        ResumenNegocio resumen = armar(ventas, yerba(), mermelada(), aceite(), fideos(),
                producto(UUID.randomUUID(), null, 4, 0, 0, null),
                producto(UUID.randomUUID(), "  ", 2, 100, null, null));

        assertThat(resumen.sinMovimiento()).extracting(ResumenNegocio.ProductoParado::nombre)
                .containsExactly("Mermelada BC", "Aceite Cocinero", "Producto sin nombre", "Producto sin nombre");
        assertThat(resumen.sinMovimiento().getFirst().capital()).isEqualByComparingTo("36000");
        assertThat(resumen.sinMovimiento().get(1).capital()).isNull();
    }

    @Test
    @DisplayName("Lista a lo sumo 10 productos parados")
    void armarShouldLimitStoppedProductsToTen() {
        List<Registro> productos = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            productos.add(producto(UUID.randomUUID(), "Producto " + i, 5, 100, 50, null));
        }

        assertThat(armar(List.of(), productos.toArray(new Registro[0])).sinMovimiento()).hasSize(10);
    }

    // --- stock ---

    @Test
    @DisplayName("El stock lista los agotados y los bajo el mínimo, cuánto pedir y lo que vale el inventario")
    void armarShouldReportMissingStockAndInventoryValue() {
        Registro enElMinimo = producto(UUID.randomUUID(), "Harina", 5, 1100, 750, 5);
        Registro sinCantidad = producto(UUID.randomUUID(), "Sal", null, 900, 600, 5);
        Registro sinMinimo = producto(UUID.randomUUID(), "Agua", 0, 1100, null, null);
        Registro minimoEnCero = producto(UUID.randomUUID(), "Pan", 5, 800, 500, 0);
        Registro agotadoConMinimoEnCero = producto(UUID.randomUUID(), "Leche", 0, 1650, 1250, 0);

        ResumenNegocio.Stock stock = armar(List.of(), yerba(), fideos(), aceite(), enElMinimo, sinCantidad, sinMinimo,
                minimoEnCero, agotadoConMinimoEnCero).stock();

        assertThat(stock.productos()).isEqualTo(8);
        assertThat(stock.totalFaltantes()).isEqualTo(5);
        assertThat(stock.faltantes()).extracting(ResumenNegocio.Faltante::nombre)
                .containsExactly("Fideos Luchetti", "Agua", "Leche", "Aceite Cocinero", "Harina");
        assertThat(stock.faltantes().get(0).pedir()).isEqualByComparingTo("12");
        assertThat(stock.faltantes().get(1).pedir()).isNull();
        assertThat(stock.faltantes().get(2).pedir()).isNull();
        assertThat(stock.faltantes().get(3).pedir()).isEqualByComparingTo("9");
        assertThat(stock.faltantes().get(4).pedir()).isEqualByComparingTo("5");
        assertThat(stock.valorACosto()).isEqualByComparingTo("114250");
        assertThat(stock.valorAPrecio()).isEqualByComparingTo("153900");
    }

    @Test
    @DisplayName("Pide al menos una unidad aunque ya haya más del doble del mínimo")
    void armarShouldAskForAtLeastOneUnit() {
        Registro casiSuficiente = producto(UUID.randomUUID(), "Yerba", 5, 100, 50, 5);
        Registro enElMinimo = producto(UUID.randomUUID(), "Té", 10, 100, 50, 10);

        List<ResumenNegocio.Faltante> faltantes = armar(List.of(), casiSuficiente, enElMinimo).stock().faltantes();

        assertThat(faltantes).extracting(ResumenNegocio.Faltante::pedir)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("5"), new BigDecimal("10"));
    }

    @Test
    @DisplayName("Lista 15 faltantes y dice cuántos son en total")
    void armarShouldLimitMissingStockToFifteen() {
        List<Registro> productos = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            productos.add(producto(UUID.randomUUID(), "Producto " + i, 0, 100, 50, 5));
        }

        ResumenNegocio.Stock stock = armar(List.of(), productos.toArray(new Registro[0])).stock();

        assertThat(stock.faltantes()).hasSize(15);
        assertThat(stock.totalFaltantes()).isEqualTo(18);
    }

    // --- clientes y fiado ---

    @Test
    @DisplayName("Los clientes se agrupan sin importar tildes ni mayúsculas y el fiado suma lo vendido a crédito")
    void armarShouldGroupClientsAndSumCredit() {
        List<Registro> ventas = List.of(
                venta(YERBA, 1, 3400, "2026-10-10", "Doña Rosa", true),
                venta(FIDEOS, 2, 1700, "2026-10-11", "DOÑA ROSA", false),
                venta(YERBA, 1, 3400, "2026-10-12", "Bar La Esquina", true),
                venta(YERBA, 1, 3400, "2026-10-12", null, false),
                venta(YERBA, 4, 3400, "2026-08-12", "Club Social", true));

        ResumenNegocio.Clientes clientes = armar(ventas, yerba(), fideos()).clientes();

        assertThat(clientes.top()).extracting(ResumenNegocio.ClienteTop::nombre)
                .containsExactly("Doña Rosa", "Bar La Esquina");
        assertThat(clientes.top().getFirst().compras()).isEqualTo(2);
        assertThat(clientes.top().getFirst().ingresos()).isEqualByComparingTo("6800");
        assertThat(clientes.fiado()).isEqualByComparingTo("6800");
        assertThat(clientes.ventasFiadas()).isEqualTo(2);
        assertThat(clientes.vacio()).isFalse();
    }

    @Test
    @DisplayName("El cliente guardado como fila de la tabla de clientes se muestra con su nombre, nunca con el id")
    void armarShouldShowClientNamesInsteadOfIds() {
        String rosaId = "a1b2c3d4-0000-4000-8000-000000000001";
        String desconocidoId = "A1B2C3D4-0000-4000-8000-000000000002";
        List<Registro> ventas = List.of(
                venta(YERBA, 1, 3400, "2026-10-10", rosaId, true),
                venta(YERBA, 2, 3400, "2026-10-11", rosaId, false),
                venta(YERBA, 1, 3400, "2026-10-12", desconocidoId, false),
                venta(YERBA, 1, 3400, "2026-10-12", "Bar La Esquina", false));
        DatosDeProyecto datos = new DatosDeProyecto("Almacén de prueba",
                List.of(AvisoTestData.inventarioConPrecios(yerba())), List.of(bloqueDeVentas(ventas)), List.of(),
                Map.of(rosaId, "Doña Rosa"));

        ResumenNegocio.Clientes clientes = ResumenNegocio.armar("Almacén de Ana", datos, AnalisisZona.sinZona(),
                List.of(), PERIODO, HOY).clientes();

        assertThat(clientes.top()).extracting(ResumenNegocio.ClienteTop::nombre)
                .containsExactlyInAnyOrder("Doña Rosa", "Bar La Esquina", "Cliente sin nombre");
        assertThat(clientes.top().getFirst().nombre()).isEqualTo("Doña Rosa");
        assertThat(clientes.top().getFirst().compras()).isEqualTo(2);
        assertThat(clientes.top()).extracting(ResumenNegocio.ClienteTop::nombre)
                .noneMatch(nombre -> nombre.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    @DisplayName("Las ventas a fiado sin cliente cargado igual cuentan como fiado")
    void armarShouldCountCreditSalesWithoutClient() {
        ResumenNegocio.Clientes clientes = armar(List.of(venta(YERBA, 1, 3400, "2026-10-10", null, true)), yerba())
                .clientes();

        assertThat(clientes.top()).isEmpty();
        assertThat(clientes.ventasFiadas()).isEqualTo(1);
        assertThat(clientes.vacio()).isFalse();
    }

    @Test
    @DisplayName("Lista a lo sumo 5 clientes")
    void armarShouldLimitClientsToFive() {
        List<Registro> ventas = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            ventas.add(venta(YERBA, 1, 3400, "2026-10-10", "Cliente " + i, false));
        }

        assertThat(armar(ventas, yerba()).clientes().top()).hasSize(5);
    }

    // --- gastos ---

    @Test
    @DisplayName("Sin tabla de gastos con importe no hay sección de gastos")
    void armarShouldHaveNoExpensesWithoutAnExpensesTable() {
        assertThat(armar(List.of(), yerba()).gastos()).isNull();
        assertThat(armarConGastos(List.of(), gastosSinImporte()).gastos()).isNull();
    }

    @Test
    @DisplayName("Suma los gastos del período por categoría y calcula el resultado contra los ingresos")
    void armarShouldSumExpensesByCategory() {
        List<Registro> gastos = List.of(
                gasto("Proveedor", "2026-10-15", 32000, "Mercadería"),
                gasto("Otro proveedor", "2026-10-16", 8000, "Mercadería"),
                gasto("Luz", "2026-10-10", 5000, "Servicios"),
                gasto("Sin categoría", "2026-10-11", 1000, null),
                gasto("En blanco", "2026-10-11", 500, "  "),
                gasto("Viejo", "2026-08-10", 99999, "Mercadería"),
                gasto("Sin fecha", null, 99999, "Mercadería"),
                gasto("Fecha rota", "ayer", 99999, "Mercadería"),
                gasto("Sin importe", "2026-10-12", null, "Mercadería"));
        List<Registro> ventas = List.of(venta(YERBA, 10, 3400, "2026-10-10", null, false));

        ResumenNegocio.Gastos resultado = armarConGastos(ventas, gastosDe(gastos)).gastos();

        assertThat(resultado.total()).isEqualByComparingTo("46500");
        assertThat(resultado.cantidad()).isEqualTo(5);
        assertThat(resultado.resultado()).isEqualByComparingTo("-12500");
        assertThat(resultado.porCategoria()).extracting(ResumenNegocio.GastoPorCategoria::categoria)
                .containsExactly("Mercadería", "Servicios", "Sin categoría");
        assertThat(resultado.porCategoria().getFirst().total()).isEqualByComparingTo("40000");
        assertThat(resultado.porCategoria().getLast().total()).isEqualByComparingTo("1500");
    }

    // --- zona ---

    @Test
    @DisplayName("Compara los precios con el rango de la zona y lista los más desalineados")
    void armarShouldComparePricesWithZoneRange() {
        AnalisisZona zona = new AnalisisZona("San Justo", 8, List.of(
                rango("Yerba Nobleza Gaucha", "3500", "4800", 8),
                rango("Fideos Luchetti", "1100", "1500", 7),
                rango("Mermelada BC", "1700", "3000", 6),
                rango("Aceite Cocinero", "2000", "3000", 2)));

        ResumenNegocio.ZonaResumen resumen = armar(List.of(), zona, List.of(), yerba(), fideos(), mermelada(), aceite(),
                producto(UUID.randomUUID(), "Sin precio", 1, null, null, null)).zona();

        assertThat(resumen.nombre()).isEqualTo("San Justo");
        assertThat(resumen.comercios()).isEqualTo(8);
        assertThat(resumen.debajo()).isEqualTo(1);
        assertThat(resumen.dentro()).isEqualTo(1);
        assertThat(resumen.encima()).isEqualTo(1);
        assertThat(resumen.desalineados()).extracting(ResumenNegocio.ProductoDesalineado::nombre)
                .containsExactly("Fideos Luchetti", "Yerba Nobleza Gaucha");
        assertThat(resumen.desalineados().get(0).porcentaje()).isEqualByComparingTo("13");
        assertThat(resumen.desalineados().get(1).porcentaje()).isEqualByComparingTo("-3");
    }

    @Test
    @DisplayName("Lista a lo sumo 5 productos desalineados")
    void armarShouldLimitMisalignedProductsToFive() {
        List<Registro> productos = new ArrayList<>();
        List<AgregadoZonaProducto> rangos = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            productos.add(producto(UUID.randomUUID(), "Producto " + i, 1, 1000 + i * 100, null, null));
            rangos.add(rango("Producto " + i, "100", "200", 5));
        }

        ResumenNegocio.ZonaResumen resumen = armar(List.of(), new AnalisisZona("San Justo", 8, rangos), List.of(),
                productos.toArray(new Registro[0])).zona();

        assertThat(resumen.encima()).isEqualTo(7);
        assertThat(resumen.desalineados()).hasSize(5);
        assertThat(resumen.desalineados().getFirst().nombre()).isEqualTo("Producto 6");
    }

    @Test
    @DisplayName("Sin zona, con menos de 3 comercios o sin productos comparables no hay sección de zona")
    void armarShouldHaveNoZoneWithoutAnonymousRange() {
        AnalisisZona chica = new AnalisisZona("San Justo", 2, List.of(rango("Yerba Nobleza Gaucha", "3500", "4800", 8)));
        AnalisisZona sinCoincidencias = new AnalisisZona("San Justo", 8, List.of(rango("Otra cosa", "1", "2", 8)));

        assertThat(armar(List.of(), AnalisisZona.sinZona(), List.of(), yerba()).zona()).isNull();
        assertThat(armar(List.of(), chica, List.of(), yerba()).zona()).isNull();
        assertThat(armar(List.of(), sinCoincidencias, List.of(), yerba()).zona()).isNull();
    }

    // --- avisos ---

    @Test
    @DisplayName("Cuenta los avisos pendientes por tipo y lo que se aplicó y descartó dentro del período")
    void armarShouldCountAvisosByTypeAndResolutionInPeriod() {
        List<Aviso> avisos = List.of(
                aviso(TipoAviso.STOCK, EstadoAviso.PENDIENTE, null),
                aviso(TipoAviso.STOCK, EstadoAviso.PENDIENTE, null),
                aviso(TipoAviso.COMBO, EstadoAviso.PENDIENTE, null),
                aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO, AHORA.minusDays(3)),
                aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO, AHORA.minusDays(60)),
                aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO, null),
                aviso(TipoAviso.STOCK, EstadoAviso.DESCARTADO, AHORA.minusDays(2)),
                aviso(TipoAviso.STOCK, EstadoAviso.DESCARTADO, AHORA.minusDays(90)),
                aviso(TipoAviso.STOCK, EstadoAviso.VENCIDO, null));

        ResumenNegocio.AvisosResumen resumen = armar(List.of(), AnalisisZona.sinZona(), avisos, yerba()).avisos();

        assertThat(resumen.pendientes()).containsEntry(TipoAviso.STOCK, 2L).containsEntry(TipoAviso.COMBO, 1L)
                .hasSize(2);
        assertThat(resumen.totalPendientes()).isEqualTo(3);
        assertThat(resumen.aplicados()).isEqualTo(1);
        assertThat(resumen.descartados()).isEqualTo(1);
        assertThat(resumen.medidos()).isZero();
        assertThat(resumen.ingresoSemanalDelta()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Suma el cambio de ingreso semanal de los avisos aplicados que ya se midieron")
    void armarShouldSumMeasuredImpact() {
        Aviso medido = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO, AHORA.minusDays(3));
        medido.registrarImpacto(new ImpactoAviso(14, BigDecimal.ONE, BigDecimal.TWO, new BigDecimal("1000"),
                new BigDecimal("1500")), AHORA);
        Aviso sinMedir = aviso(TipoAviso.STOCK, EstadoAviso.APLICADO, AHORA.minusDays(4));

        ResumenNegocio.AvisosResumen resumen = armar(List.of(), AnalisisZona.sinZona(), List.of(medido, sinMedir),
                yerba()).avisos();

        assertThat(resumen.aplicados()).isEqualTo(2);
        assertThat(resumen.medidos()).isEqualTo(1);
        assertThat(resumen.ingresoSemanalDelta()).isEqualByComparingTo("3500");
    }

    // --- act ---
    private static ResumenNegocio armar(List<Registro> ventas, Registro... productos) {
        return armar(ventas, AnalisisZona.sinZona(), List.of(), productos);
    }

    private static ResumenNegocio armar(List<Registro> ventas, AnalisisZona zona, List<Aviso> avisos,
                                        Registro... productos) {
        DatosDeProyecto datos = new DatosDeProyecto("Almacén de prueba",
                List.of(AvisoTestData.inventarioConPrecios(productos)), List.of(bloqueDeVentas(ventas)), List.of());
        return ResumenNegocio.armar("Almacén de Ana", datos, zona, avisos, PERIODO, HOY);
    }

    private static ResumenNegocio armarConGastos(List<Registro> ventas, BloqueGastos gastos) {
        DatosDeProyecto datos = new DatosDeProyecto("Almacén de prueba",
                List.of(AvisoTestData.inventarioConPrecios(yerba())), List.of(bloqueDeVentas(ventas)), List.of(gastos));
        return ResumenNegocio.armar("Almacén de Ana", datos, AnalisisZona.sinZona(), List.of(), PERIODO, HOY);
    }

    // --- helpers: productos y ventas ---
    private static Registro yerba() {
        return producto(YERBA, "Yerba Nobleza Gaucha", 40, 3400, 2700, 10);
    }

    private static Registro fideos() {
        return producto(FIDEOS, "Fideos Luchetti", 0, 1700, null, 6);
    }

    private static Registro mermelada() {
        return producto(MERMELADA, "Mermelada BC", 20, 2600, 1800, 4);
    }

    private static Registro aceite() {
        return producto(ACEITE, "Aceite Cocinero", 3, 2800, null, 6);
    }

    private static Registro producto(UUID id, String nombre, Object cantidad, Object precio, Object costo,
                                     Object minimo) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, AvisoTestData.CAMPO_NOMBRE_ID, nombre);
        poner(valores, AvisoTestData.CAMPO_CANTIDAD_ID, cantidad);
        poner(valores, AvisoTestData.CAMPO_PRECIO_ID, precio);
        poner(valores, AvisoTestData.CAMPO_COSTO_ID, costo);
        poner(valores, AvisoTestData.CAMPO_MINIMO_ID, minimo);
        return Registro.builder().id(RegistroId.de(id)).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.INVENTARIO_ID).valores(valores).build();
    }

    private static Registro venta(UUID producto, int cantidad, Integer precio, String fecha, String cliente,
                                  boolean fiado) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, AvisoTestData.CAMPO_PRODUCTO_ID, producto.toString());
        poner(valores, AvisoTestData.CAMPO_VENTA_CANTIDAD_ID, cantidad);
        poner(valores, AvisoTestData.CAMPO_VENTA_PRECIO_ID, precio);
        poner(valores, AvisoTestData.CAMPO_FECHA_ID, fecha);
        poner(valores, AvisoTestData.CAMPO_CLIENTE_ID, cliente);
        poner(valores, CAMPO_FIADO_ID, fiado);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.VENTAS_ID).valores(valores).build();
    }

    private static BloqueVentas bloqueDeVentas(List<Registro> ventas) {
        Bloque base = AvisoTestData.bloqueVentas();
        List<Campo> campos = new ArrayList<>(base.getCampos());
        campos.add(campo(CAMPO_FIADO_ID, AvisoTestData.VENTAS_ID, "credit"));
        Bloque bloque = Bloque.builder().id(base.getId()).negocioId(base.getNegocioId())
                .proyectoId(base.getProyectoId()).tipo(base.getTipo()).campos(campos).build();
        return new BloqueVentas(bloque, AvisoTestData.camposPorClave(bloque), ventas);
    }

    // --- helpers: gastos ---
    private static Registro gasto(String nombre, String fecha, Integer total, String categoria) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, GASTO_NOMBRE_ID, nombre);
        poner(valores, GASTO_FECHA_ID, fecha);
        poner(valores, GASTO_TOTAL_ID, total);
        poner(valores, GASTO_CATEGORIA_ID, categoria);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(GASTOS_ID)
                .valores(valores).build();
    }

    private static BloqueGastos gastosDe(List<Registro> gastos) {
        return new BloqueGastos(bloqueDeGastos(List.of(
                campo(GASTO_NOMBRE_ID, GASTOS_ID, "name"), campo(GASTO_FECHA_ID, GASTOS_ID, "date"),
                campo(GASTO_TOTAL_ID, GASTOS_ID, "total"), campo(GASTO_CATEGORIA_ID, GASTOS_ID, "category"))),
                Map.of("name", campo(GASTO_NOMBRE_ID, GASTOS_ID, "name"), "date", campo(GASTO_FECHA_ID, GASTOS_ID, "date"),
                        "total", campo(GASTO_TOTAL_ID, GASTOS_ID, "total"),
                        "category", campo(GASTO_CATEGORIA_ID, GASTOS_ID, "category")), gastos);
    }

    private static BloqueGastos gastosSinImporte() {
        return new BloqueGastos(bloqueDeGastos(List.of()), Map.of(), List.of());
    }

    private static Bloque bloqueDeGastos(List<Campo> campos) {
        return Bloque.builder().id(GASTOS_ID).negocioId(AvisoTestData.NEGOCIO_ID).proyectoId(AvisoTestData.PROYECTO_ID)
                .tipo(TipoBloque.GASTOS).campos(campos).build();
    }

    // --- helpers: zona y avisos ---
    private static AgregadoZonaProducto rango(String nombre, String minimo, String maximo, int comercios) {
        return new AgregadoZonaProducto(nombre, comercios, comercios, new BigDecimal(minimo), new BigDecimal(minimo),
                new BigDecimal(maximo), BigDecimal.TEN);
    }

    private static Aviso aviso(TipoAviso tipo, EstadoAviso estado, OffsetDateTime resueltoEn) {
        return Aviso.builder().tipo(tipo).estado(estado).resueltoEn(resueltoEn).prioridad((short) 1)
                .datos(Map.of()).acciones(List.of()).build();
    }

    private static Campo campo(CampoId id, BloqueId bloqueId, String clave) {
        return Campo.builder().id(id).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(bloqueId).clave(clave).build();
    }

    private static void poner(Map<String, Object> valores, CampoId campoId, Object valor) {
        if (valor != null) {
            valores.put(campoId.toString(), valor);
        }
    }
}
