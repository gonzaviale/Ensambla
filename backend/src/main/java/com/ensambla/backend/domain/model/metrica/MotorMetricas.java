package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica.Fila;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.BloqueGastos;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calcula una {@link DefinicionMetrica} sobre las tablas de un cuaderno. Es lo único que pone el número: la receta sólo
 * elige tabla, cuenta, reparto y período. Lee los datos por su clave semántica (así no importa cómo se llame la columna
 * en cada cuaderno), deja afuera las filas de ejemplo (ya vienen filtradas en {@link DatosDeProyecto}) y, si no hay
 * con qué calcular, dice por qué en vez de devolver un cero.
 */
public class MotorMetricas {

    /** Cuántas partes del reparto se muestran como máximo. */
    public static final int MAXIMO_DE_FILAS = 10;

    private static final String SIN_CLIENTE = "Sin cliente";

    private static final String SIN_NOMBRE = "Sin nombre";

    private static final String SIN_CATEGORIA = "Sin categoría";

    private static final String SIN_FECHA = "Sin fecha";

    private static final List<String> DIAS = List.of(
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");

    public ResultadoMetrica calcular(DefinicionMetrica definicion, DatosDeProyecto datos, LocalDate hoy) {
        FormatoMetrica formato = definicion.formato();
        FuenteMetrica fuente = definicion.fuente();
        if (!hayTabla(fuente, datos)) {
            return ResultadoMetrica.sinValor(formato, "No encontramos " + fuente.getTabla() + " en este cuaderno.",
                    deDondeSale(definicion, hoy, 0, 0));
        }
        List<Linea> enElPeriodo = lineas(fuente, datos).stream()
                .filter(linea -> definicion.periodo().incluye(linea.fecha(), hoy))
                .filter(linea -> !definicion.soloFiado() || linea.fiado())
                .toList();
        if (enElPeriodo.isEmpty()) {
            return ResultadoMetrica.sinValor(formato, sinFilas(definicion), deDondeSale(definicion, hoy, 0, 0));
        }
        List<Linea> usables = enElPeriodo.stream().filter(linea -> aporta(linea, definicion.calculo())).toList();
        long sinDato = enElPeriodo.size() - (long) usables.size();
        if (usables.isEmpty()) {
            return ResultadoMetrica.sinValor(
                    formato, datoFaltante(definicion), deDondeSale(definicion, hoy, 0, sinDato));
        }
        return ResultadoMetrica.conValor(
                formato,
                resultado(usables, definicion.calculo()),
                reparto(usables, definicion),
                deDondeSale(definicion, hoy, usables.size(), sinDato));
    }

    private static boolean hayTabla(FuenteMetrica fuente, DatosDeProyecto datos) {
        return switch (fuente) {
            case VENTAS -> !datos.ventas().isEmpty();
            case GASTOS -> datos.gastos().stream().anyMatch(BloqueGastos::tieneImporte);
            case INVENTARIO -> !datos.inventarios().isEmpty();
        };
    }

    private static DeDondeSale deDondeSale(DefinicionMetrica definicion, LocalDate hoy, long usadas, long sinDato) {
        LocalDate desde = definicion.periodo().desde(hoy);
        return new DeDondeSale(definicion.fuente(), usadas, sinDato, desde, desde == null ? null : hoy);
    }

    private static String sinFilas(DefinicionMetrica definicion) {
        String filas = definicion.soloFiado() ? definicion.fuente().getFilas() + " fiadas"
                : definicion.fuente().getFilas();
        return definicion.periodo() == PeriodoMetrica.TODO
                ? "Todavía no cargaste " + filas + "."
                : "No hay " + filas + " en este período.";
    }

    private static String datoFaltante(DefinicionMetrica definicion) {
        if (!definicion.calculo().usaImporte()) {
            return "Cargá la cantidad de tus " + definicion.fuente().getFilas() + " para poder calcularlo.";
        }
        return switch (definicion.fuente()) {
            case VENTAS -> "Cargá el precio de tus ventas para poder calcularlo.";
            case GASTOS -> "Cargá el importe de tus gastos para poder calcularlo.";
            case INVENTARIO -> "Cargá el costo de tus productos para poder calcularlo.";
        };
    }

    private static boolean aporta(Linea linea, CalculoMetrica calculo) {
        return switch (calculo) {
            case SUMA, PROMEDIO -> linea.importe() != null;
            case UNIDADES -> linea.unidades() != null;
            case CONTEO -> true;
        };
    }

    private static BigDecimal resultado(List<Linea> lineas, CalculoMetrica calculo) {
        return switch (calculo) {
            case SUMA -> suma(lineas, Linea::importe);
            case PROMEDIO -> suma(lineas, Linea::importe).divide(BigDecimal.valueOf(lineas.size()), 2,
                    RoundingMode.HALF_UP);
            case CONTEO -> BigDecimal.valueOf(lineas.size());
            case UNIDADES -> suma(lineas, Linea::unidades);
        };
    }

    private static BigDecimal suma(List<Linea> lineas, Function<Linea, BigDecimal> dato) {
        return lineas.stream().map(dato).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static List<Fila> reparto(List<Linea> lineas, DefinicionMetrica definicion) {
        AgrupacionMetrica agrupar = definicion.agrupar();
        if (agrupar == AgrupacionMetrica.NINGUNA) {
            return List.of();
        }
        Map<String, List<Linea>> grupos = lineas.stream().collect(
                Collectors.groupingBy(linea -> clave(linea, agrupar), LinkedHashMap::new, Collectors.toList()));
        List<Fila> filas = new ArrayList<>();
        grupos.forEach((etiqueta, delGrupo) ->
                filas.add(new Fila(etiqueta, resultado(delGrupo, definicion.calculo()))));
        if (agrupar == AgrupacionMetrica.DIA_DE_LA_SEMANA) {
            filas.sort(Comparator.comparingInt(fila -> lugarDelDia(fila.etiqueta())));
            return filas;
        }
        filas.sort(Comparator.comparing(Fila::valor).reversed().thenComparing(Fila::etiqueta));
        return filas.stream().limit(MAXIMO_DE_FILAS).toList();
    }

    private static String clave(Linea linea, AgrupacionMetrica agrupar) {
        return switch (agrupar) {
            case PRODUCTO -> linea.producto();
            case CLIENTE -> linea.cliente();
            case CATEGORIA -> linea.categoria();
            default -> linea.fecha() == null ? SIN_FECHA : DIAS.get(linea.fecha().getDayOfWeek().ordinal());
        };
    }

    private static int lugarDelDia(String etiqueta) {
        int lugar = DIAS.indexOf(etiqueta);
        return lugar < 0 ? DIAS.size() : lugar;
    }

    // --- lectura de las tablas ---

    private static List<Linea> lineas(FuenteMetrica fuente, DatosDeProyecto datos) {
        return switch (fuente) {
            case VENTAS -> lineasDeVentas(datos);
            case GASTOS -> lineasDeGastos(datos);
            case INVENTARIO -> lineasDeInventario(datos);
        };
    }

    private static List<Linea> lineasDeVentas(DatosDeProyecto datos) {
        Map<RegistroId, String> nombres = nombresDeProductos(datos);
        List<Linea> lineas = new ArrayList<>();
        for (BloqueVentas bloque : datos.ventas()) {
            for (Registro registro : bloque.registros()) {
                VentaProducto.desde(bloque, registro).ifPresent(venta -> lineas.add(new Linea(
                        nombres.getOrDefault(venta.productoId(), SIN_NOMBRE),
                        cliente(venta.cliente(), datos.nombresDeClientes()),
                        null,
                        venta.fecha(),
                        venta.precio() == null ? null : venta.precio().multiply(venta.cantidad()),
                        venta.cantidad(),
                        Boolean.TRUE.equals(bloque.valorDe(registro, "credit")))));
            }
        }
        return lineas;
    }

    private static String cliente(String cliente, Map<String, String> nombresDeClientes) {
        return cliente == null ? SIN_CLIENTE : nombresDeClientes.getOrDefault(cliente, cliente);
    }

    private static List<Linea> lineasDeGastos(DatosDeProyecto datos) {
        List<Linea> lineas = new ArrayList<>();
        for (BloqueGastos bloque : datos.gastos()) {
            if (!bloque.tieneImporte()) {
                continue;
            }
            for (Registro registro : bloque.registros()) {
                lineas.add(new Linea(
                        SIN_NOMBRE,
                        SIN_CLIENTE,
                        texto(bloque.valorDe(registro, "category"), SIN_CATEGORIA),
                        fecha(bloque.valorDe(registro, "date")),
                        numero(bloque.valorDe(registro, "total")),
                        null,
                        false));
            }
        }
        return lineas;
    }

    private static List<Linea> lineasDeInventario(DatosDeProyecto datos) {
        List<Linea> lineas = new ArrayList<>();
        for (BloqueInventario bloque : datos.inventarios()) {
            for (Registro registro : bloque.registros()) {
                BigDecimal cantidad = numero(bloque.valorDe(registro, "quantity"));
                BigDecimal costo = numero(bloque.valorDe(registro, "cost"));
                lineas.add(new Linea(
                        texto(bloque.valorDe(registro, "name"), SIN_NOMBRE),
                        SIN_CLIENTE,
                        null,
                        null,
                        cantidad == null || costo == null ? null : cantidad.multiply(costo),
                        cantidad,
                        false));
            }
        }
        return lineas;
    }

    private static Map<RegistroId, String> nombresDeProductos(DatosDeProyecto datos) {
        Map<RegistroId, String> nombres = new LinkedHashMap<>();
        for (BloqueInventario bloque : datos.inventarios()) {
            for (Registro registro : bloque.registros()) {
                nombres.put(registro.getId(), texto(bloque.valorDe(registro, "name"), SIN_NOMBRE));
            }
        }
        return nombres;
    }

    private static String texto(Object valor, String porDefecto) {
        return valor instanceof String texto && !texto.isBlank() ? texto.trim() : porDefecto;
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

    /** Una fila de cualquiera de las tablas, ya leída: lo que hace falta para calcular y repartir. */
    private record Linea(String producto, String cliente, String categoria, LocalDate fecha, BigDecimal importe,
                         BigDecimal unidades, boolean fiado) {
    }
}
