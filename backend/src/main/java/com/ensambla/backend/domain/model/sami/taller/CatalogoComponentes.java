package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Los componentes del editor que Sami puede ensamblar. Es único: se genera desde el catálogo del frontend
 * ({@code ia/catalogo-componentes.json}), así lo que el editor sabe hacer y lo que Sami puede pedir no se separan.
 *
 * @param aviso de dónde sale el archivo; no se usa
 */
public record CatalogoComponentes(String aviso, List<ComponenteCatalogo> componentes) {

    /** Tipo de la tabla a medida: la única pieza que se repite y que siempre lleva nombre. */
    public static final String TIPO_A_MEDIDA = "form";

    public CatalogoComponentes {
        componentes = List.copyOf(Objects.requireNonNullElse(componentes, List.of()));
    }

    public Optional<ComponenteCatalogo> buscar(String tipo) {
        return componentes.stream().filter(componente -> componente.tipo().equals(tipo)).findFirst();
    }
}
