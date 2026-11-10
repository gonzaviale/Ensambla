package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Cómo viene el negocio en los últimos 30 días, dicho en pesos enteros y sin términos contables, con el resultado de
 * las últimas semanas. Usa el mismo cálculo de ingresos, gastos y fiado que el resumen en PDF, así que para el mismo
 * período los números coinciden. Es lógica pura: recibe las tablas de todos los sistemas del negocio ya cargadas.
 *
 * @param hayVentas  si hubo alguna venta en el período; sin ventas todos los importes son cero y no hay semanas
 * @param vendiste   lo que entró por las ventas con precio
 * @param mercaderia lo que costó lo que se vendió, con el costo actual de cada producto
 * @param gastos     los gastos del período, sin la categoría «Mercadería» (ya se descuenta al vender)
 * @param queda      lo que queda de lo vendido después de pagar la mercadería y los gastos
 * @param teDeben    lo vendido a fiado en el período
 * @param semanas    el resultado de los últimos 30 días al cierre de cada una de las últimas semanas, de la más
 *                   antigua a «Hoy»; vacío si no hay ventas
 */
public record ComoVieneElNegocio(
        boolean hayVentas,
        long vendiste,
        long mercaderia,
        long gastos,
        long queda,
        long teDeben,
        List<Semana> semanas) {

    /** Los días que mira el resultado: el mes que termina hoy. */
    public static final int DIAS_DEL_PERIODO = PeriodoResumen.DIAS_POR_DEFECTO;

    /** Cuántas semanas de historia se muestran, hoy incluido. */
    public static final int SEMANAS_DE_HISTORIAL = 6;

    private static final int DIAS_DE_UNA_SEMANA = 7;
    private static final String HOY = "Hoy";
    private static final String MERCADERIA = "Mercadería";

    /** {@code semana} es «Hoy» o «Hace N sem.»; {@code queda} es el resultado de los 30 días que terminan esa semana. */
    public record Semana(String semana, long queda) {
    }

    private record Cuenta(BigDecimal vendiste, BigDecimal mercaderia, BigDecimal gastos, BigDecimal teDeben,
                          long ventas) {

        BigDecimal queda() {
            return vendiste.subtract(mercaderia).subtract(gastos);
        }
    }

    public static ComoVieneElNegocio armar(List<DatosDeProyecto> proyectos, LocalDate hoy) {
        Cuenta actual = cuenta(proyectos, hasta(hoy));
        if (actual.ventas() == 0) {
            return new ComoVieneElNegocio(false, 0, 0, 0, 0, 0, List.of());
        }
        long vendiste = pesos(actual.vendiste());
        long mercaderia = pesos(actual.mercaderia());
        long gastos = pesos(actual.gastos());
        return new ComoVieneElNegocio(true, vendiste, mercaderia, gastos, vendiste - mercaderia - gastos,
                pesos(actual.teDeben()), semanas(proyectos, hoy));
    }

    private static List<Semana> semanas(List<DatosDeProyecto> proyectos, LocalDate hoy) {
        List<Semana> semanas = new ArrayList<>();
        for (int atras = SEMANAS_DE_HISTORIAL - 1; atras >= 0; atras--) {
            Cuenta cuenta = cuenta(proyectos, hasta(hoy.minusDays((long) atras * DIAS_DE_UNA_SEMANA)));
            semanas.add(new Semana(atras == 0 ? HOY : "Hace " + atras + " sem.", pesos(cuenta.queda())));
        }
        return semanas;
    }

    private static PeriodoResumen hasta(LocalDate fin) {
        return new PeriodoResumen(fin.minusDays(DIAS_DEL_PERIODO - 1L), fin);
    }

    private static Cuenta cuenta(List<DatosDeProyecto> proyectos, PeriodoResumen periodo) {
        BigDecimal vendiste = BigDecimal.ZERO;
        BigDecimal mercaderia = BigDecimal.ZERO;
        BigDecimal gastos = BigDecimal.ZERO;
        BigDecimal teDeben = BigDecimal.ZERO;
        long ventas = 0;
        for (DatosDeProyecto datos : proyectos) {
            ResumenNegocio resumen = ResumenNegocio.armar(
                    datos.nombre(), datos, AnalisisZona.sinZona(), List.of(), periodo, periodo.hasta());
            vendiste = vendiste.add(resumen.ventas().ingresos());
            ventas += resumen.ventas().cantidad();
            teDeben = teDeben.add(resumen.clientes().fiado());
            mercaderia = mercaderia.add(costoDeLoVendido(datos, periodo));
            if (resumen.gastos() != null) {
                gastos = gastos.add(resumen.gastos().total().subtract(deMercaderia(resumen)));
            }
        }
        return new Cuenta(vendiste, mercaderia, gastos, teDeben, ventas);
    }

    private static BigDecimal deMercaderia(ResumenNegocio resumen) {
        return resumen.gastos().porCategoria().stream()
                .filter(categoria -> MERCADERIA.equals(categoria.categoria()))
                .map(ResumenNegocio.GastoPorCategoria::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Cantidad vendida por el costo actual de cada producto; un producto sin costo no suma. */
    private static BigDecimal costoDeLoVendido(DatosDeProyecto datos, PeriodoResumen periodo) {
        Map<RegistroId, BigDecimal> costos = new HashMap<>();
        for (BloqueInventario inventario : datos.inventarios()) {
            inventario.registros().forEach(registro -> {
                if (inventario.valorDe(registro, "cost") instanceof Number costo && costo.doubleValue() > 0) {
                    costos.put(registro.getId(), new BigDecimal(costo.toString()));
                }
            });
        }
        return ventasDelPeriodo(datos, periodo)
                .filter(venta -> costos.containsKey(venta.productoId()))
                .map(venta -> costos.get(venta.productoId()).multiply(venta.cantidad()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Las unidades vendidas de cada producto del inventario en los mismos 30 días que mira el resultado (hoy incluido).
     * Un producto sin ventas en el período queda en cero; las ventas de un producto que ya no está en el inventario no
     * se cuentan. Es la cuenta que usan los pasos concretos de los objetivos.
     */
    public static Map<RegistroId, BigDecimal> vendidasEn30Dias(DatosDeProyecto datos, LocalDate hoy) {
        Map<RegistroId, BigDecimal> vendidas = new LinkedHashMap<>();
        datos.inventarios().forEach(inventario -> inventario.registros()
                .forEach(registro -> vendidas.put(registro.getId(), BigDecimal.ZERO)));
        ventasDelPeriodo(datos, hasta(hoy))
                .filter(venta -> vendidas.containsKey(venta.productoId()))
                .forEach(venta -> vendidas.merge(venta.productoId(), venta.cantidad(), BigDecimal::add));
        return vendidas;
    }

    private static Stream<VentaProducto> ventasDelPeriodo(DatosDeProyecto datos, PeriodoResumen periodo) {
        return datos.ventas().stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .flatMap(registro -> VentaProducto.desde(bloque, registro).stream()))
                .filter(venta -> periodo.incluye(venta.fecha()));
    }

    private static long pesos(BigDecimal importe) {
        return importe.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
