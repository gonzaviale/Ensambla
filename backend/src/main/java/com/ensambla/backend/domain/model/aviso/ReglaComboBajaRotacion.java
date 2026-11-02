package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import static com.ensambla.backend.domain.model.aviso.FormatoAviso.porcentaje;

/**
 * Pares de productos que se venden juntos y de los que al menos uno rota poco: propone armar un combo con un precio
 * menor que la suma de los dos. Una canasta es lo que un mismo cliente se llevó el mismo día (las ventas no tienen
 * ticket), y un par cuenta si apareció en al menos {@code frecuenciaMinima} canastas del último período. Sin cliente
 * en las ventas no hay canastas, así que no avisa. Aplicar el aviso crea el combo como un producto nuevo del inventario.
 *
 * @param frecuenciaMinima canastas en las que tiene que aparecer el par
 * @param descuento        fracción que se descuenta de la suma de los precios (0,1 es un 10 % menos)
 */
@RequiredArgsConstructor
public class ReglaComboBajaRotacion implements ReglaAviso {

    private static final short PRIORIDAD = 1;

    private final MetricasVentas metricasVentas;

    private final int frecuenciaMinima;

    private final BigDecimal descuento;

    /** Los dos productos de un par, ordenados por id: el mismo par siempre es la misma clave. */
    private record Par(RegistroId primero, RegistroId segundo) {

        static Par de(RegistroId uno, RegistroId otro) {
            return uno.toString().compareTo(otro.toString()) <= 0 ? new Par(uno, otro) : new Par(otro, uno);
        }

        boolean incluye(Registro registro) {
            return registro.getId().equals(primero) || registro.getId().equals(segundo);
        }
    }

    @Override
    public TipoAviso tipo() {
        return TipoAviso.COMBO;
    }

    @Override
    public List<ResultadoRegla> evaluar(ContextoNegocio contexto) {
        if (contexto.hoy() == null) {
            return List.of();
        }
        Map<RegistroId, MetricaProducto> metricas = metricasVentas.calcular(contexto.ventas(), contexto.hoy());
        Map<Par, Integer> frecuentes = canastasPorPar(contexto.ventas(), contexto.hoy());
        return frecuentes.entrySet().stream()
                .filter(par -> par.getValue() >= frecuenciaMinima)
                .flatMap(par -> contexto.inventarios().stream()
                        .map(inventario -> evaluarPar(inventario, par.getKey(), par.getValue(), metricas))
                        .filter(Objects::nonNull))
                .toList();
    }

    /** Cuántas canastas del período tienen cada par de productos. */
    private Map<Par, Integer> canastasPorPar(List<BloqueVentas> ventas, LocalDate hoy) {
        LocalDate desde = hoy.minusDays(metricasVentas.periodoDias());
        Map<String, Set<RegistroId>> canastas = new LinkedHashMap<>();
        metricasVentas.ventasValidas(ventas, hoy).stream()
                .filter(venta -> venta.cliente() != null && venta.fecha().isAfter(desde))
                .forEach(venta -> canastas
                        .computeIfAbsent(TextoNormalizado.de(venta.cliente()) + "|" + venta.fecha(),
                                clave -> new LinkedHashSet<>())
                        .add(venta.productoId()));
        Map<Par, Integer> pares = new LinkedHashMap<>();
        canastas.values().forEach(productos -> {
            List<RegistroId> lista = new ArrayList<>(productos);
            for (int i = 0; i < lista.size(); i++) {
                for (int j = i + 1; j < lista.size(); j++) {
                    pares.merge(Par.de(lista.get(i), lista.get(j)), 1, Integer::sum);
                }
            }
        });
        return pares;
    }

    private ResultadoRegla evaluarPar(BloqueInventario inventario, Par par, int veces,
                                      Map<RegistroId, MetricaProducto> metricas) {
        // El orden lo da el inventario, así el aviso se lee siempre igual.
        List<Registro> productos = inventario.registros().stream().filter(par::incluye).toList();
        if (productos.size() != 2) {
            return null;
        }
        Registro a = productos.get(0);
        Registro b = productos.get(1);
        BigDecimal precioA = positivo(inventario.valorDe(a, CLAVE_PRECIO));
        BigDecimal precioB = positivo(inventario.valorDe(b, CLAVE_PRECIO));
        if (precioA == null || precioB == null) {
            return null;
        }
        boolean aRotaPoco = rotaPoco(a, metricas);
        boolean bRotaPoco = rotaPoco(b, metricas);
        if (!aRotaPoco && !bRotaPoco) {
            return null;
        }
        String nombreA = nombreDe(inventario, a);
        String nombreB = nombreDe(inventario, b);
        BigDecimal suma = precioA.add(precioB);
        BigDecimal sugerido = aDiez(suma.multiply(BigDecimal.ONE.subtract(descuento)));
        if (sugerido.compareTo(suma) >= 0 || yaExisteElCombo(inventario, nombreA, nombreB)) {
            return null;
        }
        String quienRotaPoco = aRotaPoco && bRotaPoco ? "Los dos salen poco" : (aRotaPoco ? nombreA : nombreB) + " sale poco";
        return resultado(inventario, a, b, veces, quienRotaPoco, suma, sugerido, precioA, precioB);
    }

    private boolean rotaPoco(Registro registro, Map<RegistroId, MetricaProducto> metricas) {
        MetricaProducto metrica = metricas.getOrDefault(registro.getId(), MetricaProducto.sinVentas(registro.getId()));
        return metricasVentas.esBajaRotacion(metrica, metricas.values());
    }

    /** Un producto del inventario que ya se llama «combo» y nombra a los dos: no se propone de nuevo. */
    private static boolean yaExisteElCombo(BloqueInventario inventario, String nombreA, String nombreB) {
        String a = TextoNormalizado.de(nombreA);
        String b = TextoNormalizado.de(nombreB);
        return inventario.registros().stream()
                .map(registro -> TextoNormalizado.de(nombreDe(inventario, registro)))
                .anyMatch(nombre -> nombre.contains("combo") && nombre.contains(a) && nombre.contains(b));
    }

    private ResultadoRegla resultado(BloqueInventario inventario, Registro a, Registro b, int veces,
                                     String quienRotaPoco, BigDecimal suma, BigDecimal sugerido, BigDecimal precioA,
                                     BigDecimal precioB) {
        String nombreA = nombreDe(inventario, a);
        String nombreB = nombreDe(inventario, b);
        // Sólo los precios: si cambian, un aviso descartado vuelve a aparecer. Las canastas del período no entran.
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("precioA", precioA);
        datos.put("precioB", precioB);
        datos.put("sugerido", sugerido);
        return new ResultadoRegla(
                inventario.bloque().getProyectoId(),
                ResultadoRegla.entidadDeCombo(a.getId(), b.getId()),
                PRIORIDAD,
                nombreA + " y " + nombreB + " se venden juntos",
                quienRotaPoco + " por separado, pero " + veces + " veces se los llevó el mismo cliente el mismo día.",
                datos,
                List.of("Por separado suman " + pesos(suma) + "; como combo, " + pesos(sugerido) + " ("
                                + porcentaje(suma.subtract(sugerido), suma) + " % menos) puede ayudar a que roten.",
                        "El combo se crea como un producto nuevo del inventario; después cargás cuántos armás."),
                List.of(AccionesAviso.crearRegistro(inventario, "Crear el combo",
                        valoresDelCombo(inventario, a, b, nombreA, nombreB, sugerido))));
    }

    private static Map<String, Object> valoresDelCombo(BloqueInventario inventario, Registro a, Registro b,
                                                       String nombreA, String nombreB, BigDecimal sugerido) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put(CLAVE_NOMBRE, "Combo " + nombreA + " + " + nombreB);
        valores.put(CLAVE_CANTIDAD, 0);
        valores.put(CLAVE_PRECIO, sugerido);
        BigDecimal costoA = positivo(inventario.valorDe(a, CLAVE_COSTO));
        BigDecimal costoB = positivo(inventario.valorDe(b, CLAVE_COSTO));
        if (costoA != null && costoB != null) {
            valores.put(CLAVE_COSTO, costoA.add(costoB));
        }
        return valores;
    }

    /** El número cargado, o {@code null} si falta o no es mayor a cero. */
    private static BigDecimal positivo(Object valor) {
        BigDecimal numero = numero(valor);
        return numero != null && numero.signum() > 0 ? numero : null;
    }
}
