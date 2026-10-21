package com.ensambla.backend.domain.model.ubicacion;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Nombres con los que el mapa reconoce la localidad de un punto, el primero es el principal. */
public record LocalidadesDelPunto(List<String> nombres) {

    public LocalidadesDelPunto {
        nombres = List.copyOf(nombres);
    }

    public Optional<String> principal() {
        return nombres.stream().findFirst();
    }

    /** Sin mayúsculas, tildes ni espacios de más; si el mapa no reconoce ninguna, no hay nada que contradecir. */
    public boolean admite(String localidad) {
        return nombres.isEmpty() || nombres.stream().anyMatch(nombre -> normalizar(nombre).equals(normalizar(localidad)));
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .strip()
                .replaceAll("\\s+", " ");
    }
}
