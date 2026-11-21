package com.ensambla.backend.domain.model.aviso;

/**
 * Lo que el aviso le ofrece hacer a la persona. Cada acción sabe si cambia datos y cómo aplicarse; las que solo llevan
 * a una pantalla no cambian nada.
 */
public sealed interface AccionDeAviso permits AccionAbrirRegistro, AccionRegistrar, AccionVerZona,
        AccionEditarRegistro, AccionCrearRegistro, AccionInvalida, AccionDesconocida {

    default boolean modificaDatos() {
        return false;
    }

    default void aplicarEn(AplicadorDeAcciones aplicador) {
    }
}
