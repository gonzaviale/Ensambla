package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;
import java.util.Objects;

/**
 * Un registro de una pieza tal como lo ve Sami: su id y los primeros datos, como texto y en el orden de los datos de la
 * pieza. Alcanza para reconocerlo y pedir que se cambie o se quite.
 */
public record FilaTaller(String id, List<String> valores) {

    public FilaTaller {
        valores = List.copyOf(Objects.requireNonNullElse(valores, List.of()));
    }
}
