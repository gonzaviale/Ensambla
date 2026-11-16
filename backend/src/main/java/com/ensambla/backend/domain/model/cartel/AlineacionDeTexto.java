package com.ensambla.backend.domain.model.cartel;

/** Hacia dónde se alinea un texto; el valor de {@code ancla} es el que usa el dibujo. */
public enum AlineacionDeTexto {

    IZQUIERDA("start", 0),
    CENTRO("middle", -0.5);

    private final String ancla;
    private final double desplazamiento;

    AlineacionDeTexto(String ancla, double desplazamiento) {
        this.ancla = ancla;
        this.desplazamiento = desplazamiento;
    }

    /** Dónde empieza un texto de ese ancho para quedar alineado en el punto {@code x}. */
    public double inicioDe(double x, double anchoDelTexto) {
        return x + anchoDelTexto * desplazamiento;
    }

    public String ancla() {
        return ancla;
    }

    /** El punto horizontal donde se escribe el texto dentro de una zona. */
    public double puntoDe(Zona zona) {
        return this == CENTRO ? zona.x() + zona.ancho() / 2 : zona.x();
    }
}
