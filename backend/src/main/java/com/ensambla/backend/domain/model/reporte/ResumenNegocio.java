package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.common.ValorDeRegistro;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    /** {@code fiado} es el saldo actual de las ventas del período, incluidas las anotadas manualmente a fiado. */
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

    /** Un producto del inventario comparado con el rango de precios de la zona. */
    private record Comparado(ProductoDelResumen producto, AgregadoZonaProducto rango, PosicionEnLaZona posicion) {

        Optional<ProductoDesalineado> desalineado() {
            return posicion.desalineado(producto, rango);
        }
    }

    /** Un gasto cargado dentro del período, con la categoría como se la muestra. */
    private record GastoDelPeriodo(String categoria, BigDecimal importe) {
    }

    /**
     * Arma el resumen del período con los datos del proyecto.
     *
     * @param avisos los avisos del proyecto, de cualquier estado
     */
    public static ResumenNegocio armar(String negocio, DatosDeProyecto datos, AnalisisZona zona, List<Aviso> avisos,
                                       PeriodoResumen periodo, LocalDate hoy) {
        Map<RegistroId, ProductoDelResumen> productos = productosDe(datos.inventarios());
        List<VentaDelResumen> todas = ventasRegistradas(datos);
        List<VentaDelResumen> delPeriodo = ventasEn(todas, periodo);
        BigDecimal ingresos = ingresosDe(delPeriodo);
        return new ResumenNegocio(negocio, datos.nombre(), periodo, hoy,
                ventasDelPeriodo(todas, delPeriodo, productos, periodo, ingresos),
                masVendidos(delPeriodo, productos),
                sinMovimiento(delPeriodo, productos),
                stock(productos),
                clientes(delPeriodo, datos, periodo),
                gastos(datos.gastos(), periodo, ingresos),
                zona(zona, productos),
                avisos(avisos, periodo));
    }

    // --- ventas ---

    private static Ventas ventasDelPeriodo(List<VentaDelResumen> todas, List<VentaDelResumen> delPeriodo,
                                           Map<RegistroId, ProductoDelResumen> productos, PeriodoResumen periodo,
                                           BigDecimal ingresos) {
        BigDecimal ingresosAnteriores = ingresosDe(ventasEn(todas, periodo.anterior()));
        return new Ventas(
                ingresos,
                unidadesDe(delPeriodo),
                delPeriodo.size(),
                ingresos.divide(BigDecimal.valueOf(periodo.dias()), 2, RoundingMode.HALF_UP),
                ingresosAnteriores,
                variacion(ingresos, ingresosAnteriores),
                ganancia(delPeriodo, productos));
    }

    /** Cuánto cambiaron los ingresos en porcentaje; {@code null} si antes no hubo ingresos contra qué comparar. */
    private static BigDecimal variacion(BigDecimal ingresos, BigDecimal anteriores) {
        if (anteriores.signum() <= 0) {
            return null;
        }
        return ingresos.subtract(anteriores).multiply(CIEN).divide(anteriores, 0, RoundingMode.HALF_UP);
    }

    /** Lo vendido con precio menos el costo actual de cada producto; sólo cuentan los productos que tienen costo. */
    private static BigDecimal ganancia(List<VentaDelResumen> ventas, Map<RegistroId, ProductoDelResumen> productos) {
        List<BigDecimal> ganancias = ventas.stream()
                .map(venta -> venta.ganancia(productos))
                .filter(Objects::nonNull)
                .toList();
        return ganancias.isEmpty() ? null : suma(ganancias.stream());
    }

    private static List<ProductoVendido> masVendidos(List<VentaDelResumen> delPeriodo,
                                                     Map<RegistroId, ProductoDelResumen> productos) {
        return porProducto(delPeriodo).entrySet().stream()
                .map(entrada -> productoVendido(entrada.getKey(), entrada.getValue(), productos))
                .sorted(Comparator.comparing(ProductoVendido::unidades).reversed()
                        .thenComparing(ProductoVendido::ingresos, Comparator.reverseOrder()))
                .limit(MAXIMO_PRODUCTOS)
                .toList();
    }

    private static Map<RegistroId, List<VentaDelResumen>> porProducto(List<VentaDelResumen> ventas) {
        return ventas.stream()
                .filter(VentaDelResumen::esDeUnProducto)
                .collect(Collectors.groupingBy(VentaDelResumen::productoId, LinkedHashMap::new, Collectors.toList()));
    }

    private static ProductoVendido productoVendido(RegistroId productoId, List<VentaDelResumen> ventas,
                                                   Map<RegistroId, ProductoDelResumen> productos) {
        ProductoDelResumen producto = productos.get(productoId);
        String nombre = producto == null ? ELIMINADO : producto.nombre();
        return new ProductoVendido(nombre, unidadesDe(ventas), ingresosDe(ventas), ganancia(ventas, productos));
    }

    private static List<ProductoParado> sinMovimiento(List<VentaDelResumen> delPeriodo,
                                                      Map<RegistroId, ProductoDelResumen> productos) {
        Set<RegistroId> vendidos = delPeriodo.stream().map(VentaDelResumen::productoId).collect(Collectors.toSet());
        return productos.entrySet().stream()
                .filter(entrada -> !vendidos.contains(entrada.getKey()))
                .map(Map.Entry::getValue)
                .filter(ProductoDelResumen::hayStock)
                .map(ProductoDelResumen::parado)
                .sorted(Comparator.comparing(ProductoParado::capital, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAXIMO_PRODUCTOS)
                .toList();
    }

    // --- stock ---

    private static Stock stock(Map<RegistroId, ProductoDelResumen> productos) {
        List<Faltante> faltantes = productos.values().stream()
                .filter(ProductoDelResumen::estaEnFalta)
                .map(ProductoDelResumen::faltante)
                .sorted(Comparator.comparing(Faltante::cantidad))
                .toList();
        return new Stock(productos.size(), faltantes.size(), faltantes.stream().limit(MAXIMO_FALTANTES).toList(),
                suma(productos.values().stream().map(ProductoDelResumen::valorACosto)),
                suma(productos.values().stream().map(ProductoDelResumen::valorAPrecio)));
    }

    // --- clientes y fiado ---

    private static Clientes clientes(List<VentaDelResumen> delPeriodo, DatosDeProyecto datos, PeriodoResumen periodo) {
        List<VentaDelResumen> fiadas = delPeriodo.stream().filter(VentaDelResumen::fiado).toList();
        List<VentaCaja> pendientesDeCaja = ventasDeCajaConSaldo(datos.ventasCaja(), periodo);
        BigDecimal fiado = ingresosDe(fiadas).add(suma(pendientesDeCaja.stream().map(VentaCaja::saldo)));
        return new Clientes(mejoresClientes(delPeriodo, datos.nombresDeClientes()), fiado,
                fiadas.size() + pendientesDeCaja.size());
    }

    private static List<ClienteTop> mejoresClientes(List<VentaDelResumen> delPeriodo, Map<String, String> nombres) {
        return delPeriodo.stream()
                .filter(VentaDelResumen::tieneCliente)
                .collect(Collectors.groupingBy(venta -> TextoNormalizado.de(nombreDelCliente(venta.cliente(), nombres)),
                        LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(compras -> clienteTop(compras, nombres))
                .sorted(Comparator.comparing(ClienteTop::ingresos).reversed())
                .limit(MAXIMO_CLIENTES)
                .toList();
    }

    private static ClienteTop clienteTop(List<VentaDelResumen> compras, Map<String, String> nombres) {
        return new ClienteTop(nombreDelCliente(compras.getFirst().cliente(), nombres), compras.size(),
                ingresosDe(compras));
    }

    private static List<VentaCaja> ventasDeCajaConSaldo(List<VentaCaja> ventasCaja, PeriodoResumen periodo) {
        return ventasCaja.stream()
                .filter(venta -> periodo.incluye(ZonaNegocio.dia(venta.creadoEn())))
                .filter(venta -> venta.saldo().signum() > 0)
                .toList();
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
        List<GastoDelPeriodo> gastos = gastosDelPeriodo(conImporte, periodo);
        BigDecimal total = suma(gastos.stream().map(GastoDelPeriodo::importe));
        return new Gastos(total, gastos.size(), porCategoria(gastos), ingresos.subtract(total));
    }

    private static List<GastoDelPeriodo> gastosDelPeriodo(List<BloqueGastos> bloques, PeriodoResumen periodo) {
        return bloques.stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .flatMap(registro -> gastoDelPeriodo(bloque, registro, periodo).stream()))
                .toList();
    }

    /** El gasto del registro si tiene importe y una fecha dentro del período. */
    private static Optional<GastoDelPeriodo> gastoDelPeriodo(BloqueGastos bloque, Registro registro,
                                                             PeriodoResumen periodo) {
        BigDecimal importe = ValorDeRegistro.de(bloque.valorDe(registro, "total")).comoNumero();
        LocalDate fecha = ValorDeRegistro.de(bloque.valorDe(registro, "date")).comoFecha();
        if (importe == null || !periodo.incluye(fecha)) {
            return Optional.empty();
        }
        String categoria = ValorDeRegistro.de(bloque.valorDe(registro, "category")).comoTextoO(SIN_CATEGORIA);
        return Optional.of(new GastoDelPeriodo(categoria, importe));
    }

    private static List<GastoPorCategoria> porCategoria(List<GastoDelPeriodo> gastos) {
        return gastos.stream()
                .collect(Collectors.groupingBy(GastoDelPeriodo::categoria, LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, GastoDelPeriodo::importe, BigDecimal::add)))
                .entrySet().stream()
                .map(entrada -> new GastoPorCategoria(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparing(GastoPorCategoria::total).reversed())
                .toList();
    }

    // --- zona ---

    private static ZonaResumen zona(AnalisisZona zona, Map<RegistroId, ProductoDelResumen> productos) {
        List<Comparado> comparados = comparadosConLaZona(zona, productos);
        if (comparados.isEmpty()) {
            return null;
        }
        return new ZonaResumen(zona.nombre(), zona.comercios(),
                cuantosQuedan(comparados, PosicionEnLaZona.DEBAJO),
                cuantosQuedan(comparados, PosicionEnLaZona.DENTRO),
                cuantosQuedan(comparados, PosicionEnLaZona.ENCIMA),
                masAlejados(comparados));
    }

    private static List<Comparado> comparadosConLaZona(AnalisisZona zona, Map<RegistroId, ProductoDelResumen> productos) {
        return productos.values().stream()
                .filter(producto -> producto.precio() != null)
                .flatMap(producto -> zona.productoConRango(producto.nombre()).stream()
                        .map(rango -> new Comparado(producto, rango, PosicionEnLaZona.de(producto.precio(), rango))))
                .toList();
    }

    private static long cuantosQuedan(List<Comparado> comparados, PosicionEnLaZona posicion) {
        return comparados.stream().filter(comparado -> comparado.posicion() == posicion).count();
    }

    private static List<ProductoDesalineado> masAlejados(List<Comparado> comparados) {
        return comparados.stream()
                .flatMap(comparado -> comparado.desalineado().stream())
                .sorted(Comparator.comparing((ProductoDesalineado producto) -> producto.porcentaje().abs()).reversed())
                .limit(MAXIMO_DESALINEADOS)
                .toList();
    }

    // --- avisos ---

    private static AvisosResumen avisos(List<Aviso> avisos, PeriodoResumen periodo) {
        List<Aviso> aplicados = resueltosEn(avisos, EstadoAviso.APLICADO, periodo);
        List<ImpactoAviso> medidos = aplicados.stream().flatMap(aviso -> aviso.impacto().stream()).toList();
        return new AvisosResumen(
                pendientesPorTipo(avisos),
                aplicados.size(),
                resueltosEn(avisos, EstadoAviso.DESCARTADO, periodo).size(),
                medidos.size(),
                suma(medidos.stream().map(ImpactoAviso::deltaIngresoSemanal)));
    }

    private static Map<TipoAviso, Long> pendientesPorTipo(List<Aviso> avisos) {
        Map<TipoAviso, Long> pendientes = new EnumMap<>(TipoAviso.class);
        avisos.stream().filter(Aviso::estaPendiente)
                .forEach(aviso -> pendientes.merge(aviso.getTipo(), 1L, Long::sum));
        return pendientes;
    }

    private static List<Aviso> resueltosEn(List<Aviso> avisos, EstadoAviso estado, PeriodoResumen periodo) {
        return avisos.stream()
                .filter(aviso -> aviso.getEstado() == estado && aviso.getResueltoEn() != null)
                .filter(aviso -> periodo.incluye(ZonaNegocio.dia(aviso.getResueltoEn())))
                .toList();
    }

    // --- lectura de las tablas ---

    private static Map<RegistroId, ProductoDelResumen> productosDe(List<BloqueInventario> inventarios) {
        Map<RegistroId, ProductoDelResumen> productos = new LinkedHashMap<>();
        inventarios.forEach(inventario -> inventario.registros().forEach(registro ->
                productos.put(registro.getId(), ProductoDelResumen.de(inventario, registro))));
        return productos;
    }

    private static List<VentaDelResumen> ventasRegistradas(DatosDeProyecto datos) {
        Stream<VentaDelResumen> articulos = ventasDe(datos.ventas(), datos.origenesVentasCaja());
        Stream<VentaDelResumen> importes = datos.importesLibres().stream().map(venta -> new VentaDelResumen(venta, false));
        return Stream.concat(articulos, importes).toList();
    }

    private static Stream<VentaDelResumen> ventasDe(List<BloqueVentas> bloques, Set<String> origenesCaja) {
        return bloques.stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .flatMap(registro -> VentaProducto.desde(bloque, registro).stream()
                                .map(venta -> new VentaDelResumen(venta, quedoAFiado(bloque, registro, origenesCaja)))));
    }

    /** Las ventas que vienen de Caja no cuentan como fiado acá: su saldo se suma desde el comprobante. */
    private static boolean quedoAFiado(BloqueVentas bloque, Registro registro, Set<String> origenesCaja) {
        return Boolean.TRUE.equals(bloque.valorDe(registro, "credit")) && !origenesCaja.contains(registro.getOrigen());
    }

    private static List<VentaDelResumen> ventasEn(List<VentaDelResumen> ventas, PeriodoResumen periodo) {
        return ventas.stream().filter(venta -> venta.estaEn(periodo)).toList();
    }

    private static BigDecimal ingresosDe(List<VentaDelResumen> ventas) {
        return suma(ventas.stream().map(VentaDelResumen::ingreso));
    }

    private static BigDecimal unidadesDe(List<VentaDelResumen> ventas) {
        return suma(ventas.stream().map(VentaDelResumen::unidades));
    }

    private static BigDecimal suma(Stream<BigDecimal> valores) {
        return valores.reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}