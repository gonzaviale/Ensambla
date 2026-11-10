package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
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
 * Las métricas salen de {@link ResumenNegocio}, así que coinciden con el resumen en PDF del mismo período.
 *
 * <p>El flujo de caja se arma con lo que ya se carga, no con un circuito de cobros y pagos:
 * <ul>
 *   <li>entradas: ventas con precio que no están marcadas «fiado»;</li>
 *   <li>salidas: gastos más las compras de mercadería (entradas de inventario valuadas al costo actual del
 *   producto).</li>
 * </ul>
 * Lo vendido a fiado se informa aparte como {@code teDeben}: como no hay forma de registrar el cobro, no pasa a ser
 * entrada. Sin ventas en el período el panel está vacío ({@code metricas} es {@code null}): no se inventan ceros.
 *
 * @param metricas    {@code null} si el período no tiene ventas
 * @param flujoDeCaja una semana tras otra desde el primer día del período; vacío si no hay ventas
 */
public record PanelNegocio(PeriodoResumen periodo, Metricas metricas, List<SemanaDeCaja> flujoDeCaja) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int DIAS_POR_SEMANA = 7;

    /**
     * @param vendiste       todo lo vendido con precio, también lo fiado (es el «ingresos» del PDF)
     * @param mercaderia     lo comprado en el período; {@code null} si el sistema no tiene tabla de entradas
     * @param gastos         {@code null} si el sistema no tiene una tabla de gastos con importe
     * @param queda          lo cobrado menos gastos y mercadería: el saldo con que cierra el flujo de caja
     * @param teDeben        lo vendido a fiado en el período
     * @param margen         ganancia estimada sobre lo vendido (en %); {@code null} si ningún producto vendido
     *                       tiene costo
     * @param ticketPromedio lo vendido por cada venta cargada
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

    public boolean vacio() {
        return metricas == null;
    }

    public static PanelNegocio armar(DatosDeProyecto datos, PeriodoResumen periodo, LocalDate hoy) {
        ResumenNegocio resumen = ResumenNegocio.armar("", datos, AnalisisZona.sinZona(), List.of(), periodo, hoy);
        if (resumen.ventas().cantidad() == 0) {
            return new PanelNegocio(periodo, null, List.of());
        }
        List<Movimiento> cobradas = cobradas(datos.ventas(), periodo);
        List<Movimiento> gastos = gastos(datos.gastos(), periodo);
        List<Movimiento> mercaderia = mercaderia(datos, periodo);
        List<Movimiento> salidas = Stream.concat(gastos.stream(), mercaderia.stream()).toList();
        List<SemanaDeCaja> flujo = flujoDeCaja(periodo, cobradas, salidas);
        BigDecimal saldoFinal = flujo.getLast().saldoAcumulado();
        return new PanelNegocio(periodo, metricas(resumen, datos, mercaderia, saldoFinal), flujo);
    }

    private static Metricas metricas(ResumenNegocio resumen, DatosDeProyecto datos, List<Movimiento> mercaderia,
                                    BigDecimal queda) {
        ResumenNegocio.Ventas ventas = resumen.ventas();
        return new Metricas(
                ventas.ingresos(),
                datos.entradas().isEmpty() ? null : suma(mercaderia.stream().map(Movimiento::importe)),
                resumen.gastos() == null ? null : resumen.gastos().total(),
                queda,
                resumen.clientes().fiado(),
                ventas.gananciaEstimada() == null
                        ? null
                        : ventas.gananciaEstimada().multiply(CIEN).divide(ventas.ingresos(), 1, RoundingMode.HALF_UP),
                ventas.ingresos().divide(BigDecimal.valueOf(ventas.cantidad()), 2, RoundingMode.HALF_UP),
                ventas.variacion());
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

    /** Las ventas con precio que no son fiadas, por día. */
    private static List<Movimiento> cobradas(List<BloqueVentas> bloques, PeriodoResumen periodo) {
        return bloques.stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .filter(registro -> !Boolean.TRUE.equals(bloque.valorDe(registro, "credit")))
                        .flatMap(registro -> VentaProducto.desde(bloque, registro).stream()))
                .filter(venta -> venta.precio() != null && periodo.incluye(venta.fecha()))
                .map(venta -> new Movimiento(venta.fecha(), venta.precio().multiply(venta.cantidad())))
                .toList();
    }

    private static List<Movimiento> gastos(List<BloqueGastos> bloques, PeriodoResumen periodo) {
        List<Movimiento> gastos = new ArrayList<>();
        for (BloqueGastos bloque : bloques) {
            for (Registro registro : bloque.registros()) {
                BigDecimal importe = numero(bloque.valorDe(registro, "total"));
                LocalDate fecha = fecha(bloque.valorDe(registro, "date"));
                if (importe != null && periodo.incluye(fecha)) {
                    gastos.add(new Movimiento(fecha, importe));
                }
            }
        }
        return gastos;
    }

    /** Las compras del período al costo actual de cada producto; las de un producto sin costo no se pueden valuar. */
    private static List<Movimiento> mercaderia(DatosDeProyecto datos, PeriodoResumen periodo) {
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

    // --- lectura de las tablas ---

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

    private static BigDecimal suma(Stream<BigDecimal> valores) {
        return valores.reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
