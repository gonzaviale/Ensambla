package com.ensambla.backend.domain.model.sami.taller;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Cómo se llama cada pieza según cómo la nombra una acción: por id, por tipo o por nombre. */
public final class NombresDePiezas {

    private final Map<String, String> nombres = new HashMap<>();

    private NombresDePiezas() {
    }

    /** Las piezas que el taller ya tiene, por su id. */
    public static NombresDePiezas porId(List<PiezaTaller> piezas) {
        NombresDePiezas nombres = new NombresDePiezas();
        piezas.forEach(pieza -> nombres.anotar(pieza.id(), pieza.nombre()));
        return nombres;
    }

    /** Las piezas que pueden venir por id, tipo o nombre (las sumadas en el turno no tienen id). */
    public static NombresDePiezas porCualquierReferencia(List<PiezaTaller> piezas) {
        NombresDePiezas nombres = new NombresDePiezas();
        piezas.forEach(pieza -> {
            nombres.anotar(String.valueOf(pieza.id()), pieza.nombre());
            nombres.anotar(pieza.tipo(), pieza.nombre());
            nombres.anotar(pieza.nombre(), pieza.nombre());
        });
        return nombres;
    }

    /** Las piezas que se suman en el turno se anotan con su nombre, así las acciones siguientes las nombran bien. */
    public void anotar(String referencia, String nombre) {
        nombres.put(referencia, nombre);
    }

    public String de(Object referencia) {
        return nombres.getOrDefault(String.valueOf(referencia), String.valueOf(referencia)).toLowerCase(Locale.ROOT);
    }
}
