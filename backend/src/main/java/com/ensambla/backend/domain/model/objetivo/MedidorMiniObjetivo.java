package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Lo que se mide de un negocio hoy para proponer y seguir mini-objetivos: sus productos, sus ventas, sus gastos por
 * categoría y el resultado de los últimos 30 días. Es lógica pura: recibe las tablas de todos los sistemas del negocio
 * ya cargadas. «Semana» es «los últimos 7 días» y «mes», «los últimos 30», contando hoy y en hora argentina.
 */
public final class MedidorMiniObjetivo {

    public static final int DIAS_SEMANA = 7;

    public static final int DIAS_MES = PeriodoResumen.DIAS_POR_DEFECTO;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /** La categoría de la compra de mercadería ya se descuenta al vender: no es un gasto que recortar. */
    private static final String MERCADERIA = TextoNormalizado.de("Mercadería");

    /** Un producto del inventario con nombre, precio y costo cargados. */
    public record Producto(RegistroId id, String nombre, BigDecimal precio, BigDecimal costo, BigDecimal stock) {

        public BigDecimal gananciaPorUnidad() {
            return precio.subtract(costo);
        }
    }

    /** Lo gastado en una categoría en los últimos 30 días. */
    public record Gasto(String categoria, BigDecimal total) {
    }

    private final LocalDate hoy;

    private final Map<RegistroId, Producto> productos;

    private final List<VentaProducto> ventas;

    private final ComoVieneElNegocio negocio;

    private final List<Gasto> gastos;

    private MedidorMiniObjetivo(LocalDate hoy, Map<RegistroId, Producto> productos, List<VentaProducto> ventas,
                                ComoVieneElNegocio negocio, List<Gasto> gastos) {
        this.hoy = hoy;
        this.productos = productos;
        this.ventas = ventas;
        this.negocio = negocio;
        this.gastos = gastos;
    }

    public static MedidorMiniObjetivo de(List<DatosDeProyecto> datos, MetricasVentas metricasVentas, LocalDate hoy) {
        return new MedidorMiniObjetivo(
                hoy,
                productosDe(datos),
                metricasVentas.ventasValidas(datos.stream().flatMap(dato -> dato.ventas().stream()).toList(), hoy),
                ComoVieneElNegocio.armar(datos, hoy),
                gastosDe(datos, hoy));
    }

    /** Si hubo alguna venta en los últimos 30 días; sin ventas no hay nada que medir ni proponer. */
    public boolean hayVentas() {
        return negocio.hayVentas();
    }

    /** Lo que queda de lo vendido en los últimos 30 días después de pagar la mercadería y los gastos. */
    public BigDecimal resultado() {
        return BigDecimal.valueOf(negocio.queda());
    }

    public long vendiste() {
        return negocio.vendiste();
    }

    public List<Producto> productos() {
        return List.copyOf(productos.values());
    }

    public List<Gasto> gastos() {
        return gastos;
    }

    /** Lo que queda de cada $100 vendidos después de pagar la mercadería; vacío si no hay costos con qué calcularlo. */
    public Optional<BigDecimal> margen() {
        if (negocio.vendiste() <= 0 || negocio.mercaderia() <= 0) {
            return Optional.empty();
        }
        return Optional.of(BigDecimal.valueOf(negocio.vendiste() - negocio.mercaderia())
                .multiply(CIEN)
                .divide(BigDecimal.valueOf(negocio.vendiste()), 2, RoundingMode.HALF_UP));
    }

    /** Lo vendido con precio en los últimos 7 días. */
    public BigDecimal ventasSemana() {
        return ventasDe(DIAS_SEMANA)
                .filter(venta -> venta.precio() != null)
                .map(venta -> venta.precio().multiply(venta.cantidad()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Las unidades que se vendieron del producto en los últimos {@code dias} días. */
    public BigDecimal unidades(RegistroId productoId, int dias) {
        return ventasDe(dias)
                .filter(venta -> venta.productoId().equals(productoId))
                .map(VentaProducto::cantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Lo que dejó el producto en los últimos 7 días: lo vendido (al precio de cada venta) menos su costo actual. */
    public BigDecimal gananciaSemanal(Producto producto) {
        return ventasDe(DIAS_SEMANA)
                .filter(venta -> venta.productoId().equals(producto.id()))
                .map(venta -> (venta.precio() == null ? producto.precio() : venta.precio())
                        .subtract(producto.costo())
                        .multiply(venta.cantidad()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * El valor de la métrica de un mini-objetivo hoy; vacío si ya no se puede medir (el producto salió del cuaderno o
     * perdió su precio o su costo, o no hay costos para el margen).
     */
    public Optional<BigDecimal> valorDe(TipoMiniObjetivo tipo, RegistroId productoId, String referencia) {
        Optional<Producto> producto = Optional.ofNullable(productoId).map(productos::get);
        return switch (tipo) {
            case SUBIR_PRECIO -> producto.map(this::gananciaSemanal);
            case MOVER_PARADO, PONER_A_LA_VISTA -> producto.map(dato -> unidades(dato.id(), DIAS_SEMANA));
            case RECORTAR_GASTO -> Optional.of(gastoDe(referencia));
            case MEJORAR_MARGEN -> margen();
            case VENTAS_SEMANALES -> Optional.of(ventasSemana());
        };
    }

    private BigDecimal gastoDe(String categoria) {
        String clave = TextoNormalizado.de(categoria);
        return gastos.stream()
                .filter(gasto -> TextoNormalizado.de(gasto.categoria()).equals(clave))
                .map(Gasto::total)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    /** Las ventas válidas de los últimos {@code dias} días, hoy incluido. */
    private Stream<VentaProducto> ventasDe(int dias) {
        return ventas.stream().filter(venta -> venta.fecha().isAfter(hoy.minusDays(dias)));
    }

    private static Map<RegistroId, Producto> productosDe(List<DatosDeProyecto> datos) {
        Map<RegistroId, Producto> productos = new LinkedHashMap<>();
        for (DatosDeProyecto dato : datos) {
            for (BloqueInventario inventario : dato.inventarios()) {
                inventario.registros().forEach(registro -> productoDe(inventario, registro)
                        .ifPresent(producto -> productos.put(producto.id(), producto)));
            }
        }
        return productos;
    }

    /** Un producto sirve si tiene nombre, precio y costo cargados; sin stock cargado cuenta como cero. */
    private static Optional<Producto> productoDe(BloqueInventario inventario, Registro registro) {
        BigDecimal precio = positivo(inventario.valorDe(registro, "price"));
        BigDecimal costo = positivo(inventario.valorDe(registro, "cost"));
        if (!(inventario.valorDe(registro, "name") instanceof String nombre) || nombre.isBlank()
                || precio == null || costo == null) {
            return Optional.empty();
        }
        BigDecimal stock = numero(inventario.valorDe(registro, "quantity"));
        return Optional.of(new Producto(
                registro.getId(), nombre.trim(), precio, costo, stock == null ? BigDecimal.ZERO : stock));
    }

    /** El gasto de cada categoría en los últimos 30 días, de todos los sistemas, sin la mercadería. */
    private static List<Gasto> gastosDe(List<DatosDeProyecto> datos, LocalDate hoy) {
        PeriodoResumen periodo = new PeriodoResumen(hoy.minusDays(DIAS_MES - 1L), hoy);
        Map<String, Gasto> porCategoria = new LinkedHashMap<>();
        for (DatosDeProyecto dato : datos) {
            ResumenNegocio resumen = ResumenNegocio.armar(
                    dato.nombre(), dato, AnalisisZona.sinZona(), List.of(), periodo, hoy);
            if (resumen.gastos() != null) {
                resumen.gastos().porCategoria().stream()
                        .filter(gasto -> !MERCADERIA.equals(TextoNormalizado.de(gasto.categoria())))
                        .forEach(gasto -> porCategoria.merge(
                                TextoNormalizado.de(gasto.categoria()),
                                new Gasto(gasto.categoria(), gasto.total()),
                                (anterior, nuevo) -> new Gasto(anterior.categoria(),
                                        anterior.total().add(nuevo.total()))));
            }
        }
        return porCategoria.values().stream()
                .filter(gasto -> gasto.total().signum() > 0)
                .toList();
    }

    private static BigDecimal numero(Object valor) {
        return valor instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }

    private static BigDecimal positivo(Object valor) {
        BigDecimal numero = numero(valor);
        return numero != null && numero.signum() > 0 ? numero : null;
    }
}
