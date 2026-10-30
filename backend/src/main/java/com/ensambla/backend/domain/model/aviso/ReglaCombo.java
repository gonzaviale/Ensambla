package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.ensambla.backend.domain.model.aviso.FormatoAviso.CLAVE_CANTIDAD;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.CLAVE_COSTO;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.CLAVE_NOMBRE;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.CLAVE_PRECIO;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.aDiez;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.nombreDe;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.numero;
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.pesos;

/**
 * Dos productos de la misma tabla que el mismo cliente se lleva el mismo día una y otra vez, y de los que al menos uno
 * rota poco: sugiere armar un combo con un 10 % de descuento sobre la suma de sus precios. El combo se crea como un
 * producto nuevo del inventario. No avisa si ya hay un combo de esos dos productos.
 */
public class ReglaCombo implements ReglaAviso {

    private static final BigDecimal DESCUENTO = new BigDecimal("0.9");
    private static final short PRIORIDAD = 1;
    private static final Set<String> PALABRAS_VACIAS = Set.of("del", "los", "las", "con", "sin", "por", "para", "kg",
            "lts", "unidad", "unidades", "pack", "docena");

    private final MetricasVentas metricasVentas;

    private final CanastasVentas canastasVentas;

    public ReglaCombo(MetricasVentas metricasVentas, CanastasVentas canastasVentas) {
        this.metricasVentas = metricasVentas;
        this.canastasVentas = canastasVentas;
    }

    @Override
    public TipoAviso tipo() {
        return TipoAviso.COMBO;
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoNegocio contexto) {
        LocalDate hoy = contexto.hoy();
        if (hoy == null) {
            return List.of();
        }
        List<VentaProducto> ventas = metricasVentas.ventasValidas(contexto.ventas(), hoy);
        List<ParProductos> pares = canastasVentas.paresFrecuentes(ventas, hoy);
        if (pares.isEmpty()) {
            return List.of();
        }
        Map<RegistroId, MetricaProducto> metricas = metricasVentas.calcular(contexto.ventas(), hoy);
        return pares.stream()
                .map(par -> evaluarPar(contexto, par, metricas))
                .filter(Objects::nonNull)
                .toList();
    }

    private ResultadoRegla evaluarPar(ContextoNegocio contexto, ParProductos par, Map<RegistroId, MetricaProducto> metricas) {
        if (!rotaPoco(par.productoA(), metricas) && !rotaPoco(par.productoB(), metricas)) {
            return null;
        }
        for (BloqueInventario inventario : contexto.inventarios()) {
            // En el orden del inventario, para que el aviso se lea siempre igual.
            List<Registro> productos = inventario.registros().stream()
                    .filter(registro -> registro.getId().equals(par.productoA()) || registro.getId().equals(par.productoB()))
                    .toList();
            if (productos.size() == 2) {
                return resultadoDelCombo(inventario, par, productos.get(0), productos.get(1));
            }
        }
        return null;
    }

    private boolean rotaPoco(RegistroId productoId, Map<RegistroId, MetricaProducto> metricas) {
        MetricaProducto metrica = metricas.getOrDefault(productoId, MetricaProducto.sinVentas(productoId));
        return metricasVentas.esBajaRotacion(metrica, metricas.values());
    }

    private ResultadoRegla resultadoDelCombo(BloqueInventario inventario, ParProductos par, Registro primero,
                                             Registro segundo) {
        String nombrePrimero = nombreDe(inventario, primero);
        String nombreSegundo = nombreDe(inventario, segundo);
        BigDecimal precioPrimero = numero(inventario.valorDe(primero, CLAVE_PRECIO));
        BigDecimal precioSegundo = numero(inventario.valorDe(segundo, CLAVE_PRECIO));
        if (!positivo(precioPrimero) || !positivo(precioSegundo) || yaExiste(inventario, nombrePrimero, nombreSegundo)) {
            return null;
        }
        BigDecimal suma = precioPrimero.add(precioSegundo);
        BigDecimal precio = aDiez(suma.multiply(DESCUENTO));
        if (precio.signum() <= 0 || precio.compareTo(suma) >= 0) {
            return null;
        }
        boolean primeroEsA = primero.getId().equals(par.productoA());
        // Sólo los precios: si cambian, un aviso descartado vuelve a aparecer. Las veces que se vendieron juntos no entran.
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("precioA", primeroEsA ? precioPrimero : precioSegundo);
        datos.put("precioB", primeroEsA ? precioSegundo : precioPrimero);
        datos.put("sugerido", precio);
        return new ResultadoRegla(
                inventario.bloque().getProyectoId(),
                "combo:" + par.productoA() + ":" + par.productoB(),
                PRIORIDAD,
                nombrePrimero + " y " + nombreSegundo + " se venden juntos",
                "Salen poco por separado, pero " + par.veces() + " veces se los llevó el mismo cliente el mismo día.",
                datos,
                List.of("Por separado suman " + pesos(suma) + "; como combo, " + pesos(precio)
                                + " (10 % menos) puede ayudar a que roten.",
                        "El combo se crea como un producto nuevo del inventario, con las unidades que podés armar hoy con tu stock."),
                List.of(accionCrear(inventario, nombrePrimero, nombreSegundo, precio,
                        costo(inventario, primero, segundo), unidadesArmables(inventario, primero, segundo))));
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    /** La suma de los costos que el negocio cargó; {@code null} si no cargó ninguno. */
    private static BigDecimal costo(BloqueInventario inventario, Registro primero, Registro segundo) {
        BigDecimal costoPrimero = numero(inventario.valorDe(primero, CLAVE_COSTO));
        BigDecimal costoSegundo = numero(inventario.valorDe(segundo, CLAVE_COSTO));
        BigDecimal suma = (positivo(costoPrimero) ? costoPrimero : BigDecimal.ZERO)
                .add(positivo(costoSegundo) ? costoSegundo : BigDecimal.ZERO);
        return suma.signum() > 0 ? suma : null;
    }

    /**
     * Cuántos combos se pueden armar con lo que hay: el menor stock de los dos productos. Si el combo naciera en cero, el
     * aviso de stock le diría enseguida que "te quedaste sin" un producto que recién se creó.
     */
    private static BigDecimal unidadesArmables(BloqueInventario inventario, Registro primero, Registro segundo) {
        BigDecimal stockPrimero = numero(inventario.valorDe(primero, CLAVE_CANTIDAD));
        BigDecimal stockSegundo = numero(inventario.valorDe(segundo, CLAVE_CANTIDAD));
        BigDecimal armables = (stockPrimero == null ? BigDecimal.ZERO : stockPrimero)
                .min(stockSegundo == null ? BigDecimal.ZERO : stockSegundo);
        return armables.max(BigDecimal.ZERO);
    }

    private static Map<String, Object> accionCrear(BloqueInventario inventario, String nombrePrimero,
                                                   String nombreSegundo, BigDecimal precio, BigDecimal costo,
                                                   BigDecimal unidades) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(CLAVE_NOMBRE, "Combo " + nombrePrimero + " + " + nombreSegundo);
        valores.put(CLAVE_CANTIDAD, unidades);
        valores.put(CLAVE_PRECIO, precio);
        if (costo != null) {
            valores.put(CLAVE_COSTO, costo);
        }
        Map<String, Object> accion = new LinkedHashMap<>();
        accion.put("tipo", "CREAR_REGISTRO");
        accion.put("texto", "Crear el combo");
        accion.put("proyectoId", inventario.bloque().getProyectoId().toString());
        accion.put("bloqueId", inventario.bloque().getId().toString());
        accion.put("valores", valores);
        return accion;
    }

    /** Ya hay un producto "combo" que nombra a los dos: lo reconoce por la primera palabra significativa de cada uno. */
    private static boolean yaExiste(BloqueInventario inventario, String nombrePrimero, String nombreSegundo) {
        String primera = primeraPalabra(nombrePrimero);
        String segunda = primeraPalabra(nombreSegundo);
        if (primera.isEmpty() || segunda.isEmpty()) {
            return false;
        }
        return inventario.registros().stream()
                .map(registro -> TextoNormalizado.de(nombreDe(inventario, registro)))
                .anyMatch(nombre -> nombre.contains("combo") && nombre.contains(primera) && nombre.contains(segunda));
    }

    private static String primeraPalabra(String nombre) {
        return Arrays.stream(TextoNormalizado.de(nombre).replaceAll("[^a-z0-9 ]+", " ").split("\\s+"))
                .filter(palabra -> palabra.length() >= 3)
                .filter(palabra -> !PALABRAS_VACIAS.contains(palabra))
                .filter(palabra -> !Character.isDigit(palabra.charAt(0)))
                .findFirst()
                .orElse("");
    }
}
