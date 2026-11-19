package com.ensambla.backend.support;

import java.util.LinkedHashMap;
import java.util.Map;

/** Mapas de texto a valor para armar los argumentos de las herramientas del taller sin repetir {@code put}. */
public final class MapasDePrueba {

    private MapasDePrueba() {
    }

    /** {@code mapa("pieza", "e1", "campo", "Precio")}: pares clave-valor, en el orden dado. */
    public static Map<String, Object> mapa(Object... clavesYValores) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < clavesYValores.length; i += 2) {
            mapa.put((String) clavesYValores[i], clavesYValores[i + 1]);
        }
        return mapa;
    }
}
