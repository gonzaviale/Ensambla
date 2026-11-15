package com.ensambla.backend.domain.model.objetivo;

/** Quién mide un mini-objetivo: el servidor con una métrica, o la persona con su respuesta de la semana. */
public enum ModoMiniObjetivo {
    /** Tiene una métrica del catálogo, una meta y un valor que el servidor calcula solo. */
    MEDIDO(true),
    /** Es una acción sin números: la persona responde si la hizo y qué pasó. */
    MANUAL(false);

    private final boolean usaElCatalogo;

    ModoMiniObjetivo(boolean usaElCatalogo) {
        this.usaElCatalogo = usaElCatalogo;
    }

    /** Si para validarlo hace falta el catálogo de métricas de hoy. */
    public boolean usaElCatalogo() {
        return usaElCatalogo;
    }
}
