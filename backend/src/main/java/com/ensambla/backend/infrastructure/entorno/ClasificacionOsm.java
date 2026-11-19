package com.ensambla.backend.infrastructure.entorno;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Cómo cuenta un lugar de OpenStreetMap para «dónde abrir», según sus etiquetas. Un mismo lugar puede contar en varias
 * categorías (un supermercado es comercio que atrae gente y también almacén). Son las mismas reglas que usa el
 * snapshot de scripts/importar-datos-zona.ps1, así que un punto elegido y una zona precargada se comparan igual.
 */
enum ClasificacionOsm {

    ESCUELA("ESCUELA", null, unoDe("amenity", "school", "kindergarten", "college", "university")),
    OFICINA("OFICINA", null, tiene("office")),
    TRANSPORTE("TRANSPORTE", null, tiene("public_transport")
            .or(unoDe("highway", "bus_stop"))
            .or(unoDe("railway", "station", "halt", "subway_entrance", "tram_stop"))),
    PARQUE("PARQUE", null, unoDe("leisure", "park", "garden", "playground")),
    SALUD("SALUD", null, unoDe("amenity", "hospital", "clinic", "doctors")),
    COMPLEMENTARIO("COMPLEMENTARIO", null, unoDe("amenity", "pharmacy", "bank", "atm").or(unoDe("shop", "supermarket"))),
    COMPETENCIA_ALMACEN("COMPETENCIA", "ALMACEN",
            unoDe("shop", "convenience", "supermarket", "greengrocer", "bakery", "deli")),
    COMPETENCIA_KIOSCO("COMPETENCIA", "KIOSCO", unoDe("shop", "kiosk", "convenience")),
    COMPETENCIA_PAPELERIA("COMPETENCIA", "PAPELERIA", unoDe("shop", "stationery", "books", "copyshop"));

    private final String categoria;
    private final String subcategoria;
    private final Predicate<Map<String, String>> aplicaA;

    ClasificacionOsm(String categoria, String subcategoria, Predicate<Map<String, String>> aplicaA) {
        this.categoria = categoria;
        this.subcategoria = subcategoria;
        this.aplicaA = aplicaA;
    }

    /** Todas las clasificaciones que le corresponden a un lugar con esas etiquetas, en el orden en que se declaran. */
    static List<ClasificacionOsm> de(Map<String, String> etiquetas) {
        return Arrays.stream(values()).filter(clasificacion -> clasificacion.aplicaA.test(etiquetas)).toList();
    }

    String categoria() {
        return categoria;
    }

    String subcategoria() {
        return subcategoria;
    }

    private static Predicate<Map<String, String>> tiene(String etiqueta) {
        return etiquetas -> etiquetas.containsKey(etiqueta);
    }

    private static Predicate<Map<String, String>> unoDe(String etiqueta, String... valores) {
        Set<String> admitidos = Set.of(valores);
        return etiquetas -> admitidos.contains(etiquetas.getOrDefault(etiqueta, ""));
    }
}
