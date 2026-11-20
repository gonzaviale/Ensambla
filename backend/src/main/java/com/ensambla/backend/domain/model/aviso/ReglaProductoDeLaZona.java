package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.common.ValorDeRegistro;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.NombreDeProducto;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.domain.model.aviso.FormatoAviso.CLAVE_CANTIDAD;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.CLAVE_NOMBRE;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.CLAVE_PRECIO;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.aDiez;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.pesos;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.texto;

/**
 * Lo que se vende en la zona y el negocio todavía no tiene: toma el ranking de los productos más vendidos de la zona
 * ({@link AnalisisZona#masVendidos()}), se queda con los que venden al menos {@code popularidadMinima} de los comercios
 * de la zona, saca los que el negocio ya ofrece y sugiere sumar los primeros {@code maximo}. Usa sólo agregados
 * anónimos (con menos de {@link AnalisisZona#MINIMO_COMERCIOS} comercios no hay ranking) y los textos nunca nombran a un
 * comercio. Sin una tabla de inventario donde sumarlo no avisa. Aplicar el aviso crea el producto en el inventario.
 *
 * @param popularidadMinima fracción de los comercios de la zona que tienen que vender el producto (0,6 es el 60 %)
 * @param maximo            cuántos avisos como máximo por negocio
 */
@RequiredArgsConstructor
public class ReglaProductoDeLaZona implements ReglaAviso {

    private static final short PRIORIDAD = 1;
    private static final int STOCK_MINIMO_SUGERIDO = 5;
    private static final List<Object> MOTIVOS = List.of(
            "Es un dato agregado de varios comercios de tu zona: nadie queda identificado.",
            "Lo cruzamos con tu stock y no encontramos nada parecido.");

    private final BigDecimal popularidadMinima;

    private final int maximo;

    @Override
    public TipoAviso tipo() {
        return TipoAviso.CONSUMO_ZONA;
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoNegocio contexto) {
        if (contexto.inventarios().isEmpty()) {
            return List.of();
        }
        BloqueInventario destino = contexto.inventarios().getFirst();
        return productosQueLeFaltan(contexto).stream()
                .map(producto -> resultado(destino, producto, contexto.zona()))
                .toList();
    }

    private List<AgregadoZonaProducto> productosQueLeFaltan(ContextoNegocio contexto) {
        List<String> tuyos = nombresDelInventario(contexto.inventarios());
        AnalisisZona zona = contexto.zona();
        return zona.masVendidos().stream()
                .filter(producto -> esPopular(producto, zona))
                .filter(producto -> noLoTiene(tuyos, producto))
                .limit(maximo)
                .toList();
    }

    private static boolean noLoTiene(List<String> tuyos, AgregadoZonaProducto producto) {
        return tuyos.stream().noneMatch(nombre -> NombreDeProducto.esElMismo(nombre, producto.nombre()));
    }

    private static List<String> nombresDelInventario(List<BloqueInventario> inventarios) {
        return inventarios.stream()
                .flatMap(inventario -> inventario.registros().stream()
                        .map(registro -> ValorDeRegistro.de(inventario.valorDe(registro, CLAVE_NOMBRE))))
                .filter(ValorDeRegistro::tieneTexto)
                .map(ValorDeRegistro::comoTexto)
                .toList();
    }

    private boolean esPopular(AgregadoZonaProducto producto, AnalisisZona zona) {
        BigDecimal parte = BigDecimal.valueOf(producto.comerciosVenden())
                .divide(BigDecimal.valueOf(zona.comercios()), 4, RoundingMode.HALF_UP);
        return parte.compareTo(popularidadMinima) >= 0;
    }

    private ResultadoRegla resultado(BloqueInventario destino, AgregadoZonaProducto producto, AnalisisZona zona) {
        BigDecimal sugerido = aDiez(producto.precioPromedio());
        return new ResultadoRegla(
                destino.bloque().getProyectoId(),
                ResultadoRegla.entidadDeZona(producto.nombre()),
                PRIORIDAD,
                "En tu zona se vende " + producto.nombre() + " y no lo tenés",
                detalle(producto, zona),
                datos(producto, sugerido),
                MOTIVOS,
                acciones(destino, producto, sugerido));
    }

    private static String detalle(AgregadoZonaProducto producto, AnalisisZona zona) {
        return producto.comerciosVenden() + " de " + zona.comercios() + " comercios lo venden, a "
                + pesos(producto.precioPromedio()) + " en promedio. Cada uno vende unas "
                + texto(producto.unidadesSemana()) + " unidades por semana.";
    }

    /** Sólo el nombre y el precio sugerido: la cantidad de comercios y las unidades cambian todo el tiempo. */
    private static Map<String, Object> datos(AgregadoZonaProducto producto, BigDecimal sugerido) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", producto.nombre());
        datos.put("precio", sugerido);
        return datos;
    }

    private static List<AccionDeAviso> acciones(
            BloqueInventario destino, AgregadoZonaProducto producto, BigDecimal sugerido) {
        return List.of(
                AccionesAviso.crearRegistro(destino, "Sumar a mi stock", valoresDelProducto(producto, sugerido)),
                AccionesAviso.verZona("Ver en Mi zona", producto.nombre()));
    }

    private static Map<String, Object> valoresDelProducto(AgregadoZonaProducto producto, BigDecimal sugerido) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(CLAVE_NOMBRE, producto.nombre());
        valores.put(CLAVE_CANTIDAD, 0);
        valores.put(CLAVE_PRECIO, sugerido);
        valores.put("minimum", STOCK_MINIMO_SUGERIDO);
        return valores;
    }
}
