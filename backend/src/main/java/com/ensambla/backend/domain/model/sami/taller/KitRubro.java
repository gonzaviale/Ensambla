package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;
import java.util.Objects;

/**
 * Un sistema listo para un rubro (almacén, velas, peluquería…), armado con componentes del editor: qué piezas lleva,
 * con qué datos y ejemplos, cómo se conectan y qué conviene preguntar después. Sami lo usa con {@code usar_kit} y
 * después lo ajusta a la persona; así la IA sólo elige y completa.
 *
 * @param id       lo que pide el modelo en {@code usar_kit}
 * @param terminos palabras que delatan el rubro, en minúscula y sin tildes
 * @param pregunta la pregunta que Sami hace después de armarlo
 */
public record KitRubro(String id, String nombre, List<String> terminos, List<PiezaKit> piezas,
                       List<ConexionKit> conexiones, OpcionesTaller pregunta) {

    public KitRubro {
        terminos = List.copyOf(Objects.requireNonNullElse(terminos, List.of()));
        piezas = List.copyOf(Objects.requireNonNullElse(piezas, List.of()));
        conexiones = List.copyOf(Objects.requireNonNullElse(conexiones, List.of()));
    }

    /**
     * Cómo nombrar en una acción a la pieza del kit que se llama {@code nombre}: una tabla a medida por su nombre y el
     * resto por su tipo, que no se repite (así también encuentra la pieza de ese tipo que el sistema ya tenía).
     */
    public String referenciaDe(String nombre) {
        return piezas.stream()
                .filter(pieza -> pieza.nombre().equals(nombre))
                .findFirst()
                .map(pieza -> CatalogoComponentes.TIPO_A_MEDIDA.equals(pieza.tipo()) ? pieza.nombre() : pieza.tipo())
                .orElse(nombre);
    }
}
