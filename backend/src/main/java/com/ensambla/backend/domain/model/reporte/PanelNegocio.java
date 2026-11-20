package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.caja.PagoCaja;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.common.ValorDeRegistro;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
/**
 * Lo que la home le muestra al comerciante de su negocio: las métricas del período y el flujo de caja por semana.
 * Lo vendido, lo que le deben y la variación salen de {@link ResumenNegocio}, así que coinciden con el resumen en
 * PDF del mismo período.
 *
 * <p>El flujo combina las anotaciones manuales y los cobros reales de Caja:
 * <ul>
 *   <li>entradas: ventas manuales que no están marcadas «fiado», entregas iniciales y cobros posteriores de Caja;</li>
 *   <li>salidas: los gastos más las compras de mercadería.</li>
 * </ul>
 * Las compras de mercadería salen de la tabla de entradas, valuadas al costo actual de cada producto. Si el sistema
 * no tiene esa tabla se toman los gastos de categoría «Mercadería», que son lo mismo anotado como gasto. Nunca se
 * suman las dos fuentes (contaría la compra dos veces), y esa categoría no cuenta como gasto: por eso el total de
 * gastos puede ser menor que el del PDF, que los suma todos.
 *
 * <p>{@code teDeben} informa el saldo actual de las ventas del período. Un cobro de una venta anterior entra en
 * su fecha real, aunque el período no tenga ventas nuevas. Sin ventas ni cobros el panel queda vacío.
 *
 * @param metricas    {@code null} si el período no tiene ventas ni cobros
 * @param flujoDeCaja una semana tras otra desde el primer día del período; vacío si no hay actividad
 */
public record PanelNegocio(PeriodoResumen periodo, Metricas metricas, List<SemanaDeCaja> flujoDeCaja) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int DIAS_POR_SEMANA = 7;
    private static final String CATEGORIA_MERCADERIA = TextoNormalizado.de("Mercadería");

    /**
     * @param vendiste       todo lo vendido con precio, también lo fiado (es el «ingresos» del PDF)
     * @param mercaderia     lo comprado en el período; {@code null} si no hay forma de saberlo (ni tabla de
     *                       entradas ni de gastos)
     * @param gastos         los gastos sin la mercadería; {@code null} si el sistema no tiene una tabla de gastos
     *                       con importe
     * @param queda          lo cobrado menos gastos y mercadería: el saldo con que cierra el flujo de caja
     * @param teDeben        el saldo actual de las ventas del período
     * @param margen         ganancia sobre lo vendido (en %), mirando solo los productos que tienen costo;
     *                       {@code null} si ninguno de los vendidos lo tiene
     * @param ticketPromedio lo vendido por cada venta con precio; {@code null} si ninguna tiene precio
     * @param variacion      cuánto cambió lo vendido contra el período anterior (en %); {@code null} si antes no
     *                       hubo
     */
    public record Metricas(
            BigDecimal vendiste,
            BigDecimal mercaderia,
            BigDecimal gastos,
            BigDecimal queda,
            BigDecimal teDeben,
            BigDecimal margen,
            BigDecimal ticketPromedio,
            BigDecimal variacion) {
    }

    /** Una semana de caja: {@code saldo} es lo que dejó esa semana y {@code saldoAcumulado} lo que lleva en total. */
    public record SemanaDeCaja(
            LocalDate desde,
            LocalDate hasta,
            BigDecimal entradas,
            BigDecimal salidas,
            BigDecimal saldo,
            BigDecimal saldoAcumulado) {
    }

    private record Movimiento(LocalDate fecha, BigDecimal importe) {
    }

    private record Gasto(Movimiento movimiento, boolean esMercaderia) {
    }

    private record Venta(VentaProducto venta, boolean fiado, boolean deCaja) {

        BigDecimal importe() {
            return venta.precio().multiply(venta.cantidad());
        }

        /** Lo cobrado en el momento: ni lo fiado ni lo de Caja, que entra por sus pagos. */
        boolean seCobroAlVender() {
            return !fiado && !deCaja;
        }

        Movimiento cobro() {
            return new Movimiento(venta.fecha(), importe());
        }

        boolean tieneCostoEn(Map<RegistroId, BigDecimal> costos) {
            return venta.productoId() != null && costos.containsKey(venta.productoId());
        }

        BigDecimal ganancia(Map<RegistroId, BigDecimal> costos) {
            return venta.precio().subtract(costos.get(venta.productoId())).multiply(venta.cantidad());
        }
    }

    /** La plata del período: lo que entró y lo que salió, separado como lo muestra el panel. */
    private record Caja(List<Venta> ventas, List<Movimiento> cobradas, List<Movimiento> gastosSinMercaderia,
                        List<Movimiento> compras) {

        List<Movimiento> salidas() {
            return Stream.concat(gastosSinMercaderia.stream(), compras.stream()).toList();
        }
    }

    public boolean vacio() {
        return metricas == null;
    }

    public static PanelNegocio armar(DatosDeProyecto datos, PeriodoResumen periodo, LocalDate hoy) {
        ResumenNegocio resumen = ResumenNegocio.armar("", datos, AnalisisZona.sinZona(), List.of(), periodo, hoy);
        List<Movimiento> cobrosCaja = cobrosCaja(datos, periodo);
        if (resumen.ventas().cantidad() == 0 && cobrosCaja.isEmpty()) {
            return new PanelNegocio(periodo, null, List.of());
        }
        Caja caja = caja(datos, periodo, cobrosCaja);
        List<SemanaDeCaja> flujo = flujoDeCaja(periodo, caja.cobradas(), caja.salidas());
        return new PanelNegocio(periodo, metricas(resumen, datos, caja, flujo), flujo);
    }

    private static Caja caja(DatosDeProyecto datos, PeriodoResumen periodo, List<Movimiento> cobrosCaja) {
        List<Venta> ventas = ventasConPrecio(datos, periodo);
        List<Gasto> gastos = gastos(datos.gastos(), periodo);
        List<Movimiento> cobradas = Stream.concat(cobrosCaja.stream(),
                ventas.stream().filter(Venta::seCobroAlVender).map(Venta::cobro)).toList();
        List<Movimiento> sinMercaderia = gastos.stream().filter(gasto -> !gasto.esMercaderia())
                .map(Gasto::movimiento).toList();
        return new Caja(ventas, cobradas, sinMercaderia, compras(datos, gastos, periodo));
    }

    // --- métricas ---

    private static Metricas metricas(ResumenNegocio resumen, DatosDeProyecto datos, Caja caja,
                                     List<SemanaDeCaja> flujo) {
        boolean hayGastos = datos.gastos().stream().anyMatch(BloqueGastos::tieneImporte);
        boolean conoceCompras = !datos.entradas().isEmpty() || hayGastos;
        return new Metricas(
                resumen.ventas().ingresos(),
                conoceCompras ? sumaDe(caja.compras()) : null,
                hayGastos ? sumaDe(caja.gastosSinMercaderia()) : null,
                flujo.getLast().saldoAcumulado(),
                resumen.clientes().fiado(),
                margen(caja.ventas(), costos(datos.inventarios())),
                ticketPromedio(caja.ventas()),
                resumen.ventas().variacion());
    }

    /** La ganancia sobre lo vendido de los productos que tienen costo; lo de los demás no entra ni arriba ni abajo. */
    private static BigDecimal margen(List<Venta> ventas, Map<RegistroId, BigDecimal> costos) {
        List<Venta> conCosto = ventas.stream().filter(venta -> venta.tieneCostoEn(costos)).toList();
        BigDecimal vendido = suma(conCosto.stream().map(Venta::importe));
        if (vendido.signum() == 0) {
            return null;
        }
        BigDecimal ganancia = suma(conCosto.stream().map(venta -> venta.ganancia(costos)));
        return ganancia.multiply(CIEN).divide(vendido, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal ticketPromedio(List<Venta> ventas) {
        if (ventas.isEmpty()) {
            return null;
        }
        return suma(ventas.stream().map(Venta::importe)).divide(BigDecimal.valueOf(ventas.size()), 2,
                RoundingMode.HALF_UP);
    }

    // --- flujo de caja ---

    private static List<Movimiento> cobrosCaja(DatosDeProyecto datos, PeriodoResumen periodo) {
        Stream<Movimiento> entregas = datos.ventasCaja().stream().map(venta -> new Movimiento(
                ZonaNegocio.dia(venta.creadoEn()), suma(venta.pagosIniciales().stream().map(PagoCaja::importe))));
        Stream<Movimiento> cobros = datos.cobrosCaja().stream()
                .map(cobro -> new Movimiento(ZonaNegocio.dia(cobro.creadoEn()), cobro.importe()));
        return Stream.concat(entregas, cobros)
                .filter(movimiento -> movimiento.importe().signum() > 0 && periodo.incluye(movimiento.fecha()))
                .toList();
    }

    private static List<SemanaDeCaja> flujoDeCaja(PeriodoResumen periodo, List<Movimiento> entradas,
                                                  List<Movimiento> salidas) {
        List<SemanaDeCaja> flujo = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (PeriodoResumen semana : semanasDe(periodo)) {
            SemanaDeCaja deLaSemana = semanaDeCaja(semana, entradas, salidas, acumulado);
            flujo.add(deLaSemana);
            acumulado = deLaSemana.saldoAcumulado();
        }
        return flujo;
    }

    /** Una semana tras otra desde el primer día del período; la última se corta en el último día. */
    private static List<PeriodoResumen> semanasDe(PeriodoResumen periodo) {
        return Stream.iterate(periodo.desde(), desde -> !desde.isAfter(periodo.hasta()),
                        desde -> desde.plusDays(DIAS_POR_SEMANA))
                .map(desde -> new PeriodoResumen(desde, finDeSemana(desde, periodo)))
                .toList();
    }

    private static LocalDate finDeSemana(LocalDate desde, PeriodoResumen periodo) {
        LocalDate hasta = desde.plusDays(DIAS_POR_SEMANA - 1L);
        return hasta.isAfter(periodo.hasta()) ? periodo.hasta() : hasta;
    }

    private static SemanaDeCaja semanaDeCaja(PeriodoResumen semana, List<Movimiento> entradas,
                                             List<Movimiento> salidas, BigDecimal acumuladoAntes) {
        BigDecimal entro = suma(en(entradas, semana));
        BigDecimal salio = suma(en(salidas, semana));
        BigDecimal saldo = entro.subtract(salio);
        return new SemanaDeCaja(semana.desde(), semana.hasta(), entro, salio, saldo, acumuladoAntes.add(saldo));
    }

    private static Stream<BigDecimal> en(List<Movimiento> movimientos, PeriodoResumen semana) {
        return movimientos.stream().filter(movimiento -> semana.incluye(movimiento.fecha())).map(Movimiento::importe);
    }

    // --- lectura de las tablas ---

    /** Las ventas del período que tienen precio; las demás cuentan como venta pero no son plata. */
    private static List<Venta> ventasConPrecio(DatosDeProyecto datos, PeriodoResumen periodo) {
        Stream<Venta> articulos = ventasDeTablas(datos.ventas(), datos.origenesVentasCaja());
        Stream<Venta> importes = datos.importesLibres().stream().map(venta -> new Venta(venta, false, true));
        return Stream.concat(articulos, importes)
                .filter(venta -> venta.venta().precio() != null && periodo.incluye(venta.venta().fecha()))
                .toList();
    }

    private static Stream<Venta> ventasDeTablas(List<BloqueVentas> bloques, Set<String> origenesCaja) {
        return bloques.stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .flatMap(registro -> VentaProducto.desde(bloque, registro).stream()
                                .map(venta -> new Venta(venta, esFiado(bloque, registro),
                                        origenesCaja.contains(registro.getOrigen())))));
    }

    private static boolean esFiado(BloqueVentas bloque, Registro registro) {
        return Boolean.TRUE.equals(bloque.valorDe(registro, "credit"));
    }

    private static List<Gasto> gastos(List<BloqueGastos> bloques, PeriodoResumen periodo) {
        return bloques.stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .flatMap(registro -> gastoDelPeriodo(bloque, registro, periodo).stream()))
                .toList();
    }

    /** El gasto del registro si tiene importe y una fecha dentro del período. */
    private static Optional<Gasto> gastoDelPeriodo(BloqueGastos bloque, Registro registro, PeriodoResumen periodo) {
        BigDecimal importe = ValorDeRegistro.de(bloque.valorDe(registro, "total")).comoNumero();
        LocalDate fecha = ValorDeRegistro.de(bloque.valorDe(registro, "date")).comoFecha();
        if (importe == null || !periodo.incluye(fecha)) {
            return Optional.empty();
        }
        boolean mercaderia = esMercaderia(ValorDeRegistro.de(bloque.valorDe(registro, "category")));
        return Optional.of(new Gasto(new Movimiento(fecha, importe), mercaderia));
    }

    private static boolean esMercaderia(ValorDeRegistro categoria) {
        return categoria.tieneTexto() && TextoNormalizado.de(categoria.comoTexto()).equals(CATEGORIA_MERCADERIA);
    }

    /**
     * Las compras del período: las entradas valuadas al costo actual de cada producto (las de un producto sin costo
     * no se pueden valuar) o, si no hay tabla de entradas, los gastos de categoría «Mercadería».
     */
    private static List<Movimiento> compras(DatosDeProyecto datos, List<Gasto> gastos, PeriodoResumen periodo) {
        if (datos.entradas().isEmpty()) {
            return gastos.stream().filter(Gasto::esMercaderia).map(Gasto::movimiento).toList();
        }
        Map<RegistroId, BigDecimal> costos = costos(datos.inventarios());
        return datos.entradas().stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .flatMap(registro -> compraDelPeriodo(bloque, registro, costos, periodo).stream()))
                .toList();
    }

    /** La entrada valuada al costo de su producto, si se puede valuar y cae dentro del período. */
    private static Optional<Movimiento> compraDelPeriodo(BloqueEntradas bloque, Registro registro,
                                                         Map<RegistroId, BigDecimal> costos, PeriodoResumen periodo) {
        BigDecimal costo = costos.get(producto(ValorDeRegistro.de(bloque.valorDe(registro, "product"))));
        BigDecimal cantidad = ValorDeRegistro.de(bloque.valorDe(registro, "quantity")).comoNumeroPositivo();
        LocalDate fecha = ValorDeRegistro.de(bloque.valorDe(registro, "date")).comoFecha();
        if (costo == null || cantidad == null || !periodo.incluye(fecha)) {
            return Optional.empty();
        }
        return Optional.of(new Movimiento(fecha, cantidad.multiply(costo)));
    }

    private static Map<RegistroId, BigDecimal> costos(List<BloqueInventario> inventarios) {
        Map<RegistroId, BigDecimal> costos = new HashMap<>();
        inventarios.forEach(inventario -> inventario.registros().forEach(registro ->
                Optional.ofNullable(ValorDeRegistro.de(inventario.valorDe(registro, "cost")).comoNumeroPositivo())
                        .ifPresent(costo -> costos.put(registro.getId(), costo))));
        return costos;
    }

    /** La conexión al producto se guarda como el id (texto) del registro de inventario. */
    private static RegistroId producto(ValorDeRegistro valor) {
        if (!valor.tieneTexto()) {
            return null;
        }
        try {
            return RegistroId.de(valor.comoTexto());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static BigDecimal sumaDe(List<Movimiento> movimientos) {
        return suma(movimientos.stream().map(Movimiento::importe));
    }

    private static BigDecimal suma(Stream<BigDecimal> valores) {
        return valores.reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}