package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Todo lo que le sirve al comerciante de un período de su negocio: cuánto vendió y cuánto ganó, qué se vendió más y qué
 * está parado, el stock que falta, quiénes le compran y cuánto le deben, cuánto gastó, cómo están sus precios frente
 * a la zona y qué le avisó Sami. Es lógica pura: recibe las tablas ya cargadas. Las secciones que no tienen datos
 * quedan en {@code null} (gastos y zona) o vacías, para que el documento las omita.
 *
 * @param emitidoEl el día en que se armó el resumen
 * @param gastos    {@code null} si el proyecto no tiene una tabla de gastos con importe
 * @param zona      {@code null} si la zona no alcanza el mínimo de comercios o ningún producto tiene rango
 */
public record ResumenNegocio(
        String negocio,
        String sistema,
        PeriodoResumen periodo,
        LocalDate emitidoEl,
        Ventas ventas,
        List<ProductoVendido> masVendidos,
        List<ProductoParado> sinMovimiento,
        Stock stock,
        Clientes clientes,
        Gastos gastos,
        ZonaResumen zona,
        AvisosResumen avisos) {

    static final int MAXIMO_PRODUCTOS = 10;
    static final int MAXIMO_FALTANTES = 15;
    static final int MAXIMO_CLIENTES = 5;
    static final int MAXIMO_DESALINEADOS = 5;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal DOS = BigDecimal.valueOf(2);
    private static final String SIN_NOMBRE = "Producto sin nombre";
    private static final String ELIMINADO = "Producto eliminado";
    private static final String SIN_CATEGORIA = "Sin categoría";
    private static final String CLIENTE_SIN_NOMBRE = "Cliente sin nombre";
    private static final Pattern ID = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /**
     * @param ingresos            lo que entró por las ventas con precio; las ventas sin precio cuentan en las unidades pero no acá
     * @param ingresoPorDia       los ingresos repartidos en todos los días del período
     * @param variacion           cuánto cambiaron los ingresos contra el período anterior (en %); {@code null} si antes no hubo
     * @param gananciaEstimada    ventas menos el costo actual de lo vendido; {@code null} si ningún producto vendido tiene costo
     */
    public record Ventas(
            BigDecimal ingresos,
            BigDecimal unidades,
            long cantidad,
            BigDecimal ingresoPorDia,
            BigDecimal ingresosAnteriores,
            BigDecimal variacion,
            BigDecimal gananciaEstimada) {
    }

    public record ProductoVendido(String nombre, BigDecimal unidades, BigDecimal ingresos, BigDecimal ganancia) {
    }

    /** Un producto con stock que no se vendió en el período; {@code capital} es lo que hay invertido (cantidad por costo). */
    public record ProductoParado(String nombre, BigDecimal cantidad, BigDecimal capital) {
    }

    /** Producto agotado o bajo su mínimo; {@code pedir} es lo que haría falta para llegar al doble del mínimo. */
    public record Faltante(String nombre, BigDecimal cantidad, BigDecimal minimo, BigDecimal pedir) {
    }

    public record Stock(
            long productos,
            long totalFaltantes,
            List<Faltante> faltantes,
            BigDecimal valorACosto,
            BigDecimal valorAPrecio) {
    }

    public record ClienteTop(String nombre, long compras, BigDecimal ingresos) {
    }

    /** {@code fiado} es lo vendido a fiado en el período. */
    public record Clientes(List<ClienteTop> top, BigDecimal fiado, long ventasFiadas) {

        public boolean vacio() {
            return top.isEmpty() && ventasFiadas == 0;
        }
    }

    public record GastoPorCategoria(String categoria, BigDecimal total) {
    }

    /** {@code resultado} son los ingresos del período menos los gastos. */
    public record Gastos(BigDecimal total, long cantidad, List<GastoPorCategoria> porCategoria, BigDecimal resultado) {
    }

    /** {@code porcentaje} es cuánto se aleja del rango: negativo si está por debajo del mínimo, positivo si pasa el máximo. */
    public record ProductoDesalineado(String nombre, BigDecimal precio, BigDecimal minimo, BigDecimal maximo,
                                      BigDecimal porcentaje) {
    }

    public record ZonaResumen(String nombre, long comercios, long debajo, long dentro, long encima,
                              List<ProductoDesalineado> desalineados) {
    }

    /**
     * @param pendientes           avisos sin resolver ahora, por tipo
     * @param aplicados            los que se aplicaron en el período
     * @param descartados          los que se descartaron en el período
     * @param medidos              los aplicados cuyo impacto ya se midió
     * @param ingresoSemanalDelta  lo que cambió el ingreso semanal en los medidos: una comparación, no una causa
     */
    public record AvisosResumen(
            Map<TipoAviso, Long> pendientes,
            long aplicados,
            long descartados,
            long medidos,
            BigDecimal ingresoSemanalDelta) {

        public long totalPendientes() {
            return pendientes.values().stream().mapToLong(Long::longValue).sum();
        }
    }

    private record Producto(String nombre, BigDecimal cantidad, BigDecimal precio, BigDecimal costo, BigDecimal minimo) {
    }

    private record Venta(VentaProducto venta, boolean fiado) {

        BigDecimal ingreso() {
            return venta.precio() == null ? BigDecimal.ZERO : venta.precio().multiply(venta.cantidad());
        }
    }

    /**
     * Arma el resumen del período con los datos del proyecto.
     *
     * @param avisos los avisos del proyecto, de cualquier estado
     */
    public static ResumenNegocio armar(String negocio, DatosDeProyecto datos, AnalisisZona zona, List<Aviso> avisos,
                                       PeriodoResumen periodo, LocalDate hoy) {
        Map<RegistroId, Producto> productos = productosDe(datos.inventarios());
        List<Venta> todas = ventasDe(datos.ventas());
        List<Venta> delPeriodo = todas.stream().filter(venta -> periodo.incluye(venta.venta().fecha())).toList();
        BigDecimal ingresos = suma(delPeriodo.stream().map(Venta::ingreso));
        return new ResumenNegocio(
                negocio,
                datos.nombre(),
                periodo,
                hoy,
                ventasDelPeriodo(todas, delPeriodo, productos, periodo, ingresos),
                masVendidos(delPeriodo, productos),
                sinMovimiento(delPeriodo, productos),
                stock(productos),
                clientes(delPeriodo, datos.nombresDeClientes()),
                gastos(datos.gastos(), periodo, ingresos),
                zona(zona, productos),
                avisos(avisos, periodo));
    }

    // --- ventas ---

    private static Ventas ventasDelPeriodo(List<Venta> todas, List<Venta> delPeriodo, Map<RegistroId, Producto> productos,
                                           PeriodoResumen periodo, BigDecimal ingresos) {
        PeriodoResumen anterior = periodo.anterior();
        BigDecimal ingresosAnteriores = suma(todas.stream()
                .filter(venta -> anterior.incluye(venta.venta().fecha()))
                .map(Venta::ingreso));
        BigDecimal variacion = ingresosAnteriores.signum() > 0
                ? ingresos.subtract(ingresosAnteriores).multiply(CIEN).divide(ingresosAnteriores, 0, RoundingMode.HALF_UP)
                : null;
        return new Ventas(
                ingresos,
                suma(delPeriodo.stream().map(venta -> venta.venta().cantidad())),
                delPeriodo.size(),
                ingresos.divide(BigDecimal.valueOf(periodo.dias()), 2, RoundingMode.HALF_UP),
                ingresosAnteriores,
                variacion,
                ganancia(delPeriodo, productos));
    }

    /** Lo vendido con precio menos el costo actual de cada producto; sólo cuentan los productos que tienen costo. */
    private static BigDecimal ganancia(List<Venta> ventas, Map<RegistroId, Producto> productos) {
        List<BigDecimal> ganancias = ventas.stream()
                .filter(venta -> venta.venta().precio() != null)
                .map(venta -> {
                    Producto producto = productos.get(venta.venta().productoId());
                    return producto == null || producto.costo() == null
                            ? null
                            : venta.venta().precio().subtract(producto.costo()).multiply(venta.venta().cantidad());
                })
                .filter(Objects::nonNull)
                .toList();
        return ganancias.isEmpty() ? null : suma(ganancias.stream());
    }

    private static List<ProductoVendido> masVendidos(List<Venta> delPeriodo, Map<RegistroId, Producto> productos) {
        return delPeriodo.stream()
                .collect(Collectors.groupingBy(venta -> venta.venta().productoId(), LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet().stream()
                .map(entrada -> {
                    Producto producto = productos.get(entrada.getKey());
                    return new ProductoVendido(
                            producto == null ? ELIMINADO : producto.nombre(),
                            suma(entrada.getValue().stream().map(venta -> venta.venta().cantidad())),
                            suma(entrada.getValue().stream().map(Venta::ingreso)),
                            ganancia(entrada.getValue(), productos));
                })
                .sorted(Comparator.comparing(ProductoVendido::unidades).reversed()
                        .thenComparing(ProductoVendido::ingresos, Comparator.reverseOrder()))
                .limit(MAXIMO_PRODUCTOS)
                .toList();
    }

    private static List<ProductoParado> sinMovimiento(List<Venta> delPeriodo, Map<RegistroId, Producto> productos) {
        Set<RegistroId> vendidos = delPeriodo.stream().map(venta -> venta.venta().productoId())
                .collect(Collectors.toCollection(HashSet::new));
        return productos.entrySet().stream()
                .filter(entrada -> !vendidos.contains(entrada.getKey()))
                .map(Map.Entry::getValue)
                .filter(producto -> producto.cantidad() != null && producto.cantidad().signum() > 0)
                .map(producto -> new ProductoParado(producto.nombre(), producto.cantidad(),
                        producto.costo() == null ? null : producto.cantidad().multiply(producto.costo())))
                .sorted(Comparator.comparing(ProductoParado::capital, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAXIMO_PRODUCTOS)
                .toList();
    }

    // --- stock ---

    private static Stock stock(Map<RegistroId, Producto> productos) {
        List<Faltante> faltantes = productos.values().stream()
                .filter(ResumenNegocio::estaEnFalta)
                .map(ResumenNegocio::faltante)
                .sorted(Comparator.comparing(Faltante::cantidad))
                .toList();
        return new Stock(
                productos.size(),
                faltantes.size(),
                faltantes.stream().limit(MAXIMO_FALTANTES).toList(),
                suma(productos.values().stream()
                        .filter(producto -> hayStock(producto) && producto.costo() != null)
                        .map(producto -> producto.cantidad().multiply(producto.costo()))),
                suma(productos.values().stream()
                        .filter(producto -> hayStock(producto) && producto.precio() != null)
                        .map(producto -> producto.cantidad().multiply(producto.precio()))));
    }

    private static boolean hayStock(Producto producto) {
        return producto.cantidad() != null && producto.cantidad().signum() > 0;
    }

    /** Agotado, o en su mínimo o por debajo cuando definió uno. */
    private static boolean estaEnFalta(Producto producto) {
        if (producto.cantidad() == null) {
            return false;
        }
        boolean conMinimo = producto.minimo() != null && producto.minimo().signum() > 0;
        return producto.cantidad().signum() <= 0 || (conMinimo && producto.cantidad().compareTo(producto.minimo()) <= 0);
    }

    private static Faltante faltante(Producto producto) {
        boolean conMinimo = producto.minimo() != null && producto.minimo().signum() > 0;
        BigDecimal pedir = conMinimo
                ? producto.minimo().multiply(DOS).subtract(producto.cantidad()).max(BigDecimal.ONE)
                : null;
        return new Faltante(producto.nombre(), producto.cantidad(), producto.minimo(), pedir);
    }

    // --- clientes y fiado ---

    private static Clientes clientes(List<Venta> delPeriodo, Map<String, String> nombresDeClientes) {
        List<ClienteTop> top = delPeriodo.stream()
                .filter(venta -> venta.venta().cliente() != null)
                .collect(Collectors.groupingBy(
                        venta -> TextoNormalizado.de(nombreDelCliente(venta.venta().cliente(), nombresDeClientes)),
                        LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(compras -> new ClienteTop(
                        nombreDelCliente(compras.getFirst().venta().cliente(), nombresDeClientes), compras.size(),
                        suma(compras.stream().map(Venta::ingreso))))
                .sorted(Comparator.comparing(ClienteTop::ingresos).reversed())
                .limit(MAXIMO_CLIENTES)
                .toList();
        List<Venta> fiadas = delPeriodo.stream().filter(Venta::fiado).toList();
        return new Clientes(top, suma(fiadas.stream().map(Venta::ingreso)), fiadas.size());
    }

    /**
     * El cliente como se lo muestra: si la venta guardó la fila de la tabla de clientes, su nombre; si es un
     * identificador que no se puede resolver, «Cliente sin nombre». Un id nunca llega al documento.
     */
    private static String nombreDelCliente(String valor, Map<String, String> nombresDeClientes) {
        String nombre = nombresDeClientes.get(valor);
        if (nombre != null) {
            return nombre;
        }
        return ID.matcher(valor).matches() ? CLIENTE_SIN_NOMBRE : valor;
    }

    // --- gastos ---

    private static Gastos gastos(List<BloqueGastos> bloques, PeriodoResumen periodo, BigDecimal ingresos) {
        List<BloqueGastos> conImporte = bloques.stream().filter(BloqueGastos::tieneImporte).toList();
        if (conImporte.isEmpty()) {
            return null;
        }
        Map<String, BigDecimal> porCategoria = new LinkedHashMap<>();
        long cantidad = 0;
        for (BloqueGastos bloque : conImporte) {
            for (Registro registro : bloque.registros()) {
                BigDecimal importe = numero(bloque.valorDe(registro, "total"));
                if (importe != null && periodo.incluye(fecha(bloque.valorDe(registro, "date")))) {
                    porCategoria.merge(categoria(bloque.valorDe(registro, "category")), importe, BigDecimal::add);
                    cantidad++;
                }
            }
        }
        BigDecimal total = suma(porCategoria.values().stream());
        List<GastoPorCategoria> ordenadas = porCategoria.entrySet().stream()
                .map(entrada -> new GastoPorCategoria(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparing(GastoPorCategoria::total).reversed())
                .toList();
        return new Gastos(total, cantidad, ordenadas, ingresos.subtract(total));
    }

    private static String categoria(Object valor) {
        return valor instanceof String texto && !texto.isBlank() ? texto.trim() : SIN_CATEGORIA;
    }

    // --- zona ---

    private static ZonaResumen zona(AnalisisZona zona, Map<RegistroId, Producto> productos) {
        long debajo = 0;
        long dentro = 0;
        long encima = 0;
        List<ProductoDesalineado> desalineados = new ArrayList<>();
        for (Producto producto : productos.values()) {
            AgregadoZonaProducto rango = producto.precio() == null
                    ? null
                    : zona.productoConRango(producto.nombre()).orElse(null);
            if (rango == null) {
                continue;
            }
            if (producto.precio().compareTo(rango.precioMinimo()) < 0) {
                debajo++;
                desalineados.add(desalineado(producto, rango, producto.precio().subtract(rango.precioMinimo()),
                        rango.precioMinimo()));
            } else if (producto.precio().compareTo(rango.precioMaximo()) > 0) {
                encima++;
                desalineados.add(desalineado(producto, rango, producto.precio().subtract(rango.precioMaximo()),
                        rango.precioMaximo()));
            } else {
                dentro++;
            }
        }
        if (debajo + dentro + encima == 0) {
            return null;
        }
        List<ProductoDesalineado> masAlejados = desalineados.stream()
                .sorted(Comparator.comparing((ProductoDesalineado producto) -> producto.porcentaje().abs()).reversed())
                .limit(MAXIMO_DESALINEADOS)
                .toList();
        return new ZonaResumen(zona.nombre(), zona.comercios(), debajo, dentro, encima, masAlejados);
    }

    private static ProductoDesalineado desalineado(Producto producto, AgregadoZonaProducto rango, BigDecimal diferencia,
                                                   BigDecimal referencia) {
        return new ProductoDesalineado(producto.nombre(), producto.precio(), rango.precioMinimo(), rango.precioMaximo(),
                diferencia.multiply(CIEN).divide(referencia, 0, RoundingMode.HALF_UP));
    }

    // --- avisos ---

    private static AvisosResumen avisos(List<Aviso> avisos, PeriodoResumen periodo) {
        Map<TipoAviso, Long> pendientes = new EnumMap<>(TipoAviso.class);
        avisos.stream().filter(Aviso::estaPendiente)
                .forEach(aviso -> pendientes.merge(aviso.getTipo(), 1L, Long::sum));
        List<Aviso> aplicados = resueltosEn(avisos, EstadoAviso.APLICADO, periodo);
        List<ImpactoAviso> medidos = aplicados.stream().flatMap(aviso -> aviso.impacto().stream()).toList();
        return new AvisosResumen(
                pendientes,
                aplicados.size(),
                resueltosEn(avisos, EstadoAviso.DESCARTADO, periodo).size(),
                medidos.size(),
                suma(medidos.stream().map(ImpactoAviso::deltaIngresoSemanal)));
    }

    private static List<Aviso> resueltosEn(List<Aviso> avisos, EstadoAviso estado, PeriodoResumen periodo) {
        return avisos.stream()
                .filter(aviso -> aviso.getEstado() == estado && aviso.getResueltoEn() != null)
                .filter(aviso -> periodo.incluye(ZonaNegocio.dia(aviso.getResueltoEn())))
                .toList();
    }

    // --- lectura de las tablas ---

    private static Map<RegistroId, Producto> productosDe(List<BloqueInventario> inventarios) {
        Map<RegistroId, Producto> productos = new LinkedHashMap<>();
        for (BloqueInventario inventario : inventarios) {
            for (Registro registro : inventario.registros()) {
                productos.put(registro.getId(), new Producto(
                        nombre(inventario.valorDe(registro, "name")),
                        numero(inventario.valorDe(registro, "quantity")),
                        positivo(inventario.valorDe(registro, "price")),
                        positivo(inventario.valorDe(registro, "cost")),
                        numero(inventario.valorDe(registro, "minimum"))));
            }
        }
        return productos;
    }

    private static List<Venta> ventasDe(List<BloqueVentas> bloques) {
        return bloques.stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .flatMap(registro -> VentaProducto.desde(bloque, registro).stream()
                                .map(venta -> new Venta(venta, Boolean.TRUE.equals(bloque.valorDe(registro, "credit"))))))
                .toList();
    }

    private static String nombre(Object valor) {
        return valor instanceof String texto && !texto.isBlank() ? texto.trim() : SIN_NOMBRE;
    }

    private static BigDecimal numero(Object valor) {
        return valor instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }

    private static BigDecimal positivo(Object valor) {
        BigDecimal numero = numero(valor);
        return numero != null && numero.signum() > 0 ? numero : null;
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
