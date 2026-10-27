package com.ensambla.backend.domain.model.procesamiento;

/** Una tabla del negocio donde se pueden cargar registros, con el nombre del proyecto al que pertenece. */
public record TablaDisponible(BloqueLectura bloque, String proyecto) {

    /** Cómo se la nombra en el chat: proyecto, una flecha y la tabla. */
    public String etiqueta() {
        return proyecto + " › " + bloque.nombre();
    }
}
