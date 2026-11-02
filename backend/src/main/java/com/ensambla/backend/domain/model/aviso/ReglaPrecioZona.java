package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.math.BigDecimal;
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
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.porcentaje;

/**
 * Productos cuyo precio de venta queda fuera del rango de la zona: por debajo del precio mínimo que cobran los comercios
 * cercanos sugiere revisar costos y margen; por encima del máximo, bajarlo para no perder ventas. El rango sale sólo de
 * los agregados anónimos de la zona y únicamente si la zona y el producto tienen al menos
 * {@link AnalisisZona#MINIMO_COMERCIOS} comercios: con menos no se calcula ni se muestra, porque dejaría adivinar qué
 * cobra cada uno.
 */
public class ReglaPrecioZona implements ReglaAviso {

    private static final short PRIORIDAD = 2;

    @Override
    public TipoAviso tipo() {
        return TipoAviso.PRECIO_ZONA;
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoNegocio contexto) {
        return contexto.inventarios().stream()
                .flatMap(inventario -> inventario.registros().stream()
                        .map(registro -> evaluarRegistro(contexto.zona(), inventario, registro))
                        .filter(Objects::nonNull))
                .toList();
    }

    private ResultadoRegla evaluarRegistro(AnalisisZona zona, BloqueInventario inventario, Registro registro) {
        BigDecimal precio = numero(inventario.valorDe(registro, CLAVE_PRECIO));
        if (precio == null || precio.signum() <= 0) {
            return null;
        }
        String nombre = nombreDe(inventario, registro);
        Optional<AgregadoZonaProducto> deLaZona = zona.productoConRango(nombre);
        if (deLaZona.isEmpty()) {
            return null;
        }
        AgregadoZonaProducto rango = deLaZona.get();
        if (precio.compareTo(rango.precioMinimo()) < 0) {
            return porDebajo(inventario, registro, nombre, precio, rango);
        }
        if (precio.compareTo(rango.precioMaximo()) > 0) {
            return porEncima(inventario, registro, nombre, precio, rango);
        }
        return null;
    }

    private ResultadoRegla porDebajo(BloqueInventario inventario, Registro registro, String nombre, BigDecimal precio,
                                     AgregadoZonaProducto rango) {
        BigDecimal diferencia = rango.precioMinimo().subtract(precio);
        String detalle = "Lo tenés a " + pesos(precio) + " y en tu zona se vende entre " + pesos(rango.precioMinimo())
                + " y " + pesos(rango.precioMaximo()) + ": " + pesos(diferencia) + " ("
                + porcentaje(diferencia, rango.precioMinimo()) + " %) menos que el mínimo. "
                + "Revisá tus costos y tu margen: puede que estés vendiendo más barato de lo necesario.";
        return resultado(inventario, registro, nombre + " está más barato que en tu zona", detalle, precio, rango,
                List.<Object>of(motivoRango(rango), motivoMargen(inventario, registro, precio)),
                List.<Object>of(AccionesAviso.abrir(inventario, registro.getId())));
    }

    private ResultadoRegla porEncima(BloqueInventario inventario, Registro registro, String nombre, BigDecimal precio,
                                     AgregadoZonaProducto rango) {
        BigDecimal diferencia = precio.subtract(rango.precioMaximo());
        BigDecimal sugerido = sugeridoPorEncima(precio, rango);
        String detalle = "Lo tenés a " + pesos(precio) + " y en tu zona se vende entre " + pesos(rango.precioMinimo())
                + " y " + pesos(rango.precioMaximo()) + ": " + pesos(diferencia) + " ("
                + porcentaje(diferencia, rango.precioMaximo()) + " %) más que el máximo. "
                + "Bajarlo a " + pesos(sugerido) + " lo deja dentro del rango y evita que pierdas ventas.";
        return resultado(inventario, registro, nombre + " está más caro que en tu zona", detalle, precio, rango,
                List.<Object>of(motivoRango(rango), "Los clientes comparan: por encima de lo que cobran al lado se compra menos."),
                List.<Object>of(AccionesAviso.cambiarPrecio(inventario, registro.getId(), "Bajar a " + pesos(sugerido), sugerido),
                        AccionesAviso.abrir(inventario, registro.getId())));
    }

    /** El máximo de la zona redondeado a la decena, salvo que el redondeo no lo deje por debajo del precio actual. */
    private static BigDecimal sugeridoPorEncima(BigDecimal precio, AgregadoZonaProducto rango) {
        BigDecimal redondeado = aDiez(rango.precioMaximo());
        return redondeado.compareTo(precio) < 0 ? redondeado : rango.precioMaximo();
    }

    private static String motivoRango(AgregadoZonaProducto rango) {
        return "En tu zona, " + rango.comercios() + " comercios lo venden entre " + pesos(rango.precioMinimo())
                + " y " + pesos(rango.precioMaximo()) + ".";
    }

    private String motivoMargen(BloqueInventario inventario, Registro registro, BigDecimal precio) {
        BigDecimal costo = numero(inventario.valorDe(registro, CLAVE_COSTO));
        if (costo == null || costo.signum() <= 0) {
            return "Todavía no cargaste el costo de este producto: con él podemos ver cuánto te queda de margen.";
        }
        BigDecimal margen = precio.subtract(costo);
        return margen.signum() < 0
                ? "Con tu costo de " + pesos(costo) + ", perdés " + pesos(margen.negate()) + " por unidad."
                : "Con tu costo de " + pesos(costo) + ", te quedan " + pesos(margen) + " por unidad ("
                        + porcentaje(margen, costo) + " % sobre el costo).";
    }

    private ResultadoRegla resultado(BloqueInventario inventario, Registro registro, String titulo, String detalle,
                                     BigDecimal precio, AgregadoZonaProducto rango, List<Object> motivos,
                                     List<Object> acciones) {
        // Precio propio y rango: si cambia alguno, un aviso descartado vuelve a aparecer.
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("precio", precio);
        datos.put("minimo", rango.precioMinimo());
        datos.put("maximo", rango.precioMaximo());
        return new ResultadoRegla(
                inventario.bloque().getProyectoId(),
                ResultadoRegla.entidadDeRegistro(registro.getId()),
                PRIORIDAD,
                titulo,
                detalle,
                datos,
                motivos,
                acciones);
    }
}
