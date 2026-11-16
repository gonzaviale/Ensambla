package com.ensambla.backend.domain.model.cartel;

/** Hacia dónde se alinea un texto; el valor de {@code ancla} es el que usa el dibujo. */
public enum AlineacionDeTexto {

    IZQUIERDA("start"),
    CENTRO("middle");

    private final String ancla;

    AlineacionDeTexto(String ancla) {
        this.ancla = ancla;
    }

    public String ancla() {
        return ancla;
    }

    /** El punto horizontal donde se escribe el texto dentro de una zona. */
    public double puntoDe(Zona zona) {
        return this == CENTRO ? zona.x() + zona.ancho() / 2 : zona.x();
    }
}
