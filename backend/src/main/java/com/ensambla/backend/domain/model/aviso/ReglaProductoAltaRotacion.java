package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.ensambla.backend.domain.model.aviso.FormatoAviso.CLAVE_COSTO;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.CLAVE_PRECIO;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.aDiez;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.nombreDe;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.numero;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.pesos;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.texto;

/**
 * Productos que se venden más que de costumbre (las unidades del período superan a su promedio por período) y que
 * están por debajo del rango de precios de la zona. Sugiere subirlos al mínimo de la zona, redondeado a la decena: si
 * la gente lo compra igual, hay margen que se está regalando. Usa sólo los agregados anónimos de la zona, así que con
 * menos de {@link com.ensambla.backend.domain.model.zona.AnalisisZona#MINIMO_COMERCIOS} comercios no avisa. Sin ventas
 * anotadas no hay cómo saber qué se vende más que de costumbre.
 */
@RequiredArgsConstructor
public class ReglaProductoAltaRotacion implements ReglaAviso {

    private static final short PRIORIDAD = 1;

    private final MetricasVentas metricasVentas;

    @Override
    public TipoAviso tipo() {
        return TipoAviso.MUY_VENDIDO;
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoNegocio contexto) {
        if (contexto.hoy() == null) {
            return List.of();
        }
        Map<RegistroId, MetricaProducto> metricas = metricasVentas.calcular(contexto.ventas(), contexto.hoy());
        return contexto.inventarios().stream()
                .flatMap(inventario -> inventario.registros().stream()
                        .map(registro -> evaluarRegistro(contexto, inventario, registro, metricas))
                        .filter(Objects::nonNull))
                .toList();
    }

    private ResultadoRegla evaluarRegistro(ContextoNegocio contexto, BloqueInventario inventario, Registro registro,
                                           Map<RegistroId, MetricaProducto> metricas) {
        MetricaProducto metrica = metricas.get(registro.getId());
        BigDecimal precio = numero(inventario.valorDe(registro, CLAVE_PRECIO));
        if (metrica == null || precio == null || precio.signum() <= 0 || !vendeMasQueSuPromedio(metrica)) {
            return null;
        }
        String nombre = nombreDe(inventario, registro);
        Optional<AgregadoZonaProducto> rango = contexto.zona().productoConRango(nombre);
        if (rango.isEmpty() || precio.compareTo(rango.get().precioMinimo()) >= 0) {
            return null;
        }
        BigDecimal sugerido = sugerido(precio, rango.get());
        return resultado(inventario, registro, nombre, precio, sugerido, metrica, rango.get());
    }

    private static boolean vendeMasQueSuPromedio(MetricaProducto metrica) {
        return metrica.unidadesPeriodo().compareTo(metrica.promedioPorPeriodo()) > 0;
    }

    /** El mínimo de la zona redondeado a la decena, salvo que el redondeo no lo deje por encima del precio actual. */
    private static BigDecimal sugerido(BigDecimal precio, AgregadoZonaProducto rango) {
        BigDecimal redondeado = aDiez(rango.precioMinimo());
        return redondeado.compareTo(precio) > 0 ? redondeado : rango.precioMinimo();
    }

    private ResultadoRegla resultado(BloqueInventario inventario, Registro registro, String nombre, BigDecimal precio,
                                     BigDecimal sugerido, MetricaProducto metrica, AgregadoZonaProducto rango) {
        // Sólo los precios: si cambian, un aviso descartado vuelve a aparecer. Las ventas del período no entran.
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("precio", precio);
        datos.put("sugerido", sugerido);
        return new ResultadoRegla(
                inventario.bloque().getProyectoId(),
                ResultadoRegla.entidadDeRegistro(registro.getId()),
                PRIORIDAD,
                nombre + " se vende más que de costumbre y lo tenés por debajo de tu zona",
                detalle(precio, sugerido, metrica, rango),
                datos,
                motivos(inventario, registro, precio, sugerido, metrica, rango),
                List.of(AccionesAviso.cambiarPrecio(inventario, registro.getId(), "Subir a " + pesos(sugerido), sugerido),
                        AccionesAviso.abrir(inventario, registro.getId())));
    }

    private String detalle(BigDecimal precio, BigDecimal sugerido, MetricaProducto metrica, AgregadoZonaProducto rango) {
        return "Vendiste " + texto(metrica.unidadesPeriodo()) + " en los últimos " + metricasVentas.periodoDias()
                + " días, más que tu promedio de " + texto(metrica.promedioPorPeriodo()) + " por período, y lo tenés a "
                + pesos(precio) + " cuando en tu zona se vende entre " + pesos(rango.precioMinimo()) + " y "
                + pesos(rango.precioMaximo()) + ". Si lo subís a " + pesos(sugerido) + ", queda dentro del rango.";
    }

    private List<Object> motivos(BloqueInventario inventario, Registro registro, BigDecimal precio,
                                 BigDecimal sugerido, MetricaProducto metrica, AgregadoZonaProducto rango) {
        List<Object> motivos = new ArrayList<>();
        motivos.add("Vendiste " + texto(metrica.unidadesPeriodo()) + " unidades en los últimos "
                + metricasVentas.periodoDias() + " días y tu promedio por período es " + texto(metrica.promedioPorPeriodo())
                + ".");
        motivos.add("En tu zona, " + rango.comercios() + " comercios lo venden entre " + pesos(rango.precioMinimo())
                + " y " + pesos(rango.precioMaximo()) + ".");
        BigDecimal costo = numero(inventario.valorDe(registro, CLAVE_COSTO));
        if (costo != null && costo.signum() > 0) {
            motivos.add("Hoy te quedan " + pesos(precio.subtract(costo)) + " por unidad; a " + pesos(sugerido)
                    + " te quedarían " + pesos(sugerido.subtract(costo)) + ".");
        }
        return motivos;
    }
}
