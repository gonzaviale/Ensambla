package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;

/**
 * Para comparar campo por campo una entity (ids {@code UUID}) con su modelo (ids tipados): dos ids son iguales si tienen
 * el mismo UUID. Se usa con {@code usingRecursiveComparison().withEqualsForFieldsMatchingRegexes(...)}.
 */
public final class IdsDePrueba {

    /** Los campos de id: {@code id}, {@code negocioId}, {@code bloqueRelacionadoId}... */
    public static final String CAMPOS_DE_ID = "(.*\\.)?(id|\\w+Id)";

    private IdsDePrueba() {
    }

    public static boolean mismoValor(Object uno, Object otro) {
        return Objects.equals(valor(uno), valor(otro));
    }

    private static Object valor(Object id) {
        return id instanceof Identificador tipado ? tipado.valor() : id;
    }
}
