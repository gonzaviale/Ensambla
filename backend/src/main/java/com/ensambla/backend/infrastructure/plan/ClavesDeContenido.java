package com.ensambla.backend.infrastructure.plan;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cómo se escribe una clave de contenido fuera del dominio: el dominio habla de avisos y productos; los avisos del
 * servidor se guardan y se piden con el prefijo {@code servidor:}, que los distingue de los de demostración.
 */
public final class ClavesDeContenido {

    private static final Map<TipoContenidoPlan, String> PREFIJOS = Map.of(TipoContenidoPlan.AVISOS, "servidor:");

    private ClavesDeContenido() {
    }

    public static String guardada(TipoContenidoPlan tipo, String claveDelDominio) {
        return prefijoDe(tipo) + claveDelDominio;
    }

    public static String delDominio(TipoContenidoPlan tipo, String claveGuardada) {
        String prefijo = prefijoDe(tipo);
        return claveGuardada.startsWith(prefijo) ? claveGuardada.substring(prefijo.length()) : claveGuardada;
    }

    /** Cada clave del dominio con la clave tal como la pidió la persona, en el orden del pedido. */
    public static Map<String, String> origenesDelPedido(TipoContenidoPlan tipo, List<String> clavesPedidas) {
        Map<String, String> origenes = new LinkedHashMap<>();
        clavesPedidas.forEach(pedida -> origenes.put(delDominio(tipo, pedida), pedida));
        return origenes;
    }

    private static String prefijoDe(TipoContenidoPlan tipo) {
        return PREFIJOS.getOrDefault(tipo, "");
    }
}
