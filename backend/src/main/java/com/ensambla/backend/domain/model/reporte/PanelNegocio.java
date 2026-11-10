package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Lo que la home le muestra al comerciante de su negocio: las métricas del período y el flujo de caja por semana.
 * Lo vendido, lo que le deben y la variación salen de {@link ResumenNegocio}, así que coinciden con el resumen en
 * PDF del mismo período.
 *
 * <p>El flujo de caja se arma con lo que ya se carga, no con un circuito de cobros y pagos:
 * <ul>
 *   <li>entradas: ventas con precio que no están marcadas «fiado»;</li>
 *   <li>salidas: los gastos más las compras de mercadería.</li>
 * </ul>
 * Las compras de mercadería salen de la tabla de entradas, valuadas al costo actual de cada producto. Si el sistema
 * no tiene esa tabla se toman los gastos de categoría «Mercadería», que son lo mismo anotado como gasto. Nunca se
 * suman las dos fuentes (contaría la compra dos veces), y esa categoría no cuenta como gasto: por eso el total de
 * gastos puede ser menor que el del PDF, que los suma todos.
 *
 * <p>Lo vendido a fiado se informa aparte como {@code teDeben}: como no hay forma de registrar el cobro, no pasa a
 * ser entrada. Sin ventas en el período el panel está vacío ({@code metricas} es {@code null}): no se inventan
 * ceros.
 *
 * @param metricas    {@code null} si el período no tiene ventas
 * @param flujoDeCaja una semana tras otra desde el primer día del período; vacío si no hay ventas
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
     * @param teDeben        lo vendido a fiado en el período
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

    private record Venta(VentaProducto venta, boolean fiado) {

        BigDecimal importe() {
            return venta.precio().multiply(venta.cantidad());
        }
    }

    public boolean vacio() {
        return metricas == null;
    }

    public static PanelNegocio armar(DatosDeProyecto datos, PeriodoResumen periodo, LocalDate hoy) {
        ResumenNegocio resumen = ResumenNegocio.armar("", datos, AnalisisZona.sinZona(), List.of(), periodo, hoy);
        if (resumen.ventas().cantidad() == 0) {
            return new PanelNegocio(periodo, null, List.of());
        }
        List<Venta> ventas = ventasConPrecio(datos.ventas(), periodo);
        List<Gasto> gastos = gastos(datos.gastos(), periodo);
        List<Movimiento> sinMercaderia = gastos.stream().filter(gasto -> !gasto.esMercaderia())
                .map(Gasto::movimiento).toList();
        List<Movimiento> compras = compras(datos, gastos, periodo);
        List<Movimiento> cobradas = ventas.stream().filter(venta -> !venta.fiado())
                .map(venta -> new Movimiento(venta.venta().fecha(), venta.importe())).toList();
        List<SemanaDeCaja> flujo = flujoDeCaja(
                periodo, cobradas, Stream.concat(sinMercaderia.stream(), compras.stream()).toList());
        boolean hayGastos = datos.gastos().stream().anyMatch(BloqueGastos::tieneImporte);
        boolean conoceCompras = !datos.entradas().isEmpty() || hayGastos;
        return new PanelNegocio(periodo,
                new Metricas(
                        resumen.ventas().ingresos(),
                        conoceCompras ? sumaDe(compras) : null,
                        hayGastos ? sumaDe(sinMercaderia) : null,
                        flujo.getLast().saldoAcumulado(),
                        resumen.clientes().fiado(),
                        margen(ventas, costos(datos.inventarios())),
                        ticketPromedio(ventas),
                        resumen.ventas().variacion()),
                flujo);
    }

    // --- métricas ---

    /** La ganancia sobre lo vendido de los productos que tienen costo; lo de los demás no entra ni arriba ni abajo. */
    private static BigDecimal margen(List<Venta> ventas, Map<RegistroId, BigDecimal> costos) {
        List<Venta> conCosto = ventas.stream().filter(venta -> costos.containsKey(venta.venta().productoId())).toList();
        BigDecimal vendido = suma(conCosto.stream().map(Venta::importe));
        if (vendido.signum() == 0) {
            return null;
        }
        BigDecimal ganancia = suma(conCosto.stream().map(venta -> venta.venta().precio()
                .subtract(costos.get(venta.venta().productoId())).multiply(venta.venta().cantidad())));
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

    private static List<SemanaDeCaja> flujoDeCaja(PeriodoResumen periodo, List<Movimiento> entradas,
                                                  List<Movimiento> salidas) {
        List<SemanaDeCaja> semanas = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        LocalDate desde = periodo.desde();
        while (!desde.isAfter(periodo.hasta())) {
            LocalDate hasta = desde.plusDays(DIAS_POR_SEMANA - 1L);
            PeriodoResumen semana = new PeriodoResumen(desde, hasta.isAfter(periodo.hasta()) ? periodo.hasta() : hasta);
            BigDecimal entro = suma(en(entradas, semana));
            BigDecimal salio = suma(en(salidas, semana));
            acumulado = acumulado.add(entro).subtract(salio);
            semanas.add(new SemanaDeCaja(
                    semana.desde(), semana.hasta(), entro, salio, entro.subtract(salio), acumulado));
            desde = desde.plusDays(DIAS_POR_SEMANA);
        }
        return semanas;
    }

    private static Stream<BigDecimal> en(List<Movimiento> movimientos, PeriodoResumen semana) {
        return movimientos.stream().filter(movimiento -> semana.incluye(movimiento.fecha())).map(Movimiento::importe);
    }

    // --- lectura de las tablas ---

    /** Las ventas del período que tienen precio; las demás cuentan como venta pero no son plata. */
    private static List<Venta> ventasConPrecio(List<BloqueVentas> bloques, PeriodoResumen periodo) {
        return bloques.stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .flatMap(registro -> VentaProducto.desde(bloque, registro).stream()
                                .map(venta -> new Venta(venta, esFiado(bloque, registro)))))
                .filter(venta -> venta.venta().precio() != null && periodo.incluye(venta.venta().fecha()))
                .toList();
    }

    private static boolean esFiado(BloqueVentas bloque, Registro registro) {
        return Boolean.TRUE.equals(bloque.valorDe(registro, "credit"));
    }

    private static List<Gasto> gastos(List<BloqueGastos> bloques, PeriodoResumen periodo) {
        List<Gasto> gastos = new ArrayList<>();
        for (BloqueGastos bloque : bloques) {
            for (Registro registro : bloque.registros()) {
                BigDecimal importe = numero(bloque.valorDe(registro, "total"));
                LocalDate fecha = fecha(bloque.valorDe(registro, "date"));
                if (importe != null && periodo.incluye(fecha)) {
                    boolean mercaderia = esMercaderia(bloque.valorDe(registro, "category"));
                    gastos.add(new Gasto(new Movimiento(fecha, importe), mercaderia));
                }
            }
        }
        return gastos;
    }

    private static boolean esMercaderia(Object categoria) {
        return categoria instanceof String texto && TextoNormalizado.de(texto).equals(CATEGORIA_MERCADERIA);
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
        List<Movimiento> compras = new ArrayList<>();
        for (BloqueEntradas bloque : datos.entradas()) {
            for (Registro registro : bloque.registros()) {
                BigDecimal costo = costos.get(producto(bloque.valorDe(registro, "product")));
                BigDecimal cantidad = numero(bloque.valorDe(registro, "quantity"));
                LocalDate fecha = fecha(bloque.valorDe(registro, "date"));
                if (costo != null && cantidad != null && cantidad.signum() > 0 && periodo.incluye(fecha)) {
                    compras.add(new Movimiento(fecha, cantidad.multiply(costo)));
                }
            }
        }
        return compras;
    }

    private static Map<RegistroId, BigDecimal> costos(List<BloqueInventario> inventarios) {
        Map<RegistroId, BigDecimal> costos = new HashMap<>();
        for (BloqueInventario inventario : inventarios) {
            for (Registro registro : inventario.registros()) {
                BigDecimal costo = numero(inventario.valorDe(registro, "cost"));
                if (costo != null && costo.signum() > 0) {
                    costos.put(registro.getId(), costo);
                }
            }
        }
        return costos;
    }

    /** La conexión al producto se guarda como el id (texto) del registro de inventario. */
    private static RegistroId producto(Object valor) {
        if (!(valor instanceof String texto)) {
            return null;
        }
        try {
            return RegistroId.de(texto.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static BigDecimal numero(Object valor) {
        return valor instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }

    private static LocalDate fecha(Object valor) {
        if (!(valor instanceof String texto)) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
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
