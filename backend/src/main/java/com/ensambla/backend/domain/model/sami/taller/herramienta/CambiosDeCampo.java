package com.ensambla.backend.domain.model.sami.taller.herramienta;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Lo que cambia un {@code editar_campo}, en el orden en que viaja; si una parte llegó inválida, no vale nada. */
final class CambiosDeCampo {

    private final Map<String, Object> argumentos = new LinkedHashMap<>();
    private boolean invalido;

    void exigir(boolean valido) {
        invalido |= !valido;
    }

    void anotar(String clave, Object valor) {
        argumentos.put(clave, valor);
    }

    Optional<Map<String, Object>> validos() {
        return invalido || argumentos.isEmpty() ? Optional.empty() : Optional.of(argumentos);
    }
}
